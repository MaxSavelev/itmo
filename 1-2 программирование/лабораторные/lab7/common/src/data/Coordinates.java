package data;

import java.util.Objects;
import java.io.Serializable;

/**
 * Класс координат объекта.
 * <p>
 * Хранит две координаты: {@code x} и {@code y}.
 * Эти координаты используются у объекта {@link Vehicle}.
 * </p>
 *
 * @author makssavelev
 * @version 1.0
 */
public class Coordinates implements Comparable<Coordinates>, Serializable {
    private static final long serialVersionUID = 1L;

    private Long x; 
    private double y; 

    /**
     * Создает объект координат.
     *
     * @param x значение координаты по оси X
     * @param y значение координаты по оси Y
     * @throws IllegalArgumentException если {@code x == null} или {@code y <= -372}
     */
    public Coordinates(Long x,double y){
        
        validateX(x);
        validateY(y);

        this.x = x;
        this.y = y;
    }

    /**
     * Возвращает координату X.
     *
     * @return значение X
     */
    public Long getX() {
        return x;
    }

    /**
     * Возвращает координату Y.
     *
     * @return значение Y
     */
    public double getY() {
        return y;
    }

    /**
     * Возвращает координаты в виде строки.
     *
     * @return строка с координатами
     */
    @Override
    public String toString() {
        return "(" + x + "; " + y + ")";
    }

    /**
     * Сравнивает текущие координаты с другими.
     * <p>
     * Сначала сравнивается {@code x}, а если он одинаковый,
     * тогда сравнивается {@code y}.
     * </p>
     *
     * @param o координаты, с которыми нужно сравнить текущий объект
     * @return отрицательное число, если текущий объект меньше;
     *         положительное, если больше;
     *         {@code 0}, если координаты равны
     */
    @Override
    public int compareTo(Coordinates o) {
        if (!Objects.equals(x, o.x)) return x.compareTo(o.x);
        return Double.compare(y, o.y);
    }

    private static void validateX(Long x) {
        if (x == null) {
            throw new IllegalArgumentException("Поле coordinates.x не может быть null.");
        }
    }

    private static void validateY(double y) {
        if (y <= -372) {
            throw new IllegalArgumentException("Поле coordinates.y должно быть больше -372.");
        }
    }

}
