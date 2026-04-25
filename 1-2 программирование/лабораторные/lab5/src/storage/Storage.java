package storage;

import java.util.List;

import data.FuelType;
import data.Vehicle;

/**
 * Интерфейс для работы с коллекцией {@link Vehicle}.
 * <p>
 * Здесь описаны основные действия:
 * добавление, обновление, удаление, поиск,
 * фильтрация и сохранение коллекции.
 * </p>
 *
 * @author makssavelev
 * @version 1.0
 */
public interface Storage{
    /**
     * Возвращает информацию о коллекции.
     *
     * @return объект с данными о коллекции
     */
    CollectionInfo info();

    /**
     * Возвращает все элементы коллекции.
     *
     * @return список всех объектов
     */
    List<Vehicle> show();

    /**
     * Добавляет объект в коллекцию.
     *
     * @param vehicle объект, который нужно добавить
     */
    void add(Vehicle vehicle);

    /**
     * Обновляет объект по его id.
     *
     * @param id id объекта, который нужно изменить
     * @param vehicle объект с новыми значениями полей
     */
    void update(Integer id, Vehicle vehicle);

    /**
     * Удаляет объект по id.
     *
     * @param id id объекта, который нужно удалить
     */
    void removeById(Integer id);

    /**
     * Полностью очищает коллекцию.
     */
    void clear();

    /**
     * Сохраняет текущее состояние коллекции.
     *
     * @return строка, которую можно записать в файл
     */
    String save();

    /**
     * Возвращает первый элемент коллекции.
     *
     * @return первый объект коллекции
     */
    Vehicle head();

    /**
     * Добавляет объект, только если он больше максимального.
     *
     * @param vehicle объект, который нужно сравнить и, возможно, добавить
     */
    void addIfMax(Vehicle vehicle);

    /**
     * Удаляет из коллекции все объекты, которые больше указанного.
     *
     * @param vehicle объект, с которым сравниваются элементы коллекции
     */
    void removeGreater(Vehicle vehicle);

    /**
     * Считает, сколько объектов имеют указанное число колес.
     *
     * @param numberOfWheels значение поля numberOfWheels для поиска
     * @return количество найденных объектов
     */
    int countByNumberOfWheels(int numberOfWheels);

    /**
     * Возвращает объект по id.
     *
     * @param id id нужного объекта
     * @return найденный объект
     */
    Vehicle getById(Integer id);

    /**
     * Возвращает объекты с указанным типом топлива.
     *
     * @param fuelType тип топлива, по которому идет поиск
     * @return список найденных объектов
     */
    List<Vehicle> filterByFuelType(FuelType fuelType);

    /**
     * Возвращает объекты, имя которых начинается с указанной строки.
     *
     * @param name начало имени для поиска
     * @return список найденных объектов
     */
    List<Vehicle> filterStartsWithName(String name);
}
