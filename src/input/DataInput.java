package input;

public class DataInput {
    private DataReader reader;

    public DataInput(DataReader reader) {
        this.reader = reader;
    }

    public Integer[] getData(int length) {
        return reader.getData(length);
    }
}
