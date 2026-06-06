package server;

import auth.UserService;
import common.request.AddIfMaxRequest;
import common.request.AddRequest;
import common.request.AuthenticatedRequest;
import common.request.ClearRequest;
import common.request.CommandRequest;
import common.request.CountByNumberOfWheelsRequest;
import common.request.ExecuteScriptRequest;
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
import common.response.CommandResponse;
import database.DatabaseException;
import data.Vehicle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import storage.Storage;
import storage.StorageException;
import storage.VehicleRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
    private final UserService userService;
    private final VehicleRepository vehicleRepository;

    /**
     * Создаёт обработчик команд для указанного хранилища коллекции.
     *
     * @param collection объект, который управляет коллекцией Vehicle
     */
    public ServerCommandProcessor(Storage collection) {
        this(collection, null, null);
    }

    /**
     * Создаёт обработчик команд для указанного хранилища и сервиса пользователей.
     *
     * @param collection объект, который управляет коллекцией Vehicle
     * @param userService сервис регистрации и авторизации
     * @param vehicleRepository репозиторий объектов Vehicle в PostgreSQL
     */
    public ServerCommandProcessor(Storage collection, UserService userService, VehicleRepository vehicleRepository) {
        this.collection = collection;
        this.userService = userService;
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Определяет тип запроса и выполняет соответствующую команду.
     *
     * @param request объект команды от клиента
     * @return ответ сервера для клиента
     */
    public CommandResponse process(CommandRequest request) {
        try {
            if (request == null) {
                return failure("Команда не передана.");
            }

            logger.info("Обработка команды {}", request.getClass().getSimpleName());

            if (request instanceof RegisterRequest registerRequest) {
                requireUserService().register(registerRequest.getLogin(), registerRequest.getPassword());
                return success("Пользователь успешно зарегистрирован.", null);
            }
            if (request instanceof LoginRequest loginRequest) {
                requireUserService().login(loginRequest.getLogin(), loginRequest.getPassword());
                return success("Авторизация успешна.", null);
            }

            if (request instanceof AuthenticatedRequest authenticatedRequest) {
                requireUserService().login(authenticatedRequest.getLogin(), authenticatedRequest.getPassword());
                return processAuthorized(authenticatedRequest.getCommand(), authenticatedRequest.getLogin());
            }

            return failure("Сначала авторизуйтесь.");
        } catch (StorageException | IllegalArgumentException | DatabaseException e) {
            logger.warn("Ошибка выполнения команды {}: {}", request.getClass().getSimpleName(), e.getMessage());
            return failure(e.getMessage());
        }
    }

    /**
     * Выполняет команду после успешной авторизации пользователя.
     *
     * @param request команда клиента
     * @return ответ сервера
     */
    private CommandResponse processAuthorized(CommandRequest request, String login) {
        if (request == null) {
            return failure("Команда не передана.");
        }

        if (request instanceof RegisterRequest || request instanceof LoginRequest || request instanceof AuthenticatedRequest) {
            return failure("Некорректный запрос авторизации.");
        }

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
            Vehicle savedVehicle = requireVehicleRepository().add(addRequest.getVehicle(), login);
            collection.add(savedVehicle);
            return success("Элемент успешно добавлен.", null);
        }
        if (request instanceof UpdateRequest updateRequest) {
            Vehicle currentVehicle = collection.getById(updateRequest.getId());
            checkOwner(currentVehicle, login);
            requireVehicleRepository().update(updateRequest.getId(), updateRequest.getVehicle(), login);
            collection.update(updateRequest.getId(), updateRequest.getVehicle());
            return success("Элемент успешно обновлен.", null);
        }
        if (request instanceof RemoveByIdRequest removeByIdRequest) {
            Vehicle currentVehicle = collection.getById(removeByIdRequest.getId());
            checkOwner(currentVehicle, login);
            requireVehicleRepository().removeById(removeByIdRequest.getId(), login);
            collection.removeById(removeByIdRequest.getId());
            return success("Элемент успешно удален.", null);
        }
        if (request instanceof ClearRequest) {
            requireVehicleRepository().clear(login);
            collection.removeByOwner(login);
            return success("Ваши элементы успешно удалены.", null);
        }
        if (request instanceof HeadRequest) {
            return success(null, collection.head());
        }
        if (request instanceof AddIfMaxRequest addIfMaxRequest) {
            Vehicle vehicle = addIfMaxRequest.getVehicle();
            checkIfMax(vehicle);
            Vehicle savedVehicle = requireVehicleRepository().add(vehicle, login);
            collection.add(savedVehicle);
            return success("Элемент успешно добавлен.", null);
        }
        if (request instanceof RemoveGreaterRequest removeGreaterRequest) {
            List<Integer> idsToRemove = getGreaterOwnedIds(removeGreaterRequest.getVehicle(), login);
            requireVehicleRepository().removeByIds(idsToRemove, login);
            collection.removeByIds(idsToRemove);
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

    private UserService requireUserService() {
        if (userService == null) {
            throw new IllegalArgumentException("Сервис пользователей не настроен.");
        }
        return userService;
    }

    private VehicleRepository requireVehicleRepository() {
        if (vehicleRepository == null) {
            throw new IllegalArgumentException("Репозиторий коллекции не настроен.");
        }
        return vehicleRepository;
    }

    private void checkIfMax(Vehicle vehicle) {
        boolean isMax = collection.show().stream()
                .max(Vehicle::compareTo)
                .map(currentMax -> vehicle.compareTo(currentMax) > 0)
                .orElse(true);

        if (!isMax) {
            throw new StorageException("Элемент не добавлен, так как он не превышает максимальный.");
        }
    }

    private void checkOwner(Vehicle vehicle, String login) {
        if (!Objects.equals(vehicle.getOwnerLogin(), login)) {
            throw new StorageException("Вы можете изменять только свои объекты.");
        }
    }

    private List<Integer> getGreaterOwnedIds(Vehicle vehicle, String login) {
        List<Integer> ids = collection.show().stream()
                .filter(currentVehicle -> currentVehicle.compareTo(vehicle) > 0)
                .filter(currentVehicle -> Objects.equals(currentVehicle.getOwnerLogin(), login))
                .map(Vehicle::getId)
                .toList();

        if (ids.isEmpty()) {
            throw new StorageException("Ваши элементы, превышающие заданный, не найдены.");
        }
        return ids;
    }

    /**
     * Возвращает текст справки по доступным клиентским командам.
     *
     * @return строка со списком команд
     */
    private String help() {
        return "register / register {login} / register {login} {password} - зарегистрировать пользователя\n"
                + "login / login {login} / login {login} {password} - авторизоваться\n"
                + "help - вывести справку по доступным командам\n"
                + "info - вывести информацию о коллекции\n"
                + "show - вывести все элементы коллекции\n"
                + "add {element} - добавить новый элемент\n"
                + "update [id] {element} - обновить элемент по id\n"
                + "remove_by_id [id] - удалить элемент по id\n"
                + "clear - очистить коллекцию\n"
                + "head - вывести первый элемент коллекции\n"
                + "add_if_max {element} - добавить элемент, если он больше максимального\n"
                + "remove_greater {element} - удалить элементы, превышающие заданный\n"
                + "count_by_number_of_wheels [numberOfWheels] - посчитать элементы с указанным количеством колес\n"
                + "filter_by_fuel_type [fuelType] - вывести элементы с указанным типом топлива\n"
                + "filter_starts_with_name [name] - вывести элементы, имя которых начинается с заданной строки\n"
                + "execute_script file_name - выполнить команды из файла на клиенте\n"
                + "whoami - показать текущего пользователя\n"
                + "logout - выйти из текущего пользователя\n"
                + "exit - завершить клиентское приложение";
    }

}
