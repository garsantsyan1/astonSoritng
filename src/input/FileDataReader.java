package input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class FileDataReader implements DataReader {
    private String filePath;

    public FileDataReader(String filePath) {
        this.filePath = filePath;
    }

    public static void validateFilePath(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new RuntimeException("Файл не найден: " + filePath);
        }
    }

    @Override
    public int[] getData(int length) {
        validateFilePath(filePath);
        int[] numbers = new int[length];
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("[,\\s]+");
                for (String token : tokens) {
                    if (count >= length) {
                        throw new RuntimeException("В файле содержится больше чисел, чем ожидается: " + length);
                    }
                    try {
                        numbers[count++] = Integer.parseInt(token.trim());
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Файл содержит некорректное значение: " + token + " Введите путь к" +
                                "файлу с целыми числами");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения файла: " + e.getMessage());
        }

        if (count != length) {
            throw new RuntimeException("В файле содержится " + count + " чисел, но ожидается " + length);
        }

        return numbers;
    }
}
