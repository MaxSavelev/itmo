package parser;

import data.Coordinates;
import data.FuelType;
import data.Vehicle;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.function.Function;

/**
 * Парсер коллекции {@link Vehicle}.
 * <p>
 * Читает текст из {@link Reader}
 * и превращает его в список объектов {@link Vehicle}.
 * </p>
 *
 * @author makssavelev
 * @version 1.0
 */
public class VehicleCollectionParser implements Parser<List<Vehicle>> {
    private boolean inQuotes = false;
    private String buffer = "";
    private static final char EOF = '\0'; //спец символ - конец файла
    private final Reader reader;
    private static final Set<Character> stopSet = Set.of(',', ':', EOF, '[', ']', '{', '}');

    /**
     * Проверяет, нужно ли читать еще символы в буфер.
     *
     * @return {@code true}, если чтение нужно продолжать, иначе {@code false}
     */
    private boolean needChar() {
        return buffer.isEmpty() || !stopSet.contains(buffer.charAt(buffer.length() - 1)) || inQuotes;
    }

    /**
     * Считывает один символ и добавляет его в буфер.
     * <p>
     * Пробелы вне строк пропускаются.
     * Если файл закончился, в буфер добавляется символ конца файла.
     * </p>
     *
     * @throws IOException если произошла ошибка чтения
     */
    private void addChar() throws IOException {
        int read = reader.read();
        if (read == -1) {
            buffer += EOF;
        } else {
            char c = (char) read;
            if (Character.isWhitespace(c) && !inQuotes) {
                return;
            }
            buffer += c;
            if (c == '"' && !(buffer.length() > 2 && buffer.charAt(buffer.length() - 2) == '\\')) inQuotes = !inQuotes;
        }
    }

    /**
     * Возвращает следующую лексему из буфера или входного потока.
     *
     * @return следующая лексема
     * @throws IOException если произошла ошибка чтения
     */
    private String take() throws IOException {
        String res;
        if (!buffer.isEmpty() && stopSet.contains(buffer.charAt(0))) { //buffer.charAt(0)) первый символ
            res = buffer.substring(0, 1); // 0 включительно, 1 не включительно
            buffer = buffer.substring(1); // c 1 и до конца
            return res;
        }
        do addChar(); while (needChar());
        if (buffer.length() > 1) {
            res = buffer.substring(0, buffer.length() - 1);
            buffer = buffer.substring(buffer.length() - 1);
        } else {
            res = buffer;
            buffer = "";
        }
        return res;

    }

    /**
     * Создает парсер.
     *
     * @param reader источник данных, из которого читается текст
     */
    public VehicleCollectionParser(Reader reader) {
        this.reader = reader;
    }

    /**
     * Проверяет, что следующая лексема совпадает с ожидаемой.
     *
     * @param s строка, которую ожидает парсер
     * @throws IOException если в данных встретилось не то, что ожидалось
     */
    private void assertTake(String s) throws IOException {
        if (!s.equals(take())) {
            throw new IOException("Не верный формат json файла. ");
        }
    }

    /**
     * Убирает внешние кавычки у строки.
     *
     * @param s строка в кавычках
     * @return строка без крайних кавычек
     */
    private static String removeQuotes(String s) {
        return s.substring(1, s.length() - 1);
    }

    /**
     * Проверяет, что нужное поле есть в наборе полей.
     *
     * @param fields все считанные поля объекта
     * @param fieldName имя поля, которое нужно найти
     * @return значение найденного поля
     * @throws IOException если поле отсутствует
     */
    private static String requireField(Map<String, String> fields, String fieldName) throws IOException {
        String value = fields.get(fieldName);
        if (value == null) {
            throw new IOException("Не верный формат json файла. Отсутствует поле " + fieldName);
        }
        return value;
    }

    /**
     * Преобразует значение поля в нужный тип.
     *
     * @param type тип, в который нужно преобразовать значение
     * @param fields все считанные поля объекта
     * @param fieldName имя поля, которое нужно преобразовать
     * @return значение поля нужного типа
     * @throws IOException если значение поля некорректно
     */
    private static Object parseField(Class<?> type, Map<String, String> fields, String fieldName) throws IOException {
        try {
            return valueParsers.get(type).apply(requireField(fields, fieldName));
        } catch (RuntimeException e) {
            throw new IOException("Не верный формат json файла. Некорректное значение поля " + fieldName);
        }
    }

    /**
     * Преобразует поле в нужный тип и проверяет, что оно не равно {@code null}.
     *
     * @param type тип, в который нужно преобразовать значение
     * @param fields все считанные поля объекта
     * @param fieldName имя поля, которое нужно прочитать
     * @return значение поля нужного типа
     * @throws IOException если поле равно {@code null} или заполнено неверно
     */
    private static Object parseNonNullField(Class<?> type, Map<String, String> fields, String fieldName) throws IOException {
        Object value = parseField(type, fields, fieldName);
        if (value == null) {
            throw new IOException("Не верный формат json файла. Поле " + fieldName + " не может быть null");
        }
        return value;
    }

    /**
     * Разбирает один объект {@link Vehicle}.
     *
     * @return созданный объект транспортного средства
     * @throws IOException если формат входных данных неверный
     */
    private Vehicle parseVehicle() throws IOException {
        assertTake("{");
        Map<String, String> fields = new HashMap<>();
        Coordinates coordinate = null;
        do {
            String key = removeQuotes(take());
            assertTake(":");
            if (key.equals("coordinates")) {
                coordinate = parseCoordinates();
            } else fields.put(key, take());
        }
        while (delimiterAndEndWith(",", "}"));
        if (coordinate == null) {
            throw new IOException("Не верный формат json файла. Отсутствует поле coordinates");
        }
        return new Vehicle((String) parseNonNullField(String.class, fields, "name"),
                coordinate,
                (Double) parseField(Double.class, fields, "enginePower"),
                (Integer) parseNonNullField(Integer.class, fields, "numberOfWheels"),
                (Float) parseNonNullField(Float.class, fields, "capacity"),
                (FuelType) parseNonNullField(FuelType.class, fields, "fuelType")
        );
        //...
        //assertTake("}");
    }

    /**
     * Проверяет, что между элементами стоит правильный разделитель или конец блока.
     *
     * @param delimiter символ-разделитель, который ожидается между элементами
     * @param end символ, который означает конец блока
     * @return {@code true}, если прочитан разделитель;
     *         {@code false}, если прочитан конец блока
     * @throws IOException если встретился неожиданный символ
     */
    private boolean delimiterAndEndWith(String delimiter, String end) throws IOException {
        String t = take();
        if (t.equals(delimiter)) return true;
        if (!t.equals(end)) throw new IOException("Не верный формат json файла. ");
        return false;
    }

    /**
     * Разбирает массив объектов {@link Vehicle}.
     *
     * @return список транспортных средств
     * @throws IOException если произошла ошибка чтения или формат данных неверный
     */
    private List<Vehicle> parseArray() throws IOException {
        List<Vehicle> res = new ArrayList<>();
        assertTake("[");
        do res.add(parseVehicle());
        while (delimiterAndEndWith(",", "]"));
        return res;
    }

    /**
     * Преобразует строку в нужный тип и поддерживает значение {@code null}.
     *
     * @param s строка, которую нужно преобразовать
     * @param parser функция, которая умеет преобразовывать строку
     * @return {@code null}, если строка равна {@code "null"},
     *         иначе результат работы функции преобразования
     */
    private static Object parseWithNull(String s, Function<String, Object> parser) {
        if (s == null) {
            return null;
        }
        return s.equals("null") ? null : parser.apply(s);
    }

    private static final Map<Class<?>, Function<String, Object>> valueParsers = Map.of(
            String.class, s -> parseWithNull(s, VehicleCollectionParser::removeQuotes), // remove quotes
            Double.class, s -> parseWithNull(s, Double::parseDouble),
            Integer.class, s -> parseWithNull(s, Integer::parseInt),
            Long.class, s -> parseWithNull(s, Long::parseLong),
            Float.class, s -> parseWithNull(s, Float::parseFloat),
            FuelType.class, s -> parseWithNull(s, s1 -> FuelType.valueOf(removeQuotes(s1)))
    );

    /**
     * Разбирает объект координат {@link Coordinates}.
     *
     * @return созданный объект координат
     * @throws IOException если координаты записаны в неверном формате
     */
    private Coordinates parseCoordinates() throws IOException {
        assertTake("{");
        Map<String, String> fields = new HashMap<>();
        do {
            String key = removeQuotes(take());
            assertTake(":");
            fields.put(key, take());
        }
        while (delimiterAndEndWith(",", "}"));
        return new Coordinates((Long) parseNonNullField(Long.class, fields, "x"),
                (Double) parseNonNullField(Double.class, fields, "y")
        );
    }

    /**
     * Запускает разбор всей коллекции.
     *
     * @return список объектов, считанных из входного потока
     * @throws IOException если при чтении или разборе произошла ошибка
     */
    @Override
    public List<Vehicle> parse() throws IOException {
        return parseArray();
    }
}
