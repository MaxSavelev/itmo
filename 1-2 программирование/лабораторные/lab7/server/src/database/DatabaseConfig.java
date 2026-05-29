package database;

/**
 * Настройки подключения к PostgreSQL.
 * <p>
 * По умолчанию используются данные кафедрального сервера:
 * host {@code pg}, база {@code studs}, порт {@code 5432}.
 * Логин и пароль можно передать через переменные окружения.
 * </p>
 */
public class DatabaseConfig {
    private static final String DEFAULT_HOST = "pg";
    private static final String DEFAULT_PORT = "5432";
    private static final String DEFAULT_DATABASE = "studs";

    private final String host;
    private final String port;
    private final String database;
    private final String user;
    private final String password;

    public DatabaseConfig(String host, String port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    /**
     * Создаёт конфиг из переменных окружения.
     * <p>
     * Для кафедрального сервера обычно достаточно указать {@code DB_USER}
     * и {@code DB_PASSWORD}. Также поддерживаются {@code PGUSER} и
     * {@code PGPASSWORD}.
     * </p>
     *
     * @return настройки подключения
     */
    public static DatabaseConfig fromEnvironment() {
        String host = readEnv("DB_HOST", DEFAULT_HOST);
        String port = readEnv("DB_PORT", DEFAULT_PORT);
        String database = readEnv("DB_NAME", DEFAULT_DATABASE);
        String user = readEnv("DB_USER", readEnv("PGUSER", System.getProperty("user.name")));
        String password = readEnv("DB_PASSWORD", readEnv("PGPASSWORD", ""));

        return new DatabaseConfig(host, port, database, user, password);
    }

    public String getUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getConnectionInfo() {
        return host + ":" + port + "/" + database + ", user=" + user;
    }

    private static String readEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
