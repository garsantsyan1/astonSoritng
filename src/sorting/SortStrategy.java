package sorting;

public interface SortStrategy <T extends Number> {
    void sort(T[] array);
}