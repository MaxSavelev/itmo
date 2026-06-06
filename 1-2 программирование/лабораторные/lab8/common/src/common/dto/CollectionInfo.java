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
 * @author makssavelev
 * @version 1.0
 */
public class CollectionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String elementsTypeName;
    private final String collectionTypeName;
    private final LocalDate creationDate;
    private final int elementsCount;

    public CollectionInfo(String elementsTypeName, String collectionTypeName, LocalDate creationDate, int elementsCount) {
        this.elementsTypeName = elementsTypeName;
        this.collectionTypeName = collectionTypeName;
        this.creationDate = creationDate;
        this.elementsCount = elementsCount;
    }

    public String getElementsTypeName() {
        return elementsTypeName;
    }

    public String getCollectionTypeName() {
        return collectionTypeName;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public int getElementsCount() {
        return elementsCount;
    }

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
