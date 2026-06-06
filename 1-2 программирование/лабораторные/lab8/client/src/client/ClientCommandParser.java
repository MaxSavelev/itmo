package client;

import common.request.AddIfMaxRequest;
import common.request.AddRequest;
import common.request.ClearRequest;
import common.request.CommandRequest;
import common.request.CountByNumberOfWheelsRequest;
import common.request.FilterByFuelTypeRequest;
import common.request.FilterStartsWithNameRequest;
import common.request.HeadRequest;
import common.request.HelpRequest;
import common.request.InfoRequest;
import common.request.LoginRequest;
import common.request.RemoveByIdRequest;
import common.request.RemoveGreaterRequest;
import common.request.RegisterRequest;
import common.request.ShowRequest;
import common.request.UpdateRequest;
import data.FuelType;
import data.Vehicle;

import java.util.Locale;

/**
 * Превращает строку, введённую пользователем, в объект запроса.
 * <p>
 * Например, строка {@code show} становится {@link ShowRequest},
 * а строка {@code remove_by_id 5} становится {@link RemoveByIdRequest}.
 * </p>
 */
public class ClientCommandParser {
    private final ClientVehicleReader vehicleReader;

    /**
     * Создаёт парсер команд с объектом для ввода Vehicle.
     *
     * @param vehicleReader читатель полей Vehicle для команд add/update
     */
    public ClientCommandParser(ClientVehicleReader vehicleReader) {
        this.vehicleReader = vehicleReader;
    }

    /**
     * Создаёт парсер без чтения Vehicle.
     * <p>
     * Такой вариант полезен только для команд, которым не нужен ввод объекта.
     * </p>
     */
    public ClientCommandParser() {
        this(null);
    }

    /**
     * Разбирает строку команды и создаёт подходящий объект запроса.
     *
     * @param line строка команды из консоли или скрипта
     * @return объект запроса для отправки серверу
     * @throws IllegalArgumentException если команда или её аргументы некорректны
     */
    public CommandRequest parse(String line) {
        String[] parts = line.trim().split("\\s+");
        String command = parts[0].toLowerCase(Locale.ROOT);

        return switch (command) {
            case "register" -> parseRegister(parts);
            case "login" -> parseLogin(parts);
            case "help" -> requireArgumentsCount(parts, new HelpRequest());
            case "info" -> requireArgumentsCount(parts, new InfoRequest());
            case "show" -> requireArgumentsCount(parts, new ShowRequest());
            case "clear" -> requireArgumentsCount(parts, new ClearRequest());
            case "head" -> requireArgumentsCount(parts, new HeadRequest());
            case "remove_by_id" -> new RemoveByIdRequest(parseIntegerArgument(parts, "id"));
            case "count_by_number_of_wheels" -> new CountByNumberOfWheelsRequest(parseIntegerArgument(parts, "numberOfWheels"));
            case "filter_starts_with_name" -> new FilterStartsWithNameRequest(parseStringArgument(parts, "prefix"));
            case "filter_by_fuel_type" -> new FilterByFuelTypeRequest(parseFuelTypeArgument(parts));
            case "add" -> {
                checkArgumentsCount(parts);
                yield new AddRequest(readVehicle());
            }
            case "update" -> new UpdateRequest(parseIntegerArgument(parts, "id"), readVehicle());
            case "add_if_max" -> {
                checkArgumentsCount(parts);
                yield new AddIfMaxRequest(readVehicle());
            }
            case "remove_greater" -> {
                checkArgumentsCount(parts);
                yield new RemoveGreaterRequest(readVehicle());
            }
            default -> throw new IllegalArgumentException("Неизвестная команда: " + command);
        };
    }

    private RegisterRequest parseRegister(String[] parts) {
        checkCredentialsArguments(parts, "register");
        return new RegisterRequest(parts[1], parts[2]);
    }

    private LoginRequest parseLogin(String[] parts) {
        checkCredentialsArguments(parts, "login");
        return new LoginRequest(parts[1], parts[2]);
    }

    private void checkCredentialsArguments(String[] parts, String command) {
        if (parts.length != 3) {
            throw new IllegalArgumentException("Использование: " + command + " {login} {password}");
        }
    }

    private CommandRequest requireArgumentsCount(String[] parts, CommandRequest request) {
        checkArgumentsCount(parts);
        return request;
    }

    /**
     * Проверяет, что команда введена без лишних аргументов.
     *
     * @param parts части команды после split
     */
    private void checkArgumentsCount(String[] parts) {
        if (parts.length != 1) {
            throw new IllegalArgumentException("У этой команды не должно быть аргументов.");
        }
    }

    /**
     * Читает один целочисленный аргумент команды.
     *
     * @param parts части команды
     * @param argumentName название аргумента для сообщения об ошибке
     * @return значение аргумента
     */
    private int parseIntegerArgument(String[] parts, String argumentName) {
        if (parts.length != 2) {
            throw new IllegalArgumentException("Нужно указать один аргумент: " + argumentName);
        }

        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(argumentName + " должен быть целым числом.");
        }
    }

    /**
     * Читает один строковый аргумент команды.
     *
     * @param parts части команды
     * @param argumentName название аргумента для сообщения об ошибке
     * @return строковый аргумент
     */
    private String parseStringArgument(String[] parts, String argumentName) {
        if (parts.length != 2) {
            throw new IllegalArgumentException("Нужно указать один аргумент: " + argumentName);
        }
        return parts[1];
    }

    /**
     * Читает fuelType из аргумента команды.
     * <p>
     * Поддерживает ввод и по номеру, и по названию enum.
     * </p>
     *
     * @param parts части команды
     * @return тип топлива
     */
    private FuelType parseFuelTypeArgument(String[] parts) {
        String value = parseStringArgument(parts, "fuelType");
        FuelType fuelType = ClientFuelTypeParser.parse(value);
        if (fuelType != null) {
            return fuelType;
        }
        throw new IllegalArgumentException("Неизвестный fuelType. Возможные значения: 1-5 или GASOLINE, ELECTRICITY, DIESEL, MANPOWER, NUCLEAR.");
    }

    /**
     * Запрашивает у пользователя поля Vehicle для команд, которым нужен объект.
     *
     * @return введённый объект Vehicle
     */
    private Vehicle readVehicle() {
        if (vehicleReader == null) {
            throw new IllegalArgumentException("Для этой команды нужен ввод Vehicle.");
        }
        return vehicleReader.readVehicle();
    }
}
