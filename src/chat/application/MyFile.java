package chat.application;

/**
 * id - the id of the object name - the name of the file data - the file data
 * fileExtension - the file extension
 *
 */
public class MyFile {

    private int id;
    private String name;
    private byte[] data;

    public MyFile(int id, String name, byte[] data) {
        this.id = id;
        this.name = name;
        this.data = data;

    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

}
