package client.gui;

import client.ClientCommandParser;
import client.ClientVehicleReader;

import common.request.AddRequest;
import common.request.AddIfMaxRequest;
import common.request.ClearRequest;
import common.request.CommandRequest;
import common.request.CountByNumberOfWheelsRequest;
import common.request.FilterByFuelTypeRequest;
import common.request.FilterStartsWithNameRequest;
import common.request.HeadRequest;
import common.request.HelpRequest;
import common.request.InfoRequest;
import common.request.LoginRequest;
import common.request.RemoveByIdRequest;
import common.request.RemoveGreaterRequest;
import common.request.RegisterRequest;
import common.request.ShowRequest;
import common.request.UpdateRequest;
import common.response.CommandResponse;
import data.FuelType;
import data.Vehicle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import java.text.NumberFormat;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Главное окно GUI-клиента.
 */
public final class MainFrame extends JFrame {
    private static final int AUTO_REFRESH_DELAY_MS = 3000;

    private final GuiClientService clientService;
    private final GuiSession session;
    private final LocalizationManager localizationManager;
    private final JComboBox<LocaleOption> languageComboBox;
    private final JButton logoutButton;
    private final JButton showButton;
    private final JButton exitButton;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JButton infoButton;
    private final JButton helpButton;
    private final JButton headButton;
    private final JButton clearButton;
    private final JComboBox<OtherCommand> otherCommandsComboBox;
    private final JButton executeOtherCommandButton;
    private final JButton executeScriptButton;
    private final VehicleTableModel vehicleTableModel;
    private final List<Vehicle> allVehicles;
    private final List<Vehicle> displayedVehicles;
    private final JComboBox<FilterColumn> filterColumnComboBox;
    private final JTextField filterField;
    private final JComboBox<SortColumn> sortColumnComboBox;
    private final JComboBox<SortDirection> sortDirectionComboBox;
    private final JButton applyTableOptionsButton;
    private final JButton resetTableOptionsButton;
    private final Timer autoRefreshTimer;
    private GuiMessages messages;
    private JLabel titleLabel;
    private JLabel userLabel;
    private JLabel languageLabel;
    private JLabel otherCommandsLabel;
    private JLabel scriptLabel;
    private JLabel filterLabel;
    private JLabel sortLabel;
    private JPanel commandPanel;
    private JPanel tablePanel;
    private JPanel visualizationPanel;
    private JPanel selectedObjectPanel;
    private JTable vehicleTable;
    private Vehicle selectedVehicle;
    private boolean suppressTableSelectionEvents;
    private boolean collectionRefreshInProgress;
    private VehicleCanvasPanel vehicleCanvasPanel;
    private JLabel selectedIdLabel;
    private JLabel selectedNameLabel;
    private JLabel selectedOwnerLabel;
    private JLabel selectedCoordinatesLabel;
    private JLabel selectedEnginePowerLabel;
    private JLabel selectedWheelsLabel;
    private JLabel selectedCapacityLabel;
    private JLabel selectedFuelTypeLabel;

    public MainFrame(GuiClientService clientService, GuiSession session, Locale locale) {
        super(GuiMessages.forLocale(Objects.requireNonNull(locale)).get("app.title"));
        this.clientService = Objects.requireNonNull(clientService);
        this.session = Objects.requireNonNull(session);
        localizationManager = new LocalizationManager(locale);
        messages = localizationManager.getMessages();
        this.clientService.setMessages(messages);
        languageComboBox = new JComboBox<>(LocaleOption.values());
        languageComboBox.setSelectedItem(LocaleOption.fromLocale(locale));
        logoutButton = new JButton(messages.get("button.logout"));
        showButton = new JButton(messages.get("button.show"));
        exitButton = new JButton(messages.get("button.exit"));
        addButton = new JButton(messages.get("button.add"));
        editButton = new JButton(messages.get("button.edit"));
        deleteButton = new JButton(messages.get("button.deleteSelected"));
        infoButton = new JButton(messages.get("button.info"));
        helpButton = new JButton(messages.get("button.help"));
        headButton = new JButton(messages.get("button.head"));
        clearButton = new JButton(messages.get("button.clear"));
        otherCommandsComboBox = new JComboBox<>(OtherCommand.values());
        executeOtherCommandButton = new JButton(messages.get("button.execute"));
        executeScriptButton = new JButton(messages.get("button.executeScript"));
        vehicleTableModel = new VehicleTableModel(messages);
        allVehicles = new ArrayList<>();
        displayedVehicles = new ArrayList<>();
        filterColumnComboBox = new JComboBox<>(FilterColumn.values());
        filterField = new JTextField(18);
        sortColumnComboBox = new JComboBox<>(SortColumn.values());
        sortDirectionComboBox = new JComboBox<>(SortDirection.values());
        applyTableOptionsButton = new JButton(messages.get("button.apply"));
        resetTableOptionsButton = new JButton(messages.get("button.reset"));
        autoRefreshTimer = new Timer(AUTO_REFRESH_DELAY_MS, event -> refreshCollection(false));
        autoRefreshTimer.setRepeats(true);
        configureComboBoxRenderers();

        configureFrame();
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createContentPanel(), BorderLayout.CENTER);
        configureActions();
        updateTexts();
        pack();
        setLocationRelativeTo(null);
        autoRefreshTimer.start();
        refreshCollection();
    }

    @Override
    public void dispose() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
        super.dispose();
    }

    private void configureFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setLayout(new BorderLayout());
    }

    private void configureComboBoxRenderers() {
        configureRenderer(otherCommandsComboBox, command -> command.displayName(messages));
        configureRenderer(filterColumnComboBox, column -> column.displayName(messages));
        configureRenderer(sortColumnComboBox, column -> column.displayName(messages));
        configureRenderer(sortDirectionComboBox, direction -> direction.displayName(messages));
        configureRenderer(languageComboBox, option -> option.displayName(messages));
    }

    private <T> void configureRenderer(JComboBox<T> comboBox, Function<T, String> displayNameProvider) {
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (component instanceof JLabel label && value != null) {
                    @SuppressWarnings("unchecked")
                    T typedValue = (T) value;
                    label.setText(displayNameProvider.apply(typedValue));
                }
                return component;
            }
        });
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(16, 0));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        titleLabel = new JLabel();
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        userLabel = new JLabel();
        userLabel.setFont(userLabel.getFont().deriveFont(Font.BOLD, 16f));
        headerPanel.add(userLabel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        languageLabel = new JLabel();
        rightPanel.add(languageLabel);
        rightPanel.add(languageComboBox);
        rightPanel.add(logoutButton);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        panel.add(createCommandPanel(), BorderLayout.WEST);
        panel.add(createWorkspacePanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCommandPanel() {
        commandPanel = new JPanel();
        commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.Y_AXIS));
        commandPanel.setPreferredSize(new Dimension(190, 0));

        addCommandButton(commandPanel, addButton);
        addCommandButton(commandPanel, editButton);
        addCommandButton(commandPanel, deleteButton);
        addCommandButton(commandPanel, showButton);
        addCommandButton(commandPanel, infoButton);
        addCommandButton(commandPanel, helpButton);
        addCommandButton(commandPanel, headButton);
        addCommandButton(commandPanel, clearButton);

        commandPanel.add(Box.createVerticalStrut(16));
        otherCommandsLabel = new JLabel();
        addCommandSectionLabel(commandPanel, otherCommandsLabel);
        commandPanel.add(Box.createVerticalStrut(6));
        otherCommandsComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        commandPanel.add(otherCommandsComboBox);
        commandPanel.add(Box.createVerticalStrut(6));
        addCommandButton(commandPanel, executeOtherCommandButton);

        commandPanel.add(Box.createVerticalStrut(16));
        scriptLabel = new JLabel();
        addCommandSectionLabel(commandPanel, scriptLabel);
        commandPanel.add(Box.createVerticalStrut(6));
        addCommandButton(commandPanel, executeScriptButton);
        commandPanel.add(Box.createVerticalGlue());
        addCommandButton(commandPanel, exitButton);

        return commandPanel;
    }

    private void addCommandButton(JPanel panel, String text) {
        addCommandButton(panel, new JButton(text));
    }

    private void addCommandSectionLabel(JPanel panel, JLabel label) {
        label.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        label.setHorizontalAlignment(JLabel.LEFT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        panel.add(label);
    }

    private void addCommandButton(JPanel panel, JButton button) {
        button.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        panel.add(button);
        panel.add(Box.createVerticalStrut(6));
    }

    private JPanel createWorkspacePanel() {
        JPanel workspacePanel = new JPanel(new BorderLayout(10, 10));
        workspacePanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        workspacePanel.add(createTableArea(), BorderLayout.CENTER);
        workspacePanel.add(createBottomPanel(), BorderLayout.SOUTH);

        return workspacePanel;
    }

    private JPanel createTableArea() {
        tablePanel = new JPanel(new BorderLayout(8, 8));
        tablePanel.setPreferredSize(new Dimension(0, 310));

        vehicleTable = new JTable(vehicleTableModel);
        vehicleTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        vehicleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vehicleTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !suppressTableSelectionEvents) {
                selectVehicleFromTable();
            }
        });
        tablePanel.add(createFilterPanel(), BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(vehicleTable), BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterLabel = new JLabel();
        filterPanel.add(filterLabel);
        filterPanel.add(filterColumnComboBox);
        filterPanel.add(filterField);
        sortLabel = new JLabel();
        filterPanel.add(sortLabel);
        filterPanel.add(sortColumnComboBox);
        filterPanel.add(sortDirectionComboBox);
        filterPanel.add(applyTableOptionsButton);
        filterPanel.add(resetTableOptionsButton);
        return filterPanel;
    }

    private JSplitPane createBottomPanel() {
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                createVisualizationArea(),
                createSelectedObjectPanel()
        );
        splitPane.setResizeWeight(0.62);
        splitPane.setPreferredSize(new Dimension(0, 280));
        return splitPane;
    }

    private JPanel createVisualizationArea() {
        visualizationPanel = new JPanel(new BorderLayout());

        vehicleCanvasPanel = new VehicleCanvasPanel();
        vehicleCanvasPanel.setVehicleSelectionListener(this::selectVehicleFromVisualization);
        vehicleCanvasPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        visualizationPanel.add(new JScrollPane(vehicleCanvasPanel), BorderLayout.CENTER);

        return visualizationPanel;
    }

    private JPanel createSelectedObjectPanel() {
        selectedObjectPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        selectedObjectPanel.setPreferredSize(new Dimension(330, 0));

        selectedIdLabel = new JLabel();
        selectedNameLabel = new JLabel();
        selectedOwnerLabel = new JLabel();
        selectedCoordinatesLabel = new JLabel();
        selectedEnginePowerLabel = new JLabel();
        selectedWheelsLabel = new JLabel();
        selectedCapacityLabel = new JLabel();
        selectedFuelTypeLabel = new JLabel();

        selectedObjectPanel.add(selectedIdLabel);
        selectedObjectPanel.add(selectedNameLabel);
        selectedObjectPanel.add(selectedOwnerLabel);
        selectedObjectPanel.add(selectedCoordinatesLabel);
        selectedObjectPanel.add(selectedEnginePowerLabel);
        selectedObjectPanel.add(selectedWheelsLabel);
        selectedObjectPanel.add(selectedCapacityLabel);
        selectedObjectPanel.add(selectedFuelTypeLabel);
        clearSelectedObjectPanel();

        return selectedObjectPanel;
    }

    private void configureActions() {
        logoutButton.addActionListener(event -> logout());
        showButton.addActionListener(event -> refreshCollection());
        exitButton.addActionListener(event -> exitApplication());
        addButton.addActionListener(event -> openAddDialog());
        editButton.addActionListener(event -> openUpdateDialog());
        deleteButton.addActionListener(event -> confirmAndDeleteSelectedVehicle());
        infoButton.addActionListener(event -> sendCommandAndShowResult(new InfoRequest(), messages.get("dialog.info.title"), false));
        helpButton.addActionListener(event -> sendCommandAndShowResult(new HelpRequest(), messages.get("dialog.help.title"), false));
        headButton.addActionListener(event -> sendCommandAndShowResult(new HeadRequest(), messages.get("dialog.head.title"), false));
        clearButton.addActionListener(event -> sendCommandAndShowResult(new ClearRequest(), messages.get("dialog.clear.title"), true));
        executeOtherCommandButton.addActionListener(event -> executeSelectedOtherCommand());
        executeScriptButton.addActionListener(event -> openScriptFile());
        applyTableOptionsButton.addActionListener(event -> applyTableOptions());
        resetTableOptionsButton.addActionListener(event -> resetTableOptions());
        languageComboBox.addActionListener(event -> changeLanguage());
        updateSelectionControls();
    }

    private void changeLanguage() {
        localizationManager.setLocale(getSelectedLocale());
        messages = localizationManager.getMessages();
        clientService.setMessages(messages);
        updateTexts();
    }

    private void updateTexts() {
        setTitle(messages.get("app.title"));
        titleLabel.setText(messages.get("app.title"));
        userLabel.setText(messages.format("label.currentUser", session.getLogin()));
        languageLabel.setText(messages.get("label.language"));
        logoutButton.setText(messages.get("button.logout"));
        showButton.setText(messages.get("button.show"));
        exitButton.setText(messages.get("button.exit"));
        addButton.setText(messages.get("button.add"));
        editButton.setText(messages.get("button.edit"));
        deleteButton.setText(messages.get("button.deleteSelected"));
        infoButton.setText(messages.get("button.info"));
        helpButton.setText(messages.get("button.help"));
        headButton.setText(messages.get("button.head"));
        clearButton.setText(messages.get("button.clear"));
        executeOtherCommandButton.setText(messages.get("button.execute"));
        executeScriptButton.setText(messages.get("button.executeScript"));
        applyTableOptionsButton.setText(messages.get("button.apply"));
        resetTableOptionsButton.setText(messages.get("button.reset"));
        otherCommandsLabel.setText(messages.get("panel.otherCommands"));
        scriptLabel.setText(messages.get("panel.script"));
        filterLabel.setText(messages.get("filter.label"));
        sortLabel.setText(messages.get("sort.label"));

        commandPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(messages.get("panel.commands")),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        tablePanel.setBorder(BorderFactory.createTitledBorder(messages.get("panel.table")));
        visualizationPanel.setBorder(BorderFactory.createTitledBorder(messages.get("panel.visualization")));
        selectedObjectPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(messages.get("panel.selectedObject")),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        vehicleTableModel.setMessages(messages);
        applyTableOptions();
        if (selectedVehicle == null) {
            clearSelectedObjectPanel();
        } else {
            updateSelectedObjectPanel(selectedVehicle);
            selectTableRowForVehicle(selectedVehicle);
        }
        repaintComboBoxes();
        revalidate();
        repaint();
    }

    private void repaintComboBoxes() {
        languageComboBox.repaint();
        otherCommandsComboBox.repaint();
        filterColumnComboBox.repaint();
        sortColumnComboBox.repaint();
        sortDirectionComboBox.repaint();
    }

    private void openAddDialog() {
        VehicleAddDialog dialog = new VehicleAddDialog(this, messages);
        dialog.setSaveAction(vehicle -> addVehicle(dialog, vehicle));
        dialog.setVisible(true);
    }

    private void openUpdateDialog() {
        if (!isOwnSelectedVehicle()) {
            JOptionPane.showMessageDialog(
                    this,
                    messages.get("error.selectOwnForEdit"),
                    messages.get("dialog.edit.warningTitle"),
                    JOptionPane.WARNING_MESSAGE
            );
            updateSelectionControls();
            return;
        }

        VehicleUpdateDialog dialog = new VehicleUpdateDialog(this, selectedVehicle, messages);
        dialog.setSaveAction(vehicle -> updateVehicle(dialog, dialog.getVehicleId(), vehicle));
        dialog.setVisible(true);
    }

    private void addVehicle(VehicleAddDialog dialog, Vehicle vehicle) {
        dialog.setSaving(true);

        new SwingWorker<CommandResponse, Void>() {
            @Override
            protected CommandResponse doInBackground() throws GuiClientException {
                return clientService.send(new AddRequest(vehicle));
            }

            @Override
            protected void done() {
                if (!dialog.isDisplayable()) {
                    return;
                }
                handleAddResponse(dialog, this);
            }
        }.execute();
    }

    private void handleAddResponse(VehicleAddDialog dialog, SwingWorker<CommandResponse, Void> worker) {
        try {
            CommandResponse response = worker.get();
            if (GuiErrorDialogs.showServerErrorIfNeeded(dialog, response, messages)) {
                dialog.setSaving(false);
                return;
            }
            dialog.dispose();
            refreshCollection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            dialog.setSaving(false);
            GuiErrorDialogs.showClientError(dialog, new GuiClientException(messages.get("error.addInterrupted"), e), messages);
        } catch (ExecutionException e) {
            dialog.setSaving(false);
            Throwable cause = e.getCause();
            if (cause instanceof GuiClientException guiClientException) {
                GuiErrorDialogs.showClientError(dialog, guiClientException, messages);
            } else {
                GuiErrorDialogs.showClientError(dialog, new GuiClientException(messages.get("error.addFailed"), cause), messages);
            }
        }
    }

    private void updateVehicle(VehicleUpdateDialog dialog, int id, Vehicle vehicle) {
        dialog.setSaving(true);

        new SwingWorker<CommandResponse, Void>() {
            @Override
            protected CommandResponse doInBackground() throws GuiClientException {
                return clientService.send(new UpdateRequest(id, vehicle));
            }

            @Override
            protected void done() {
                if (!dialog.isDisplayable()) {
                    return;
                }
                handleUpdateResponse(dialog, this);
            }
        }.execute();
    }

    private void handleUpdateResponse(VehicleUpdateDialog dialog, SwingWorker<CommandResponse, Void> worker) {
        try {
            CommandResponse response = worker.get();
            if (GuiErrorDialogs.showServerErrorIfNeeded(dialog, response, messages)) {
                dialog.setSaving(false);
                return;
            }
            dialog.dispose();
            refreshCollection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            dialog.setSaving(false);
            GuiErrorDialogs.showClientError(dialog, new GuiClientException(messages.get("error.updateInterrupted"), e), messages);
        } catch (ExecutionException e) {
            dialog.setSaving(false);
            Throwable cause = e.getCause();
            if (cause instanceof GuiClientException guiClientException) {
                GuiErrorDialogs.showClientError(dialog, guiClientException, messages);
            } else {
                GuiErrorDialogs.showClientError(dialog, new GuiClientException(messages.get("error.updateFailed"), cause), messages);
            }
        }
    }

    private void confirmAndDeleteSelectedVehicle() {
        if (!isOwnSelectedVehicle()) {
            JOptionPane.showMessageDialog(
                    this,
                    messages.get("error.selectOwnForDelete"),
                    messages.get("dialog.delete.warningTitle"),
                    JOptionPane.WARNING_MESSAGE
            );
            updateSelectionControls();
            return;
        }

        Vehicle vehicle = selectedVehicle;
        int result = JOptionPane.showConfirmDialog(
                this,
                createDeleteConfirmationPanel(vehicle),
                messages.get("dialog.delete.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result == JOptionPane.YES_OPTION) {
            deleteVehicle(vehicle.getId());
        }
    }

    private JPanel createDeleteConfirmationPanel(Vehicle vehicle) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel(messages.get("dialog.delete.message")));
        panel.add(new JLabel(messages.format("selected.id", vehicle.getId())));
        panel.add(new JLabel(messages.format("selected.name", vehicle.getName())));
        panel.add(new JLabel(messages.format("selected.owner", vehicle.getOwnerLogin())));
        return panel;
    }

    private void deleteVehicle(int id) {
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        new SwingWorker<CommandResponse, Void>() {
            @Override
            protected CommandResponse doInBackground() throws GuiClientException {
                return clientService.send(new RemoveByIdRequest(id));
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                handleDeleteResponse(this);
            }
        }.execute();
    }

    private void handleDeleteResponse(SwingWorker<CommandResponse, Void> worker) {
        try {
            CommandResponse response = worker.get();
            if (GuiErrorDialogs.showServerErrorIfNeeded(this, response, messages)) {
                updateSelectionControls();
                return;
            }
            selectedVehicle = null;
            clearSelectedObjectPanel();
            updateSelectionControls();
            refreshCollection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateSelectionControls();
            GuiErrorDialogs.showClientError(this, new GuiClientException(messages.get("error.deleteInterrupted"), e), messages);
        } catch (ExecutionException e) {
            updateSelectionControls();
            Throwable cause = e.getCause();
            if (cause instanceof GuiClientException guiClientException) {
                GuiErrorDialogs.showClientError(this, guiClientException, messages);
            } else {
                GuiErrorDialogs.showClientError(this, new GuiClientException(messages.get("error.deleteFailed"), cause), messages);
            }
        }
    }

    private void executeSelectedOtherCommand() {
        OtherCommand command = (OtherCommand) otherCommandsComboBox.getSelectedItem();
        if (command == null) {
            return;
        }
        switch (command) {
            case ADD_IF_MAX -> openVehicleCommandDialog(
                    messages.get("other.addIfMax"),
                    vehicle -> new AddIfMaxRequest(vehicle)
            );
            case REMOVE_GREATER -> openVehicleCommandDialog(
                    messages.get("other.removeGreater"),
                    vehicle -> new RemoveGreaterRequest(vehicle)
            );
            case FILTER_BY_FUEL_TYPE -> executeFilterByFuelType();
            case FILTER_STARTS_WITH_NAME -> executeFilterStartsWithName();
            case COUNT_BY_NUMBER_OF_WHEELS -> executeCountByNumberOfWheels();
        }
    }

    private void openVehicleCommandDialog(String title, Function<Vehicle, CommandRequest> requestFactory) {
        VehicleAddDialog dialog = new VehicleAddDialog(this, messages);
        dialog.setTitle(title);
        dialog.setSaveAction(vehicle -> executeVehicleCommand(dialog, requestFactory.apply(vehicle), title));
        dialog.setVisible(true);
    }

    private void executeVehicleCommand(VehicleAddDialog dialog, CommandRequest request, String title) {
        dialog.setSaving(true);

        new SwingWorker<CommandResponse, Void>() {
            @Override
            protected CommandResponse doInBackground() throws GuiClientException {
                return clientService.send(request);
            }

            @Override
            protected void done() {
                if (!dialog.isDisplayable()) {
                    return;
                }
                handleVehicleCommandResponse(dialog, this, title);
            }
        }.execute();
    }

    private void handleVehicleCommandResponse(VehicleAddDialog dialog,
                                              SwingWorker<CommandResponse, Void> worker,
                                              String title) {
        try {
            CommandResponse response = worker.get();
            if (GuiErrorDialogs.showServerErrorIfNeeded(dialog, response, messages)) {
                dialog.setSaving(false);
                return;
            }
            dialog.dispose();
            showCommandResult(title, response);
            refreshCollection();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            dialog.setSaving(false);
            GuiErrorDialogs.showClientError(dialog, new GuiClientException(messages.get("error.commandInterrupted"), e), messages);
        } catch (ExecutionException e) {
            dialog.setSaving(false);
            Throwable cause = e.getCause();
            if (cause instanceof GuiClientException guiClientException) {
                GuiErrorDialogs.showClientError(dialog, guiClientException, messages);
            } else {
                GuiErrorDialogs.showClientError(dialog, new GuiClientException(messages.get("error.commandFailed"), cause), messages);
            }
        }
    }

    private void executeFilterByFuelType() {
        FuelType fuelType = (FuelType) JOptionPane.showInputDialog(
                this,
                messages.get("prompt.selectFuelType"),
                messages.get("dialog.filterByFuelType.title"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                FuelType.values(),
                FuelType.GASOLINE
        );
        if (fuelType != null) {
            sendCommandAndShowResult(new FilterByFuelTypeRequest(fuelType), messages.get("dialog.filterByFuelType.title"), false);
        }
    }

    private void executeFilterStartsWithName() {
        String prefix = JOptionPane.showInputDialog(
                this,
                messages.get("prompt.namePrefix"),
                messages.get("dialog.filterStartsWithName.title"),
                JOptionPane.QUESTION_MESSAGE
        );
        if (prefix == null) {
            return;
        }
        String trimmedPrefix = prefix.trim();
        if (trimmedPrefix.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    messages.get("error.namePrefixBlank"),
                    messages.get("dialog.inputError.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        sendCommandAndShowResult(new FilterStartsWithNameRequest(trimmedPrefix), messages.get("dialog.filterStartsWithName.title"), false);
    }

    private void executeCountByNumberOfWheels() {
        String input = JOptionPane.showInputDialog(
                this,
                messages.get("prompt.numberOfWheels"),
                messages.get("dialog.countByWheels.title"),
                JOptionPane.QUESTION_MESSAGE
        );
        if (input == null) {
            return;
        }
        try {
            int numberOfWheels = Integer.parseInt(input.trim());
            if (numberOfWheels <= 0) {
                JOptionPane.showMessageDialog(this,
                        messages.get("error.wheelsPositive"),
                        messages.get("dialog.inputError.title"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            sendCommandAndShowResult(new CountByNumberOfWheelsRequest(numberOfWheels), messages.get("dialog.countByWheels.title"), false);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    messages.format("error.integerField", messages.get("column.wheels")),
                    messages.get("dialog.inputError.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendCommandAndShowResult(CommandRequest request, String title, boolean refreshAfterSuccess) {
        new SwingWorker<CommandResponse, Void>() {
            @Override
            protected CommandResponse doInBackground() throws GuiClientException {
                return clientService.send(request);
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                handleSimpleCommandResponse(this, title, refreshAfterSuccess);
            }
        }.execute();
    }

    private void handleSimpleCommandResponse(SwingWorker<CommandResponse, Void> worker,
                                             String title,
                                             boolean refreshAfterSuccess) {
        try {
            CommandResponse response = worker.get();
            if (GuiErrorDialogs.showServerErrorIfNeeded(this, response, messages)) {
                return;
            }
            showCommandResult(title, response);
            if (refreshAfterSuccess) {
                refreshCollection();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            GuiErrorDialogs.showClientError(this, new GuiClientException(messages.get("error.commandInterrupted"), e), messages);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof GuiClientException guiClientException) {
                GuiErrorDialogs.showClientError(this, guiClientException, messages);
            } else {
                GuiErrorDialogs.showClientError(this, new GuiClientException(messages.get("error.commandFailed"), cause), messages);
            }
        }
    }

    private void showCommandResult(String title, CommandResponse response) {
        String text = formatResponseText(response);
        showTextDialog(title, text);
    }

    private void showTextDialog(String title, String text) {
        JTextArea textArea = new JTextArea(text, 12, 48);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea), title, JOptionPane.INFORMATION_MESSAGE);
    }

    private String formatResponseText(CommandResponse response) {
        String message = response.getMessage();
        Object result = response.getResult();
        String resultText = formatResult(result);
        if (message != null && resultText != null) {
            return message + System.lineSeparator() + resultText;
        }
        if (message != null) {
            return message;
        }
        if (resultText != null) {
            return resultText;
        }
        return messages.get("message.commandDone");
    }

    private String formatResult(Object result) {
        if (result instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return messages.get("message.emptyResult");
            }
            StringBuilder builder = new StringBuilder();
            for (Object item : collection) {
                if (!builder.isEmpty()) {
                    builder.append(System.lineSeparator());
                }
                builder.append(item);
            }
            return builder.toString();
        }
        return result == null ? null : result.toString();
    }

    private void openScriptFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path scriptPath = fileChooser.getSelectedFile().toPath();
        executeScript(scriptPath);
    }

    private void executeScript(Path scriptPath) {
        executeScriptButton.setEnabled(false);

        new SwingWorker<ScriptExecutionResult, Void>() {
            @Override
            protected ScriptExecutionResult doInBackground() throws GuiClientException {
                Set<Path> executingScripts = new HashSet<>();
                Deque<Path> scriptPathStack = new ArrayDeque<>();
                StringBuilder report = new StringBuilder();
                try {
                    ScriptFileResult result = executeScriptFile(
                            scriptPath,
                            executingScripts,
                            scriptPathStack,
                            report
                    );
                    return new ScriptExecutionResult(report.toString(), result.collectionChanged());
                } catch (GuiClientException e) {
                    throw new ScriptExecutionException(e.getMessage(), e, report.toString());
                }
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                executeScriptButton.setEnabled(true);
                handleScriptExecutionResult(this);
            }
        }.execute();
    }

    private ScriptFileResult executeScriptFile(Path scriptPath,
                                               Set<Path> executingScripts,
                                               Deque<Path> scriptPathStack,
                                               StringBuilder report) throws GuiClientException {
        Path normalizedPath = normalizeScriptPath(scriptPath);
        if (executingScripts.contains(normalizedPath)) {
            Path parentScript = scriptPathStack.peek();
            throw new GuiClientException(messages.format("error.scriptRecursive",
                    parentScript + " -> " + normalizedPath));
        }

        executingScripts.add(normalizedPath);
        scriptPathStack.push(normalizedPath);
        report.append(messages.format("message.script", normalizedPath)).append(System.lineSeparator());

        boolean collectionChanged = false;
        boolean stopped = false;
        try (Scanner scanner = new Scanner(Files.newBufferedReader(normalizedPath, StandardCharsets.UTF_8))) {
            ClientCommandParser parser = new ClientCommandParser(new ClientVehicleReader(scanner, false));
            int lineNumber = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                ScriptLineResult lineResult = executeScriptLine(
                        line,
                        normalizedPath,
                        lineNumber,
                        parser,
                        executingScripts,
                        scriptPathStack
                );
                report.append(lineResult.text()).append(System.lineSeparator());
                collectionChanged = collectionChanged || lineResult.collectionChanged();
                if (lineResult.stopScript()) {
                    stopped = true;
                    break;
                }
            }
            report.append(messages.format("message.scriptFinished", normalizedPath)).append(System.lineSeparator());
            return new ScriptFileResult(collectionChanged, stopped);
        } catch (IOException e) {
            throw new GuiClientException(messages.format("error.scriptRead", normalizedPath), e);
        } catch (IllegalArgumentException e) {
            throw new GuiClientException(messages.format("error.scriptGeneral", normalizedPath, e.getMessage()), e);
        } finally {
            executingScripts.remove(normalizedPath);
            scriptPathStack.pop();
        }
    }

    private Path normalizeScriptPath(Path scriptPath) throws GuiClientException {
        try {
            return scriptPath.toAbsolutePath().normalize().toRealPath();
        } catch (IOException e) {
            throw new GuiClientException(messages.get("error.scriptPath"), e);
        }
    }

    private ScriptLineResult executeScriptLine(String line,
                                               Path currentScript,
                                               int lineNumber,
                                               ClientCommandParser parser,
                                               Set<Path> executingScripts,
                                               Deque<Path> scriptPathStack) throws GuiClientException {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase(Locale.ROOT);
        String prefix = currentScript.getFileName() + ":" + lineNumber + " > " + line;

        if (command.equals("exit")) {
            return new ScriptLineResult(messages.format("message.scriptStopped",
                    prefix + System.lineSeparator()), false, true);
        }
        if (command.equals("logout") || command.equals("login") || command.equals("register") || command.equals("whoami")) {
            return new ScriptLineResult(messages.format("error.scriptUnavailable",
                    prefix + System.lineSeparator()), false, false);
        }
        if (command.equals("execute_script")) {
            if (parts.length != 2) {
                throw new GuiClientException(prefix + System.lineSeparator()
                        + messages.get("error.scriptArgument"));
            }
            Path nestedPath = currentScript.getParent() == null
                    ? Path.of(parts[1])
                    : currentScript.getParent().resolve(parts[1]);
            StringBuilder nestedReport = new StringBuilder(prefix).append(System.lineSeparator());
            ScriptFileResult nestedResult = executeScriptFile(nestedPath, executingScripts, scriptPathStack, nestedReport);
            return new ScriptLineResult(
                    nestedReport.toString(),
                    nestedResult.collectionChanged(),
                    nestedResult.stopped()
            );
        }

        CommandRequest request;
        try {
            request = parser.parse(line);
        } catch (IllegalArgumentException e) {
            throw new GuiClientException(prefix + System.lineSeparator() + e.getMessage(), e);
        }

        CommandResponse response = clientService.send(request);
        boolean collectionChanged = response.isSuccess() && isCollectionChangingRequest(request);
        String text = prefix + System.lineSeparator() + formatResponseText(response);
        return new ScriptLineResult(text, collectionChanged, false);
    }

    private boolean isCollectionChangingRequest(CommandRequest request) {
        return request instanceof AddRequest
                || request instanceof UpdateRequest
                || request instanceof RemoveByIdRequest
                || request instanceof ClearRequest
                || request instanceof AddIfMaxRequest
                || request instanceof RemoveGreaterRequest;
    }

    private void handleScriptExecutionResult(SwingWorker<ScriptExecutionResult, Void> worker) {
        try {
            ScriptExecutionResult result = worker.get();
            showTextDialog(messages.get("dialog.scriptResult.title"), result.report().isBlank()
                    ? messages.get("message.scriptDone")
                    : result.report());
            if (result.collectionChanged()) {
                refreshCollection();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            GuiErrorDialogs.showClientError(this, new GuiClientException(messages.get("error.scriptInterrupted"), e), messages);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ScriptExecutionException scriptExecutionException) {
                showScriptExecutionError(scriptExecutionException);
                return;
            }
            if (cause instanceof GuiClientException guiClientException) {
                GuiErrorDialogs.showClientError(this, guiClientException, messages);
            } else {
                GuiErrorDialogs.showClientError(this, new GuiClientException(messages.get("error.scriptFailed"), cause), messages);
            }
        }
    }

    private void showScriptExecutionError(ScriptExecutionException e) {
        String report = e.report().isBlank()
                ? e.getMessage()
                : e.report() + System.lineSeparator() + messages.format("message.errorPrefix", e.getMessage());
        showTextDialog(messages.get("dialog.scriptError.title"), report);
    }

    private void logout() {
        Locale selectedLocale = getSelectedLocale();
        session.clear();
        LoginFrame loginFrame = new LoginFrame(clientService, session, selectedLocale);
        loginFrame.setVisible(true);
        dispose();
    }

    private void exitApplication() {
        dispose();
        System.exit(0);
    }

    private void refreshCollection() {
        refreshCollection(true);
    }

    private void refreshCollection(boolean showErrors) {
        if (collectionRefreshInProgress) {
            return;
        }
        collectionRefreshInProgress = true;
        setCollectionLoading(true);

        new SwingWorker<CommandResponse, Void>() {
            @Override
            protected CommandResponse doInBackground() throws GuiClientException {
                return clientService.send(new ShowRequest());
            }

            @Override
            protected void done() {
                collectionRefreshInProgress = false;
                setCollectionLoading(false);
                if (!isDisplayable()) {
                    return;
                }
                handleCollectionResponse(this, showErrors);
            }
        }.execute();
    }

    private void setCollectionLoading(boolean loading) {
        if (isDisplayable()) {
            showButton.setEnabled(!loading);
        }
    }

    private void handleCollectionResponse(SwingWorker<CommandResponse, Void> worker, boolean showErrors) {
        try {
            CommandResponse response = worker.get();
            if (!response.isSuccess()) {
                if (showErrors) {
                    GuiErrorDialogs.showServerErrorIfNeeded(this, response, messages);
                }
                return;
            }
            updateAllVehicles(extractVehicles(response));
        } catch (GuiClientException e) {
            if (showErrors) {
                GuiErrorDialogs.showClientError(this, e, messages);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (showErrors) {
                GuiErrorDialogs.showClientError(this, new GuiClientException(messages.get("error.collectionInterrupted"), e), messages);
            }
        } catch (ExecutionException e) {
            if (!showErrors) {
                return;
            }
            Throwable cause = e.getCause();
            if (cause instanceof GuiClientException guiClientException) {
                GuiErrorDialogs.showClientError(this, guiClientException, messages);
            } else {
                GuiErrorDialogs.showClientError(this, new GuiClientException(messages.get("error.collectionFailed"), cause), messages);
            }
        }
    }

    private List<Vehicle> extractVehicles(CommandResponse response) throws GuiClientException {
        Object result = response.getResult();
        if (result instanceof Collection<?> collection) {
            List<Vehicle> vehicles = collection.stream()
                    .filter(Vehicle.class::isInstance)
                    .map(Vehicle.class::cast)
                    .toList();
            if (vehicles.size() == collection.size()) {
                return vehicles;
            }
        }
        throw new GuiClientException(messages.get("error.collectionType"));
    }

    private void updateAllVehicles(List<Vehicle> vehicles) {
        Set<Integer> animatedVehicleIds = findAppearedOrChangedVehicleIds(vehicles);
        allVehicles.clear();
        allVehicles.addAll(vehicles);
        applyTableOptions();
        startVisualizationAnimations(animatedVehicleIds);
    }

    private Set<Integer> findAppearedOrChangedVehicleIds(List<Vehicle> vehicles) {
        Map<Integer, Vehicle> previousVehiclesById = new HashMap<>();
        for (Vehicle vehicle : allVehicles) {
            if (vehicle.getId() != null) {
                previousVehiclesById.put(vehicle.getId(), vehicle);
            }
        }

        Set<Integer> changedVehicleIds = new HashSet<>();
        for (Vehicle vehicle : vehicles) {
            Integer id = vehicle.getId();
            if (id == null) {
                continue;
            }
            Vehicle previousVehicle = previousVehiclesById.get(id);
            if (previousVehicle == null || isVehicleChanged(previousVehicle, vehicle)) {
                changedVehicleIds.add(id);
            }
        }
        return changedVehicleIds;
    }

    private boolean isVehicleChanged(Vehicle previousVehicle, Vehicle currentVehicle) {
        return !Objects.equals(previousVehicle.getName(), currentVehicle.getName())
                || !Objects.equals(previousVehicle.getCoordinates().getX(), currentVehicle.getCoordinates().getX())
                || Double.compare(previousVehicle.getCoordinates().getY(), currentVehicle.getCoordinates().getY()) != 0
                || !Objects.equals(previousVehicle.getCreationDate(), currentVehicle.getCreationDate())
                || !Objects.equals(previousVehicle.getEnginePower(), currentVehicle.getEnginePower())
                || previousVehicle.getNumberOfWheels() != currentVehicle.getNumberOfWheels()
                || Float.compare(previousVehicle.getCapacity(), currentVehicle.getCapacity()) != 0
                || previousVehicle.getFuelType() != currentVehicle.getFuelType()
                || !Objects.equals(previousVehicle.getOwnerLogin(), currentVehicle.getOwnerLogin());
    }

    private void startVisualizationAnimations(Set<Integer> vehicleIds) {
        if (vehicleCanvasPanel == null || vehicleIds.isEmpty()) {
            return;
        }

        Set<Integer> visibleVehicleIds = new HashSet<>();
        for (Vehicle vehicle : displayedVehicles) {
            Integer vehicleId = vehicle.getId();
            if (vehicleId != null && vehicleIds.contains(vehicleId)) {
                visibleVehicleIds.add(vehicleId);
            }
        }

        if (!visibleVehicleIds.isEmpty()) {
            vehicleCanvasPanel.startAppearanceAnimation(visibleVehicleIds);
        }
    }

    private void applyTableOptions() {
        FilterColumn filterColumn = (FilterColumn) filterColumnComboBox.getSelectedItem();
        SortColumn sortColumn = (SortColumn) sortColumnComboBox.getSelectedItem();
        SortDirection sortDirection = (SortDirection) sortDirectionComboBox.getSelectedItem();
        String filterText = filterField.getText().trim();

        String normalizedFilter = filterText.toLowerCase(Locale.ROOT);
        Locale locale = getSelectedLocale();
        Predicate<Vehicle> predicate = vehicle -> filterColumn == null
                || normalizedFilter.isBlank()
                || filterColumn.valueOf(vehicle, locale).toLowerCase(Locale.ROOT).contains(normalizedFilter);
        Comparator<Vehicle> comparator = sortColumn == null ? null : sortColumn.comparator();
        if (comparator != null && sortDirection == SortDirection.DESCENDING) {
            comparator = comparator.reversed();
        }

        var tableStream = allVehicles.stream().filter(predicate);
        List<Vehicle> tableVehicles = comparator == null
                ? tableStream.toList()
                : tableStream.sorted(comparator).toList();
        updateDisplayedVehicles(tableVehicles);
    }

    private void resetTableOptions() {
        filterField.setText("");
        filterColumnComboBox.setSelectedIndex(0);
        sortColumnComboBox.setSelectedItem(SortColumn.NONE);
        sortDirectionComboBox.setSelectedItem(SortDirection.ASCENDING);
        applyTableOptions();
    }

    private void updateDisplayedVehicles(List<Vehicle> vehicles) {
        Integer selectedVehicleId = selectedVehicle == null ? null : selectedVehicle.getId();
        boolean previousSuppressTableSelectionEvents = suppressTableSelectionEvents;
        suppressTableSelectionEvents = true;
        try {
            displayedVehicles.clear();
            displayedVehicles.addAll(vehicles);
            updateTable();
            restoreSelectedVehicle(selectedVehicleId);
            updateVisualization();
        } finally {
            suppressTableSelectionEvents = previousSuppressTableSelectionEvents;
        }
    }

    private void restoreSelectedVehicle(Integer selectedVehicleId) {
        selectedVehicle = null;
        if (vehicleTable != null) {
            vehicleTable.clearSelection();
        }

        if (selectedVehicleId != null) {
            for (Vehicle vehicle : displayedVehicles) {
                if (Objects.equals(selectedVehicleId, vehicle.getId())) {
                    selectedVehicle = vehicle;
                    selectTableRowForVehicle(vehicle);
                    updateSelectedObjectPanel(vehicle);
                    updateSelectionControls();
                    return;
                }
            }
        }

        clearSelectedObjectPanel();
        updateSelectionControls();
    }

    private void updateTable() {
        vehicleTableModel.setVehicles(displayedVehicles);
    }

    private void updateVisualization() {
        if (vehicleCanvasPanel != null) {
            vehicleCanvasPanel.setVehicles(displayedVehicles);
            vehicleCanvasPanel.setSelectedVehicle(selectedVehicle);
        }
    }

    private void clearSelectedObjectPanel() {
        String empty = messages.get("selected.empty");
        selectedIdLabel.setText(messages.format("selected.id", empty));
        selectedNameLabel.setText(messages.format("selected.name", empty));
        selectedOwnerLabel.setText(messages.format("selected.owner", empty));
        selectedCoordinatesLabel.setText(messages.format("selected.coordinates", empty));
        selectedEnginePowerLabel.setText(messages.format("selected.enginePower", empty));
        selectedWheelsLabel.setText(messages.format("selected.wheels", empty));
        selectedCapacityLabel.setText(messages.format("selected.capacity", empty));
        selectedFuelTypeLabel.setText(messages.format("selected.fuelType", empty));
    }

    private void selectVehicleFromTable() {
        int selectedRow = vehicleTable.getSelectedRow();
        if (selectedRow < 0) {
            selectedVehicle = null;
            clearSelectedObjectPanel();
            updateSelectionControls();
            updateVisualization();
            return;
        }

        int modelRow = vehicleTable.convertRowIndexToModel(selectedRow);
        selectedVehicle = vehicleTableModel.getVehicleAt(modelRow);
        updateSelectedObjectPanel(selectedVehicle);
        updateSelectionControls();
        updateVisualization();
    }

    private void selectVehicleFromVisualization(Vehicle vehicle) {
        selectedVehicle = vehicle;
        selectTableRowForVehicle(vehicle);
        updateSelectedObjectPanel(selectedVehicle);
        updateSelectionControls();
        if (vehicleCanvasPanel != null) {
            vehicleCanvasPanel.setSelectedVehicle(selectedVehicle);
        }
    }

    private void selectTableRowForVehicle(Vehicle vehicle) {
        if (vehicleTable == null || vehicle == null || vehicle.getId() == null) {
            return;
        }
        for (int row = 0; row < vehicleTableModel.getRowCount(); row++) {
            Vehicle rowVehicle = vehicleTableModel.getVehicleAt(row);
            if (Objects.equals(vehicle.getId(), rowVehicle.getId())) {
                int viewRow = vehicleTable.convertRowIndexToView(row);
                if (viewRow >= 0) {
                    boolean previousSuppressTableSelectionEvents = suppressTableSelectionEvents;
                    suppressTableSelectionEvents = true;
                    try {
                        vehicleTable.setRowSelectionInterval(viewRow, viewRow);
                        vehicleTable.scrollRectToVisible(vehicleTable.getCellRect(viewRow, 0, true));
                    } finally {
                        suppressTableSelectionEvents = previousSuppressTableSelectionEvents;
                    }
                }
                return;
            }
        }
    }

    private void updateSelectedObjectPanel(Vehicle vehicle) {
        selectedIdLabel.setText(messages.format("selected.id", vehicle.getId()));
        selectedNameLabel.setText(messages.format("selected.name", vehicle.getName()));
        selectedOwnerLabel.setText(messages.format("selected.owner", vehicle.getOwnerLogin()));
        selectedCoordinatesLabel.setText(messages.format("selected.coordinates", vehicle.getCoordinates()));
        selectedEnginePowerLabel.setText(messages.format("selected.enginePower", formatNullableNumber(vehicle.getEnginePower())));
        selectedWheelsLabel.setText(messages.format("selected.wheels", vehicle.getNumberOfWheels()));
        selectedCapacityLabel.setText(messages.format("selected.capacity", formatNumber(vehicle.getCapacity())));
        selectedFuelTypeLabel.setText(messages.format("selected.fuelType", vehicle.getFuelType()));
    }

    private String formatNullableNumber(Number value) {
        return value == null ? "" : formatNumber(value);
    }

    private String formatNumber(Number value) {
        return NumberFormat.getNumberInstance(getSelectedLocale()).format(value);
    }

    private void updateSelectionControls() {
        boolean ownVehicle = isOwnSelectedVehicle();
        editButton.setEnabled(ownVehicle);
        deleteButton.setEnabled(ownVehicle);
    }

    private boolean isOwnSelectedVehicle() {
        return isOwnVehicle(selectedVehicle);
    }

    private boolean isOwnVehicle(Vehicle vehicle) {
        return vehicle != null
                && vehicle.getOwnerLogin() != null
                && vehicle.getOwnerLogin().equals(session.getLogin());
    }

    private Locale getSelectedLocale() {
        LocaleOption selected = (LocaleOption) languageComboBox.getSelectedItem();
        return selected == null ? Locale.forLanguageTag("ru") : selected.locale;
    }

    private enum OtherCommand {
        ADD_IF_MAX("other.addIfMax"),
        REMOVE_GREATER("other.removeGreater"),
        FILTER_BY_FUEL_TYPE("other.filterByFuelType"),
        FILTER_STARTS_WITH_NAME("other.filterStartsWithName"),
        COUNT_BY_NUMBER_OF_WHEELS("other.countByWheels");

        private final String messageKey;

        OtherCommand(String messageKey) {
            this.messageKey = messageKey;
        }

        String displayName(GuiMessages messages) {
            return messages.get(messageKey);
        }

        @Override
        public String toString() {
            return GuiMessages.forLocale(Locale.forLanguageTag("ru")).get(messageKey);
        }
    }

    private enum FilterColumn {
        ID("column.id") {
            @Override
            String valueOf(Vehicle vehicle, Locale locale) {
                return String.valueOf(vehicle.getId());
            }
        },
        NAME("column.name") {
            @Override
            String valueOf(Vehicle vehicle, Locale locale) {
                return vehicle.getName();
            }
        },
        X("column.x") {
            @Override
            String valueOf(Vehicle vehicle, Locale locale) {
                return String.valueOf(vehicle.getCoordinates().getX());
            }
        },
        Y("column.y") {
            @Override
            String valueOf(Vehicle vehicle, Locale locale) {
                return String.valueOf(vehicle.getCoordinates().getY());
            }
        },
        CREATION_DATE("column.creationDate") {
            @Override
            String valueOf(Vehicle vehicle, Locale locale) {
                return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                        .withLocale(locale)
                        .format(vehicle.getCreationDate());
            }
        },
        ENGINE_POWER("column.enginePower") {
            @Override
            String valueOf(Vehicle vehicle, Locale locale) {
                Double enginePower = vehicle.getEnginePower();
                return enginePower == null ? "" : NumberFormat.getNumberInstance(locale).format(enginePower);
            }
        },
        NUMBER_OF_WHEELS("column.wheels") {
            @Override
            String valueOf(Vehicle vehicle, Locale locale) {
                return String.valueOf(vehicle.getNumberOfWheels());
            }
        },
        CAPACITY("column.capacity") {
            @Override
            String valueOf(Vehicle vehicle, Locale locale) {
                return NumberFormat.getNumberInstance(locale).format(vehicle.getCapacity());
            }
        },
        FUEL_TYPE("column.fuelType") {
            @Override
            String valueOf(Vehicle vehicle, Locale locale) {
                FuelType fuelType = vehicle.getFuelType();
                return fuelType == null ? "" : fuelType.name();
            }
        },
        OWNER("column.owner") {
            @Override
            String valueOf(Vehicle vehicle, Locale locale) {
                String ownerLogin = vehicle.getOwnerLogin();
                return ownerLogin == null ? "" : ownerLogin;
            }
        };

        private final String messageKey;

        FilterColumn(String messageKey) {
            this.messageKey = messageKey;
        }

        abstract String valueOf(Vehicle vehicle, Locale locale);

        String displayName(GuiMessages messages) {
            return messages.get(messageKey);
        }

        @Override
        public String toString() {
            return GuiMessages.forLocale(Locale.forLanguageTag("ru")).get(messageKey);
        }
    }

    private enum SortColumn {
        NONE("sort.none", null),
        ID("column.id", Comparator.comparing(Vehicle::getId)),
        NAME("column.name", Comparator.comparing(Vehicle::getName, String.CASE_INSENSITIVE_ORDER)),
        X("column.x", Comparator.comparing(vehicle -> vehicle.getCoordinates().getX())),
        Y("column.y", Comparator.comparingDouble(vehicle -> vehicle.getCoordinates().getY())),
        CREATION_DATE("column.creationDate", Comparator.comparing(Vehicle::getCreationDate)),
        ENGINE_POWER("column.enginePower", Comparator.comparing(
                Vehicle::getEnginePower,
                Comparator.nullsFirst(Double::compareTo)
        )),
        NUMBER_OF_WHEELS("column.wheels", Comparator.comparingInt(Vehicle::getNumberOfWheels)),
        CAPACITY("column.capacity", Comparator.comparingDouble(Vehicle::getCapacity)),
        FUEL_TYPE("column.fuelType", Comparator.comparing(Vehicle::getFuelType)),
        OWNER("column.owner", Comparator.comparing(
                Vehicle::getOwnerLogin,
                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)
        ));

        private final String messageKey;
        private final Comparator<Vehicle> comparator;

        SortColumn(String messageKey, Comparator<Vehicle> comparator) {
            this.messageKey = messageKey;
            this.comparator = comparator;
        }

        Comparator<Vehicle> comparator() {
            return comparator;
        }

        String displayName(GuiMessages messages) {
            return messages.get(messageKey);
        }

        @Override
        public String toString() {
            return GuiMessages.forLocale(Locale.forLanguageTag("ru")).get(messageKey);
        }
    }

    private enum SortDirection {
        ASCENDING("sort.ascending"),
        DESCENDING("sort.descending");

        private final String messageKey;

        SortDirection(String messageKey) {
            this.messageKey = messageKey;
        }

        String displayName(GuiMessages messages) {
            return messages.get(messageKey);
        }

        @Override
        public String toString() {
            return GuiMessages.forLocale(Locale.forLanguageTag("ru")).get(messageKey);
        }
    }

    private record ScriptExecutionResult(String report, boolean collectionChanged) {
    }

    private record ScriptFileResult(boolean collectionChanged, boolean stopped) {
    }

    private record ScriptLineResult(String text, boolean collectionChanged, boolean stopScript) {
    }

    private static final class ScriptExecutionException extends GuiClientException {
        private final String report;

        private ScriptExecutionException(String message, Throwable cause, String report) {
            super(message, cause);
            this.report = report;
        }

        private String report() {
            return report;
        }
    }

    private enum LocaleOption {
        RU("locale.ru", Locale.forLanguageTag("ru")),
        SL("locale.sl", Locale.forLanguageTag("sl")),
        FR("locale.fr", Locale.forLanguageTag("fr")),
        ES_SV("locale.es_sv", Locale.forLanguageTag("es-SV"));

        private final String displayNameKey;
        private final Locale locale;

        LocaleOption(String displayNameKey, Locale locale) {
            this.displayNameKey = displayNameKey;
            this.locale = locale;
        }

        private String displayName(GuiMessages messages) {
            return messages.get(displayNameKey);
        }

        private static LocaleOption fromLocale(Locale locale) {
            for (LocaleOption option : values()) {
                if (option.locale.toLanguageTag().equals(locale.toLanguageTag())) {
                    return option;
                }
            }
            return RU;
        }

        @Override
        public String toString() {
            return displayNameKey;
        }
    }
}
