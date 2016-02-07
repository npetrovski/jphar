package name.npetrovski.jphar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;

@Data
public class Phar extends File {

    private Stub stub = new Stub();

    private Manifest manifest = new Manifest();

    private List<Entry> entries = new LinkedList<>();

    private Signature signature = new Signature();

    public Phar(String pathname) {
        super(pathname);

        if (super.exists() && super.isFile()) {
            try {
                parse();
            } catch (IOException e) {
            }
        } else {
            manifest.getAlias().setName(getName());
        }
    }

    public Phar(File file) {
        this(file.getPath());
    }

    private interface EntryProvider {

        List<Entry> getPharEntries() throws IOException;
    }

    private final class FileEntryProvider implements EntryProvider {

        private final File file;
        private final Compression.Type compression;

        public FileEntryProvider(final File file, final Compression.Type pharCompression) {
            this.file = file;
            this.compression = pharCompression;
        }

        @Override
        public List<Entry> getPharEntries()
                throws IOException {
            return new LinkedList<Entry>() {
                {
                    add(Entry.createFromFile(file, compression));
                }
            };
        }

    }

    private final class DirectoryEntryProvider implements EntryProvider {

        private final File source;

        private final Compression.Type compression;

        public DirectoryEntryProvider(final File file, final Compression.Type pharCompression) {
            this.source = file;
            this.compression = pharCompression;
        }

        @Override
        public List<Entry> getPharEntries()
                throws IOException {
            List<Entry> pharEntries = new LinkedList<>();
            addPharEntriesRecursively(pharEntries, source.toPath());
            return pharEntries;

        }

        private void addPharEntriesRecursively(final List<Entry> pharEntries, final java.nio.file.Path directory)
                throws IOException {
            try (DirectoryStream<java.nio.file.Path> directoryStream = Files.newDirectoryStream(directory)) {
                for (java.nio.file.Path element : directoryStream) {

                    String path = String.format("%s/%s", source.getName(), source.toPath().relativize(element).toString());

                    File file = element.toFile();
                    if (file.isDirectory() && file.list().length > 0) {
                        addPharEntriesRecursively(pharEntries, element);
                    } else {

                        if (file.isDirectory()) {
                            path = path + "/";
                        }
                        Entry entry = Entry.createFromFile(file, compression);
                        entry.getEntryManifest().getPath().setName(path.replace("\\", "/"));
                        pharEntries.add(entry);
                    }
                }
            }
        }
    }

    /**
     * Adds an entry to be stored in the archive.
     *
     * @param file
     * @throws java.io.IOException
     */
    public void add(final File file) throws IOException {
        add(file, Compression.Type.NONE);
    }

    /**
     * Adds an entry to be stored in the archive.
     *
     * @param file
     * @param entryCompression
     * @throws IOException
     */
    public void add(final File file, Compression.Type entryCompression) throws IOException {
        if (file.isDirectory()) {
            add(new DirectoryEntryProvider(file, entryCompression));
        } else {
            add(new FileEntryProvider(file, entryCompression));
        }
    }

    private void add(final EntryProvider pharEntryProvider) throws IOException {
        for (Entry entry : pharEntryProvider.getPharEntries()) {
            entries.add(entry);
            manifest.getEntryManifest().add(entry.getEntryManifest());
            manifest.setNumberOfFiles(entries.size());
        }
    }

    public void setStub(String stub) {
        this.stub = new Stub(stub);
    }

    public void setStub(File stubFile) throws IOException {
        setStub(new String(Files.readAllBytes(stubFile.toPath())));
    }

    public void setMetadata(Serializable meta) {
        manifest.getMetadata().setMeta(meta);
    }

    public final void parse() throws IOException {
        parse(new FileInputStream(this));
    }

    public void parse(InputStream inp) throws IOException {

        try (PharInputStream is = new PharInputStream(inp)) {

            stub.read(is);

            manifest.read(is);

            for (EntryManifest e : manifest.getEntryManifest()) {
                Entry entry = new Entry(e);
                entry.setSource(this);
                entry.read(is);
                entries.add(entry);
            }

            signature.read(is);

            is.close();
        }
    }

    public void write() throws IOException {
        try (PharOutputStream out = new PharOutputStream(new FileOutputStream(this))) {
            // Prepare entry data
            ByteArrayOutputStream entryData = new ByteArrayOutputStream();
            try (PharOutputStream pos = new PharOutputStream(entryData)) {
                for (Entry entry : entries) {
                    pos.write(entry);
                }
                pos.flush();
            }
            
            out.write(stub);
            out.write(manifest);
            out.write(entryData.toByteArray());
            out.flush();
            
            signature.calcSignature(this);
            out.write(signature);
            out.flush();
        }
    }

    public Entry findEntry(String name) {
        for (Iterator<Entry> it = entries.iterator(); it.hasNext();) {
            Entry entry = it.next();
            if (entry.getName().equals(name) || entry.getName().equals(name + "/")) {
                return entry;
            }
        }

        return null;
    }

    @Override
    public String[] list() {
        List<String> list = new LinkedList<>();

        for (EntryManifest e : manifest.getEntryManifest()) {
            list.add(e.getPath().toString());
        }

        return list.toArray(new String[list.size()]);
    }

    public String[] list(String folder) {
        List<String> list = new LinkedList<>();
        for (EntryManifest e : manifest.getEntryManifest()) {
            if (e.getPath().toString().startsWith(folder)) {
                list.add(e.getPath().toString());
            }
        }
        return list.toArray(new String[list.size()]);
    }

    public void rm(final String name) {
        for (Iterator<Entry> it = entries.iterator(); it.hasNext();) {
            Entry entry = it.next();
            if (entry.getName().equals(name)) {
                manifest.getEntryManifest().remove(entry.getEntryManifest());
                it.remove();
            }
        }
    }
    
    public void rmdir(final String folder) {
        for (Iterator<Entry> it = entries.iterator(); it.hasNext();) {
            Entry entry = it.next();
            if (entry.getName().startsWith(folder)) {
                manifest.getEntryManifest().remove(entry.getEntryManifest());
                it.remove();
            }
        }
    }

    public boolean mkdir(String folder) throws IOException {
        if (folder.endsWith("/")) {
            folder = folder + "/";
        }
        
        for (Entry entry : entries) {
            if (entry.getName().startsWith(folder))
                return false;
        }
        
        final String folderName = folder;
        add(new EntryProvider() {
            @Override
            public List<Entry> getPharEntries() throws IOException {
                List<Entry> list = new LinkedList<>();
                list.add(new Entry(folderName));
                return list;
            }
        });
        
        return true;
    }

}
