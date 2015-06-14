package com.javaphar;

import de.ailis.pherialize.Pherialize;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PharInputStream extends FilterInputStream {

    private long pos = 0;

    private long mark = 0;

    private final Phar phar;

    private static final int PHAR_FILE_COMPRESSION_MASK = 0x00F00000;

    private static final int PHAR_ENT_COMPRESSION_MASK = 0x0000F000;

    public PharInputStream(InputStream input, Phar phar) throws IOException {
        super(input);
        this.phar = phar;
        parse();
    }

    private void parse() throws IOException {
        int c;
        while ((c = this.read()) != -1) {
            this.phar.stubCode = this.phar.stubCode.concat(Character.toString((char) c));
            if (this.phar.stubCode.length() >= 3 && (this.phar.stubCode.endsWith("?>\r\n") || this.phar.stubCode.endsWith("?>\n"))) {
                break;
            }
        }

        int manifestLength = this.readRInt();
        if (manifestLength == 0) {
            throw new IllegalArgumentException("Manifest length cannot be zero.");
        }

        long dataPosition = this.pos + manifestLength;

        int filesNum = this.readRInt();
        if (filesNum == 0) {
            throw new IllegalArgumentException("Total number of files cannot be zero.");
        }

        // Version
        // @todo parse it
        this.read();
        this.read();

        int globalBitmappedFlags = this.readRInt();
        this.phar.pharCompression = PharCompression.getEnumByInt(globalBitmappedFlags & PHAR_FILE_COMPRESSION_MASK);

        int aliasLen = this.readRInt();
        if (aliasLen > 0) {
            byte[] alias = new byte[aliasLen];
            this.read(alias, 0, aliasLen);

            this.phar.alias = new String(alias);
        }

        int metadataLen = this.readRInt();
        if (metadataLen > 0) {
            byte[] metadata = new byte[metadataLen];
            this.read(metadata, 0, metadataLen);

            String metaSerialized = new String(metadata);
            this.phar.metadata = (Map<String, String>) Pherialize.unserialize(metaSerialized);
        }

        long offset = dataPosition;
        while (this.pos < dataPosition) {

            int fileLen = this.readRInt();

            byte[] namebytes = new byte[fileLen];
            this.read(namebytes, 0, fileLen);
            String path = new String(namebytes);

            int uncompressedSize = this.readRInt();

            int unixTimestamp = this.readRInt();

            int compressedLen = this.readRInt();

            int crcChecksum = this.readRInt();

            int fileFlags = this.readRInt();

            int fileMetaLen = this.readRInt();
            if (fileMetaLen > 0) {
                byte[] filemeta = new byte[fileMetaLen];
                this.read(filemeta, 0, fileMetaLen);
            }

            PharEntry entry = new PharEntry(path, PharCompression.getEnumByInt(fileFlags & PHAR_ENT_COMPRESSION_MASK), (long) crcChecksum, (long) uncompressedSize);
            entry.setDataOffset(offset, compressedLen);

            this.phar.entries.add(entry);

            offset += compressedLen;
        }
    }

    /**
     * <p>
     * Get the stream position.</p>
     *
     * <p>
     * Eventually, the position will roll over to a negative number. Reading 1
     * Tb per second, this would occur after approximately three months.
     * Applications should account for this possibility in their design.</p>
     *
     * @return the current stream position.
     */
    public synchronized long getPosition() {
        return pos;
    }

    @Override
    public synchronized int read()
            throws IOException {
        int b = super.read();
        if (b >= 0) {
            pos += 1;
        }
        return b;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len)
            throws IOException {
        int n = super.read(b, off, len);
        if (n > 0) {
            pos += n;
        }
        return n;
    }

    @Override
    public synchronized long skip(long skip)
            throws IOException {
        long n = super.skip(skip);
        if (n > 0) {
            pos += n;
        }
        return n;
    }

    @Override
    public synchronized void mark(int readlimit) {
        super.mark(readlimit);
        mark = pos;
    }

    @Override
    public synchronized void reset()
            throws IOException {
        /* A call to reset can still succeed if mark is not supported, but the 
         * resulting stream position is undefined, so it's not allowed here. */
        if (!markSupported()) {
            throw new IOException("Mark not supported.");
        }
        super.reset();
        pos = mark;
    }

    protected int readRInt() throws IOException {
        int ch1 = this.read();
        int ch2 = this.read();
        int ch3 = this.read();
        int ch4 = this.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0) {
            throw new EOFException();
        }

        return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
    }
}
