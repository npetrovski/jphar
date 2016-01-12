package name.npetrovski.jphar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.CRC32;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

public final class PharEntry implements PharWritable {

    private String name;

    private PharCompression compression;

    private int checksum = 0;

    private byte[] compressedBytes = new byte[0];

    private byte[] decompressedBytes = new byte[0];
    
    // required for unpack
    private int decompressedSize = 0;

    private long offset;

    private int size = 0;

    private int unixModTime = (int) (System.currentTimeMillis() / 1000L);

    private PharMetadata fileMeta = new PharMetadata("");

    public PharEntry(final String path, final PharCompression compression) throws IOException {
        this.name = path;
        this.compression = compression;
    }

    public void pack(final File source) throws IOException {

        if (source.isDirectory()) {

            name = name + "/";
            unixModTime = (int) source.lastModified() / 1000;

        } else if (source.isFile() && source.canRead()) {

            unixModTime = (int) source.lastModified() / 1000;

            decompressedBytes = Files.readAllBytes(source.toPath());

            CRC32 crc = new CRC32();
            crc.update(this.decompressedBytes);

            checksum = (int) crc.getValue();

            ByteArrayOutputStream compressed = new ByteArrayOutputStream();

            try (OutputStream compressor = getCompressorOutputStream(compressed)) {
                Files.copy(source.toPath(), compressor);
                compressor.flush();
                compressor.close();
            }

            switch (compression) {
                case GZIP:
                    compressedBytes = Arrays.copyOfRange(compressed.toByteArray(), 10, compressed.toByteArray().length - 8);
                    break;
                case BZIP2:
                default:
                    compressedBytes = compressed.toByteArray();
            }
        }
    }

    public void unpack(byte[] compressedBytes) throws IOException {

        if (null != compressedBytes && compressedBytes.length > 0) {

            // GZIP is not in the full specification format
            if (compression == PharCompression.GZIP) {
                compressedBytes = ArrayUtils.addAll(new byte[]{0x1f, (byte) 0x8b, 0x08, 0, 0, 0, 0, 0, 0, (byte) 0xff}, compressedBytes);
                compressedBytes = ArrayUtils.addAll(compressedBytes, ByteBuffer.allocate(4).putInt(Integer.reverseBytes((int) this.checksum)).array());
                compressedBytes = ArrayUtils.addAll(compressedBytes, ByteBuffer.allocate(4).putInt(Integer.reverseBytes((int) this.decompressedSize)).array());
            }

            this.compressedBytes = compressedBytes;

            try (InputStream decompressor = getCompressorInputStream(new ByteArrayInputStream(compressedBytes))) {
                this.decompressedBytes = IOUtils.toByteArray(decompressor);
                decompressor.close();
            }

        }
    }

    private OutputStream getCompressorOutputStream(final OutputStream outputStream) throws IOException {
        switch (this.compression) {
            case GZIP:
                return new GzipCompressorOutputStream(outputStream);
            case BZIP2:
                return new BZip2CompressorOutputStream(outputStream);
            case NONE:
                return outputStream;
            default:
                throw new IllegalArgumentException("Compression is not supported: " + this.compression.name());
        }
    }

    private InputStream getCompressorInputStream(final InputStream inputStream) throws IOException {
        switch (this.compression) {
            case GZIP:
                return new GzipCompressorInputStream(inputStream, true);
            case BZIP2:
                return new BZip2CompressorInputStream(inputStream, true);
            case NONE:
                return inputStream;
            default:
                throw new IllegalArgumentException("Compression is not supported: " + this.compression.name());
        }
    }

    public PharWritable getPharEntryManifest() {
        return new PharWritable() {

            @Override
            public void write(PharOutputStream out) throws IOException {

                byte[] filenameBytes = PharEntry.this.name.replace('\\', '/').getBytes("UTF-8");

                // Filename length in bytes
                out.writeInt(filenameBytes.length);

                // Filename (length specified in previous)
                out.write(filenameBytes);

                // Un-compressed file size in bytes
                out.writeInt((int) PharEntry.this.decompressedBytes.length);

                // Unix timestamp of file
                out.writeInt((int) (PharEntry.this.unixModTime));

                // Compressed file size in bytes
                out.writeInt((int) PharEntry.this.compressedBytes.length);

                // CRC32 checksum of un-compressed file contents
                out.writeInt((int) PharEntry.this.checksum);

                // Bit-mapped File-specific flags
                out.write(PharEntry.this.compression.getBitmapFlag());

                // write meta-data
                out.write(fileMeta);
            }
        };
    }

    @Override
    public void write(final PharOutputStream out) throws IOException {
        out.write(PharEntry.this.compressedBytes);
    }

    public boolean isDirectory() {
        return this.name.endsWith("/");
    }

    public String getName() {
        return this.name;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public byte[] getContents() {
        return this.decompressedBytes;
    }

    public Date getLastModifiedDate() {
        return new Date(unixModTime * 1000);
    }

    public void setModTime(int ts) {
        this.unixModTime = ts;
    }

    public int getModTime() {
        return this.unixModTime;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public PharMetadata getFileMeta() {
        return fileMeta;
    }

    public void setFileMeta(PharMetadata fileMeta) {
        this.fileMeta = fileMeta;
    }

    public PharCompression getCompression() {
        return compression;
    }

    public void setDecompressedSize(int decompressedSize) {
        this.decompressedSize = decompressedSize;
    }
 
}
