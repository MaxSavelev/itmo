package common.request;

public class AuthenticatedRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final String login;
    private final String password;
    private final CommandRequest command;

    public AuthenticatedRequest(String login, String password, CommandRequest command) {
        this.login = login;
        this.password = password;
        this.command = command;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public CommandRequest getCommand() {
        return command;
    }
}
