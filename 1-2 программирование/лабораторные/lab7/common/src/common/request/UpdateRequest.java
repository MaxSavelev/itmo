package common.request;

import data.Vehicle;

public class UpdateRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final Vehicle vehicle;

    public UpdateRequest(int id, Vehicle vehicle) {
        this.id = id;
        this.vehicle = vehicle;
    }

    public int getId() {
        return id;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }
}
