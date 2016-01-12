package name.npetrovski.jphar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;

import de.ailis.pherialize.Mixed;
import java.util.Iterator;

public final class Phar extends File {

    static final String DEFAULT_PHAR_VERSION = "1.1.1";

    static final String STRING_ENCODING = "UTF-8";

    static final int MAX_MANIFEST_SIZE = 1024 * 1024;

    static final int PHAR_SIGNATURE_MASK = 0x00F00000;

    static final int PHAR_COMPRESSION_MASK = 0x0000F000;
    
    static final int PHAR_FILE_PERMISSIONS_MASK = 0x00000FFF;

    static final int BITMAP_SIGNATURE_FLAG = 0x00010000;

    public PharStub stub = new PharStub();

    protected PharCompression compression = PharCompression.NONE;

    protected String version = Phar.DEFAULT_PHAR_VERSION;

    protected String alias = "";

    protected List<PharEntry> entries = new LinkedList<PharEntry>();

    protected PharMetadata pharMeta = new PharMetadata("");
    
    protected Phar phar;

    public Phar(final File file) {
        this(file.getAbsolutePath());
    }
    
    public Phar(final String path) {
        this(path, PharCompression.NONE);
    }

    public Phar(final String path, final PharCompression compression) {
        this(path, compression, Paths.get(path).getFileName().toString());
    }

    public Phar(final String path, final PharCompression compression, final String alias) {
        super(path);

        this.phar = this;
        
        if (this.exists() && this.isFile() && this.canRead()) {
            try {
                parse();
            } catch (IOException e) {

            }
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

    private interface EntryProvider {

        List<PharEntry> getPharEntries() throws IOException;
    }

    private final class FileEntryProvider implements EntryProvider {

        private final File file;
        private final PharCompression compression;

        public FileEntryProvider(final File file, final PharCompression pharCompression) {
            this.file = file;
            this.compression = pharCompression;
        }

        @Override
        public List<PharEntry> getPharEntries() throws IOException {
            return new LinkedList<PharEntry>() {
                {
                    PharEntry entry = new PharEntry(file.getName(), compression);
                    entry.pack(file);
                    add(entry);
                }
            };
        }

    }

    private final class DirectoryEntryProvider implements EntryProvider {

        private final File file;

        private final PharCompression compression;

        public DirectoryEntryProvider(final File file, final PharCompression pharCompression) {
            this.file = file;
            this.compression = pharCompression;
        }

        @Override
        public List<PharEntry> getPharEntries() throws IOException {
            List<PharEntry> pharEntries = new LinkedList<PharEntry>();
            addPharEntriesRecursively(pharEntries, file.toPath());
            return pharEntries;

        }

        private void addPharEntriesRecursively(final List<PharEntry> pharEntries, final Path directory) throws IOException {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
                for (Path element : directoryStream) {

                    String path = String.format("%s/%s", file.getName(), file.toPath().relativize(element).toString());

                    File file = element.toFile();
                    if (file.isDirectory() && file.list().length > 0) {
                        addPharEntriesRecursively(pharEntries, element);
                    } else {
                        PharEntry entry = new PharEntry(path, compression);
                        entry.pack(file);
                        pharEntries.add(entry);
                    }
                }
            }
        }
    }

    public void addEmptyFolder(String name) throws IOException {
        if (!name.endsWith("/")) {
            name = name + "/";
        }

        entries.add(new PharEntry(name, compression));
    }

    public void rm(String name) {
        for (Iterator<PharEntry> it = entries.iterator(); it.hasNext();) {
            if (it.next().getName().startsWith(name)) {
                it.remove();
            }
        }
    }

    public PharEntry findEntry(String name) {
        for (Iterator<PharEntry> it = entries.iterator(); it.hasNext();) {
            PharEntry e = it.next();
            if (e.getName().equals(name) || e.getName().equals(name + "/")) {
                return e;
            }
        }

        return null;
    }

    /**
     * Adds an entry to be stored in the archive.
     *
     * @param file
     * @throws java.io.IOException
     */
    public void add(final File file) throws IOException {
        add(file, this.compression);
    }

    /**
     * Adds an entry to be stored in the archive.
     *
     * @param file
     * @param entryCompression
     * @throws IOException
     */
    public void add(final File file, PharCompression entryCompression) throws IOException {
        if (file.isDirectory()) {
            add(new DirectoryEntryProvider(file, entryCompression));
        } else {
            add(new FileEntryProvider(file, entryCompression));
        }
    }

    private void add(final EntryProvider pharEntryProvider) throws IOException {
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

    @Override
    public String[] list() {
        List<String> list = new ArrayList<String>();

        for (PharEntry entry : entries) {
            list.add(entry.getName());
        }

        return list.toArray(new String[list.size()]);
    }

    public void write() throws IOException {

        PharOutputStream out = new PharOutputStream(new FileOutputStream(this));

        // write stub
        out.write(stub);

        // prepare phar entries manifest
        byte[] pharEntriesManifest = writePharEntriesManifest();

        // prepare phar global manifest
        byte[] pharGlobalManifest = writePharManifest(pharEntriesManifest);

        out.write(pharGlobalManifest);

        out.write(pharEntriesManifest);

        for (PharEntry pharEntry : entries) {
            out.write(pharEntry);
        }

        // flush to file before creating signature
        out.flush();

        // writing signature
        out.write(new PharSignature(this, PharSignatureType.SHA1));

        out.flush();
        out.close();
    }

    private byte[] writePharManifest(byte[] pharEntriesManifest) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        PharOutputStream out = new PharOutputStream(byteArrayOutputStream);

        // Meta-data
        ByteArrayOutputStream metadataOutputStream = new ByteArrayOutputStream();

        PharOutputStream pharOutputStream = new PharOutputStream(metadataOutputStream);
        pharOutputStream.write(pharMeta);
        pharOutputStream.flush();
        pharOutputStream.close();

        byte[] metadataBytes = metadataOutputStream.toByteArray();

        byte[] pharAlias = alias.getBytes(STRING_ENCODING);

        // Length of manifest in bytes
        if (metadataBytes.length > MAX_MANIFEST_SIZE) {
            throw new RuntimeException("Phar manifest too large.");
        }
        out.writeInt(metadataBytes.length + pharEntriesManifest.length + pharAlias.length + 14);

        // Number of files in the Phar
        out.writeInt(entries.size());

        // API version of the Phar manifest (currently 1.1.1)
        out.write(PharVersion.getVersionNibbles(version));

        // Global Phar bitmapped flags
        int globalZlibFlag = 0;
        int globalBzipFlag = 0;

        for (PharEntry entry : entries) {
            // Phar contains at least 1 file that is compressed with bzip compression 
            if (entry.getCompression() == PharCompression.BZIP2) {
                globalBzipFlag = 0x00002000;
            }

            // Phar contains at least 1 file that is compressed with zlib compression 
            if (entry.getCompression() == PharCompression.GZIP) {
                globalZlibFlag = 0x00001000;
            }
        }
        out.writeInt(BITMAP_SIGNATURE_FLAG | globalZlibFlag | globalBzipFlag);

        // Length of Phar alias
        out.writeInt(pharAlias.length);

        // Phar alias (length based on previous)
        out.write(pharAlias);

        // write serializedMeta
        out.write(metadataBytes);

        out.flush();
        out.close();

        return byteArrayOutputStream.toByteArray();
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

    private void parse() throws IOException {

        PharInputStream is = new PharInputStream(new FileInputStream(this));

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
        compression = PharCompression.getEnumByInt(globalBitmappedFlags & PHAR_COMPRESSION_MASK);

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
            String entryName = new String(namebytes);

            int decompressedSize = is.readRInt();

            int unixTimestamp = is.readRInt();

            int compressedLen = is.readRInt();

            int crcChecksum = is.readRInt();

            int entryCompressionFlag = is.readRInt();

            int fileMetaLen = is.readRInt();
            if (fileMetaLen > 0) {
                byte[] filemeta = new byte[fileMetaLen];
                is.read(filemeta, 0, fileMetaLen);
            }

            PharCompression entryCompression
                    = PharCompression.getEnumByInt(entryCompressionFlag & PHAR_COMPRESSION_MASK);


            PharEntry entry = new PharEntry(entryName, entryCompression);
            entry.setDecompressedSize(decompressedSize);
            entry.setModTime(unixTimestamp);
            entry.setOffset(offset);
            entry.setSize(compressedLen);
            entry.setChecksum(crcChecksum);

            entries.add(entry);

            offset += compressedLen;
        }
        is.close();

        unpackEntries();
    }

    private void unpackEntries() throws IOException {

        for (PharEntry entry : entries) {

            if (!entry.isDirectory() && entry.getSize() > 0) {

                try (RandomAccessFile reader = new RandomAccessFile(this, "r")) {
                    reader.seek(entry.getOffset());

                    byte[] data = new byte[(int) entry.getSize()];
                    reader.read(data, 0, (int) entry.getSize());
                    reader.close();

                    entry.unpack(data);
                }
            }
        }
    }

}
