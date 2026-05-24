package server;

import data.Vehicle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import parser.VehicleCollectionParser;
import storage.VehicleCollection;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.BindException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Точка входа серверного приложения.
 * <p>
 * Загружает коллекцию из файла, создаёт обработчик команд,
 * запускает сервер и отдельную серверную консоль для команд {@code save} и {@code exit}.
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
        VehicleCollection collection = loadCollection();
        ServerCommandProcessor processor = new ServerCommandProcessor(collection);
        Server server = new Server(port, processor);

        System.out.println("Элементов в коллекции: " + collection.show().size());
        logger.info("Коллекция подготовлена. Элементов: {}", collection.show().size());
        startServerConsole(scanner, server, collection);

        try {
            server.run();
            saveCollection(collection);
        } catch (BindException e) {
            System.out.println("Порт уже занят. Остановите другую программу или запустите сервер на другом порту.");
            logger.error("Порт уже занят", e);
        } catch (IOException e) {
            System.out.println("Не удалось запустить сервер. Проверьте порт и сетевые настройки.");
            logger.error("Не удалось запустить сервер", e);
        }
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
     * Загружает коллекцию из файла, указанного в переменной окружения FILE_NAME.
     * <p>
     * Если файл не найден или данные некорректны, создаётся пустая коллекция,
     * чтобы сервер мог продолжить работу.
     * </p>
     *
     * @return загруженная или пустая коллекция
     */
    private static VehicleCollection loadCollection() {
        String fileName = System.getenv("FILE_NAME");
        if (fileName == null || fileName.isBlank()) {
            System.out.println("Переменная окружения FILE_NAME не задана.");
            System.out.println("Будет создана пустая коллекция.");
            logger.warn("Переменная окружения FILE_NAME не задана. Создаётся пустая коллекция");
            return new VehicleCollection();
        }

        try {
            logger.info("Загрузка коллекции из файла {}", fileName);
            VehicleCollectionParser parser = new VehicleCollectionParser(
                    new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8)
            );
            List<Vehicle> vehicles = parser.parse();
            logger.info("Коллекция загружена из файла {}. Элементов: {}", fileName, vehicles.size());
            return new VehicleCollection(vehicles);
        } catch (FileNotFoundException e) {
            System.out.println("Файл коллекции не найден. Проверьте путь в FILE_NAME.");
            System.out.println("Будет создана пустая коллекция.");
            logger.warn("Файл коллекции не найден: {}. Создаётся пустая коллекция", fileName);
            return new VehicleCollection();
        } catch (IOException e) {
            System.out.println("Не удалось загрузить коллекцию из файла. Проверьте доступ, формат и значения в FILE_NAME.");
            System.out.println("Будет создана пустая коллекция.");
            logger.warn("Не удалось загрузить коллекцию из файла {}. Создаётся пустая коллекция", fileName, e);
            return new VehicleCollection();
        } catch (RuntimeException e) {
            
            
            System.out.println("Не удалось загрузить коллекцию из файла. Проверьте формат и значения в FILE_NAME.");
            System.out.println("Будет создана пустая коллекция.");
            logger.warn("Некорректные данные в файле {}. Создаётся пустая коллекция", fileName, e);
            return new VehicleCollection();
        }
    }

    /**
     * Запускает отдельный поток для серверных команд.
     * <p>
     * Здесь доступны команды {@code save} и {@code exit}; клиент такую команду
     * {@code save} отправить не может.
     * </p>
     *
     * @param scanner сканер консоли сервера
     * @param server сервер, который можно остановить командой exit
     * @param collection коллекция, которую можно сохранить командой save
     */
    private static void startServerConsole(Scanner scanner, Server server, VehicleCollection collection) {
        Thread consoleThread = new Thread(() -> {
            System.out.println("Серверные команды: save, exit.");
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
                    case "save" -> {
                        logger.info("Получена серверная команда save");
                        saveCollection(collection);
                    }
                    case "exit" -> {
                        System.out.println("Сервер завершает работу.");
                        logger.info("Получена серверная команда exit");
                        server.stop();
                        return;
                    }
                    default -> System.out.println("Неизвестная серверная команда. Доступно: save, exit.");
                }
            }
        });

        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    /**
     * Сохраняет коллекцию в файл из переменной окружения FILE_NAME.
     *
     * @param collection коллекция для сохранения
     */
    private static void saveCollection(VehicleCollection collection) {
        String fileName = System.getenv("FILE_NAME");
        if (fileName == null || fileName.isBlank()) {
            System.out.println("Коллекция не сохранена: переменная окружения FILE_NAME не задана.");
            logger.warn("Коллекция не сохранена: FILE_NAME не задана");
            return;
        }

        try {
            Files.writeString(Path.of(fileName), collection.save(), StandardCharsets.UTF_8);
            System.out.println("Коллекция сохранена в файл: " + fileName);
            logger.info("Коллекция сохранена в файл {}", fileName);
        } catch (InvalidPathException e) {
            System.out.println("Коллекция не сохранена: некорректный путь в FILE_NAME.");
            logger.warn("Коллекция не сохранена: некорректный путь {}", fileName, e);
        } catch (IOException e) {
            System.out.println("Коллекция не сохранена: не удалось записать файл.");
            logger.warn("Коллекция не сохранена: не удалось записать файл {}", fileName, e);
        }
    }
}
