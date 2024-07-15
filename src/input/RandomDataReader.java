package input;

import java.util.Random;

public class RandomDataReader implements DataReader<Integer> {

    @Override
    public Integer[] getData(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Длина массива должна быть положительным числом.");
        }
        Random random = new Random();
        Integer[] data = new Integer[length];

        for (int i = 0; i < length; i++) {
            data[i] = random.nextInt(100); // Генерация чисел от 0 до 99
        }
        return data;
    }
}