package storage;

import database.DatabaseException;
import database.DatabaseManager;
import data.Coordinates;
import data.FuelType;
import data.Vehicle;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

/**
 * Загружает объекты Vehicle из PostgreSQL.
 */
public class VehicleRepository {
    private final DatabaseManager databaseManager;

    public VehicleRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Загружает все объекты коллекции из таблицы lab7_vehicles.
     *
     * @return список объектов из базы данных
     */
    public List<Vehicle> loadAll() {
        String sql = """
                SELECT id,
                       name,
                       coordinates_x,
                       coordinates_y,
                       creation_date,
                       engine_power,
                       number_of_wheels,
                       capacity,
                       fuel_type,
                       owner_login
                FROM lab7_vehicles
                ORDER BY id
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<Vehicle> vehicles = new LinkedList<>();
            while (resultSet.next()) {
                vehicles.add(readVehicle(resultSet));
            }
            return vehicles;
        } catch (SQLException | IllegalArgumentException e) {
            throw new DatabaseException("Не удалось загрузить коллекцию из базы данных.", e);
        }
    }

    /**
     * Добавляет объект в таблицу и возвращает объект с id из базы данных.
     *
     * @param vehicle данные объекта
     * @param ownerLogin логин владельца объекта
     * @return объект, сохранённый в базе данных
     */
    public Vehicle add(Vehicle vehicle, String ownerLogin) {
        String sql = """
                INSERT INTO lab7_vehicles (
                    name,
                    coordinates_x,
                    coordinates_y,
                    engine_power,
                    number_of_wheels,
                    capacity,
                    fuel_type,
                    owner_login
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id,
                          name,
                          coordinates_x,
                          coordinates_y,
                          creation_date,
                          engine_power,
                          number_of_wheels,
                          capacity,
                          fuel_type,
                          owner_login
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillVehicleStatement(statement, vehicle);
            statement.setString(8, ownerLogin);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return readVehicle(resultSet);
                }
                throw new DatabaseException("База данных не вернула добавленный объект.", null);
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw new DatabaseException("Не удалось добавить объект в базу данных.", e);
        }
    }

    /**
     * Обновляет объект в базе данных.
     *
     * @param id id объекта
     * @param vehicle новые данные объекта
     * @param ownerLogin логин владельца объекта
     */
    public void update(Integer id, Vehicle vehicle, String ownerLogin) {
        String sql = """
                UPDATE lab7_vehicles
                SET name = ?,
                    coordinates_x = ?,
                    coordinates_y = ?,
                    engine_power = ?,
                    number_of_wheels = ?,
                    capacity = ?,
                    fuel_type = ?
                WHERE id = ? AND owner_login = ?
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fillVehicleStatement(statement, vehicle);
            statement.setInt(8, id);
            statement.setString(9, ownerLogin);
            int updatedRows = statement.executeUpdate();
            if (updatedRows == 0) {
                throw new StorageException("Элемент не найден или принадлежит другому пользователю.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось обновить объект в базе данных.", e);
        }
    }

    /**
     * Удаляет объект из базы данных по id.
     *
     * @param id id объекта
     * @param ownerLogin логин владельца объекта
     */
    public void removeById(Integer id, String ownerLogin) {
        String sql = "DELETE FROM lab7_vehicles WHERE id = ? AND owner_login = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, ownerLogin);
            int deletedRows = statement.executeUpdate();
            if (deletedRows == 0) {
                throw new StorageException("Элемент не найден или принадлежит другому пользователю.");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось удалить объект из базы данных.", e);
        }
    }

    /**
     * Удаляет из базы данных все объекты указанного владельца.
     *
     * @param ownerLogin логин владельца
     */
    public void clear(String ownerLogin) {
        String sql = "DELETE FROM lab7_vehicles WHERE owner_login = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerLogin);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось очистить коллекцию в базе данных.", e);
        }
    }

    /**
     * Удаляет из базы данных объекты с указанными id.
     *
     * @param ids id объектов
     * @param ownerLogin логин владельца объектов
     */
    public void removeByIds(List<Integer> ids, String ownerLogin) {
        if (ids.isEmpty()) {
            throw new StorageException("Элементы для удаления не найдены.");
        }

        String sql = "DELETE FROM lab7_vehicles WHERE id = ? AND owner_login = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);

            for (Integer id : ids) {
                statement.setInt(1, id);
                statement.setString(2, ownerLogin);
                statement.addBatch();
            }

            int deletedRows = countChangedRows(statement.executeBatch());
            if (deletedRows != ids.size()) {
                connection.rollback();
                throw new StorageException("Не все элементы найдены или принадлежат пользователю.");
            }

            connection.commit();
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось удалить объекты из базы данных.", e);
        }
    }

    private Vehicle readVehicle(ResultSet resultSet) throws SQLException {
        double enginePowerValue = resultSet.getDouble("engine_power");
        Double enginePower = resultSet.wasNull() ? null : enginePowerValue;

        Date date = resultSet.getDate("creation_date");
        LocalDate creationDate = date.toLocalDate();

        return new Vehicle(
                resultSet.getInt("id"),
                resultSet.getString("name"),
                new Coordinates(resultSet.getLong("coordinates_x"), resultSet.getDouble("coordinates_y")),
                creationDate,
                enginePower,
                resultSet.getInt("number_of_wheels"),
                resultSet.getFloat("capacity"),
                FuelType.valueOf(resultSet.getString("fuel_type")),
                resultSet.getString("owner_login")
        );
    }

    private void fillVehicleStatement(PreparedStatement statement, Vehicle vehicle) throws SQLException {
        statement.setString(1, vehicle.getName());
        statement.setLong(2, vehicle.getCoordinates().getX());
        statement.setDouble(3, vehicle.getCoordinates().getY());
        if (vehicle.getEnginePower() == null) {
            statement.setNull(4, Types.DOUBLE);
        } else {
            statement.setDouble(4, vehicle.getEnginePower());
        }
        statement.setInt(5, vehicle.getNumberOfWheels());
        statement.setFloat(6, vehicle.getCapacity());
        statement.setString(7, vehicle.getFuelType().name());
    }

    private int countChangedRows(int[] batchResult) {
        int total = 0;
        for (int current : batchResult) {
            if (current > 0) {
                total += current;
            }
        }
        return total;
    }
}
