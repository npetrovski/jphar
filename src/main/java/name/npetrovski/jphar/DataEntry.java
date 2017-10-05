/*
 * The MIT License
 *
 * Copyright 2016 npetrovski.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package name.npetrovski.jphar;

import lombok.Data;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class DataEntry implements Entry, Readable, Writable {

    private Long offset;

    private File source;

    private EntryManifest entryManifest = new EntryManifest();

    public DataEntry() {
        this(new EntryManifest());
    }

    public DataEntry(EntryManifest em) {
        this.entryManifest = em;
    }

    public DataEntry(String name) {
        this.entryManifest.getPath().setName(name);
    }

    /**
     * Create entry from file
     *
     */
    public static DataEntry createFromFile(File file, Compression.Type compression)
            throws IOException {

        EntryManifest em = new EntryManifest();
        em.setCompression(new Compression(compression));
        em.setTimestamp((int) file.lastModified() / 1000);
        em.getPath().setName(file.toPath().toString().replace("\\", "/"));

        DataEntry entry = new DataEntry(em);
        entry.setSource(file.getCanonicalFile());

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

    @Override
    public boolean isDirectory() {
        return getName().endsWith("/");
    }

    @Override
    public String getName() {
        return entryManifest.getPath().toString();
    }

    @Override
    public Integer getSize() {
        return entryManifest.getUncompressedSize();
    }

    @Override
    public Integer getLastModified() {
        return entryManifest.getTimestamp();
    }

    @Override
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

    @Override
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

    public void read(PharInputStream is) throws IOException {
        offset = is.getPosition();
        is.skip(entryManifest.getCompressedSize());
    }

    public void write(PharOutputStream out) throws IOException {

        ByteArrayOutputStream compressedData = (ByteArrayOutputStream) getOutputStream();
        if (null != compressedData) {
            this.entryManifest.setCompressedSize(compressedData.toByteArray().length);

            out.write(compressedData.toByteArray());
        }
    }
}
