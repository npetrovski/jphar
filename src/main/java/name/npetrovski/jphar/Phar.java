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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class Phar extends File {

    private static final Logger LOGGER = Logger.getLogger(Phar.class.getName());

    private Stub stub = new Stub();

    private Manifest manifest = new Manifest();

    private List<DataEntry> entries = new LinkedList<>();

    private Signature signature = new Signature();

    public Phar(String pathname) {
        super(pathname);

        if (super.exists() && super.isFile()) {
            read();
        } else {
            manifest.getAlias().setName(super.getName());
        }
    }

    public Phar(File file) {
        this(file.getPath());
    }

    private interface EntryProvider {

        List<DataEntry> getPharEntries() throws IOException;
    }

    private final class FileEntryProvider implements EntryProvider {

        private final File source;
        private final Compression.Type compression;

        public FileEntryProvider(final File file, final Compression.Type compression) {
            this.source = file;
            this.compression = compression;
        }

        @Override
        public List<DataEntry> getPharEntries() throws IOException {
            return new LinkedList<DataEntry>() {
                {
                    add(DataEntry.createFromFile(source, compression));
                }
            };
        }

    }

    private final class DirectoryEntryProvider implements EntryProvider {

        private final File source;
        private final Compression.Type compression;

        public DirectoryEntryProvider(final File file, final Compression.Type compression) {
            this.source = file;
            this.compression = compression;
        }

        @Override
        public List<DataEntry> getPharEntries()
                throws IOException {
            List<DataEntry> entries = new LinkedList<>();
            addPharEntriesRecursively(entries, source.toPath());
            return entries;

        }

        private void addPharEntriesRecursively(final List<DataEntry> entries, final java.nio.file.Path directory)
                throws IOException {
            try (DirectoryStream<java.nio.file.Path> directoryStream = Files.newDirectoryStream(directory)) {
                for (java.nio.file.Path element : directoryStream) {

                    String path = String.format("%s/%s", source.getName(),
                            source.toPath().relativize(element).toString());

                    File file = element.toFile();
                    if (file.isDirectory() && file.list().length > 0) {
                        addPharEntriesRecursively(entries, element);
                    } else {

                        if (file.isDirectory()) {
                            path = path + "/";
                        }
                        DataEntry entry = DataEntry.createFromFile(file, this.compression);
                        entry.getEntryManifest().getPath().setName(path.replace("\\", "/"));
                        entries.add(entry);
                    }
                }
            }
        }
    }


    /**
     * Adds an entry to be stored in the archive.
     *
     * @param pharEntryProvider
     * @throws IOException
     */
    private void add(final EntryProvider pharEntryProvider) throws IOException {
        for (DataEntry entry : pharEntryProvider.getPharEntries()) {
            entries.add(entry);
            manifest.getEntryManifest().add(entry.getEntryManifest());
            manifest.setNumberOfFiles(entries.size());
        }
    }

    public void add(final File file, final Compression.Type compression) throws IOException {
        if (file.isDirectory()) {
            add(new DirectoryEntryProvider(file, compression));
        } else {
            add(new FileEntryProvider(file, compression));
        }
    }

    public void add(final File file) throws IOException {
        add(file, Compression.Type.NONE);
    }

    public void add(final File file, String compression) throws IOException {
        Compression.Type type = Compression.Type.getEnumByName(compression);
        if (null == type) {
            add(file);
        } else {
            add(file, type);
        }
    }

    /**
     * Set stub code
     *
     * @param stub
     */
    public void setStub(final String stub) {
        this.stub = new Stub(stub);
    }

    public void setStub(final File stubFile) throws IOException {
        this.stub = new Stub(stubFile);
    }

    /**
     * Set PHAR signature algorithm
     *
     * @param type
     */
    public void setSignatureAlgorithm(final Signature.Algorithm algorithm) {
        signature.setAlgorithm(algorithm);
    }

    public void setSignatureAlgorithm(final String algorithm) {
        Signature.Algorithm sign = Signature.Algorithm.getEnumByName(algorithm);
        if (null != sign) {
            setSignatureAlgorithm(sign);
        }
    }

    /**
     * Set PHAR file meta-data
     *
     * @param data
     */
    public void setMetadata(final Serializable data) {
        manifest.setMetadata(new Metadata(data));
    }

    /**
     * Read PHAR file
     *
     * @throws IOException
     */
    private void read() {
        try {
            read(new FileInputStream(this));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
    }

    /**
     * Read PHAR file
     *
     * @param inp
     * @throws IOException
     */
    private void read(InputStream inp) throws IOException {

        try (PharInputStream is = new PharInputStream(inp)) {

            stub.read(is);

            manifest.read(is);

            for (EntryManifest e : manifest.getEntryManifest()) {
                DataEntry entry = new DataEntry(e);
                entry.setSource(this);
                entry.read(is);
                entries.add(entry);
            }

            signature.read(is);

            is.close();
        }
    }

    /**
     * Write into PHAR file
     *
     * @throws IOException
     */
    public void write() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try (PharOutputStream out = new PharOutputStream(data)) {
            // Prepare entry data
            ByteArrayOutputStream entryData = new ByteArrayOutputStream();
            try (PharOutputStream pos = new PharOutputStream(entryData)) {
                for (DataEntry entry : entries) {
                    pos.write(entry);
                }
                pos.flush();
            }

            out.write(stub);
            out.write(manifest);
            out.write(entryData.toByteArray());
            out.flush();

        }

        try (PharOutputStream phar = new PharOutputStream(new FileOutputStream(this))) {
            phar.write(data.toByteArray());
            phar.flush();

            signature.calcSignature(this);
            phar.write(signature);
            phar.flush();
            phar.close();
        }
    }

    /**
     * Find PHAR entry by name
     *
     * @param name
     * @return
     */
    public Entry findEntry(String name) {

        if (name.equals(Stub.DEFAULT_PATH)) {
            return stub;
        }

        for (Iterator<DataEntry> it = entries.iterator(); it.hasNext();) {
            DataEntry entry = it.next();
            if (entry.getName().equals(name) || entry.getName().equals(name + "/")) {
                return entry;
            }
        }

        return null;
    }

    /**
     * List PHAR entries
     *
     * @return
     */
    @Override
    public String[] list() {
        List<String> list = new LinkedList<>();
        list.add(Stub.DEFAULT_PATH);
        for (EntryManifest e : manifest.getEntryManifest()) {
            list.add(e.getPath().toString());
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * List PHAR entries filtered by name
     *
     * @param startsWith
     * @return
     */
    public String[] list(String startsWith) {
        List<String> list = new LinkedList<>();

        for (String s : list()) {
            if (s.startsWith(startsWith)) {
                list.add(s);
            }
        }

        return list.toArray(new String[list.size()]);
    }

    /**
     * Remove PHAR entry by name
     *
     * @param name
     */
    public void rm(final String name) {
        for (Iterator<DataEntry> it = entries.iterator(); it.hasNext();) {
            DataEntry entry = it.next();
            if (entry.getName().equals(name)) {
                manifest.getEntryManifest().remove(entry.getEntryManifest());
                it.remove();
            }
        }

        manifest.setNumberOfFiles(entries.size());
    }

    /**
     * Remove PHAR entries/directory
     *
     * @param startsWith
     */
    public void rmdir(final String startsWith) {
        for (Iterator<DataEntry> it = entries.iterator(); it.hasNext();) {
            DataEntry entry = it.next();
            if (entry.getName().startsWith(startsWith)) {
                manifest.getEntryManifest().remove(entry.getEntryManifest());
                it.remove();
            }
        }

        manifest.setNumberOfFiles(entries.size());
    }

    /**
     * Create PHAR empty folder
     *
     * @param folder
     * @return DataEntry
     * @throws IOException
     */
    public DataEntry mkdir(String folder) throws IOException {
        if (folder.endsWith("/")) {
            folder = folder + "/";
        }

        for (DataEntry e : entries) {
            if (e.getName().startsWith(folder)) {
                return e;
            }
        }

        final String folderName = folder;
        final DataEntry entry = new DataEntry(folderName);
        add(new EntryProvider() {
            @Override
            public List<DataEntry> getPharEntries() throws IOException {
                List<DataEntry> list = new LinkedList<>();
                list.add(entry);
                return list;
            }
        });

        return entry;
    }

    @Data
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    static class Root {

        @XmlElement
        private Phar phar;
    }

    /**
     * Convert into XML
     *
     * @return String
     * @throws JAXBException
     */
    public String toXml() throws JAXBException {

        StringWriter sw = new StringWriter();

        JAXBContext jaxbContext = JAXBContext.newInstance(Root.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        Root root = new Root();
        root.setPhar(this);

        jaxbMarshaller.marshal(root, sw);

        return sw.toString();
    }

    public void toXml(File file) throws IOException, JAXBException {

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        JAXBContext jaxbContext = JAXBContext.newInstance(Root.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        Root root = new Root();
        root.setPhar(this);

        jaxbMarshaller.marshal(root, writer);
    }

    /**
     * Convert from XML
     *
     * @param xml String
     * @return Phar
     * @throws JAXBException
     */
    public static Phar fromXml(String xml) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Root.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        return ((Root) jaxbUnmarshaller.unmarshal(new StringReader(xml))).getPhar();
    }

    public static Phar fromXml(File file) throws IOException, JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Root.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        return ((Root) jaxbUnmarshaller.unmarshal(file)).getPhar();
    }

}
