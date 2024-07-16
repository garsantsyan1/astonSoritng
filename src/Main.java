import input.FileDataReader;
import input.ManualDataReader;
import input.RandomDataReader;
import sorting.CustomSort;
import sorting.LibrarySortStrategy;
import sorting.SortContext;
import sorting.TimSortStrategy;

import input.DataInput;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {
    private static final String ANSI_RED = "\u001B[31m";
    private static final String DEFAULT = "\u001B[0m";
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        boolean running = true;

        while (running) {
            int fillOption = 0;
            int length = 0;
            int sortOption = 0;
            int sortType = 0;
            String fileName = "";

            // Fill Option Validation
            while (true) {
                System.out.println(DEFAULT + "Выберите способ заполнения массива:");
                System.out.println("1. Из файла");
                System.out.println("2. Случайные данные");
                System.out.println("3. Ввод вручную");

                try {
                    fillOption = scanner.nextInt();
                    if (fillOption < 1 || fillOption > 3) {
                        System.out.println(ANSI_RED + "Неверный выбор. Пожалуйста, выберите значение от 1 до 3.");
                        continue;
                    } else if (fillOption == 1) {
                        System.out.println("Введите путь к файлу: ");
                        scanner.nextLine();
                        fileName = scanner.nextLine();
                    }
                    break;
                } catch (InputMismatchException e) {
                    System.out.println(ANSI_RED + "Неверный ввод. Пожалуйста, введите число.");
                    scanner.next(); // Clear the invalid input
                }
            }

            // Length Validation
            while (true) {
                System.out.println("Введите длину массива:");
                try {
                    length = scanner.nextInt();
                    if (length <= 0) {
                        System.out.println(ANSI_RED + "Длина массива должна быть положительным числом.");
                        continue;
                    }
                    break;
                } catch (InputMismatchException e) {
                    System.out.println(ANSI_RED + "Неверный ввод. Пожалуйста, введите положительное число.");
                    scanner.next(); // Clear the invalid input
                }
            }

            int[] dataArray = null;
            switch (fillOption) {
                case 1:
                    FileDataReader fileDataReader = new FileDataReader(fileName);
                    dataArray = new DataInput(fileDataReader).getData(length);
                    break;
                case 2:
                    dataArray = new DataInput(new RandomDataReader()).getData(length);
                    break;
                case 3:
                    dataArray = new DataInput(new ManualDataReader()).getData(length);
                    break;
                case 0:
                    System.exit(fillOption);
            }

            // Sort Option Validation
            while (true) {
                System.out.println(DEFAULT + "Выберите алгоритм сортировки:");
                System.out.println("1. TimSort");
                System.out.println("2. Library sort");
                try {
                    sortOption = scanner.nextInt();
                    if (sortOption < 1 || sortOption > 2) {
                        System.out.println(ANSI_RED + "Неверный выбор. Пожалуйста, выберите значение от 1 до 2.");
                        continue;
                    }
                    break;
                } catch (InputMismatchException e) {
                    System.out.println(ANSI_RED + "Неверный ввод. Пожалуйста, введите число.");
                    scanner.next(); // Clear the invalid input
                }
            }

            // Sort Type Validation
            while (true) {
                System.out.println(DEFAULT + "Выберите тип сортировки:");
                System.out.println("1. Все данные");
                System.out.println("2. Только четные");
                System.out.println("3. Только нечетные");
                try {
                    sortType = scanner.nextInt();
                    if (sortType < 1 || sortType > 3) {
                        System.out.println(ANSI_RED + "Неверный выбор. Пожалуйста, выберите значение от 1 до 3.");
                        continue;
                    }
                    break;
                } catch (InputMismatchException e) {
                    System.out.println(ANSI_RED + "Неверный ввод. Пожалуйста, введите число.");
                    scanner.next(); // Clear the invalid input
                }
            }

            SortContext sortContext = new SortContext();
            switch (sortOption) {
                case 1:
                    sortContext.setStrategy(new TimSortStrategy());
                    break;
                case 2:
                    sortContext.setStrategy(new LibrarySortStrategy());
                    break;
            }

            switch (sortType) {
                case 1:
                    sortContext.executeStrategy(dataArray);
                    break;
                case 2:
                    CustomSort.sortEvenOdd(dataArray, sortContext.getStrategy(), true);
                    break;
                case 3:
                    CustomSort.sortEvenOdd(dataArray, sortContext.getStrategy(), false);
                    break;
            }

            System.out.println("Отсортированный массив: " + Arrays.toString(dataArray));

            System.out.println("Хотите выйти? (да/нет)");
            String exitOption = scanner.next();
            if (exitOption.equalsIgnoreCase("да")) {
                running = false;
            }
        }
        scanner.close();
    }
}
