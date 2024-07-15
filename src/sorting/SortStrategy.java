package sorting;

public interface SortStrategy<T extends Number & Comparable<T>> {
    void sort(T[] array);
}