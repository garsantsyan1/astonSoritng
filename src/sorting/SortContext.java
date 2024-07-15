package sorting;

public class SortContext<T extends Number & Comparable<T>> {
    private SortStrategy<T> strategy;

    public void setStrategy(SortStrategy<T> strategy) {
        this.strategy = strategy;
    }

    public SortStrategy<T> getStrategy() {
        return strategy;
    }

    public void executeStrategy(T[] array) {
        strategy.sort(array);
    }

}
