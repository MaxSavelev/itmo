package client.gui;

import data.Vehicle;

import javax.swing.table.AbstractTableModel;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Модель таблицы объектов Vehicle для GUI-клиента.
 */
public final class VehicleTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    private static final String[] COLUMN_KEYS = {
            "column.id",
            "column.name",
            "column.x",
            "column.y",
            "column.creationDate",
            "column.enginePower",
            "column.wheels",
            "column.capacity",
            "column.fuelType",
            "column.owner"
    };

    private final List<Vehicle> vehicles = new ArrayList<>();
    private GuiMessages messages;

    public VehicleTableModel() {
        this(GuiMessages.forLocale(Locale.forLanguageTag("ru")));
    }

    public VehicleTableModel(GuiMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    public void setMessages(GuiMessages messages) {
        this.messages = Objects.requireNonNull(messages);
        fireTableStructureChanged();
    }

    @Override
    public int getRowCount() {
        return vehicles.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_KEYS.length;
    }

    @Override
    public String getColumnName(int column) {
        return messages.get(COLUMN_KEYS[column]);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Vehicle vehicle = vehicles.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> vehicle.getId();
            case 1 -> vehicle.getName();
            case 2 -> vehicle.getCoordinates().getX();
            case 3 -> vehicle.getCoordinates().getY();
            case 4 -> formatDate(vehicle);
            case 5 -> formatNullableNumber(vehicle.getEnginePower());
            case 6 -> vehicle.getNumberOfWheels();
            case 7 -> formatNumber(vehicle.getCapacity());
            case 8 -> vehicle.getFuelType();
            case 9 -> vehicle.getOwnerLogin();
            default -> null;
        };
    }

    private String formatDate(Vehicle vehicle) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                .withLocale(messages.locale())
                .format(vehicle.getCreationDate());
    }

    private String formatNullableNumber(Number value) {
        return value == null ? "" : formatNumber(value);
    }

    private String formatNumber(Number value) {
        return NumberFormat.getNumberInstance(messages.locale()).format(value);
    }

    /**
     * Заменяет список объектов, который отображается в таблице.
     *
     * @param newVehicles новый список объектов
     */
    public void setVehicles(List<Vehicle> newVehicles) {
        vehicles.clear();
        vehicles.addAll(newVehicles);
        fireTableDataChanged();
    }

    public Vehicle getVehicleAt(int rowIndex) {
        return vehicles.get(rowIndex);
    }
}
