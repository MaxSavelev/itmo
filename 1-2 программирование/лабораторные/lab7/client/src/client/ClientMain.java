package client;

import common.request.AuthenticatedRequest;
import common.request.CommandRequest;
import common.request.LoginRequest;
import common.request.RegisterRequest;
import common.response.CommandResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

/**
 * Точка входа клиентского приложения.
 * <p>
 * Читает команды из консоли, обрабатывает локальные команды
 * {@code exit} и {@code execute_script}, а остальные команды отправляет серверу.
 * </p>
 */
public class ClientMain {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5555;
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    /**
     * Запускает клиент: выбирает host и port, затем начинает читать команды пользователя.
     *
     * @param args аргументы запуска: пусто, port или host port
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String host = parseHost(args);
        int port = parsePort(args, scanner);

        Client client = new Client(host, port);
        ClientCommandParser parser = new ClientCommandParser(new ClientVehicleReader(scanner));
        UserCredentials credentials = new UserCredentials();
        
        
        Set<String> executingScripts = new HashSet<>();
        
        Deque<String> scriptPathStack = new ArrayDeque<>();

        System.out.println("Клиент запущен. Сервер: " + host + ":" + port);
        if (!authenticateAtStart(scanner, client, credentials)) {
            return;
        }

        System.out.println("Введите help для списка команд или exit для выхода.");

        while (true) {
            System.out.print("> ");
            
            if (!scanner.hasNextLine()) {
                break;
            }

            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (!executeCommand(line, client, parser, credentials, executingScripts, scriptPathStack, scanner, true)) {
                break;
            }
        }
    }

    /**
     * Выполняет одну строку команды.
     * <p>
     * Локальные команды обрабатываются сразу на клиенте,
     * а обычные команды превращаются в запрос и отправляются серверу.
     * </p>
     *
     * @param line строка команды
     * @param client объект для сетевого обмена с сервером
     * @param parser парсер команд
     * @param credentials данные текущего пользователя
     * @param executingScripts набор уже выполняющихся скриптов
     * @param scriptPathStack стек вложенных скриптов
     * @param inputScanner источник ввода, если команда требует уточнения
     * @param interactiveInput можно ли задавать уточняющие вопросы пользователю
     * @return {@code false}, если клиент должен завершиться
     */
    private static boolean executeCommand(String line,
                                          Client client,
                                          ClientCommandParser parser,
                                          UserCredentials credentials,
                                          Set<String> executingScripts,
                                          Deque<String> scriptPathStack,
                                          Scanner inputScanner,
                                          boolean interactiveInput) {
        String[] parts = line.trim().split("\\s+");
        String command = parts[0].toLowerCase(Locale.ROOT);

        if (command.equals("exit")) {
            
            System.out.println("Клиент завершает работу.");
            return false;
        }

        if (command.equals("logout")) {
            if (!interactiveInput) {
                System.out.println("Команда logout доступна только в обычной консоли.");
                return true;
            }
            credentials.clear();
            System.out.println("Вы вышли из текущего пользователя.");
            return authenticateAtStart(inputScanner, client, credentials);
        }

        if (command.equals("whoami")) {
            if (parts.length != 1) {
                System.out.println("Использование: whoami");
                return true;
            }
            if (!credentials.isAuthorized()) {
                System.out.println("Вы не авторизованы.");
                return true;
            }
            System.out.println("Текущий пользователь: " + credentials.login);
            return true;
        }

        try {
            if (command.equals("execute_script")) {
                
                return executeScript(parts, client, credentials, executingScripts, scriptPathStack);
            }

            if (interactiveInput) {
                line = fillMissingCommandArgument(command, parts, inputScanner);
                if (line == null) {
                    return false;
                }
                parts = line.trim().split("\\s+");
                command = parts[0].toLowerCase(Locale.ROOT);
            }

            CommandRequest request;
            if (interactiveInput && (command.equals("login") || command.equals("register"))) {
                request = readInteractiveAuthenticationCommand(command, parts, inputScanner);
                if (request == null) {
                    return false;
                }
            } else {
                request = parser.parse(line);
            }

            CommandRequest requestToSend = prepareRequest(request, credentials);
            if (requestToSend == null) {
                System.out.println("Сначала выполните login или register.");
                return true;
            }

            CommandResponse response = client.send(requestToSend);
            updateCredentialsAfterSuccess(request, response, credentials);
            printResponse(response);
        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка ввода: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println("Не удалось найти сервер.");
        } catch (ConnectException e) {
            System.out.println("Сервер временно недоступен. Проверьте, что серверное приложение запущено и порт указан верно.");
        } catch (SocketTimeoutException e) {
            System.out.println("Сервер не ответил за отведённое время. Попробуйте позже.");
        } catch (NoRouteToHostException e) {
            System.out.println("Не удалось проложить маршрут до сервера. Проверьте адрес сервера и подключение к сети.");
        } catch (IOException e) {
            System.out.println("Не удалось обменяться данными с сервером. Проверьте подключение и повторите команду.");
        } catch (ClassNotFoundException e) {
            System.out.println("Клиент получил ответ неизвестного типа. Проверьте, что клиент и сервер собраны из одной версии проекта.");
        }

        return true;
    }

    /**
     * Выполняет команды из файла скрипта.
     * <p>
     * Скрипт выполняется на клиенте, потому что файл находится на стороне пользователя.
     * Каждая строка скрипта обрабатывается как обычная команда.
     * </p>
     *
     * @param parts части команды execute_script
     * @param client объект для отправки запросов серверу
     * @param credentials данные текущего пользователя
     * @param executingScripts набор уже выполняющихся скриптов для защиты от рекурсии
     * @param scriptPathStack стек путей скриптов для понятной ошибки рекурсии
     * @return {@code false}, если внутри скрипта встретился exit
     */
    private static boolean executeScript(String[] parts,
                                         Client client,
                                         UserCredentials credentials,
                                         Set<String> executingScripts,
                                         Deque<String> scriptPathStack) {
        if (parts.length != 2) {
            throw new IllegalArgumentException("Нужно указать ровно один файл скрипта.");
        }

        String fileName = parts[1];
        File scriptFile;
        try {
            scriptFile = new File(fileName).getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException("Не удалось определить путь к файлу скрипта.");
        }

        String scriptPath = scriptFile.getPath();
        if (executingScripts.contains(scriptPath)) {
            
            String parentScript = scriptPathStack.isEmpty() ? "консоль" : scriptPathStack.peek();
            throw new IllegalArgumentException("Рекурсивный вызов скрипта запрещен. Скрипт "
                    + parentScript + " пытается вызвать " + scriptPath);
        }

        executingScripts.add(scriptPath);
        scriptPathStack.push(scriptPath);

        try (Scanner scriptScanner = new Scanner(
                new InputStreamReader(new FileInputStream(scriptFile), StandardCharsets.UTF_8)
        )) {
            System.out.println("Выполняется скрипт: " + scriptPath);
            
            ClientCommandParser scriptParser = new ClientCommandParser(new ClientVehicleReader(scriptScanner, false));

            while (scriptScanner.hasNextLine()) {
                String scriptLine = scriptScanner.nextLine().trim();
                if (scriptLine.isEmpty()) {
                    continue;
                }

                
                if (!executeCommand(scriptLine, client, scriptParser, credentials, executingScripts, scriptPathStack, scriptScanner, false)) {
                    return false;
                }
            }

            System.out.println("Скрипт завершён: " + scriptPath);
            return true;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Недостаточно прав или файла не существует.");
        } finally {
            executingScripts.remove(scriptPath);
            scriptPathStack.pop();
        }
    }

    /**
     * Подготавливает запрос к отправке на сервер.
     * <p>
     * register и login отправляются без обёртки, а остальные команды
     * отправляются вместе с логином и паролем текущего пользователя.
     * </p>
     *
     * @param request исходная команда
     * @param credentials данные текущего пользователя
     * @return запрос для отправки или {@code null}, если пользователь не вошёл
     */
    private static CommandRequest prepareRequest(CommandRequest request, UserCredentials credentials) {
        if (request instanceof RegisterRequest || request instanceof LoginRequest) {
            return request;
        }
        if (!credentials.isAuthorized()) {
            return null;
        }
        return new AuthenticatedRequest(credentials.login, credentials.password, request);
    }

    /**
     * Запоминает пользователя после успешного login.
     *
     * @param request исходная команда
     * @param response ответ сервера
     * @param credentials данные текущего пользователя
     */
    private static void updateCredentialsAfterSuccess(CommandRequest request,
                                                      CommandResponse response,
                                                      UserCredentials credentials) {
        if (!response.isSuccess()) {
            return;
        }
        if (request instanceof LoginRequest loginRequest) {
            credentials.login = loginRequest.getLogin();
            credentials.password = loginRequest.getPassword();
        }
        if (request instanceof RegisterRequest registerRequest) {
            credentials.login = registerRequest.getLogin();
            credentials.password = registerRequest.getPassword();
        }
    }

    /**
     * Просит пользователя войти или зарегистрироваться перед началом работы.
     *
     * @param scanner источник ввода
     * @param client клиент для отправки запроса на сервер
     * @param credentials данные текущего пользователя
     * @return {@code true}, если пользователь успешно вошёл или зарегистрировался
     */
    private static boolean authenticateAtStart(Scanner scanner, Client client, UserCredentials credentials) {
        while (true) {
            System.out.print("Выберите действие (login/register/exit): ");
            if (!scanner.hasNextLine()) {
                return false;
            }

            String action = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (action.equals("exit")) {
                System.out.println("Клиент завершает работу.");
                return false;
            }
            if (!action.equals("login") && !action.equals("register")) {
                System.out.println("Введите login, register или exit.");
                continue;
            }

            String login = readRequiredValue(scanner, "login: ");
            if (login == null) {
                return false;
            }
            String password = readRequiredValue(scanner, "password: ");
            if (password == null) {
                return false;
            }

            CommandRequest request = action.equals("login")
                    ? new LoginRequest(login, password)
                    : new RegisterRequest(login, password);

            if (sendAuthenticationRequest(client, credentials, request)) {
                return true;
            }
        }
    }

    private static String readRequiredValue(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return null;
            }

            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("Значение не может быть пустым.");
        }
    }

    /**
     * Дочитывает один аргумент команды в обычной консоли.
     * <p>
     * Например, пользователь может написать {@code remove_by_id}, а id ввести
     * уже после подсказки. В скриптах этот режим не используется.
     * </p>
     *
     * @param command название команды
     * @param parts части введённой строки
     * @param scanner источник ввода
     * @return исходная или дополненная строка команды, либо {@code null}, если ввод закончился
     */
    private static String fillMissingCommandArgument(String command, String[] parts, Scanner scanner) {
        if (parts.length != 1) {
            return String.join(" ", parts);
        }

        String prompt = switch (command) {
            case "remove_by_id", "update" -> "id: ";
            case "count_by_number_of_wheels" -> "numberOfWheels: ";
            case "filter_starts_with_name" -> "prefix: ";
            case "filter_by_fuel_type" -> {
                System.out.print(ClientFuelTypeParser.valuesWithNumbers());
                yield "fuelType: ";
            }
            default -> null;
        };

        if (prompt == null) {
            return String.join(" ", parts);
        }

        String value = readRequiredValue(scanner, prompt);
        if (value == null) {
            return null;
        }
        return command + " " + value;
    }

    /**
     * Читает login/register в удобном режиме для обычной консоли.
     * <p>
     * Поддерживаются варианты: {@code login}, {@code login user1},
     * {@code login user1 123}. Для скриптов этот режим не используется.
     * </p>
     *
     * @param command команда login или register
     * @param parts части введённой строки
     * @param scanner источник ввода
     * @return запрос авторизации или {@code null}, если ввод закончился
     */
    private static CommandRequest readInteractiveAuthenticationCommand(String command, String[] parts, Scanner scanner) {
        if (parts.length > 3) {
            throw new IllegalArgumentException("Использование: " + command + " {login} {password}");
        }

        String login = parts.length >= 2 ? parts[1] : readRequiredValue(scanner, "login: ");
        if (login == null) {
            return null;
        }

        String password = parts.length == 3 ? parts[2] : readRequiredValue(scanner, "password: ");
        if (password == null) {
            return null;
        }

        if (command.equals("login")) {
            return new LoginRequest(login, password);
        }
        return new RegisterRequest(login, password);
    }

    private static boolean sendAuthenticationRequest(Client client,
                                                     UserCredentials credentials,
                                                     CommandRequest request) {
        try {
            CommandResponse response = client.send(request);
            updateCredentialsAfterSuccess(request, response, credentials);
            printResponse(response);
            return response.isSuccess();
        } catch (UnknownHostException e) {
            System.out.println("Не удалось найти сервер.");
        } catch (ConnectException e) {
            System.out.println("Сервер временно недоступен. Проверьте, что серверное приложение запущено и порт указан верно.");
        } catch (SocketTimeoutException e) {
            System.out.println("Сервер не ответил за отведённое время. Попробуйте позже.");
        } catch (NoRouteToHostException e) {
            System.out.println("Не удалось проложить маршрут до сервера. Проверьте адрес сервера и подключение к сети.");
        } catch (IOException e) {
            System.out.println("Не удалось обменяться данными с сервером. Проверьте подключение и повторите команду.");
        } catch (ClassNotFoundException e) {
            System.out.println("Клиент получил ответ неизвестного типа. Проверьте, что клиент и сервер собраны из одной версии проекта.");
        }
        return false;
    }

    /**
     * Получает host из аргументов запуска.
     *
     * @param args аргументы запуска
     * @return host сервера или значение по умолчанию
     */
    private static String parseHost(String[] args) {
        if (args.length == 2) {
            return args[0];
        }
        return DEFAULT_HOST;
    }

    /**
     * Получает port из аргументов запуска.
     *
     * @param args аргументы запуска
     * @param scanner сканер консоли, если порт нужно спросить заново
     * @return порт сервера
     */
    private static int parsePort(String[] args, Scanner scanner) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }
        if (args.length > 2) {
            System.out.println("Слишком много аргументов.");
            System.out.println("Использование: client.ClientMain [port] или client.ClientMain [host] [port]");
            return askPort(scanner);
        }
        String value = args.length == 1 ? args[0] : args[1];
        return parsePortValue(value, scanner);
    }

    /**
     * Просит пользователя ввести порт вручную.
     *
     * @param scanner сканер консоли
     * @return корректный порт
     */
    private static int askPort(Scanner scanner) {
        return parsePortValue("", scanner);
    }

    /**
     * Преобразует строку в номер порта и проверяет диапазон.
     *
     * @param value исходное значение порта
     * @param scanner сканер консоли для повторного ввода
     * @return корректный порт
     */
    private static int parsePortValue(String value, Scanner scanner) {
        String currentValue = value;

        while (true) {
            try {
                int port = Integer.parseInt(currentValue);
                if (port < MIN_PORT || port > MAX_PORT) {
                    throw new NumberFormatException();
                }
                return port;
            } catch (NumberFormatException e) {
                System.out.println("Порт должен быть числом от " + MIN_PORT + " до " + MAX_PORT + ".");
                System.out.print("Введите порт или нажмите Enter, чтобы использовать порт по умолчанию " + DEFAULT_PORT + ": ");
                if (!scanner.hasNextLine()) {
                    System.out.println("Порт не введён, используется порт по умолчанию: " + DEFAULT_PORT);
                    return DEFAULT_PORT;
                }
                currentValue = scanner.nextLine().trim();
                if (currentValue.isEmpty()) {
                    return DEFAULT_PORT;
                }
            }
        }
    }

    /**
     * Печатает ответ сервера в консоль клиента.
     *
     * @param response ответ сервера
     */
    private static void printResponse(CommandResponse response) {
        if (!response.isSuccess()) {
            System.out.println("Ошибка: " + response.getMessage());
            return;
        }

        if (response.getMessage() != null && !response.getMessage().isBlank()) {
            System.out.println(response.getMessage());
        }

        Object result = response.getResult();
        if (result == null) {
            return;
        }

        if (result instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                System.out.println("Коллекция пуста.");
                return;
            }
            for (Object item : collection) {
                System.out.println(item);
            }
            return;
        }

        System.out.println(result);
    }

    /**
     * Данные пользователя, который прошёл login.
     */
    private static class UserCredentials {
        private String login;
        private String password;

        private boolean isAuthorized() {
            return login != null && password != null;
        }

        private void clear() {
            login = null;
            password = null;
        }
    }
}
