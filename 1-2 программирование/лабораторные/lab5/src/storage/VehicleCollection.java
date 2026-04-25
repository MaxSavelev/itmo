package storage;

import java.util.*;
import java.util.stream.Collectors;

import data.FuelType;
import data.Vehicle;

import java.time.LocalDate;

/**
 * Класс для хранения коллекции объектов {@link Vehicle}.
 * <p>
 * Реализует основные операции с коллекцией:
 * добавление, обновление, удаление, поиск и сохранение.
 * </p>
 *
 * @author makssavelev
 * @version 1.0
 */
public class VehicleCollection implements Storage {
    private final LocalDate creationDate;
    private final List<Vehicle> list;

    /**
     * Создает пустую коллекцию.
     */
    public VehicleCollection() {
        list = new LinkedList<>();
        creationDate = LocalDate.now();
    }

    /**
     * Создает коллекцию на основе готового списка.
     *
     * @param vehicles список объектов, которыми нужно заполнить коллекцию
     */
    public VehicleCollection(List<Vehicle> vehicles) {
        list = new LinkedList<>(vehicles);
        list.sort(Comparator.comparing(Vehicle::getName));
        creationDate = LocalDate.now();
    }

    /**
     * Возвращает информацию о коллекции.
     *
     * @return объект с данными о коллекции
     */
    @Override
    public CollectionInfo info() {
        return new CollectionInfo(Vehicle.class, this.getClass(), creationDate, list.size());
    }

    /**
     * Возвращает все элементы коллекции.
     *
     * @return список объектов коллекции
     */
    @Override
    public List<Vehicle> show() {
        return list;
    }

    /**
     * Добавляет объект в коллекцию и сортирует ее.
     *
     * @param vehicle объект, который нужно добавить
     */
    @Override
    public void add(Vehicle vehicle) {
        list.add(vehicle);
        list.sort(Comparator.comparing(Vehicle::getName));
    }


    /**
     * Ищет объект по id.
     *
     * @param id id объекта, который нужно найти
     * @return найденный объект
     * @throws StorageException если объект с таким id не найден
     */
    @Override
    public Vehicle getById(Integer id) {
        Optional<Vehicle> vO = list.stream().filter(s -> (Objects.equals(s.getId(), id))).findFirst();// обертка
        if (vO.isPresent()) {
            return vO.get();
        } else throw new StorageException("Элемент с ID " + id + " не найден.");
    }


    /**
     * Обновляет объект по id.
     *
     * @param id id объекта, который нужно изменить
     * @param vehicle объект, из которого берутся новые значения полей
     * @throws StorageException если объект с таким id не найден
     */
    @Override
    public void update(Integer id, Vehicle vehicle) {
        Vehicle v = getById(id);
        v.setName(vehicle.getName());
        v.setCapacity(vehicle.getCapacity());
        v.setCoordinates(vehicle.getCoordinates());
        v.setFuelType(vehicle.getFuelType());
        v.setEnginePower(vehicle.getEnginePower());
        v.setNumberOfWheels(vehicle.getNumberOfWheels());
    }


    /**
     * Удаляет объект из коллекции по id.
     *
     * @param id id объекта, который нужно удалить
     * @throws StorageException если объект с таким id не найден
     */
    @Override
    public void removeById(Integer id) {
        boolean removed = list.removeIf(v -> v.getId().equals(id));
        if (!removed) {
            throw new StorageException("Элемент с ID " + id + " не найден.");
        }
    }

    /**
     * Удаляет все элементы из коллекции.
     */
    @Override
    public void clear() {
        list.clear();
    }

    /**
     * Преобразует всю коллекцию в строку JSON.
     *
     * @return строка со всей коллекцией
     */
    @Override
    public String save() {
        String res = "[ ";
        for(Vehicle s: list){
            if (list.get(list.size()-1)!=s) {
                res += s.toJson() + ",\n";
            }
            else res += s.toJson();
        }
        res+= " ]";
        return res;
    }

    /**
     * Возвращает первый элемент коллекции.
     *
     * @return первый объект или {@code null}, если коллекция пустая
     */
    @Override
    public Vehicle head() {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Добавляет объект, только если он больше максимального элемента.
     *
     * @param vehicle объект, который нужно сравнить с максимальным
     * @throws StorageException если объект не больше максимального элемента
     */
    @Override
    public void addIfMax(Vehicle vehicle) {
        if (list.isEmpty()) {
            add(vehicle);
            return;
        }


        Vehicle maxVehicle = Collections.max(list);

        if (vehicle.compareTo(maxVehicle) > 0) {
            add(vehicle);
        } else {
            throw new StorageException("Элемент не добавлен, так как он не превышает максимальный.");
        }
    }

    /**
     * Удаляет все элементы, которые больше указанного объекта.
     *
     * @param vehicle объект, с которым сравниваются элементы коллекции
     * @throws StorageException если таких элементов не нашлось
     */
    @Override
    public void removeGreater(Vehicle vehicle) {
        boolean removed = list.removeIf(v -> v.compareTo(vehicle) > 0);
        if (!removed) {
            throw new StorageException("Элементы, превышающие заданный, не найдены.");
        }
    }

    /**
     * Считает количество объектов с заданным числом колес.
     *
     * @param numberOfWheels значение numberOfWheels для поиска
     * @return количество найденных объектов
     */
    @Override
    public int countByNumberOfWheels(int numberOfWheels) {
        //вывести количество элементов, значение поля numberOfWheels которых равно заданному
        int c = 0;
        for (Vehicle v : list) {
            if (v.getNumberOfWheels() == numberOfWheels) c++;
        }
        return c;
    }

    /**
     * Возвращает список объектов с нужным типом топлива.
     *
     * @param fuelType тип топлива для поиска
     * @return список найденных объектов
     */
    @Override
    public List<Vehicle> filterByFuelType(FuelType fuelType) {
        List<Vehicle> c = new LinkedList<>();
        for (Vehicle v : list) {
            if (v.getFuelType() == fuelType) c.add(v);
        }
        return c;
    }

    /**
     * Возвращает список объектов, имя которых начинается с указанной строки.
     *
     * @param name начало имени для поиска
     * @return список найденных объектов
     */
    @Override
    public List<Vehicle> filterStartsWithName(String name) {
        List<Vehicle> c = new LinkedList<>();
        for (Vehicle v : list) {
            if (v.getName().startsWith(name)) c.add(v);
        }
        return c;
    }
}
