package input;

import java.io.FileNotFoundException;

public class DataInput<T extends Number> {
    private DataReader<T> reader;

    public DataInput(DataReader<T> reader) {
        this.reader = reader;
    }

    public T[] getData(int length) throws FileNotFoundException {
        return reader.getData(length);
    }
}