package client.gui;

import data.Coordinates;
import data.FuelType;
import data.Vehicle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Диалог редактирования выбранного Vehicle.
 */
public final class VehicleUpdateDialog extends JDialog {
    private final GuiMessages messages;
    private final Vehicle sourceVehicle;
    private final JTextField nameField;
    private final JTextField xField;
    private final JTextField yField;
    private final JTextField enginePowerField;
    private final JTextField wheelsField;
    private final JTextField capacityField;
    private final JComboBox<FuelType> fuelTypeComboBox;
    private final JButton saveButton;
    private final JButton cancelButton;
    private Consumer<Vehicle> saveAction;

    public VehicleUpdateDialog(Window owner, Vehicle vehicle) {
        this(owner, vehicle, GuiMessages.forLocale(Locale.forLanguageTag("ru")));
    }

    public VehicleUpdateDialog(Window owner, Vehicle vehicle, GuiMessages messages) {
        super(owner, messages.get("dialog.update.title"), ModalityType.APPLICATION_MODAL);
        this.messages = Objects.requireNonNull(messages);
        sourceVehicle = Objects.requireNonNull(vehicle);
        nameField = new JTextField(22);
        xField = new JTextField(22);
        yField = new JTextField(22);
        enginePowerField = new JTextField(22);
        wheelsField = new JTextField(22);
        capacityField = new JTextField(22);
        fuelTypeComboBox = new JComboBox<>(FuelType.values());
        saveButton = new JButton(messages.get("button.save"));
        cancelButton = new JButton(messages.get("button.cancel"));
        saveAction = updatedVehicle -> {
        };

        fillFields(sourceVehicle);
        configureDialog();
        add(createContentPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        configureActions();
        pack();
        setLocationRelativeTo(owner);
    }

    public int getVehicleId() {
        return sourceVehicle.getId();
    }

    public void setSaveAction(Consumer<Vehicle> saveAction) {
        this.saveAction = Objects.requireNonNull(saveAction);
    }

    public void setSaving(boolean saving) {
        setDefaultCloseOperation(saving ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
        saveButton.setEnabled(!saving);
        cancelButton.setEnabled(!saving);
        nameField.setEnabled(!saving);
        xField.setEnabled(!saving);
        yField.setEnabled(!saving);
        enginePowerField.setEnabled(!saving);
        wheelsField.setEnabled(!saving);
        capacityField.setEnabled(!saving);
        fuelTypeComboBox.setEnabled(!saving);
    }

    private void fillFields(Vehicle vehicle) {
        nameField.setText(vehicle.getName());
        xField.setText(String.valueOf(vehicle.getCoordinates().getX()));
        yField.setText(String.valueOf(vehicle.getCoordinates().getY()));
        enginePowerField.setText(vehicle.getEnginePower() == null ? "" : String.valueOf(vehicle.getEnginePower()));
        wheelsField.setText(String.valueOf(vehicle.getNumberOfWheels()));
        capacityField.setText(String.valueOf(vehicle.getCapacity()));
        fuelTypeComboBox.setSelectedItem(vehicle.getFuelType());
    }

    private void configureDialog() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(460, 430));
        setLayout(new BorderLayout());
    }

    private JPanel createContentPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        root.add(createInfoPanel(), BorderLayout.NORTH);
        root.add(createFormPanel(), BorderLayout.CENTER);
        return root;
    }

    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(messages.get("panel.serviceFields")),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        GridBagConstraints constraints = createConstraints();
        addInfoRow(panel, constraints, 0, messages.get("column.id"), String.valueOf(sourceVehicle.getId()));
        addInfoRow(panel, constraints, 1, messages.get("column.creationDate"), String.valueOf(sourceVehicle.getCreationDate()));
        addInfoRow(panel, constraints, 2, messages.get("column.owner"), sourceVehicle.getOwnerLogin());
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(messages.get("panel.vehicleData")),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        GridBagConstraints constraints = createConstraints();
        addFormRow(panel, constraints, 0, messages.get("column.name"), nameField);
        addFormRow(panel, constraints, 1, messages.get("column.x"), xField);
        addFormRow(panel, constraints, 2, messages.get("column.y"), yField);
        addFormRow(panel, constraints, 3, messages.get("column.enginePower"), enginePowerField);
        addFormRow(panel, constraints, 4, messages.get("column.wheels"), wheelsField);
        addFormRow(panel, constraints, 5, messages.get("column.capacity"), capacityField);
        addFormRow(panel, constraints, 6, messages.get("column.fuelType"), fuelTypeComboBox);
        return panel;
    }

    private GridBagConstraints createConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 6, 5, 6);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        return constraints;
    }

    private void addInfoRow(JPanel panel,
                            GridBagConstraints constraints,
                            int row,
                            String labelText,
                            String value) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        panel.add(new JLabel(labelText), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(new JLabel(value == null ? "-" : value), constraints);
    }

    private void addFormRow(JPanel panel,
                            GridBagConstraints constraints,
                            int row,
                            String labelText,
                            Component field) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        panel.add(new JLabel(labelText), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(field, constraints);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        panel.add(saveButton);
        panel.add(cancelButton);
        return panel;
    }

    private void configureActions() {
        saveButton.addActionListener(event -> saveVehicle());
        cancelButton.addActionListener(event -> dispose());
    }

    private void saveVehicle() {
        Vehicle vehicle = createVehicleFromInput();
        if (vehicle != null) {
            saveAction.accept(vehicle);
        }
    }

    private Vehicle createVehicleFromInput() {
        VehicleInput input = validateInput();
        if (input == null) {
            return null;
        }
        try {
            return new Vehicle(
                    input.name(),
                    new Coordinates(input.x(), input.y()),
                    input.enginePower(),
                    input.wheels(),
                    input.capacity(),
                    input.fuelType()
            );
        } catch (IllegalArgumentException e) {
            showInputError(e.getMessage(), nameField);
            return null;
        }
    }

    private VehicleInput validateInput() {
        String name = nameField.getText().trim();
        if (name.isBlank()) {
            showInputError(messages.get("error.nameBlank"), nameField);
            return null;
        }
        Long x = parseLongField(xField, messages.get("column.x"));
        if (x == null) {
            return null;
        }
        Double y = parseDoubleField(yField, messages.get("column.y"));
        if (y == null) {
            return null;
        }
        if (y <= -372) {
            showInputError(messages.get("error.yGreaterThan"), yField);
            return null;
        }
        Double enginePower = parseOptionalDoubleField(enginePowerField, messages.get("column.enginePower"));
        if (enginePower == null && !enginePowerField.getText().trim().isBlank()) {
            return null;
        }
        if (enginePower != null && enginePower <= 0) {
            showInputError(messages.get("error.enginePowerPositiveOrBlank"), enginePowerField);
            return null;
        }
        Integer wheels = parseIntegerField(wheelsField, messages.get("column.wheels"));
        if (wheels == null) {
            return null;
        }
        if (wheels <= 0) {
            showInputError(messages.get("error.wheelsPositive"), wheelsField);
            return null;
        }
        Float capacity = parseFloatField(capacityField, messages.get("column.capacity"));
        if (capacity == null) {
            return null;
        }
        if (capacity <= 0) {
            showInputError(messages.get("error.capacityPositive"), capacityField);
            return null;
        }
        return new VehicleInput(name, x, y, enginePower, wheels, capacity, getFuelType());
    }

    private FuelType getFuelType() {
        return (FuelType) Objects.requireNonNull(fuelTypeComboBox.getSelectedItem());
    }

    private Long parseLongField(JTextField field, String fieldName) {
        try {
            return Long.parseLong(field.getText().trim());
        } catch (NumberFormatException e) {
            showInputError(messages.format("error.integerField", fieldName), field);
            return null;
        }
    }

    private Integer parseIntegerField(JTextField field, String fieldName) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            showInputError(messages.format("error.integerField", fieldName), field);
            return null;
        }
    }

    private Double parseDoubleField(JTextField field, String fieldName) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            if (!Double.isFinite(value)) {
                showInputError(messages.format("error.finiteNumberField", fieldName), field);
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            showInputError(messages.format("error.numberField", fieldName), field);
            return null;
        }
    }

    private Double parseOptionalDoubleField(JTextField field, String fieldName) {
        if (field.getText().trim().isBlank()) {
            return null;
        }
        return parseDoubleField(field, fieldName);
    }

    private Float parseFloatField(JTextField field, String fieldName) {
        try {
            float value = Float.parseFloat(field.getText().trim());
            if (!Float.isFinite(value)) {
                showInputError(messages.format("error.finiteFloatField", fieldName), field);
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            showInputError(messages.format("error.floatField", fieldName), field);
            return null;
        }
    }

    private void showInputError(String message, JTextField field) {
        JOptionPane.showMessageDialog(
                this,
                message,
                messages.get("dialog.inputError.title"),
                JOptionPane.ERROR_MESSAGE
        );
        field.requestFocusInWindow();
    }

    private record VehicleInput(String name,
                                Long x,
                                double y,
                                Double enginePower,
                                int wheels,
                                float capacity,
                                FuelType fuelType) {
    }
}
