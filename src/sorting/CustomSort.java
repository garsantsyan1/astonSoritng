package sorting;

import java.util.ArrayList;
import java.util.List;

public class CustomSort {
    public static void sortEvenOdd(int[] dataArray, SortStrategy strategy, boolean sortEven) {
        List<Integer> subList = new ArrayList<>();

        // ���������� ������ ��� �������� ���������
        for (int num : dataArray) {
            if ((sortEven && num % 2 == 0) || (!sortEven && num % 2 != 0)) {
                subList.add(num);
            }
        }

        // �������������� List � ������
        int[] subArray = subList.stream().mapToInt(i -> i).toArray();

        // ���������� ����������
        strategy.sort(subArray);

        // ������� ��������������� ��������� ������� � ������
        int subIndex = 0;
        for (int i = 0; i < dataArray.length; i++) {
            if ((sortEven && dataArray[i] % 2 == 0) || (!sortEven && dataArray[i] % 2 != 0)) {
                dataArray[i] = subArray[subIndex++];
            }
        }
    }
}
