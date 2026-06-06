package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Отвечает за подключение к PostgreSQL и подготовку таблиц lab7.
 */
public class DatabaseManager {
    private final DatabaseConfig config;

    public DatabaseManager(DatabaseConfig config) {
        this.config = config;
    }

    /**
     * Открывает новое подключение к базе данных.
     *
     * @return подключение к PostgreSQL
     * @throws SQLException если подключиться не удалось
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
    }

    /**
     * Создаёт sequence и таблицы, если их ещё нет.
     */
    public void initialize() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE SEQUENCE IF NOT EXISTS lab7_vehicle_id_seq
                    START WITH 1
                    INCREMENT BY 1
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lab7_users (
                        login VARCHAR(80) PRIMARY KEY,
                        password_hash VARCHAR(64) NOT NULL,
                        CONSTRAINT lab7_users_login_not_blank CHECK (length(trim(login)) > 0)
                    )
                    """);

            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lab7_vehicles (
                        id INTEGER PRIMARY KEY DEFAULT nextval('lab7_vehicle_id_seq'),
                        name TEXT NOT NULL,
                        coordinates_x BIGINT NOT NULL,
                        coordinates_y DOUBLE PRECISION NOT NULL,
                        creation_date DATE NOT NULL DEFAULT CURRENT_DATE,
                        engine_power DOUBLE PRECISION,
                        number_of_wheels INTEGER NOT NULL,
                        capacity REAL NOT NULL,
                        fuel_type VARCHAR(32) NOT NULL,
                        owner_login VARCHAR(80) NOT NULL REFERENCES lab7_users(login) ON DELETE CASCADE,
                        CONSTRAINT lab7_vehicles_name_not_blank CHECK (length(trim(name)) > 0),
                        CONSTRAINT lab7_vehicles_coordinates_y_check CHECK (coordinates_y > -372),
                        CONSTRAINT lab7_vehicles_engine_power_check CHECK (engine_power IS NULL OR engine_power > 0),
                        CONSTRAINT lab7_vehicles_number_of_wheels_check CHECK (number_of_wheels > 0),
                        CONSTRAINT lab7_vehicles_capacity_check CHECK (capacity > 0),
                        CONSTRAINT lab7_vehicles_fuel_type_check CHECK (
                            fuel_type IN ('GASOLINE', 'ELECTRICITY', 'DIESEL', 'MANPOWER', 'NUCLEAR')
                        )
                    )
                    """);

            statement.executeUpdate("""
                    ALTER SEQUENCE lab7_vehicle_id_seq
                    OWNED BY lab7_vehicles.id
                    """);
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось подготовить базу данных.", e);
        }
    }

    public String getConnectionInfo() {
        return config.getConnectionInfo();
    }
}
