package server;

import common.request.AddIfMaxRequest;
import common.request.AddRequest;
import common.request.ClearRequest;
import common.request.CommandRequest;
import common.request.CountByNumberOfWheelsRequest;
import common.request.ExecuteScriptRequest;
import common.request.FilterByFuelTypeRequest;
import common.request.FilterStartsWithNameRequest;
import common.request.HeadRequest;
import common.request.HelpRequest;
import common.request.InfoRequest;
import common.request.RemoveByIdRequest;
import common.request.RemoveGreaterRequest;
import common.request.ShowRequest;
import common.request.UpdateRequest;
import common.response.CommandResponse;
import data.Vehicle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import storage.Storage;
import storage.StorageException;

import java.util.Comparator;

/**
 * Выполняет команды, полученные сервером от клиента.
 * <p>
 * На вход приходит объект {@link CommandRequest}, а на выход возвращается
 * {@link CommandResponse} с результатом выполнения.
 * </p>
 */
public class ServerCommandProcessor {
    private static final Logger logger = LogManager.getLogger(ServerCommandProcessor.class);

    private final Storage collection;

    /**
     * Создаёт обработчик команд для указанного хранилища коллекции.
     *
     * @param collection объект, который управляет коллекцией Vehicle
     */
    public ServerCommandProcessor(Storage collection) {
        this.collection = collection;
    }

    /**
     * Определяет тип запроса и выполняет соответствующую команду.
     *
     * @param request объект команды от клиента
     * @return ответ сервера для клиента
     */
    public CommandResponse process(CommandRequest request) {
        try {
            logger.info("Обработка команды {}", request.getClass().getSimpleName());
            
            if (request instanceof HelpRequest) {
                return success(help(), null);
            }
            if (request instanceof InfoRequest) {
                return success(null, collection.info());
            }
            if (request instanceof ShowRequest) {
                
                return success(null, collection.show().stream()
                        .sorted(Comparator.comparing(Vehicle::getName))
                        .toList());
            }
            if (request instanceof AddRequest addRequest) {
                
                collection.add(createServerVehicle(addRequest.getVehicle()));
                return success("Элемент успешно добавлен.", null);
            }
            if (request instanceof UpdateRequest updateRequest) {
                collection.update(updateRequest.getId(), updateRequest.getVehicle());
                return success("Элемент успешно обновлен.", null);
            }
            if (request instanceof RemoveByIdRequest removeByIdRequest) {
                collection.removeById(removeByIdRequest.getId());
                return success("Элемент успешно удален.", null);
            }
            if (request instanceof ClearRequest) {
                collection.clear();
                return success("Коллекция успешно очищена.", null);
            }
            if (request instanceof HeadRequest) {
                return success(null, collection.head());
            }
            if (request instanceof AddIfMaxRequest addIfMaxRequest) {
                collection.addIfMax(createServerVehicle(addIfMaxRequest.getVehicle()));
                return success("Элемент успешно добавлен.", null);
            }
            if (request instanceof RemoveGreaterRequest removeGreaterRequest) {
                collection.removeGreater(createServerVehicle(removeGreaterRequest.getVehicle()));
                return success("Элементы успешно удалены.", null);
            }
            if (request instanceof CountByNumberOfWheelsRequest countRequest) {
                return success(null, collection.countByNumberOfWheels(countRequest.getNumberOfWheels()));
            }
            if (request instanceof FilterByFuelTypeRequest filterRequest) {
                return success(null, collection.filterByFuelType(filterRequest.getFuelType()));
            }
            if (request instanceof FilterStartsWithNameRequest filterRequest) {
                return success(null, collection.filterStartsWithName(filterRequest.getPrefix()));
            }
            if (request instanceof ExecuteScriptRequest) {
                
                return failure("Команда execute_script пока не реализована на сервере.");
            }
            logger.warn("Неизвестная команда {}", request.getClass().getName());
            return failure("Неизвестная команда.");
        } catch (StorageException | IllegalArgumentException e) {
            logger.warn("Ошибка выполнения команды {}: {}", request.getClass().getSimpleName(), e.getMessage());
            return failure(e.getMessage());
        }
    }

    /**
     * Создаёт успешный ответ сервера.
     *
     * @param message текстовое сообщение для клиента
     * @param result результат команды, если он нужен
     * @return объект ответа
     */
    private CommandResponse success(String message, Object result) {
        return new CommandResponse(true, message, result);
    }

    /**
     * Создаёт ответ об ошибке.
     *
     * @param message текст ошибки
     * @return объект ответа
     */
    private CommandResponse failure(String message) {
        return new CommandResponse(false, message, null);
    }

    /**
     * Возвращает текст справки по доступным клиентским командам.
     *
     * @return строка со списком команд
     */
    private String help() {
        return "help - вывести справку по доступным командам\n"
                + "info - вывести информацию о коллекции\n"
                + "show - вывести все элементы коллекции\n"
                + "add {element} - добавить новый элемент\n"
                + "update id {element} - обновить элемент по id\n"
                + "remove_by_id id - удалить элемент по id\n"
                + "clear - очистить коллекцию\n"
                + "head - вывести первый элемент коллекции\n"
                + "add_if_max {element} - добавить элемент, если он больше максимального\n"
                + "remove_greater {element} - удалить элементы, превышающие заданный\n"
                + "count_by_number_of_wheels numberOfWheels - посчитать элементы с указанным количеством колес\n"
                + "filter_by_fuel_type fuelType - вывести элементы с указанным типом топлива\n"
                + "filter_starts_with_name name - вывести элементы, имя которых начинается с заданной строки\n"
                + "execute_script file_name - выполнить команды из файла на клиенте\n"
                + "exit - завершить клиентское приложение";
    }

    /**
     * Создаёт новый Vehicle на стороне сервера.
     * <p>
     * Это нужно, чтобы автоматически генерируемые поля, например id,
     * назначались сервером, а не приходили готовыми от клиента.
     * </p>
     *
     * @param vehicle объект с данными, введёнными клиентом
     * @return новый серверный объект Vehicle
     */
    private Vehicle createServerVehicle(Vehicle vehicle) {
        
        return new Vehicle(
                vehicle.getName(),
                vehicle.getCoordinates(),
                vehicle.getEnginePower(),
                vehicle.getNumberOfWheels(),
                vehicle.getCapacity(),
                vehicle.getFuelType()
        );
    }
}
