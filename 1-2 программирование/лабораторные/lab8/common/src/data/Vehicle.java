package data;

import java.time.LocalDate;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Класс транспортного средства.
 * <p>
 * Хранит основные данные об объекте:
 * имя, координаты, мощность двигателя, количество колес,
 * вместимость и тип топлива.
 * </p>
 *
 * @author makssavelev
 * @version 1.0
 */
public class Vehicle implements Comparable<Vehicle>, Serializable {
    private static final long serialVersionUID = 1L;

    private final Integer id; 
    private String name; 
    private Coordinates coordinates; 
    private final LocalDate creationDate; 
    private Double enginePower; 
    private int numberOfWheels; 
    private float capacity; 
    private FuelType fuelType; 
    private final String ownerLogin;

    /**
     * Создает транспортное средство.
     * <p>
     * Поля {@code id} и {@code creationDate} заполняются автоматически.
     * </p>
     *
     * @param name имя транспортного средства
     * @param coordinates координаты транспортного средства
     * @param enginePower мощность двигателя
     * @param numberOfWheels количество колес
     * @param capacity вместимость
     * @param fuelType тип топлива
     * @throws IllegalArgumentException если переданы некорректные значения полей
     */
    public Vehicle(String name, Coordinates coordinates, Double enginePower, int numberOfWheels, float capacity, FuelType fuelType) {
        validateName(name);
        validateCoordinates(coordinates);
        validateEnginePower(enginePower);
        validateNumberOfWheels(numberOfWheels);
        validateCapacity(capacity);
        validateFuelType(fuelType);

        id = generateId();
        creationDate = LocalDate.now();
        this.name = name;
        this.coordinates = coordinates;
        this.enginePower = enginePower;
        this.numberOfWheels = numberOfWheels;
        this.capacity = capacity;
        this.fuelType = fuelType;
        this.ownerLogin = null;
    }

    /**
     * Создает транспортное средство с готовыми id, датой создания и владельцем.
     * <p>
     * Такой конструктор нужен серверу, когда объект загружается из базы данных.
     * </p>
     *
     * @param id id объекта из базы данных
     * @param name имя транспортного средства
     * @param coordinates координаты транспортного средства
     * @param creationDate дата создания из базы данных
     * @param enginePower мощность двигателя
     * @param numberOfWheels количество колес
     * @param capacity вместимость
     * @param fuelType тип топлива
     * @param ownerLogin логин владельца объекта
     */
    public Vehicle(Integer id,
                   String name,
                   Coordinates coordinates,
                   LocalDate creationDate,
                   Double enginePower,
                   int numberOfWheels,
                   float capacity,
                   FuelType fuelType,
                   String ownerLogin) {
        validateId(id);
        validateName(name);
        validateCoordinates(coordinates);
        validateCreationDate(creationDate);
        validateEnginePower(enginePower);
        validateNumberOfWheels(numberOfWheels);
        validateCapacity(capacity);
        validateFuelType(fuelType);
        validateOwnerLogin(ownerLogin);

        this.id = id;
        this.creationDate = creationDate;
        this.name = name;
        this.coordinates = coordinates;
        this.enginePower = enginePower;
        this.numberOfWheels = numberOfWheels;
        this.capacity = capacity;
        this.fuelType = fuelType;
        this.ownerLogin = ownerLogin;
        registerId(id);
    }

    private static final Set<Integer> idSet = new HashSet<>();
    private static int lastId = 0;

    /**
     * Генерирует новый уникальный идентификатор.
     *
     * @return новый id
     */
    private static Integer generateId() {
        while (idSet.contains(++lastId)) ;
        registerId(lastId);
        return lastId;
    }

    private static void registerId(Integer id) {
        idSet.add(id);
        if (id > lastId) {
            lastId = id;
        }
    }

    /**
     * Возвращает id объекта.
     *
     * @return идентификатор
     */
    public Integer getId() {
        return id;
    }

    /**
     * Возвращает имя транспортного средства.
     *
     * @return имя объекта
     */
    public String getName() {
        return name;
    }

    /**
     * Возвращает мощность двигателя.
     *
     * @return мощность двигателя
     */
    public Double getEnginePower() {
        return enginePower;
    }

    /**
     * Возвращает количество колес.
     *
     * @return число колес
     */
    public int getNumberOfWheels() {
        return numberOfWheels;
    }

    /**
     * Возвращает тип топлива.
     *
     * @return тип топлива
     */
    public FuelType getFuelType() {
        return fuelType;
    }

    /**
     * Возвращает вместимость.
     *
     * @return значение вместимости
     */
    public float getCapacity() {
        return capacity;
    }

    /**
     * Возвращает координаты объекта.
     *
     * @return координаты
     */
    public Coordinates getCoordinates() {
        return coordinates;
    }

    /**
     * Возвращает дату создания объекта.
     *
     * @return дата создания
     */
    public LocalDate getCreationDate() {
        return creationDate;
    }

    /**
     * Возвращает логин владельца объекта.
     *
     * @return логин владельца или {@code null}, если владелец ещё не назначен
     */
    public String getOwnerLogin() {
        return ownerLogin;
    }

    /**
     * Сравнивает текущее транспортное средство с другим.
     * <p>
     * Сравнение идет по полям по очереди:
     * имя, координаты, мощность двигателя, количество колес,
     * вместимость, тип топлива, дата создания и id.
     * </p>
     *
     * @param o объект, с которым сравнивается текущее транспортное средство
     * @return отрицательное число, если текущий объект меньше;
     *         положительное, если больше;
     *         {@code 0}, если объекты считаются равными
     */
    @Override
    public int compareTo(Vehicle o) {
        if (!name.equals(o.name)) return name.compareTo(o.name);
        if (coordinates.compareTo(o.coordinates)!=0) return coordinates.compareTo(o.coordinates);
        if (!Objects.equals(enginePower,o.enginePower)) {
            if (enginePower == null) return -1;
            if (o.enginePower == null) return 1;
            return Double.compare(enginePower, o.enginePower);
        }
        if (numberOfWheels!=o.numberOfWheels) return Integer.compare(numberOfWheels,o.numberOfWheels);
        if (capacity!=o.capacity) return Float.compare(capacity,o.capacity);
        if (fuelType!=o.fuelType) return fuelType.compareTo(o.fuelType);
        if (!Objects.equals(creationDate,o.creationDate)) return creationDate.compareTo(o.creationDate);
        return id.compareTo(o.id);
    }

    /**
     * Возвращает объект в виде строки.
     *
     * @return строка со значениями полей
     */
    @Override
    public String toString() {
        return "Vehicle{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", coordinates=" + coordinates +
                ", creationDate=" + creationDate +
                ", enginePower=" + enginePower +
                ", numberOfWheels=" + numberOfWheels +
                ", capacity=" + capacity +
                ", fuelType=" + fuelType +
                ", ownerLogin='" + ownerLogin + '\'' +
                '}';
    }

    /**
     * Изменяет имя транспортного средства.
     *
     * @param name новое имя объекта
     */
    public void setName(String name) {
        validateName(name);
        this.name = name;
    }

    /**
     * Изменяет координаты.
     *
     * @param coordinates новые координаты объекта
     */
    public void setCoordinates(Coordinates coordinates) {
        validateCoordinates(coordinates);
        this.coordinates = coordinates;
    }

    /**
     * Изменяет мощность двигателя.
     *
     * @param enginePower новое значение мощности
     */
    public void setEnginePower(Double enginePower) {
        validateEnginePower(enginePower);
        this.enginePower = enginePower;
    }

    /**
     * Изменяет количество колес.
     *
     * @param numberOfWheels новое число колес
     */
    public void setNumberOfWheels(int numberOfWheels) {
        validateNumberOfWheels(numberOfWheels);
        this.numberOfWheels = numberOfWheels;
    }

    /**
     * Изменяет вместимость.
     *
     * @param capacity новое значение вместимости
     */
    public void setCapacity(float capacity) {
        validateCapacity(capacity);
        this.capacity = capacity;
    }

    /**
     * Изменяет тип топлива.
     *
     * @param fuelType новое значение типа топлива
     */
    public void setFuelType(FuelType fuelType) {
        validateFuelType(fuelType);
        this.fuelType = fuelType;
    }

    private static void validateName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Поле name не может быть null, строка не может быть пустой.");
        }
    }

    private static void validateId(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Поле id должно быть больше 0.");
        }
    }

    private static void validateCoordinates(Coordinates coordinates) {
        if (coordinates == null) {
            throw new IllegalArgumentException("Поле coordinates не может быть null.");
        }
    }

    private static void validateCreationDate(LocalDate creationDate) {
        if (creationDate == null) {
            throw new IllegalArgumentException("Поле creationDate не может быть null.");
        }
    }

    private static void validateEnginePower(Double enginePower) {
        if (enginePower != null && enginePower <= 0) {
            throw new IllegalArgumentException("Поле enginePower должно быть null или больше 0.");
        }
    }

    private static void validateNumberOfWheels(int numberOfWheels) {
        if (numberOfWheels <= 0) {
            throw new IllegalArgumentException("Поле numberOfWheels должно быть больше 0.");
        }
    }

    private static void validateCapacity(float capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Поле capacity должно быть больше 0.");
        }
    }

    private static void validateFuelType(FuelType fuelType) {
        if (fuelType == null) {
            throw new IllegalArgumentException("Поле fuelType не может быть null.");
        }
    }

    private static void validateOwnerLogin(String ownerLogin) {
        if (ownerLogin == null || ownerLogin.isBlank()) {
            throw new IllegalArgumentException("Поле ownerLogin не может быть пустым.");
        }
    }
}
