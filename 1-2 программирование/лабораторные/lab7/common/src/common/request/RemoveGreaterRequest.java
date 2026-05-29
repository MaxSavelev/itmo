package common.request;

import data.Vehicle;

public class RemoveGreaterRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final Vehicle vehicle;

    public RemoveGreaterRequest(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}
