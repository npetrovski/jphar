package name.npetrovski.jphar;

import java.io.IOException;
import lombok.Data;

@Data
public class EntryManifest implements Parsable, Writable {

    private Path uri = new Path();
    private Integer uncompressedSize = 0;
    private Integer timestamp = 0;
    private Integer compressedSize = 0;
    private Integer CRC32 = 0;
    private Compression compression = new Compression();
    private Metadata metadata = new Metadata();

    @Override
    public void parse(PharInputStream is) throws IOException {

        uri.parse(is);

        uncompressedSize = is.readRInt();

        timestamp = is.readRInt();

        compressedSize = is.readRInt();

        CRC32 = is.readRInt();

        compression.parse(is);

        metadata.parse(is);
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        out.write(uri);
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
        sb.append("Path: " + uri + "\n");
        sb.append("UncompressedSize: " + uncompressedSize + "\n");
        sb.append("Timestamp: " + timestamp + "\n");
        sb.append("CompressedSize: " + compressedSize + "\n");
        sb.append("CRC32: " + CRC32 + "\n");
        sb.append("Compression: " + compression + "\n");
        sb.append("Metadata: " + metadata + "\n");
        
        return sb.toString();
    }

}
