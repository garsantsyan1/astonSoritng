package sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomSort {
    public static void sortEvenOdd(Integer[] dataArray, SortStrategy strategy, boolean sortEven) {
        List<Integer> subList = new ArrayList<>();

        // ���������� ������ ��� �������� ���������
        for (int num : dataArray) {
            if ((sortEven && num % 2 == 0) || (!sortEven && num % 2 != 0)) {
                subList.add(num);
            }
        }

        // �������������� List � ������
        int[] temp = subList.stream().mapToInt(i -> i).toArray();
        Integer[] subArray = Arrays.stream(temp).boxed().toArray( Integer[]::new );

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
