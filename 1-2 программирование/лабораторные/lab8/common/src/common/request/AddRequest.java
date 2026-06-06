package common.request;

import data.Vehicle;

public class AddRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final Vehicle vehicle;

    public AddRequest(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}
