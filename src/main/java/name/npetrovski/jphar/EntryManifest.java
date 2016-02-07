package name.npetrovski.jphar;

import java.io.IOException;
import lombok.Data;

@Data
public class EntryManifest implements Readable, Writable {

    private Name path = new Name();
    private Integer uncompressedSize = 0;
    private Integer timestamp = 0;
    private Integer compressedSize = 0;
    private Integer CRC32 = 0;
    private Compression compression = new Compression();
    private Metadata metadata = new Metadata();

    @Override
    public void read(PharInputStream is) throws IOException {

        path.read(is);

        uncompressedSize = is.readRInt();

        timestamp = is.readRInt();

        compressedSize = is.readRInt();

        CRC32 = is.readRInt();

        compression.read(is);

        metadata.read(is);
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        out.write(path);
        out.writeInt(uncompressedSize);
        out.writeInt(timestamp);
        out.writeInt(compressedSize);
        out.writeInt(CRC32);
        out.write(compression);
        out.write(metadata);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(path).append("\n")
            .append("UncompressedSize: ").append(uncompressedSize).append("\n")
            .append("Timestamp: ").append(timestamp).append("\n")
            .append("CompressedSize: ").append(compressedSize).append("\n")
            .append("CRC32: ").append(CRC32).append("\n")
            .append("Compression: ").append(compression).append("\n")
            .append("Metadata: ").append(metadata).append("\n");

        return sb.toString();
    }

}
