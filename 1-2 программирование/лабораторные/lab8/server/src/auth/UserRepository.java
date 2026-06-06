package auth;

import database.DatabaseException;
import database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Работает с пользователями в PostgreSQL.
 */
public class UserRepository {
    private static final String DUPLICATE_KEY_SQL_STATE = "23505";

    private final DatabaseManager databaseManager;

    public UserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Добавляет нового пользователя.
     *
     * @param login логин пользователя
     * @param passwordHash хэш пароля
     */
    public void addUser(String login, String passwordHash) {
        String sql = "INSERT INTO lab7_users (login, password_hash) VALUES (?, ?)";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
        } catch (SQLException e) {
            if (DUPLICATE_KEY_SQL_STATE.equals(e.getSQLState())) {
                throw new IllegalArgumentException("Пользователь с таким логином уже существует.");
            }
            throw new DatabaseException("Не удалось добавить пользователя.", e);
        }
    }

    /**
     * Ищет хэш пароля пользователя по логину.
     *
     * @param login логин пользователя
     * @return хэш пароля или {@code null}, если пользователя нет
     */
    public String findPasswordHash(String login) {
        String sql = "SELECT password_hash FROM lab7_users WHERE login = ?";

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("password_hash");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось найти пользователя.", e);
        }
    }
}
