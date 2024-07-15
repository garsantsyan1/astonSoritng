package input;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ManualDataReader implements DataReader<Integer> {

    @Override
    public Integer[] getData(int length) {
        Scanner scanner = new Scanner(System.in);
        List<Integer> data = new ArrayList<>();
        System.out.println("Введите числа (введите 'end' для завершения):");

        while (true) {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("end")) {
                if (data.isEmpty()) {
                    System.out.println("Необходимо ввести хотя бы одно число. Пожалуйста, введите числа:");
                    continue;
                }
                break;
            }
            try {
                data.add(Integer.parseInt(input.trim()));
            } catch (NumberFormatException e) {
                System.out.println("Неверный ввод: " + input + ". Пожалуйста, введите корректное целое число.");
            }
        }

        return data.toArray(new Integer[0]);
    }
}
