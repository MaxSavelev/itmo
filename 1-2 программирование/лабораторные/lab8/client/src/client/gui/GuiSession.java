package client.gui;

/**
 * Хранит данные текущего пользователя GUI-клиента.
 */
public final class GuiSession {
    private String login;
    private String password;

    /**
     * Проверяет, выполнен ли вход.
     *
     * @return {@code true}, если логин и пароль уже сохранены
     */
    public boolean isAuthorized() {
        return login != null && password != null;
    }

    /**
     * Запоминает пользователя после успешного входа или регистрации.
     *
     * @param login логин пользователя
     * @param password пароль пользователя
     */
    public void setCredentials(String login, String password) {
        this.login = login;
        this.password = password;
    }

    /**
     * Очищает текущую сессию.
     */
    public void clear() {
        login = null;
        password = null;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
