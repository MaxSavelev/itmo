package common.dto;

import java.time.LocalDate;
import java.io.Serializable;

/**
 * Краткая информация о коллекции.
 * <p>
 * Здесь хранятся тип элементов, тип самой коллекции,
 * дата создания и количество элементов.
 * </p>
 *
 * @param elementsTypeName какой тип объектов хранится в коллекции
 * @param collectionTypeName какой класс используется для хранения коллекции
 * @param creationDate когда была создана коллекция
 * @param elementsCount сколько элементов сейчас находится в коллекции
 *
 * @author makssavelev
 * @version 1.0
 */
public record CollectionInfo(String elementsTypeName, String collectionTypeName, LocalDate creationDate, int elementsCount) implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Возвращает информацию о коллекции в виде строки.
     *
     * @return строка с основной информацией о коллекции
     */
    @Override
    public String toString() {
        return "CollectionInfo:" +
                "\nelementsType=" + elementsTypeName +
                "\ncollectionType=" + collectionTypeName +
                "\ncreationDate=" + creationDate +
                "\nelementsCount=" + elementsCount;
    }
}
