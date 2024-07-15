package input;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileDataReader implements DataReader<Integer> {
    private String filePath;

    public FileDataReader(String filePath) {
        this.filePath = filePath;
    }

    public static void validateFilePath(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("Файл не найден: " + filePath);
        }
    }

    @Override
    public Integer[] getData(int length) throws FileNotFoundException {
        validateFilePath(filePath);
        List<Integer> numbers = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("[,\\s]+"); // Разделение по запятой и пробелам
                for (String token : tokens) {
                    try {
                        numbers.add(Integer.parseInt(token.trim()));
                    } catch (NumberFormatException e) {
                        System.out.println("Неверное число: " + token + ". Пожалуйста, убедитесь, что файл содержит только целые числа.");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка чтения файла: " + e.getMessage());
        }

        if (numbers.isEmpty()) {
            throw new RuntimeException("В файле не найдено чисел.");
        }

        return numbers.toArray(new Integer[0]);
    }
}
