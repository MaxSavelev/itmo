package cmd;

import data.Coordinates;
import data.FuelType;
import data.Vehicle;
import storage.Storage;
import storage.StorageException;
import storage.VehicleCollection;

import java.io.*;
import java.util.*;

/**
 * Класс командной строки.
 * <p>
 * Отвечает за ввод команд пользователя
 * и за работу с коллекцией транспортных средств.
 * </p>
 *
 * @author makssavelev
 * @version 1.0
 */
public class CommandLine implements Runnable {
    private static Scanner scanner = new Scanner(System.in);
    private static PrintStream out = System.out;
    private static String collectionFileName = System.getenv("FILE_NAME");
    private static Storage collection = new VehicleCollection();
    private static Set<String> executingScripts = new HashSet<>();
    private static Stack<Scanner> scannerStack = new Stack<>();
    private static Stack<PrintStream> outStack = new Stack<>();
    private static Stack<String> scriptPathStack = new Stack<>();
    private static List<Command> cmds = new ArrayList<>() {
        {
            add(new Command("help",
                    "вывести справку по доступным командам",
                    List.of(),
                    args -> cmds.forEach(c -> System.out.println(c.name() + " " + String.join(" ", c.argNames()) + "- " + c.description())),
                    null));

            add(new Command("info",
                    "вывести в стандартный поток вывода информацию о коллекции (тип, дата инициализации, количество элементов и т.д.)",
                    List.of(),
                    args -> System.out.println(collection.info()),
                    null));

            add(new Command("show", "вывести в стандартный поток вывода все элементы коллекции в строковом представлении", List.of(), args -> collection.show().forEach(System.out::println), null));//для каждого эл выполним принтдн

            add(new Command("add", "добавить новый элемент в коллекцию", List.of("{element}"), args -> collection.add(inputVehicle()), "Элемент успешно добавлен."));

            add(new Command("update", "обновить значение элемента коллекции, id которого равен заданному", List.of("id", "{element}"), CommandLine::update, "Элемент успешно обновлен."));

            add(new Command("remove_by_id", "удалить элемент из коллекции по его id", List.of("id"), CommandLine::removeById, "Элемент успешно удален."));

            add(new Command("clear", "очистить коллекцию", List.of(), args -> collection.clear(), "Коллекция успешно очищена."));

            add(new Command("save", "сохранить коллекцию в файл", List.of(), args -> save(), null));

            add(new Command("execute_script", "считать и исполнить скрипт из указанного файла. В скрипте содержатся команды в таком же виде, в котором их вводит пользователь в интерактивном режиме.", List.of(), CommandLine::executeScript, "Скрипт успешно запущен."));

            add(new Command("exit", "завершить программу (без сохранения в файл)", List.of(), args -> {
                System.out.println("Завершение программы...");
                System.exit(0);
            }, null));

            add(new Command("head", "вывести первый элемент коллекции", List.of(), args -> System.out.println(collection.head()), null));

            add(new Command("add_if_max", "добавить новый элемент в коллекцию, если его значение превышает значение наибольшего элемента этой коллекции", List.of("{element}"), args -> collection.addIfMax(inputVehicle()), "Элемент успешно добавлен."));

            add(new Command("remove_greater", "удалить из коллекции все элементы, превышающие заданный", List.of("{element}"), args -> collection.removeGreater(inputVehicle()), "Элементы успешно удалены."));

            add(new Command("count_by_number_of_wheels", "вывести количество элементов, значение поля numberOfWheels которых равно заданному", List.of("numberOfWheels"), CommandLine::countByNumberOfWheels, null));

            add(new Command("filter_by_fuel_type", "вывести элементы, значение поля fuelType которых равно заданному", List.of("fuelType"), CommandLine::filterByFuelType, null));

            add(new Command("filter_starts_with_name", "вывести элементы, значение поля name которых начинается с заданной подстроки", List.of("name"), CommandLine::filterStartsWithName, null));
        }
    };
    private static final Map<String, Command> cmdMap = new HashMap<>() {{
        for (Command c : cmds) put(c.name(), c);
    }}; // command - name

    /**
     * Создает командную строку с пустой коллекцией.
     */
    public CommandLine() {
        collection = new VehicleCollection();
    }

    /**
     * Создает командную строку для готовой коллекции.
     *
     * @param collection коллекция, с которой будет работать программа
     */
    public CommandLine(Storage collection) {
        CommandLine.collection = collection;
    }

    // Vehicle(String name, Coordinates coordinates, Double enginePower, int numberOfWheels, float capacity, FuelType fuelType)

    /**
     * Считывает с ввода все поля транспортного средства.
     *
     * @return созданный объект транспортного средства
     */
    private static Vehicle inputVehicle() {
        return inputVehicle(null);
    }

    /**
     * Считывает данные транспортного средства.
     * <p>
     * Если передан текущий объект, можно нажать Enter
     * и оставить старое значение поля.
     * </p>
     *
     * @param currentVehicle текущий объект для update; нужен, чтобы можно было оставить старые значения
     * @return объект с введенными данными
     */
    private static Vehicle inputVehicle(Vehicle currentVehicle) {
        boolean allowSkip = currentVehicle != null;
        String name = inputName(allowSkip, allowSkip ? currentVehicle.getName() : null);
        Long coordinatesX = inputCoordinatesX(allowSkip, allowSkip ? currentVehicle.getCoordinates().getX() : null);
        double coordinatesY = inputCoordinatesY(allowSkip, allowSkip ? currentVehicle.getCoordinates().getY() : null);
        Double enginePower = inputEnginePower(allowSkip, allowSkip ? currentVehicle.getEnginePower() : null);
        int numberOfWheels = inputWheels(allowSkip, allowSkip ? currentVehicle.getNumberOfWheels() : 0);
        float capacity = inputCapacity(allowSkip, allowSkip ? currentVehicle.getCapacity() : 0);
        FuelType fuelType = inputFuelType(allowSkip, allowSkip ? currentVehicle.getFuelType() : null);
        return new Vehicle(name, new Coordinates(coordinatesX, coordinatesY), enginePower, numberOfWheels, capacity, fuelType);
    }

    /**
     * Проверяет, идет ли обычный ввод с консоли.
     *
     * @return {@code true}, если команда вводится вручную, иначе {@code false}
     */
    private static boolean isInteractiveInput() {
        return scriptPathStack.empty();
    }

    /**
     * Обрабатывает ошибку ввода.
     * <p>
     * В обычном режиме просто показывает сообщение.
     * В режиме скрипта завершает выполнение с ошибкой.
     * </p>
     *
     * @param message текст ошибки для пользователя
     */
    private static void handleInputError(String message) {
        if (!isInteractiveInput()) {
            throw new StorageException(message);
        }
        System.out.println("Произошла ошибка: " + message);
    }

    /**
     * Считывает координату X.
     *
     * @param allowSkip можно ли пропустить ввод и оставить старое значение
     * @param currentValue текущее значение X, которое сохранится при пустом вводе
     * @return корректное значение X
     */
    private static Long inputCoordinatesX(boolean allowSkip, Long currentValue) {
        while (true) {
            out.print("Введите coordinates:\nx: ");
            String value = scanner.nextLine();
            if (allowSkip && value.isEmpty()) {
                return currentValue;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException e) {
                handleInputError("Поле coordinates.x введено неверно. Вводить можно только целое число типа long");
            }
        }
    }

    /**
     * Считывает координату Y.
     *
     * @param allowSkip можно ли пропустить ввод и оставить старое значение
     * @param currentValue текущее значение Y, которое сохранится при пустом вводе
     * @return корректное значение Y
     */
    private static double inputCoordinatesY(boolean allowSkip, Double currentValue) {
        while (true) {
            out.print("y: ");
            String value = scanner.nextLine();
            if (allowSkip && value.isEmpty()) {
                return currentValue;
            }
            try {
                double coordinatesY = parseDouble(value, "coordinates.y", "Поле coordinates.y введено неверно. Вводить можно только число больше -372");
                if (coordinatesY <= -372) {
                    throw new StorageException("Поле coordinates.y введено неверно. Вводить можно только число больше -372");
                }
                return coordinatesY;
            } catch (StorageException e) {
                handleInputError(e.getMessage());
            }
        }
    }

    /**
     * Преобразует строку в число типа {@code double}.
     *
     * @param value строка, которую ввел пользователь
     * @param fieldName имя поля для сообщения об ошибке
     * @param errorMessage текст ошибки при неверном вводе
     * @return число типа {@code double}
     */
    private static double parseDouble(String value, String fieldName, String errorMessage) {
        String preparedValue = value.trim();
        if (!preparedValue.matches("-?\\d+([.,]\\d+)?")) {
            throw new StorageException(errorMessage);
        }
        int separatorIndex = Math.max(preparedValue.indexOf(','), preparedValue.indexOf('.'));
        if (separatorIndex != -1 && preparedValue.length() - separatorIndex - 1 > 15) {
            throw new StorageException("Поле " + fieldName + " введено неверно. Слишком много знаков после запятой");
        }
        return Double.parseDouble(preparedValue.replace(',', '.'));
    }

    /**
     * Считывает мощность двигателя.
     *
     * @param allowSkip можно ли пропустить ввод и оставить старое значение
     * @param currentValue текущее значение мощности, которое сохранится при пустом вводе
     * @return корректное значение мощности или {@code null}
     */
    private static Double inputEnginePower(boolean allowSkip, Double currentValue) {
        while (true) {
            out.print("Введите enginePower (если вы хотите ввести null напишите 0): ");
            String value = scanner.nextLine();
            if (allowSkip && value.isEmpty()) {
                return currentValue;
            }
            try {
                double enginePower = parseDouble(value, "enginePower", "Поле enginePower введено неверно. Вводить можно число типа double больше 0 или 0 для значения null");
                if (enginePower < 0) {
                    throw new StorageException("Поле enginePower введено неверно. Вводить можно число больше 0 или 0 для значения null");
                }
                if (enginePower == 0) {
                    return null;
                }
                return enginePower;
            } catch (StorageException e) {
                handleInputError(e.getMessage());
            }
        }
    }


    /**
     * Считывает количество колес.
     *
     * @return количество колес
     */
    private static int inputWheels() {
        return inputWheels(false, 0);
    }

    /**
     * Считывает количество колес.
     *
     * @param allowSkip можно ли пропустить ввод и оставить старое значение
     * @param currentValue текущее количество колес, которое сохранится при пустом вводе
     * @return корректное количество колес
     */
    private static int inputWheels(boolean allowSkip, int currentValue) {
        while (true) {
            out.print("Введите numberOfWheels: ");
            String value = scanner.nextLine();
            if (allowSkip && value.isEmpty()) {
                return currentValue;
            }
            try {
                int numberOfWheels = Integer.parseInt(value.trim());
                if (numberOfWheels <= 0) {
                    throw new StorageException("Поле numberOfWheels введено неверно. Вводить можно только целое число больше 0");
                }
                return numberOfWheels;
            } catch (NumberFormatException e) {
                handleInputError("Поле numberOfWheels введено неверно. Вводить можно только целое число больше 0");
            } catch (StorageException e) {
                handleInputError(e.getMessage());
            }
        }
    }

    /**
     * Считывает вместимость.
     *
     * @return вместимость транспортного средства
     */
    private static float inputCapacity() {
        return inputCapacity(false, 0);
    }

    /**
     * Считывает вместимость.
     *
     * @param allowSkip можно ли пропустить ввод и оставить старое значение
     * @param currentValue текущее значение вместимости, которое сохранится при пустом вводе
     * @return корректное значение вместимости
     */
    private static float inputCapacity(boolean allowSkip, float currentValue) {
        while (true) {
            out.print("Введите capacity: ");
            String value = scanner.nextLine();
            if (allowSkip && value.isEmpty()) {
                return currentValue;
            }
            try {
                float capacity = (float) parseDouble(value, "capacity", "Поле capacity введено неверно. Вводить можно только число больше 0");
                if (capacity <= 0) {
                    throw new StorageException("Поле capacity введено неверно. Вводить можно только число больше 0");
                }
                return capacity;
            } catch (StorageException e) {
                handleInputError(e.getMessage());
            }
        }
    }

    /**
     * Считывает имя транспортного средства.
     *
     * @return введенное имя
     */
    private static String inputName() {
        return inputName(false, null);
    }

    /**
     * Считывает имя транспортного средства.
     *
     * @param allowSkip можно ли пропустить ввод и оставить старое значение
     * @param currentValue текущее имя, которое сохранится при пустом вводе
     * @return корректное имя транспортного средства
     */
    private static String inputName(boolean allowSkip, String currentValue) {
        while (true) {
            out.print("Введите name: ");
            String name = scanner.nextLine();
            if (allowSkip && name.isEmpty()) {
                return currentValue;
            }
            if (!name.isBlank()) {
                return name;
            }
            handleInputError("Значение поля name не должно быть только из пробельных символов");
        }
    }

    /**
     * Считывает тип топлива.
     *
     * @return выбранный тип топлива
     */
    private static FuelType inputFuelType() {
        return inputFuelType(false, null);
    }

    /**
     * Считывает тип топлива.
     *
     * @param allowSkip можно ли пропустить ввод и оставить старое значение
     * @param currentValue текущее значение fuelType, которое сохранится при пустом вводе
     * @return корректный тип топлива
     */
    private static FuelType inputFuelType(boolean allowSkip, FuelType currentValue) {
        while (true) {
            for (FuelType fuelType : FuelType.values()) {
                out.printf("%d - %s\n", fuelType.ordinal() + 1, fuelType);
            }
            out.print("Введите fuelType: ");
            String fuelTypeString = scanner.nextLine();
            if (allowSkip && fuelTypeString.isEmpty()) {
                return currentValue;
            }
            FuelType fuelType = parseFuelType(fuelTypeString);
            if (fuelType != null) {
                return fuelType;
            }
            handleInputError("Поле fuelType введено неверно. Введите номер из списка или название типа топлива");
        }
    }

    /**
     * Обновляет элемент коллекции по id.
     *
     * @param args список аргументов команды; в первом элементе должен быть id
     * @throws NumberFormatException если id нельзя преобразовать в число
     * @throws StorageException если элемент с таким id не найден
     */
    private static void update(List<String> args) {
        if (args.size() != 1) {
            throw new StorageException("Количество указанных id должно быть равно 1");
        }
        int id = Integer.parseInt(args.get(0));
        Vehicle currentVehicle = collection.getById(id);
        if (isInteractiveInput()) {
            System.out.println("Нажмите Enter, если не хотите изменять поле.");
        }
        Vehicle input = inputVehicle(currentVehicle);
        collection.update(id, input);

    }

    /**
     * Удаляет элемент коллекции по id.
     *
     * @param args список аргументов команды; в первом элементе должен быть id
     * @throws NumberFormatException если id нельзя преобразовать в число
     * @throws StorageException если элемент с таким id не найден
     */
    private static void removeById(List<String> args) {
        if (args.size() != 1) {
            throw new StorageException("Количество указанных id должно быть равно 1");
        }
        int id = Integer.parseInt(args.get(0));
        collection.removeById(id);
    }

    /**
     * Переключает ввод на выполнение команд из файла.
     *
     * @param args список аргументов команды; в первом элементе должно быть имя файла
     */
    private static void executeScript(List<String> args) {
        if (args.size() != 1) {
            throw new StorageException("Нужно указать ровно один файл скрипта");
        }
        String fileName = args.get(0);
        try {
            String scriptPath = new File(fileName).getCanonicalPath();
            if (executingScripts.contains(scriptPath)) {
                String parentScript = scriptPathStack.empty() ? "консоль" : scriptPathStack.peek();
                throw new StorageException("Рекурсивный вызов скрипта запрещен. Скрипт " + parentScript + " пытается вызвать " + scriptPath);
            }
            Scanner newScanner = new Scanner(new InputStreamReader(new FileInputStream(fileName)));
            scannerStack.push(scanner);
            outStack.push(out);
            scriptPathStack.push(scriptPath);
            executingScripts.add(scriptPath);
            scanner = newScanner;
            out = new PrintStream(OutputStream.nullOutputStream());
        } catch (FileNotFoundException e) {
            throw new StorageException("Недостаточно прав или файла не существует");
        } catch (IOException e) {
            throw new StorageException("Не удалось определить путь к файлу скрипта");
        }
    }

    /**
     * Считывает непустую строку для поиска по имени.
     *
     * @return непустая строка
     */
    private static String inputNonEmptyString() {
        out.print("Введите начало имени: ");
        String value = scanner.nextLine().trim();
        if (!value.isEmpty()) {
            return value;
        }
        throw new StorageException("Строка для поиска не может быть пустой");
    }
    /**
     * Считает, сколько элементов имеют указанное число колес.
     *
     * @param args список аргументов команды; в первом элементе должно быть число колес
     */
    private static void countByNumberOfWheels(List<String> args) {
        if (args.size() != 1) {
            throw new StorageException("Нужно указать ровно одно значение numberOfWheels");
        }

        int numberOfWheels;
        try {
            numberOfWheels = Integer.parseInt(args.get(0));
        } catch (NumberFormatException e) {
            throw new StorageException("numberOfWheels должно быть целым числом");
        }

        if (numberOfWheels <= 0) {
            throw new StorageException("numberOfWheels должно быть больше 0");
        }

        if (collection.countByNumberOfWheels(numberOfWheels)!=0) {
            System.out.println(collection.countByNumberOfWheels(numberOfWheels));
        } else {
            System.out.println("Элементов, значение поля numberOfWheels которых равно заданному, не найдены");
        }
    }


    /**
     * Выводит элементы, имя которых начинается с указанной строки.
     *
     * @param args список аргументов команды; может содержать начало имени
     */
    private static void filterStartsWithName(List<String> args) {
        String prefix;
        if (args.isEmpty()) {
            prefix = inputNonEmptyString();
        } else {
            prefix = String.join(" ", args).trim();
            if (prefix.isEmpty()) {
                System.out.println("Строка для поиска не может быть пустой. Попробуйте еще раз.");
                prefix = inputNonEmptyString();
            }
        }
        if (!collection.filterStartsWithName(prefix).isEmpty()) {
            System.out.println(collection.filterStartsWithName(prefix));
        } else {
            System.out.println("Элементы, значение поля name которых начинается с заданной подстроки, не найдены");
        }
    }

    /**
     * Преобразует строку в тип топлива.
     *
     * @param raw строка, введенная пользователем
     * @return найденный тип топлива или {@code null}, если строка некорректна
     */
    private static FuelType parseFuelType(String raw) {
        String value = raw.trim();
        try {
            int index = Integer.parseInt(value);
            if (index > 0 && index <= FuelType.values().length) {
                return FuelType.values()[index - 1];
            }
        } catch (NumberFormatException ignored) {
        }
        for (FuelType fuelType : FuelType.values()) {
            if (fuelType.toString().equals(value)) {
                return fuelType;
            }
        }
        return null;
    }

    /**
     * Выводит элементы с указанным типом топлива.
     *
     * @param args список аргументов команды; может содержать тип топлива
     */
    private static void filterByFuelType(List<String> args) {
        if (args.isEmpty()) {
            System.out.println(collection.filterByFuelType(inputFuelType()));
            return;
        }
        FuelType fuelType = parseFuelType(args.get(0));
        if (fuelType == null) {
            System.out.println("Значение fuelType заполнено неверно. Попробуйте еще раз.");
            fuelType = inputFuelType();
        }
        System.out.println(collection.filterByFuelType(fuelType));
    }


    /**
     * Сохраняет коллекцию в файл.
     */
    private static void save() {
        if (collectionFileName == null || collectionFileName.isBlank()) {
            System.out.println("Error: Переменная окружения FILE_NAME не задана.");
            return;
        }
        try (PrintWriter writer = new PrintWriter(new File(collectionFileName))) {
            writer.println(collection.save());
            System.out.println("Successfully wrote string to " + collectionFileName);
        } catch (FileNotFoundException e) {
            System.out.println("Error: The file " + collectionFileName + " could not be created or found.");
        }
    }


    /**
     * Запускает основной цикл обработки команд.
     * <p>
     * Метод читает команду, ищет ее в списке команд
     * и выполняет нужное действие.
     * </p>
     */
    @Override
    public void run() {
        //execute_script ./scripts/FirstFile
        while (true) {
            out.print("> ");
            if (!scanner.hasNextLine()) {
                if (!scriptPathStack.empty()) {
                    executingScripts.remove(scriptPathStack.pop());
                }
                if (!scannerStack.empty()) {
                    scanner = scannerStack.pop();
                    out = outStack.pop();
                } else {
                    setSystemStream();
                }
                continue;
            }
            String line = scanner.nextLine();
            if (line.isEmpty()) continue;//проверяет пустая ли
            Scanner lineScanner = new Scanner(line);
            String command = lineScanner.next();
            List<String> args = new ArrayList<>();
            while (lineScanner.hasNext()) {
                args.add(lineScanner.next());
            }
            if (cmdMap.containsKey(command)) { // проверка ключа
                try {
                    //cmdMap.get(command).exec().accept(args);
                    Command currentCommand = cmdMap.get(command);
                    currentCommand.exec().accept(args);
                    if (currentCommand.successMessage() != null && !currentCommand.successMessage().isBlank()) {
                        System.out.println(currentCommand.successMessage());
                    }
                } catch (StorageException e) {
                    System.out.println("Произошла ошибка: " + e.getMessage());
                } catch (NoSuchElementException e) {
                    setSystemStream();
                } catch (IllegalArgumentException e) {
                    System.out.println("Неверный формат аргументов команды");
                }

            } else System.out.println("Такой команды нет!");
        }
    }

    /**
     * Возвращает обычные потоки ввода и вывода после выполнения скрипта.
     */
    private static void setSystemStream() {
        scanner = new Scanner(System.in);
        out = System.out;
    }
}
