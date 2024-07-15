package input;

import java.io.FileNotFoundException;

public interface DataReader<T extends Number> {
    T[] getData(int length) throws FileNotFoundException;
}