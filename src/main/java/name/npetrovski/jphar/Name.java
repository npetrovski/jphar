package name.npetrovski.jphar;

import java.io.IOException;
import lombok.Data;

@Data
public class Name implements Readable, Writable {

    private String name = "";

    @Override
    public void read(PharInputStream is) throws IOException {
        int len = is.readRInt();
        if (len > 0) {
            byte[] nameBytes = new byte[len];
            is.read(nameBytes, 0, len);

            name = new String(nameBytes);
        }
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        byte[] data = name.getBytes("UTF-8");
        out.writeInt(data.length);
        out.write(data);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
