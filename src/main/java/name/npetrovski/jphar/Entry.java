package name.npetrovski.jphar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import lombok.Data;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

@Data
public class Entry implements Parsable {

    private Long offset;

    private File source;

    private EntryManifest entryManifest;

    public Entry(EntryManifest em) {
        this.entryManifest = em;
    }

    public static Entry createFromFile(File file, Compression.Type compression)
            throws IOException {

        EntryManifest em = new EntryManifest();
        em.getCompression().setType(compression);
        em.setTimestamp((int) file.lastModified() / 1000);
        em.getUri().setName(file.toPath().toString().replace("\\", "/"));

        Entry entry = new Entry(em);
        entry.setSource(file);

        if (file.isDirectory()) {
            em.getCompression().setType(Compression.Type.NONE);
        } else {
            byte[] data = Files.readAllBytes(file.toPath());
            CRC32 crc = new CRC32();
            crc.update(data);

            em.setCRC32((int) crc.getValue());
            em.setUncompressedSize(data.length);
            em.setCompressedSize(entry.getOutputStream().toByteArray().length);
        }

        return entry;

    }

    public boolean isDirectory() {
        return this.entryManifest.getUri().getName().endsWith("/");
    }

    @Override
    public void parse(PharInputStream is) throws IOException {
        offset = is.getPosition();
        is.skip(entryManifest.getCompressedSize());
    }

    public InputStream getInputStream() throws IOException {

        int size = entryManifest.getCompressedSize();
        if (!isDirectory() && size > 0) {
            try (RandomAccessFile raf = new RandomAccessFile(source, "r")) {

                byte[] data = new byte[size];

                raf.seek(offset);
                raf.read(data, 0, size);
                raf.close();

                return getCompressorInputStream(new ByteArrayInputStream(data), entryManifest.getCompression().getType());
            }
        }

        return null;
    }

    public ByteArrayOutputStream getOutputStream() throws IOException {
        if (source.isFile()) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();

            try (OutputStream compressor = getCompressorOutputStream(result, entryManifest.getCompression().getType())) {
                Files.copy(source.toPath(), compressor);
                compressor.flush();
                compressor.close();

                return result;
            }
        }

        return null;
    }

    private InputStream getCompressorInputStream(final InputStream is, Compression.Type compression) throws IOException {
        switch (compression) {
            case ZLIB:
                return new InflaterInputStream(is, new Inflater(true));
            case BZIP:
                return new BZip2CompressorInputStream(is, true);
            case NONE:
                return is;
            default:
                throw new IOException("Unsupported compression type.");
        }

    }

    private OutputStream getCompressorOutputStream(final OutputStream os, Compression.Type compression) throws IOException {
        switch (compression) {
            case ZLIB:
                return new DeflaterOutputStream(os, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
            case BZIP:
                return new BZip2CompressorOutputStream(os);
            case NONE:
                return os;
            default:
                throw new IOException("Unsupported compression type.");
        }
    }

}
