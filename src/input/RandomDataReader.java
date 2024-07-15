package input;

import java.util.Random;

public class RandomDataReader implements DataReader {

    @Override
    public int[] getData(int length) {
        Random random = new Random();
        int[] data = new int[length];

        for (int i = 0; i < length; i++) {
            data[i] = random.nextInt(100); // Генерация чисел от 0 до 99
        }
        return data;
    }
}
