package name.npetrovski.jphar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.LinkedList;

import de.ailis.pherialize.Mixed;

public final class Phar {

    static final String DEFAULT_PHAR_VERSION = "1.1.1";

    static final String STRING_ENCODING = "UTF-8";

    static final int PHAR_FILE_COMPRESSION_MASK = 0x00F00000;

    static final int PHAR_ENT_COMPRESSION_MASK = 0x0000F000;

    static final int BITMAP_SIGNATURE_FLAG = 0x00010000;

    public PharStub stub = new PharStub();

    private final File source;

    protected PharCompression compression;

    protected String version = Phar.DEFAULT_PHAR_VERSION;

    protected String alias = "";

    protected List<PharEntry> entries = new LinkedList<PharEntry>();

    protected PharMetadata pharMeta = new PharMetadata("");

    public Phar(final File source) throws IOException {
        this(source, PharCompression.NONE);
    }

    public Phar(final File source, final PharCompression compression) throws IOException {
        this(source, compression, source.getName());
    }

    public Phar(final File source, final PharCompression compression, final String alias) throws IOException {
        if (source == null) {
            throw new IllegalArgumentException("Source file cannot be null");
        }

        this.source = source;

        if (source.exists() && source.isFile() && source.canRead()) {
            parse(new PharInputStream(new FileInputStream(this.source)));
        } else {
            if (compression == null) {
                throw new IllegalArgumentException("Phar compression cannot be null");
            }

            this.compression = compression;

            if (alias == null) {
                throw new IllegalArgumentException("Phar alias cannot be null");
            }
            this.alias = alias;
        }
    }

    /**
     * Adds an entry to be stored in the archive.
     *
     * @param file
     * @throws java.io.IOException
     */
    public void add(final File file) throws IOException {
        if (file.isDirectory()) {
            add(new DirectoryPharEntryProvider(file, compression));
        } else {
            add(new FilePharEntryProvider(file, compression));
        }
    }

    private void add(final PharEntryProvider pharEntryProvider) throws IOException {
        for (PharEntry entry : pharEntryProvider.getPharEntries()) {
            entries.add(entry);
        }
    }

    /**
     * Set meta-data
     *
     * @param data
     */
    public void setMetadata(Object data) {
        pharMeta = new PharMetadata(data);
    }

    /**
     * Get PHP serialized meta-data
     *
     * @return
     */
    public Mixed getMetadata() {
        return pharMeta.unserialize();
    }

    public void setStub(String stub) {
        this.stub = new PharStub(stub);
    }

    public void setStub(File stubFile) throws IOException {
        setStub(new String(Files.readAllBytes(stubFile.toPath())));
    }

    public List<PharEntry> getEntries() {
        return entries;
    }

    public String getVersion() {
        return version;
    }

    public PharCompression getCompression() {
        return compression;
    }

    public String getAlias() {
        return alias;
    }

    public synchronized PharEntry getEntry(String path) throws IOException {

        if (path.startsWith("phar:")) {
            path = path.replaceAll("^phar\\:.*\\.phar\\/", "");
        }

        PharEntry entry = null;

        for (PharEntry e : entries) {
            if (path.equals(e.getName())) {
                entry = e;
                break;
            }
        }

        if (entry != null && !entry.isDirectory() && entry.getSize() > 0) {

            try (RandomAccessFile reader = new RandomAccessFile(source, "r")) {
                reader.seek(entry.getOffset());

                byte[] data = new byte[(int) entry.getSize()];
                reader.read(data, 0, (int) entry.getSize());
                reader.close();

                entry.unpack(data);
            }
        }

        return entry;
    }

    public void write() throws IOException, NoSuchAlgorithmException {

        PharOutputStream out = new PharOutputStream(new FileOutputStream(this.source));

        // write stub
        out.write(stub);

        // prepare phar entries manifest
        byte[] pharEntriesManifest = writePharEntriesManifest();

        // write global phar manifest
        // @see http://php.net/manual/en/phar.fileformat.phar.php
        PharManifest pharGlobalManifest = new PharManifest(
                alias, pharEntriesManifest.length, entries.size(), pharMeta);

        out.write(pharGlobalManifest);

        out.write(pharEntriesManifest);

        for (PharEntry pharEntry : entries) {
            out.write(pharEntry.getPharEntryData());
        }

        // flush to file before creating signature
        out.flush();

        // writing signature
        out.write(new PharSignature(source, PharSignatureType.SHA1));

        out.flush();
        out.close();
    }

    private byte[] writePharEntriesManifest() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        PharOutputStream out = new PharOutputStream(byteArrayOutputStream);

        for (PharEntry pharEntry : this.entries) {
            out.write(pharEntry.getPharEntryManifest());
        }

        out.flush();
        out.close();

        return byteArrayOutputStream.toByteArray();
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
        stub.setCode(stubCode);

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
        version = PharVersion.getVersionString(versionBytes);

        int globalBitmappedFlags = is.readRInt();
        compression = PharCompression.getEnumByInt(globalBitmappedFlags & PHAR_FILE_COMPRESSION_MASK);

        int aliasLen = is.readRInt();
        if (aliasLen > 0) {
            byte[] aliasBytes = new byte[aliasLen];
            is.read(aliasBytes, 0, aliasLen);

            alias = new String(aliasBytes);
        }

        int metadataLen = is.readRInt();
        if (metadataLen > 0) {
            byte[] metadataBytes = new byte[metadataLen];
            is.read(metadataBytes, 0, metadataLen);

            pharMeta = new PharMetadata(new String(metadataBytes));
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

            PharEntry entry = new PharEntry(path, PharCompression.getEnumByInt(fileFlags & PHAR_ENT_COMPRESSION_MASK));
            entry.setModTime(unixTimestamp);
            entry.setOffset(offset);
            entry.setSize(compressedLen);
            entry.setChecksum(crcChecksum);
            entry.setDecompressedSize(decompressedSize);

            entries.add(entry);

            offset += compressedLen;
        }
    }

}
