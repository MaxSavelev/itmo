package storage;

import java.time.LocalDate;

/**
 * Краткая информация о коллекции.
 * <p>
 * Здесь хранятся тип элементов, тип самой коллекции,
 * дата создания и количество элементов.
 * </p>
 *
 * @param elementsType какой тип объектов хранится в коллекции
 * @param collectionType какой класс используется для хранения коллекции
 * @param creationDate когда была создана коллекция
 * @param elementsCount сколько элементов сейчас находится в коллекции
 *
 * @author makssavelev
 * @version 1.0
 */
public record CollectionInfo(Class <?> elementsType, Class <?> collectionType, LocalDate creationDate, int elementsCount){
    /**
     * Возвращает информацию о коллекции в виде строки.
     *
     * @return строка с основной информацией о коллекции
     */
    @Override
    public String toString() {
        return "CollectionInfo:" +
                "\nelementsType=" + elementsType +
                "\ncollectionType=" + collectionType +
                "\ncreationDate=" + creationDate +
                "\nelementsCount=" + elementsCount;
    }
}
