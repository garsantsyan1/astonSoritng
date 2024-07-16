package sorting;


import java.util.Arrays;

public class LibrarySortStrategy implements SortStrategy {
    @Override
    public void sort(int[] array) {
        // LibrarySort
        int countSteps = 0;
        int arrSize = array.length;
        int libSize = 1; // Размер библиотеки
        int indexCurrElement = 1; // Индекс текущего элемента
        int multiplier = 2; // Количество библиотек
        int[] gaps = new int[arrSize + 1]; // пустые места
        int[][] library = new int[multiplier][arrSize];
        boolean[] numbered = new boolean[arrSize + 1]; // Отслеживать, был ли уже заполнен "пробел"(true) или нет(false)
        int targetLib = 0; // Целевая библиотека

        library[targetLib][0] = array[0];

        while (indexCurrElement < arrSize) {
            // Бинарный поиск
            int insert = Arrays.binarySearch(library[targetLib], 0, libSize, array[indexCurrElement]);
            if (insert < 0) {
                insert = -insert - 1; // Корректируем индекс для вставки
            }

            // Если нет пробела, вставить новый индекс
            if (numbered[insert]) {
                int tempSize = 0; // Временный размер библиотеки, кот будет использоваться при перезагрузке
                int nextTargetLib = targetLib == 0 ? 1 : 0; // Переключить целевую библотеку

                // В цикле происходит перенос элементов из старых библиотек в новые библиотеки, а также запись
                // "пробелов" в новую библиотеку.
                for (int i = 0; i <= arrSize; i++) {
                    if (numbered[i]) {
                        library[nextTargetLib][tempSize] = gaps[i];
                        tempSize++;
                        numbered[i] = false;
                        countSteps++;
                    }

                    if (i <= libSize) {
                        library[nextTargetLib][tempSize] = library[targetLib][i];
                        tempSize++;
                        countSteps++;
                    }
                }

                targetLib = nextTargetLib;
                libSize = tempSize - 1;
            } else {
                numbered[insert] = true; // Помечается, что "пробел" теперь заполнен.
                gaps[insert] = array[indexCurrElement]; // В этот "пробел" записывается текущий элемент массива.
                indexCurrElement++;
                countSteps++;
            }
        }

        // Запись отсортированных элементов в исходный массив
        int indexPosForOutput = 0;
        for (int i = 0; indexPosForOutput < arrSize; i++) {
            if (numbered[i]) {
                array[indexPosForOutput] = gaps[i];
                indexPosForOutput++;
                countSteps++;
            }

            if (i < libSize) {
                array[indexPosForOutput] = library[targetLib][i];
                indexPosForOutput++;
                countSteps++;
            }
        }

        System.out.printf("Количество перестановок: %d\n", countSteps);
    }
}