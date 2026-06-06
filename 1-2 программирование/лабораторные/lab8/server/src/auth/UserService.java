package auth;

import java.util.Objects;

/**
 * Выполняет регистрацию и авторизацию пользователей.
 */
public class UserService {
    private static final int MAX_LOGIN_LENGTH = 80;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordHasher = new PasswordHasher();
    }

    /**
     * Регистрирует нового пользователя.
     *
     * @param login логин
     * @param password пароль
     */
    public void register(String login, String password) {
        String normalizedLogin = normalizeLogin(login);
        validateCredentials(normalizedLogin, password);
        userRepository.addUser(normalizedLogin, passwordHasher.hash(password));
    }

    /**
     * Проверяет логин и пароль пользователя.
     *
     * @param login логин
     * @param password пароль
     */
    public void login(String login, String password) {
        String normalizedLogin = normalizeLogin(login);
        validateCredentials(normalizedLogin, password);

        String expectedHash = userRepository.findPasswordHash(normalizedLogin);
        String actualHash = passwordHasher.hash(password);
        if (!Objects.equals(expectedHash, actualHash)) {
            throw new IllegalArgumentException("Неверный логин или пароль.");
        }
    }

    private String normalizeLogin(String login) {
        if (login == null) {
            return null;
        }
        return login.trim();
    }

    private void validateCredentials(String login, String password) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("Логин не может быть пустым.");
        }
        if (login.length() > MAX_LOGIN_LENGTH) {
            throw new IllegalArgumentException("Логин не может быть длиннее " + MAX_LOGIN_LENGTH + " символов.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль не может быть пустым.");
        }
    }
}
