package common.request;

public class RegisterRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final String login;
    private final String password;

    public RegisterRequest(String login, String password) {
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
