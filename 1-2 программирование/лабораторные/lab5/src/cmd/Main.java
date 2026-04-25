package cmd;

import data.Vehicle;
import parser.VehicleCollectionParser;
import storage.VehicleCollection;

import java.util.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Главный класс программы.
 * <p>
 * Запускает чтение коллекции из файла,
 * а потом запускает командную строку.
 * </p>
 *
 * @author makssavelev
 * @version 1.0
 */
public class Main{
    /**
     * Точка входа в программу.
     * <p>
     * Метод пытается загрузить коллекцию из файла,
     * а если не получается, создает пустую коллекцию.
     * После этого запускается работа командной строки.
     * </p>
     *
     * @param args аргументы запуска программы
     */
    public static void main(String[] args) {
        VehicleCollection collection;
        try {
            String fileName = System.getenv("FILE_NAME");
            if (fileName == null || fileName.isBlank()) {
                throw new IOException("Переменная окружения FILE_NAME не задана");
            }
            VehicleCollectionParser p = new VehicleCollectionParser(
                    new InputStreamReader(new FileInputStream(fileName))
            );
            List<Vehicle> vehicles = p.parse();
            collection = new VehicleCollection(vehicles);
        } catch (IOException e) {
            System.out.println("Произошла ошибка при загрузке коллекции: " + e.getMessage());
            collection = new VehicleCollection();
        }
        CommandLine commandLine = new CommandLine(collection);
        commandLine.run();


    }
    Comparator<Vehicle> comp = new Comparator<Vehicle>() {
        @Override
        public int compare(Vehicle o1, Vehicle o2) {

            return o1.getId().compareTo(o2.getId());
        }
    };

}
