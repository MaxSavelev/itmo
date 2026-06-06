package storage;

/**
 * Исключение для ошибок при работе с коллекцией.
 * <p>
 * Используется, когда во время выполнения команды
 * происходит ошибка хранения, поиска, удаления или сохранения данных.
 * </p>
 *
 * @author makssavelev
 * @version 1.0
 */
public class StorageException extends RuntimeException {
    /**
     * Создает исключение по другой ошибке.
     *
     * @param cause исходная ошибка
     */
    public StorageException(Throwable cause) {
        super(cause);
    }

    /**
     * Создает исключение с текстом ошибки и причиной.
     *
     * @param message текст ошибки
     * @param cause исходная ошибка
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Создает исключение с текстом ошибки.
     *
     * @param message текст ошибки
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Создает исключение без текста ошибки.
     */
    public StorageException() {
    }
}
