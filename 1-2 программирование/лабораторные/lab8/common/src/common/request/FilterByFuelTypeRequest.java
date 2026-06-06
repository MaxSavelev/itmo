package common.request;

import data.FuelType;

public class FilterByFuelTypeRequest implements CommandRequest {
    private static final long serialVersionUID = 1L;

    private final FuelType fuelType;

    public FilterByFuelTypeRequest(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public FuelType getFuelType() {
        return fuelType;
    }
}
