package client.gui;

import common.response.CommandResponse;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.SwingWorker;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * Окно входа и регистрации GUI-клиента.
 */
public final class LoginFrame extends JFrame {
    private final GuiClientService clientService;
    private final GuiSession session;
    private final LocalizationManager localizationManager;
    private final JTextField loginField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JButton registerButton;
    private final JComboBox<LocaleOption> languageComboBox;
    private GuiMessages messages;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel loginLabel;
    private JLabel passwordLabel;
    private JLabel languageLabel;

    public LoginFrame(GuiClientService clientService, GuiSession session) {
        this(clientService, session, Locale.forLanguageTag("ru"));
    }

    public LoginFrame(GuiClientService clientService, GuiSession session, Locale locale) {
        super(GuiMessages.forLocale(Objects.requireNonNull(locale)).get("login.title"));
        this.clientService = Objects.requireNonNull(clientService);
        this.session = Objects.requireNonNull(session);
        localizationManager = new LocalizationManager(locale);
        messages = localizationManager.getMessages();
        this.clientService.setMessages(messages);
        loginField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton();
        registerButton = new JButton();
        languageComboBox = new JComboBox<>(LocaleOption.values());
        languageComboBox.setSelectedItem(LocaleOption.fromLocale(locale));
        configureComboBoxRenderer();

        configureFrame();
        add(createContentPanel(), BorderLayout.CENTER);
        configureActions();
        updateTexts();
        pack();
        setLocationRelativeTo(null);
    }

    public String getLogin() {
        return loginField.getText().trim();
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    public Locale getSelectedLocale() {
        LocaleOption selected = (LocaleOption) languageComboBox.getSelectedItem();
        return selected == null ? Locale.forLanguageTag("ru") : selected.locale;
    }

    private void configureFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(520, 360));
        setLayout(new BorderLayout());
    }

    private JPanel createContentPanel() {
        JPanel root = new JPanel();
        root.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        titleLabel = new JLabel("", SwingConstants.CENTER);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));

        subtitleLabel = new JLabel("", SwingConstants.CENTER);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        root.add(titleLabel);
        root.add(Box.createVerticalStrut(8));
        root.add(subtitleLabel);
        root.add(Box.createVerticalStrut(26));
        root.add(createFormPanel());
        root.add(Box.createVerticalStrut(18));
        root.add(createButtonPanel());

        return root;
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        loginLabel = addFormRow(formPanel, constraints, 0, loginField);
        passwordLabel = addFormRow(formPanel, constraints, 1, passwordField);
        languageLabel = addFormRow(formPanel, constraints, 2, languageComboBox);

        return formPanel;
    }

    private JLabel addFormRow(JPanel panel,
                              GridBagConstraints constraints,
                              int row,
                              java.awt.Component field) {
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 0;
        JLabel label = new JLabel();
        panel.add(label, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(field, constraints);
        return label;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        return buttonPanel;
    }

    private void configureActions() {
        loginButton.addActionListener(event -> authenticate(AuthenticationAction.LOGIN));
        registerButton.addActionListener(event -> authenticate(AuthenticationAction.REGISTER));
        languageComboBox.addActionListener(event -> changeLanguage());
    }

    private void configureComboBoxRenderer() {
        languageComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list,
                                                                   Object value,
                                                                   int index,
                                                                   boolean isSelected,
                                                                   boolean cellHasFocus) {
                java.awt.Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (component instanceof JLabel label && value instanceof LocaleOption option) {
                    label.setText(option.displayName(messages));
                }
                return component;
            }
        });
    }

    private void changeLanguage() {
        localizationManager.setLocale(getSelectedLocale());
        messages = localizationManager.getMessages();
        clientService.setMessages(messages);
        updateTexts();
    }

    private void updateTexts() {
        setTitle(messages.get("login.title"));
        titleLabel.setText(messages.get("app.title"));
        subtitleLabel.setText(messages.get("login.subtitle"));
        loginLabel.setText(messages.get("login.label.login"));
        passwordLabel.setText(messages.get("login.label.password"));
        languageLabel.setText(messages.get("label.language"));
        loginButton.setText(messages.get("button.login"));
        registerButton.setText(messages.get("button.register"));
        languageComboBox.repaint();
        revalidate();
        repaint();
    }

    private void authenticate(AuthenticationAction action) {
        String login = getLogin();
        char[] passwordChars = getPassword();
        String password = new String(passwordChars);
        Arrays.fill(passwordChars, '\0');

        if (login.isBlank()) {
            showInputError(messages.get("error.loginBlank"));
            loginField.requestFocusInWindow();
            return;
        }
        if (password.isBlank()) {
            showInputError(messages.get("error.passwordBlank"));
            passwordField.requestFocusInWindow();
            return;
        }

        setAuthenticationEnabled(false);
        new SwingWorker<CommandResponse, Void>() {
            @Override
            protected CommandResponse doInBackground() throws GuiClientException {
                return switch (action) {
                    case LOGIN -> clientService.login(login, password);
                    case REGISTER -> clientService.register(login, password);
                };
            }

            @Override
            protected void done() {
                setAuthenticationEnabled(true);
                handleAuthenticationResult(this);
            }
        }.execute();
    }

    private void openMainFrame() {
        MainFrame mainFrame = new MainFrame(clientService, session, getSelectedLocale());
        mainFrame.setVisible(true);
        dispose();
    }

    private void setAuthenticationEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
    }

    private void handleAuthenticationResult(SwingWorker<CommandResponse, Void> worker) {
        try {
            CommandResponse response = worker.get();
            if (!GuiErrorDialogs.showServerErrorIfNeeded(this, response, messages)) {
                openMainFrame();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            GuiErrorDialogs.showClientError(this, new GuiClientException(messages.get("error.operationInterrupted"), e), messages);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof GuiClientException guiClientException) {
                GuiErrorDialogs.showClientError(this, guiClientException, messages);
            } else {
                GuiErrorDialogs.showClientError(this, new GuiClientException(messages.get("error.loginFailed"), cause), messages);
            }
        }
    }

    private void showInputError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                messages.get("dialog.inputError.title"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    private enum AuthenticationAction {
        LOGIN,
        REGISTER
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
