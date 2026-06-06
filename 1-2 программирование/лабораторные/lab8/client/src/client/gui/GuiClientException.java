package client.gui;

/**
 * Ошибка сетевого слоя GUI-клиента.
 */
public class GuiClientException extends Exception {
    public GuiClientException(String message) {
        super(message);
    }

    public GuiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
