package input;

import java.util.InputMismatchException;
import java.util.Scanner;

public class ManualDataReader implements DataReader {

    @Override
    public Integer[] getData(int length) {
        Scanner scanner = new Scanner(System.in);
        Integer[] array = new Integer[length];
        int i = 0;

        while (i < length) {
            System.out.print("Введите целое число для элемента " + (i + 1) + ": ");
            try {
                array[i] = scanner.nextInt();
                i++;
            } catch (InputMismatchException e) {
                System.out.println("Ошибка ввода! Пожалуйста, введите целое число.");
                scanner.next();
            }
        }

        return array;
    }
}
