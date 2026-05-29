package common.request;

public class RemoveByIdRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final int id;

    public RemoveByIdRequest(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
