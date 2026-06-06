package client;

import data.Coordinates;
import data.FuelType;
import data.Vehicle;

import java.util.Scanner;

/**
 * Читает поля объекта Vehicle из консоли или из файла скрипта.
 * <p>
 * В интерактивном режиме печатает подсказки, а при выполнении скрипта
 * может читать значения без подсказок.
 * </p>
 */
public class ClientVehicleReader {
    private final Scanner scanner;
    private final boolean showPrompts;

    /**
     * Создаёт читатель Vehicle для интерактивного режима.
     *
     * @param scanner источник строк ввода
     */
    public ClientVehicleReader(Scanner scanner) {
        this(scanner, true);
    }

    /**
     * Создаёт читатель Vehicle.
     *
     * @param scanner источник строк ввода
     * @param showPrompts нужно ли печатать подсказки перед вводом
     */
    public ClientVehicleReader(Scanner scanner, boolean showPrompts) {
        this.scanner = scanner;
        this.showPrompts = showPrompts;
    }

    /**
     * Последовательно читает все поля Vehicle и создаёт объект.
     *
     * @return введённый объект Vehicle
     */
    public Vehicle readVehicle() {
        if (showPrompts) {
            System.out.println("Введите данные Vehicle:");
        }

        
        String name = readNonBlankString();
        Coordinates coordinates = readCoordinates();
        Double enginePower = readNullablePositiveDouble();
        int numberOfWheels = readPositiveInt();
        float capacity = readPositiveFloat();
        FuelType fuelType = readFuelType();

        return new Vehicle(name, coordinates, enginePower, numberOfWheels, capacity, fuelType);
    }

    /**
     * Читает координаты Vehicle.
     *
     * @return объект Coordinates
     */
    private Coordinates readCoordinates() {
        Long x = readLong();
        double y = readDoubleGreaterThan();
        return new Coordinates(x, y);
    }

    /**
     * Читает непустую строку.
     *
     * @return строка, которая не является пустой
     */
    private String readNonBlankString() {
        while (true) {
            String value = readLine("name: ");
            if (!value.isBlank()) {
                return value;
            }
            System.out.println("Значение не может быть пустым.");
        }
    }

    /**
     * Читает значение типа Long.
     *
     * @return введённое целое число
     */
    private Long readLong() {
        while (true) {
            String value = readLine("coordinates.x: ");
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                System.out.println("Значение должно быть целым числом.");
            }
        }
    }

    /**
     * Читает координату y, которая должна быть больше -372.
     *
     * @return корректное значение y
     */
    private double readDoubleGreaterThan() {
        while (true) {
            String value = readLine("coordinates.y (> -372): ");
            try {
                double result = Double.parseDouble(value);
                if (result > (double) -372) {
                    return result;
                }
            } catch (NumberFormatException e) {
                
            }
            System.out.println("Значение должно быть числом больше " + (double) -372 + ".");
        }
    }

    /**
     * Читает enginePower.
     * <p>
     * Пустая строка означает {@code null}, иначе значение должно быть больше 0.
     * </p>
     *
     * @return мощность двигателя или {@code null}
     */
    private Double readNullablePositiveDouble() {
        while (true) {
            String value = readLine("enginePower (> 0, Enter = null): ");
            if (value.isBlank()) {
                return null;
            }

            try {
                double result = Double.parseDouble(value);
                if (result > 0) {
                    return result;
                }
            } catch (NumberFormatException e) {
                
            }
            System.out.println("Значение должно быть числом больше 0 или пустой строкой.");
        }
    }

    /**
     * Читает положительное целое число.
     *
     * @return число больше 0
     */
    private int readPositiveInt() {
        while (true) {
            String value = readLine("numberOfWheels (> 0): ");
            try {
                int result = Integer.parseInt(value);
                if (result > 0) {
                    return result;
                }
            } catch (NumberFormatException e) {
                
            }
            System.out.println("Значение должно быть целым числом больше 0.");
        }
    }

    /**
     * Читает положительное число типа float.
     *
     * @return число больше 0
     */
    private float readPositiveFloat() {
        while (true) {
            String value = readLine("capacity (> 0): ");
            try {
                float result = Float.parseFloat(value);
                if (result > 0) {
                    return result;
                }
            } catch (NumberFormatException e) {
                
            }
            System.out.println("Значение должно быть числом больше 0.");
        }
    }

    /**
     * Читает тип топлива.
     * <p>
     * Пользователь может ввести номер из списка или название enum.
     * </p>
     *
     * @return выбранный FuelType
     */
    private FuelType readFuelType() {
        while (true) {
            if (showPrompts) {
                
                System.out.print(ClientFuelTypeParser.valuesWithNumbers());
            }
            String value = readLine("Введите fuelType: ");
            FuelType fuelType = ClientFuelTypeParser.parse(value);
            if (fuelType != null) {
                return fuelType;
            }
            System.out.println("Поле fuelType введено неверно. Введите номер из списка или название типа топлива.");
        }
    }

    /**
     * Читает одну строку из scanner.
     *
     * @param prompt подсказка для пользователя
     * @return введённая строка без пробелов по краям
     */
    private String readLine(String prompt) {
        if (showPrompts) {
            System.out.print(prompt);
        }
        if (!scanner.hasNextLine()) {
            throw new IllegalArgumentException("Ввод элемента прерван.");
        }
        return scanner.nextLine().trim();
    }
}
