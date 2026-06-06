package auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Хэширует пароли пользователей.
 */
public class PasswordHasher {
    private static final String ALGORITHM = "MD2";

    /**
     * Возвращает MD2-хэш пароля в шестнадцатеричном виде.
     *
     * @param password исходный пароль
     * @return строка с хэшем
     */
    public String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Алгоритм MD2 не поддерживается.", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte currentByte : bytes) {
            result.append(Character.forDigit((currentByte >> 4) & 0xF, 16));
            result.append(Character.forDigit(currentByte & 0xF, 16));
        }
        return result.toString();
    }
}
