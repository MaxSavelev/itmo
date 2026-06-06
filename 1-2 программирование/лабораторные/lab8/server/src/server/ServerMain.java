package server;

import auth.UserRepository;
import auth.UserService;
import database.DatabaseConfig;
import database.DatabaseException;
import database.DatabaseManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import data.Vehicle;
import storage.VehicleCollection;
import storage.VehicleRepository;

import java.io.IOException;
import java.net.BindException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Точка входа серверного приложения.
 * <p>
 * Загружает коллекцию из PostgreSQL, создаёт обработчик команд,
 * запускает сервер и отдельную серверную консоль для команды {@code exit}.
 * </p>
 */
public class ServerMain {
    private static final Logger logger = LogManager.getLogger(ServerMain.class);

    private static final int DEFAULT_PORT = 5555;
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;

    /**
     * Запускает серверное приложение.
     *
     * @param args аргументы запуска: необязательный порт сервера
     */
    public static void main(String[] args) {
        logger.info("Запуск серверного приложения");
        Scanner scanner = new Scanner(System.in);
        int port = parsePort(args, scanner);

        DatabaseManager databaseManager = prepareDatabase();
        if (databaseManager == null) {
            return;
        }

        VehicleCollection collection = loadCollection(databaseManager);
        if (collection == null) {
            return;
        }

        UserService userService = new UserService(new UserRepository(databaseManager));
        VehicleRepository vehicleRepository = new VehicleRepository(databaseManager);
        ServerCommandProcessor processor = new ServerCommandProcessor(collection, userService, vehicleRepository);
        Server server = new Server(port, processor);

        System.out.println("Элементов в коллекции: " + collection.show().size());
        logger.info("Коллекция подготовлена. Элементов: {}", collection.show().size());
        startServerConsole(scanner, server);

        try {
            server.run();
        } catch (BindException e) {
            System.out.println("Порт уже занят. Остановите другую программу или запустите сервер на другом порту.");
            logger.error("Порт уже занят", e);
        } catch (IOException e) {
            System.out.println("Не удалось запустить сервер. Проверьте порт и сетевые настройки.");
            logger.error("Не удалось запустить сервер", e);
        }
    }

    /**
     * Проверяет подключение к PostgreSQL и создаёт таблицы lab7.
     *
     * @return объект для работы с базой данных или {@code null}, если БД недоступна
     */
    private static DatabaseManager prepareDatabase() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment();
        DatabaseManager databaseManager = new DatabaseManager(config);

        try {
            System.out.println("Подключение к PostgreSQL: " + databaseManager.getConnectionInfo());
            databaseManager.initialize();
            System.out.println("База данных подготовлена.");
            logger.info("База данных подготовлена: {}", databaseManager.getConnectionInfo());
            return databaseManager;
        } catch (DatabaseException e) {
            System.out.println("Не удалось подготовить базу данных.");
            System.out.println("Причина: " + getRootCauseMessage(e));
            System.out.println("Проверьте DB_USER и DB_PASSWORD, а также доступность PostgreSQL.");
            logger.error("Не удалось подготовить базу данных", e);
            return null;
        }
    }

    /**
     * Возвращает самое нижнее сообщение ошибки.
     *
     * @param exception ошибка
     * @return понятная причина ошибки
     */
    private static String getRootCauseMessage(Throwable exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }

    /**
     * Получает порт сервера из аргументов запуска.
     *
     * @param args аргументы запуска
     * @param scanner сканер консоли для повторного ввода порта
     * @return корректный порт сервера
     */
    private static int parsePort(String[] args, Scanner scanner) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }
        if (args.length > 1) {
            System.out.println("Слишком много аргументов.");
            System.out.println("Использование: server.ServerMain [port]");
            return askPort(scanner);
        }
        return parsePortValue(args[0], scanner);
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
     * Преобразует строку в порт и проверяет допустимый диапазон.
     *
     * @param value исходное значение
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
     * Загружает коллекцию из PostgreSQL.
     * <p>
     * Все команды получения данных дальше работают уже с этой коллекцией в памяти.
     * </p>
     *
     * @param databaseManager объект для подключения к базе данных
     * @return коллекция в памяти или {@code null}, если загрузка не удалась
     */
    private static VehicleCollection loadCollection(DatabaseManager databaseManager) {
        try {
            VehicleRepository vehicleRepository = new VehicleRepository(databaseManager);
            List<Vehicle> vehicles = vehicleRepository.loadAll();
            logger.info("Коллекция загружена из PostgreSQL. Элементов: {}", vehicles.size());
            return new VehicleCollection(vehicles);
        } catch (DatabaseException e) {
            System.out.println("Не удалось загрузить коллекцию из PostgreSQL.");
            System.out.println("Причина: " + getRootCauseMessage(e));
            logger.error("Не удалось загрузить коллекцию из PostgreSQL", e);
            return null;
        }
    }

    /**
     * Запускает отдельный поток для серверных команд.
     * <p>
     * В lab7 коллекция хранится в PostgreSQL, поэтому серверная команда
     * {@code save} больше не нужна.
     * </p>
     *
     * @param scanner сканер консоли сервера
     * @param server сервер, который можно остановить командой exit
     */
    private static void startServerConsole(Scanner scanner, Server server) {
        Thread consoleThread = new Thread(() -> {
            System.out.println("Серверные команды: exit.");
            logger.info("Серверная консоль запущена");

            while (true) {
                if (!scanner.hasNextLine()) {
                    logger.info("Ввод серверной консоли завершён");
                    return;
                }

                String command = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
                if (command.isEmpty()) {
                    continue;
                }

                switch (command) {
                    case "exit" -> {
                        System.out.println("Сервер завершает работу.");
                        logger.info("Получена серверная команда exit");
                        server.stop();
                        return;
                    }
                    default -> System.out.println("Неизвестная серверная команда. Доступно: exit.");
                }
            }
        });

        consoleThread.setDaemon(true);
        consoleThread.start();
    }
}
