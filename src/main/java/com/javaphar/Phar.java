package com.javaphar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Phar {

    public static final String PHAR_VERSION = "1.1.0";

    private final File file;

    protected PharCompression pharCompression;

    protected String stubCode = "";

    protected String alias = "";

    protected List<PharEntry> entries = new ArrayList<PharEntry>();

    protected Map<String, String> metadata = new HashMap<>();

    public Phar(final File file) throws IOException {
        this(file, PharCompression.NONE);
    }

    public Phar(final File file, final PharCompression pharCompression) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        this.file = file;

        if (file.exists()) {
            read();
        }

        if (pharCompression == null) {
            throw new IllegalArgumentException("Phar compression cannot be null");
        }

        this.pharCompression = pharCompression;
    }

    /**
     * Adds an entry to be stored in the phar archive.
     *
     * @param file
     */
    public void add(final File file) throws IOException {
        add(file, file.getName());
    }

    /**
     * Adds an entry to be stored in the phar archive.
     *
     * @param file
     * @param localPath
     */
    public void add(final File file, final String localPath) throws IOException {
        if (file.isDirectory()) {
            add(new DirectoryPharEntryProvider(file, localPath, this.pharCompression));
        } else {
            add(new FilePharEntryProvider(file, localPath, this.pharCompression));
        }
    }

    private void add(final PharEntryProvider pharEntryProvider) throws IOException {
        for (PharEntry pharEntry : pharEntryProvider.getPharEntries()) {
            this.entries.add(pharEntry);
        }
    }

    public void setMetadata(final String key, final String value) {
        this.metadata.put(key, value);
    }

    public void setStub(final String stubCode) {
        this.stubCode = stubCode;
    }

    public String getStub() {
        return this.stubCode;
    }

    public List getEntries() {
        return this.entries;
    }
    
    public PharEntry getEntry(String path) throws IOException {
        
        PharEntry entry = null;
        
        for (PharEntry e : this.entries) {
            if (path.equals(e.getLocalPath())) {
                entry = e;
                break;
            }
        }
        
        RandomAccessFile reader;
        if (entry != null && entry.getDataOffset().length > 0) {
            reader = new RandomAccessFile(file, "r");

            long[] dataOffset = entry.getDataOffset();

            reader.seek(dataOffset[0]);

            byte[] data = new byte[(int) dataOffset[1]];
            reader.read(data, 0, (int) dataOffset[1]);

            entry.decompress(data);

            reader.close();
        }

        return entry;
    }

    public void read() throws IOException, FileNotFoundException {
        PharInputStream input = null;
        try {
            input = new PharInputStream(new FileInputStream(this.file), this);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    public void write() throws IOException, NoSuchAlgorithmException {
        PharOutputStream out = null;
        try {
            out = new PharOutputStream(new FileOutputStream(this.file));

            // write stub
            out.write(new PharStub(this.stubCode));

            byte[] pharFiles = writePharEntriesManifest();

            PharManifest pharManifest = new PharManifest(
                    this.file, pharFiles, this.entries, new PharMetadata(this.metadata));

            out.write(pharManifest);
            out.write(pharFiles);

            for (PharEntry pharEntry : this.entries) {
                out.write(pharEntry.getPharEntryData());
            }

            // flush to file before creating signature
            out.flush();

            // writing signature
            out.write(new PharSignature(this.file, PharSignatureType.SHA1));

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

}
