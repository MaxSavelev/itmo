package client;

import common.request.CommandRequest;
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
        
        
        Set<String> executingScripts = new HashSet<>();
        
        Deque<String> scriptPathStack = new ArrayDeque<>();

        System.out.println("Клиент запущен. Сервер: " + host + ":" + port);
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
            if (!executeCommand(line, client, parser, executingScripts, scriptPathStack)) {
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
     * @param executingScripts набор уже выполняющихся скриптов
     * @param scriptPathStack стек вложенных скриптов
     * @return {@code false}, если клиент должен завершиться
     */
    private static boolean executeCommand(String line,
                                          Client client,
                                          ClientCommandParser parser,
                                          Set<String> executingScripts,
                                          Deque<String> scriptPathStack) {
        String[] parts = line.trim().split("\\s+");
        String command = parts[0].toLowerCase(Locale.ROOT);

        if (command.equals("exit")) {
            
            System.out.println("Клиент завершает работу.");
            return false;
        }

        try {
            if (command.equals("execute_script")) {
                
                return executeScript(parts, client, executingScripts, scriptPathStack);
            }

            
            CommandRequest request = parser.parse(line);
            CommandResponse response = client.send(request);
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
     * @param executingScripts набор уже выполняющихся скриптов для защиты от рекурсии
     * @param scriptPathStack стек путей скриптов для понятной ошибки рекурсии
     * @return {@code false}, если внутри скрипта встретился exit
     */
    private static boolean executeScript(String[] parts,
                                         Client client,
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

                
                if (!executeCommand(scriptLine, client, scriptParser, executingScripts, scriptPathStack)) {
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
}
