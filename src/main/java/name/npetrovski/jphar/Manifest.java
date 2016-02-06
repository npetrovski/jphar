package name.npetrovski.jphar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Manifest implements Parsable, Writable {

    private Integer len = 0;

    private Integer numberOfFiles = 0;

    private Version version = new Version();

    private Compression compression = new Compression();

    private Path alias = new Path();

    private Metadata metadata = new Metadata();

    private List<EntryManifest> entryManifest = new ArrayList<>();

    @Override
    public void parse(PharInputStream is) throws IOException {
        len = is.readRInt() + 4;
        numberOfFiles = is.readRInt();

        version.parse(is);

        compression.parse(is);

        alias.parse(is);

        metadata.parse(is);

        for (int i = 0; i < numberOfFiles; i++) {
            EntryManifest em = new EntryManifest();
            em.parse(is);
            entryManifest.add(em);
        }

    }

    @Override
    public void write(PharOutputStream out) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Global Phar bitmapped flags
        int globalZlibFlag = 0;
        int globalBzipFlag = 0;
        for (EntryManifest em : entryManifest) {
            if (em.getCompression().getType() == Compression.Type.BZIP) {
                globalBzipFlag = 0x00002000;
            }

            if (em.getCompression().getType() == Compression.Type.ZLIB) {
                globalZlibFlag = 0x00001000;
            }
        }
        int globalCompressionFlag = Compression.BITMAP_SIGNATURE_FLAG | globalZlibFlag | globalBzipFlag;

        try (PharOutputStream buffer = new PharOutputStream(byteArrayOutputStream)) {
            buffer.writeInt(numberOfFiles);
            buffer.write(version);
            buffer.writeInt(globalCompressionFlag);
            buffer.write(alias);
            buffer.write(metadata);
            for (EntryManifest em : entryManifest) {
                buffer.write(em);
            }

            buffer.flush();
        }

        byte[] data = byteArrayOutputStream.toByteArray();
        out.writeInt(data.length);
        out.write(data);

    }

}
