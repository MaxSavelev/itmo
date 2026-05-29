package common.request;

public class LoginRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final String login;
    private final String password;

    public LoginRequest(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
