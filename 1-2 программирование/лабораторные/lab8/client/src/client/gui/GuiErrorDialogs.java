package client.gui;

import common.response.CommandResponse;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.Locale;

/**
 * Показывает ошибки GUI-клиента.
 */
public final class GuiErrorDialogs {
    private static final GuiMessages DEFAULT_MESSAGES = GuiMessages.forLocale(Locale.forLanguageTag("ru"));

    private GuiErrorDialogs() {
    }

    /**
     * Показывает сетевую ошибку.
     *
     * @param parent родительский компонент
     * @param exception ошибка GUI-клиента
     */
    public static void showClientError(Component parent, GuiClientException exception) {
        showClientError(parent, exception, DEFAULT_MESSAGES);
    }

    public static void showClientError(Component parent, GuiClientException exception, GuiMessages messages) {
        JOptionPane.showMessageDialog(
                parent,
                exception.getMessage(),
                messages.get("dialog.connectionError.title"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Показывает ошибку, которую вернул сервер.
     *
     * @param parent родительский компонент
     * @param response ответ сервера
     * @return {@code true}, если была показана ошибка
     */
    public static boolean showServerErrorIfNeeded(Component parent, CommandResponse response) {
        return showServerErrorIfNeeded(parent, response, DEFAULT_MESSAGES);
    }

    public static boolean showServerErrorIfNeeded(Component parent, CommandResponse response, GuiMessages messages) {
        if (response.isSuccess()) {
            return false;
        }

        String message = response.getMessage();
        if (message == null || message.isBlank()) {
            message = messages.get("error.serverGeneric");
        }

        JOptionPane.showMessageDialog(
                parent,
                message,
                messages.get("dialog.error.title"),
                JOptionPane.ERROR_MESSAGE
        );
        return true;
    }
}
