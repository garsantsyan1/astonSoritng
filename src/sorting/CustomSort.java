package sorting;

import java.util.ArrayList;
import java.util.List;

public class CustomSort {
    public static void sortEvenOdd(int[] dataArray, SortStrategy strategy, boolean sortEven) {
        List<Integer> subList = new ArrayList<>();

        // Извлечение четных или нечетных элементов
        for (int num : dataArray) {
            if ((sortEven && num % 2 == 0) || (!sortEven && num % 2 != 0)) {
                subList.add(num);
            }
        }

        // Преобразование List в массив
        int[] subArray = subList.stream().mapToInt(i -> i).toArray();

        // Сортировка подмассива
        strategy.sort(subArray);

        // Вставка отсортированных элементов обратно в массив
        int subIndex = 0;
        for (int i = 0; i < dataArray.length; i++) {
            if ((sortEven && dataArray[i] % 2 == 0) || (!sortEven && dataArray[i] % 2 != 0)) {
                dataArray[i] = subArray[subIndex++];
            }
        }
    }
}
