package data;

/**
 * Перечисление типов топлива для транспортного средства.
 * <p>
 * Нужен, чтобы хранить не произвольную строку,
 * а одно из заранее известных значений.
 * </p>
 *
 * @author makssavelev
 * @version 1.0
 */
public enum FuelType implements Jsonify {
    /** Бензин. */
    GASOLINE,
    /** Электричество. */
    ELECTRICITY,
    /** Дизельное топливо. */
    DIESEL,
    /** Мускульная сила человека. */
    MANPOWER,
    /** Ядерное топливо. */
    NUCLEAR;

    /**
     * Возвращает тип топлива в виде JSON-строки.
     *
     * @return строка JSON с названием типа топлива
     */
    @Override
    public String toJson() {
        return "\""+this+"\"";
    }
}
