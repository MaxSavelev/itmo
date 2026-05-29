package common.request;

public class CountByNumberOfWheelsRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final int numberOfWheels;

    public CountByNumberOfWheelsRequest(int numberOfWheels) {
        this.numberOfWheels = numberOfWheels;
    }

    public int getNumberOfWheels() {
        return numberOfWheels;
    }
}
