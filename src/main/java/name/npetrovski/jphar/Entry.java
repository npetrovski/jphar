package name.npetrovski.jphar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
public class Entry implements Readable, Writable {

    private Long offset;

    private File source;

    private EntryManifest entryManifest = new EntryManifest();

    public Entry(EntryManifest em) {
        this.entryManifest = em;
    }

    public Entry(String name) {
        this.entryManifest.getPath().setName(name);
    }

    public static Entry createFromFile(File file, Compression.Type compression)
            throws IOException {

        EntryManifest em = new EntryManifest();
        em.getCompression().setType(compression);
        em.setTimestamp((int) file.lastModified() / 1000);
        em.getPath().setName(file.toPath().toString().replace("\\", "/"));

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
        }

        return entry;

    }

    public boolean isDirectory() {
        return getName().endsWith("/");
    }

    public String getName() {
        return entryManifest.getPath().toString();
    }

    public Integer getSize() {
        return entryManifest.getUncompressedSize();
    }

    public Integer getLastmodified() {
        return entryManifest.getTimestamp();
    }

    public InputStream getInputStream() throws IOException {

        if (null != source && source.exists() && source.isFile()) {

            if (null != offset) {
                int size = entryManifest.getCompressedSize();
                if (!isDirectory() && size > 0) {
                    try (RandomAccessFile raf = new RandomAccessFile(source, "r")) {

                        byte[] data = new byte[size];

                        raf.seek(offset);
                        raf.read(data, 0, size);
                        raf.close();

                        return getCompressorInputStream(new ByteArrayInputStream(data),
                                entryManifest.getCompression().getType());
                    }
                }
            } else {
                return new FileInputStream(source);
            }
        }

        return null;
    }

    public OutputStream getOutputStream() throws IOException {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        InputStream is = getInputStream();

        if (null != is) {
            try (OutputStream compressor = getCompressorOutputStream(result,
                    entryManifest.getCompression().getType())) {

                byte[] buffer = new byte[1024];
                for (int n = 0; n >= 0; n = is.read(buffer)) {
                    compressor.write(buffer, 0, n);
                }
                is.close();
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

    @Override
    public void read(PharInputStream is) throws IOException {
        offset = is.getPosition();
        is.skip(entryManifest.getCompressedSize());
    }

    @Override
    public void write(PharOutputStream out) throws IOException {

        ByteArrayOutputStream compressedData = (ByteArrayOutputStream) getOutputStream();
        if (null != compressedData) {
            this.entryManifest.setCompressedSize(compressedData.toByteArray().length);

            out.write(compressedData.toByteArray());
        }
    }

}
