package common.request;

public class FilterStartsWithNameRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final String prefix;

    public FilterStartsWithNameRequest(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
