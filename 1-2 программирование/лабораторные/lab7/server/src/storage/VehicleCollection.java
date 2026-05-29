package storage;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CopyOnWriteArrayList;

import common.dto.CollectionInfo;
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
    private final CopyOnWriteArrayList<Vehicle> list;

    /**
     * Создает пустую коллекцию.
     */
    public VehicleCollection() {
        list = new CopyOnWriteArrayList<>();
        creationDate = LocalDate.now();
    }

    /**
     * Создает коллекцию на основе готового списка.
     *
     * @param vehicles список объектов, которыми нужно заполнить коллекцию
     */
    public VehicleCollection(List<Vehicle> vehicles) {
        list = new CopyOnWriteArrayList<>(vehicles);
        creationDate = LocalDate.now();
        sortByName();
    }

    /**
     * Возвращает информацию о коллекции.
     *
     * @return объект с данными о коллекции
     */
    @Override
    public CollectionInfo info() {
        
        
        return new CollectionInfo(Vehicle.class.getSimpleName(), getClass().getSimpleName(), creationDate, list.size());
    }

    /**
     * Возвращает все элементы коллекции.
     *
     * @return список объектов коллекции
     */
    @Override
    public List<Vehicle> show() {
        
        return list.stream()
                .collect(Collectors.toList());
    }

    /**
     * Добавляет объект в коллекцию и сортирует ее.
     *
     * @param vehicle объект, который нужно добавить
     */
    @Override
    public void add(Vehicle vehicle) {
        list.add(vehicle);
        sortByName();
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
        return list.stream()
                .filter(vehicle -> Objects.equals(vehicle.getId(), id))
                .findFirst()
                .orElseThrow(() -> new StorageException("Элемент с ID " + id + " не найден."));
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
        
        sortByName();
    }


    /**
     * Удаляет объект из коллекции по id.
     *
     * @param id id объекта, который нужно удалить
     * @throws StorageException если объект с таким id не найден
     */
    @Override
    public void removeById(Integer id) {
        Vehicle vehicle = getById(id);
        list.remove(vehicle);
    }

    /**
     * Удаляет объекты по списку id.
     *
     * @param ids id объектов, которые нужно удалить
     */
    @Override
    public void removeByIds(List<Integer> ids) {
        Set<Integer> idSet = new HashSet<>(ids);
        list.removeIf(vehicle -> idSet.contains(vehicle.getId()));
    }

    /**
     * Удаляет объекты указанного владельца.
     *
     * @param ownerLogin логин владельца
     */
    @Override
    public void removeByOwner(String ownerLogin) {
        list.removeIf(vehicle -> Objects.equals(vehicle.getOwnerLogin(), ownerLogin));
    }

    /**
     * Удаляет все элементы из коллекции.
     */
    @Override
    public void clear() {
        list.clear();
    }

    /**
     * Возвращает первый элемент коллекции.
     *
     * @return первый объект или {@code null}, если коллекция пустая
     */
    @Override
    public Vehicle head() {
        return list.stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Добавляет объект, только если он больше максимального элемента.
     *
     * @param vehicle объект, который нужно сравнить с максимальным
     * @throws StorageException если объект не больше максимального элемента
     */
    @Override
    public void addIfMax(Vehicle vehicle) {
        Vehicle maxVehicle = list.stream()
                .max(Vehicle::compareTo)
                .orElse(null);

        if (maxVehicle == null || vehicle.compareTo(maxVehicle) > 0) {
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
        List<Vehicle> vehiclesToRemove = list.stream()
                .filter(currentVehicle -> currentVehicle.compareTo(vehicle) > 0)
                .collect(Collectors.toList());

        if (vehiclesToRemove.isEmpty()) {
            throw new StorageException("Элементы, превышающие заданный, не найдены.");
        }

        list.removeAll(vehiclesToRemove);
    }

    /**
     * Считает количество объектов с заданным числом колес.
     *
     * @param numberOfWheels значение numberOfWheels для поиска
     * @return количество найденных объектов
     */
    @Override
    public int countByNumberOfWheels(int numberOfWheels) {
        
        return (int) list.stream()
                .filter(v -> v.getNumberOfWheels() == numberOfWheels)
                .count();
    }

    /**
     * Возвращает список объектов с нужным типом топлива.
     *
     * @param fuelType тип топлива для поиска
     * @return список найденных объектов
     */
    @Override
    public List<Vehicle> filterByFuelType(FuelType fuelType) {
        
        return list.stream()
                .filter(v -> v.getFuelType() == fuelType)
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список объектов, имя которых начинается с указанной строки.
     *
     * @param name начало имени для поиска
     * @return список найденных объектов
     */
    @Override
    public List<Vehicle> filterStartsWithName(String name) {
        return list.stream()
                .filter(v -> v.getName().startsWith(name))
                .collect(Collectors.toList());
    }

    private void sortByName() {
        list.sort(Comparator.comparing(Vehicle::getName));
    }
}
