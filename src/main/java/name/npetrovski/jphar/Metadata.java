package name.npetrovski.jphar;

import java.io.IOException;
import de.ailis.pherialize.Pherialize;
import java.io.Serializable;
import lombok.Data;

@Data
public class Metadata implements Readable, Writable {

    private Serializable meta;

    @Override
    public void read(PharInputStream is) throws IOException {
        int len = is.readRInt();

        if (len > 0) {
            byte[] metadataBytes = new byte[len];
            is.read(metadataBytes, 0, len);

            meta = Pherialize.unserialize(new String(metadataBytes));
        }
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        if (null != meta) {
            byte[] data = Pherialize.serialize(meta).getBytes("UTF-8");
            out.writeInt(data.length);
            out.write(data);
        } else {
            out.writeInt(0);
        }
    }

    @Override
    public String toString() {
        return (null != meta) ? Pherialize.serialize(meta) : "";
    }
}
