package name.npetrovski.jphar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import de.ailis.pherialize.Pherialize;
import de.ailis.pherialize.Mixed;

public final class Phar {

    static final String DEFAULT_PHAR_VERSION = "1.1.1";

    static final String STRING_ENCODING = "UTF-8";

    static final int PHAR_FILE_COMPRESSION_MASK = 0x00F00000;

    static final int PHAR_ENT_COMPRESSION_MASK = 0x0000F000;

    static final int BITMAP_SIGNATURE_FLAG = 0x00010000;

    private final File source;

    protected PharCompression compression;

    public PharStub stub = new PharStub();

    protected String version = Phar.DEFAULT_PHAR_VERSION;

    protected String alias = "";

    protected List<PharEntry> entries = new ArrayList<PharEntry>();

    protected String serializedMeta;

    public Phar(final File source) throws IOException {
        this(source, PharCompression.NONE);
    }

    public Phar(final File source, final PharCompression compression) throws IOException {
        if (source == null) {
            throw new IllegalArgumentException("Source file cannot be null");
        }

        this.source = source;

        if (compression == null) {
            throw new IllegalArgumentException("Phar compression cannot be null");
        }

        this.compression = compression;

        if (source.exists() && source.isFile()) {
            parse(new PharInputStream(new FileInputStream(this.source)));
        }
    }

    /**
     * Adds an entry to be stored in the phar archive.
     *
     * @param file
     * @throws java.io.IOException
     */
    public void add(final File file) throws IOException {
        add(file, file.getName());
    }

    /**
     * Adds an entry to be stored in the phar archive.
     *
     * @param file
     * @param localPath
     * @throws java.io.IOException
     */
    public void add(final File file, final String localPath) throws IOException {
        if (file.isDirectory()) {
            add(new DirectoryPharEntryProvider(file, localPath));
        } else {
            add(new FilePharEntryProvider(file, localPath, this.compression));
        }
    }

    private void add(final PharEntryProvider pharEntryProvider) throws IOException {
        for (PharEntry pharEntry : pharEntryProvider.getPharEntries()) {
            this.entries.add(pharEntry);
        }
    }

    /**
     * Set metadata
     *
     * @param data
     */
    public void setMetadata(Object data) {
        this.serializedMeta = Pherialize.serialize(data);
    }

    /**
     * Get PHP serialized metadata
     *
     * @return
     */
    public Mixed getMetadata() {
        return Pherialize.unserialize(this.serializedMeta);
    }

    public void setStubFile(File stubFile) throws IOException {
        this.stub = new PharStub(new String(Files.readAllBytes(stubFile.toPath())));
    }

    public List<PharEntry> getEntries() {
        return this.entries;
    }

    public String getVersion() {
        return this.version;
    }

    public PharCompression getCompression() {
        return this.compression;
    }

    public String getAlias() {
        return this.alias;
    }

    public synchronized PharEntry getEntry(String path) throws IOException {

        PharEntry entry = null;

        for (PharEntry e : this.entries) {
            if (path.equals(e.getName())) {
                entry = e;
                break;
            }
        }

        RandomAccessFile reader;
        if (entry != null && entry.getSize() > 0) {

            reader = new RandomAccessFile(source, "r");
            reader.seek(entry.getOffset());

            byte[] data = new byte[(int) entry.getSize()];
            reader.read(data, 0, (int) entry.getSize());

            entry.unpack(data);

            reader.close();
        }

        return entry;
    }

    public void write() throws IOException, NoSuchAlgorithmException {
        PharOutputStream out = null;
        try {
            out = new PharOutputStream(new FileOutputStream(this.source));

            // write stub
            out.write(this.stub);

            byte[] pharFiles = writePharEntriesManifest();

            PharManifest pharManifest = new PharManifest(
                    this.source, pharFiles, this.entries, new PharMetadata(this.serializedMeta));

            out.write(pharManifest);
            out.write(pharFiles);

            for (PharEntry pharEntry : this.entries) {
                out.write(pharEntry.getPharEntryData());
            }

            // flush to file before creating signature
            out.flush();

            // writing signature
            out.write(new PharSignature(this.source, PharSignatureType.SHA1));

            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private byte[] writePharEntriesManifest() throws IOException {
        PharOutputStream out = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            out = new PharOutputStream(byteArrayOutputStream);
            for (PharEntry pharEntry : this.entries) {
                out.write(pharEntry.getPharEntryManifest());
            }
            out.flush();
            return byteArrayOutputStream.toByteArray();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void parse(PharInputStream is) throws IOException {
        int c;
        String stubCode = "";
        while ((c = is.read()) != -1) {
            stubCode = stubCode.concat(Character.toString((char) c));
            if (stubCode.length() >= 3 && (stubCode.endsWith("?>\r\n") || stubCode.endsWith("?>\n"))) {
                break;
            }
        }
        this.stub.setCode(stubCode);

        int manifestLength = is.readRInt();
        if (manifestLength == 0) {
            throw new IllegalArgumentException("Manifest length cannot be zero.");
        }

        long dataPosition = is.pos + manifestLength;

        int filesNum = is.readRInt();
        if (filesNum == 0) {
            throw new IllegalArgumentException("Total number of files cannot be zero.");
        }

        byte[] versionBytes = new byte[2];
        is.read(versionBytes, 0, 2);
        this.version = PharVersion.getVersionString(versionBytes);

        int globalBitmappedFlags = is.readRInt();
        this.compression = PharCompression.getEnumByInt(globalBitmappedFlags & PHAR_FILE_COMPRESSION_MASK);

        int aliasLen = is.readRInt();
        if (aliasLen > 0) {
            byte[] aliasBytes = new byte[aliasLen];
            is.read(aliasBytes, 0, aliasLen);

            this.alias = new String(aliasBytes);
        }

        int metadataLen = is.readRInt();
        if (metadataLen > 0) {
            byte[] metadataBytes = new byte[metadataLen];
            is.read(metadataBytes, 0, metadataLen);

            this.serializedMeta = new String(metadataBytes);
        }

        long offset = dataPosition;
        while (is.pos < dataPosition) {

            int fileLen = is.readRInt();

            byte[] namebytes = new byte[fileLen];
            is.read(namebytes, 0, fileLen);
            String path = new String(namebytes);

            int decompressedSize = is.readRInt();

            int unixTimestamp = is.readRInt();

            int compressedLen = is.readRInt();

            int crcChecksum = is.readRInt();

            int fileFlags = is.readRInt();

            int fileMetaLen = is.readRInt();
            if (fileMetaLen > 0) {
                byte[] filemeta = new byte[fileMetaLen];
                is.read(filemeta, 0, fileMetaLen);
            }

            PharEntry entry = new PharEntry(null, path, PharCompression.getEnumByInt(fileFlags & PHAR_ENT_COMPRESSION_MASK));
            entry.setModTime(unixTimestamp);
            entry.setOffset(offset);
            entry.setSize(compressedLen);
            entry.setChecksum(crcChecksum);
            entry.setDecompressedSize(decompressedSize);

            this.entries.add(entry);

            offset += compressedLen;
        }
    }

}
