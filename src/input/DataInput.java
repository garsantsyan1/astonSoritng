package input;

public class DataInput {
    private DataReader reader;

    public DataInput(DataReader reader) {
        this.reader = reader;
    }

    public int[] getData(int length) {
        return reader.getData(length);
    }
}
