package client;

import data.FuelType;

import java.util.Locale;

/**
 * Вспомогательный парсер для FuelType на стороне клиента.
 * <p>
 * Поддерживает два варианта ввода: номер из списка и название enum.
 * </p>
 */
final class ClientFuelTypeParser {
    private ClientFuelTypeParser() {
    }

    /**
     * Преобразует строку пользователя в FuelType.
     *
     * @param raw строка из консоли или скрипта
     * @return найденный FuelType или {@code null}, если значение неверное
     */
    static FuelType parse(String raw) {
        
        String value = raw.trim();

        try {
            
            int index = Integer.parseInt(value);
            if (index > 0 && index <= FuelType.values().length) {
                return FuelType.values()[index - 1];
            }
        } catch (NumberFormatException e) {
            
        }

        try {
            
            return FuelType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Возвращает список значений FuelType с номерами для подсказки пользователю.
     *
     * @return строка со значениями enum и их номерами
     */
    static String valuesWithNumbers() {
        StringBuilder builder = new StringBuilder();
        for (FuelType fuelType : FuelType.values()) {
            builder.append(fuelType.ordinal() + 1)
                    .append(" - ")
                    .append(fuelType)
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }
}
