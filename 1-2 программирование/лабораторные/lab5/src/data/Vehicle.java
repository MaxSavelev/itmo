package data;

import java.time.LocalDate;
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
public class Vehicle implements Comparable<Vehicle>, Jsonify{
    private final Integer id; //Поле не может быть null, Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически
    private String name; //Поле не может быть null, Строка не может быть пустой
    private Coordinates coordinates; //Поле не может быть null
    private final LocalDate creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически
    private Double enginePower; //Поле может быть null, Значение поля должно быть больше 0
    private int numberOfWheels; //Значение поля должно быть больше 0
    private float capacity; //Значение поля должно быть больше 0
    private FuelType fuelType; //Поле не может быть null

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
     * @throws AssertionError если переданы некорректные значения полей
     */
    public Vehicle(String name, Coordinates coordinates, Double enginePower, int numberOfWheels, float capacity, FuelType fuelType) {
        assert name != null && !name.isEmpty() : "Поле name не может быть null, Строка не может быть пустой";
        assert coordinates != null : "Поле coordinates не может быть null";
        assert enginePower == null || enginePower > 0 : "Поле enginePower может быть null, а если значение указано, оно должно быть больше 0";
        assert numberOfWheels > 0 : "Значение поля numberOfWheels должно быть больше 0";
        assert capacity > 0 : "Поле capacity может быть null, Значение поля capacity должно быть больше 0";
        assert fuelType != null : "Поле fuelType не может быть null";
        id = generateId();
        creationDate = LocalDate.now();
        this.name = name;
        this.coordinates = coordinates;
        this.enginePower = enginePower;
        this.numberOfWheels = numberOfWheels;
        this.capacity = capacity;
        this.fuelType = fuelType;
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
        idSet.add(lastId);
        return lastId;
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
                '}';
    }

    /**
     * Изменяет имя транспортного средства.
     *
     * @param name новое имя объекта
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Изменяет координаты.
     *
     * @param coordinates новые координаты объекта
     */
    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Изменяет мощность двигателя.
     *
     * @param enginePower новое значение мощности
     */
    public void setEnginePower(Double enginePower) {
        this.enginePower = enginePower;
    }

    /**
     * Изменяет количество колес.
     *
     * @param numberOfWheels новое число колес
     */
    public void setNumberOfWheels(int numberOfWheels) {
        this.numberOfWheels = numberOfWheels;
    }

    /**
     * Изменяет вместимость.
     *
     * @param capacity новое значение вместимости
     */
    public void setCapacity(float capacity) {
        this.capacity = capacity;
    }

    /**
     * Изменяет тип топлива.
     *
     * @param fuelType новое значение типа топлива
     */
    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    /**
     * Возвращает объект транспортного средства в формате JSON.
     *
     * @return строка JSON с данными транспортного средства
     */
    @Override
    public String toJson() {
        return "{ " +
                "\"name\": \""+ name +"\", \n" +
                "\"coordinates\": "+ coordinates.toJson() +", \n" +
                "\"enginePower\": " + enginePower +", \n" +
                "\"numberOfWheels\": "+ numberOfWheels +", \n" +
                "\"capacity\": "+ capacity +", \n" +
                "\"fuelType\": "+ fuelType.toJson() +"\n" +
                "}";
    }
}
