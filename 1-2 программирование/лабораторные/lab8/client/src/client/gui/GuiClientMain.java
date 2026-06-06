package client.gui;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Точка входа GUI-клиента.
 */
public final class GuiClientMain {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5555;
    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;
    private static final GuiMessages MESSAGES = GuiMessages.forLocale(java.util.Locale.forLanguageTag("ru"));

    private GuiClientMain() {
    }

    /**
     * Запускает Swing-клиент.
     *
     * @param args аргументы запуска: пусто, port или host port
     */
    public static void main(String[] args) {
        ServerAddress address;
        try {
            address = parseAddress(args);
        } catch (IllegalArgumentException e) {
            showStartupError(e.getMessage());
            return;
        }

        ServerAddress serverAddress = address;
        SwingUtilities.invokeLater(() -> showStartupFrame(serverAddress));
    }

    private static void showStartupFrame(ServerAddress address) {
        setSystemLookAndFeel();

        GuiSession session = new GuiSession();
        GuiClientService clientService = new GuiClientService(address.host, address.port, session);
        LoginFrame frame = new LoginFrame(clientService, session);
        frame.setVisible(true);
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // Если системный стиль недоступен, Swing использует стандартный.
        }
    }

    private static ServerAddress parseAddress(String[] args) {
        if (args.length == 0) {
            return new ServerAddress(DEFAULT_HOST, DEFAULT_PORT);
        }
        if (args.length == 1) {
            return new ServerAddress(DEFAULT_HOST, parsePort(args[0]));
        }
        if (args.length == 2) {
            return new ServerAddress(args[0], parsePort(args[1]));
        }
        throw new IllegalArgumentException(MESSAGES.get("error.launchUsage"));
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < MIN_PORT || port > MAX_PORT) {
                throw new NumberFormatException();
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MESSAGES.format("error.portRange", MIN_PORT, MAX_PORT));
        }
    }

    private static void showStartupError(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                null,
                message,
                MESSAGES.get("error.launchTitle"),
                JOptionPane.ERROR_MESSAGE
        ));
    }

    private static final class ServerAddress {
        private final String host;
        private final int port;

        private ServerAddress(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}
