package common.request;

public class ExecuteScriptRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final String fileName;

    public ExecuteScriptRequest(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
