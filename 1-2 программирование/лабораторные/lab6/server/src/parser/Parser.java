package parser;

import java.io.IOException;

/**
 * Общий интерфейс для парсеров.
 * <p>
 * Парсер читает входные данные и превращает их в готовый результат.
 * </p>
 *
 * @param <T> тип результата, который должен вернуть парсер
 *
 * @author makssavelev
 * @version 1.0
 */
public interface Parser <T>{
    /**
     * Запускает разбор входных данных.
     *
     * @return готовый результат после разбора
     * @throws IOException если при чтении или разборе произошла ошибка
     */
    T parse() throws IOException;
}
