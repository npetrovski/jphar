package name.npetrovski.jphar;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DirectoryPharEntryProvider implements PharEntryProvider {

    private final Path rootPath;
    private final String localPath;
    private final PharCompression pharCompression;

    public DirectoryPharEntryProvider(final File directory, final PharCompression pharCompression) {

        if (directory == null) {
            throw new IllegalArgumentException("Directory cannot be null");
        }

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Directory must be a valid directory");
        }

        if (pharCompression == null) {
            throw new IllegalArgumentException("Phar compression cannot be null");
        }
        this.rootPath = directory.toPath();
        this.localPath = directory.getName();
        this.pharCompression = pharCompression;

    }

    @Override
    public List<PharEntry> getPharEntries() throws IOException {
        List<PharEntry> pharEntries = new ArrayList<PharEntry>();
        addPharEntriesRecursively(pharEntries, rootPath);
        return pharEntries;

    }

    private void addPharEntriesRecursively(final List<PharEntry> pharEntries, final Path directory) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path element : directoryStream) {

                String path = String.format("%s/%s", localPath, rootPath.relativize(element).toString());

                File file = element.toFile();
                if (file.isDirectory() && file.list().length > 0) {
                    addPharEntriesRecursively(pharEntries, element);
                } else {
                    PharEntry entry = new PharEntry(path, pharCompression);
                    entry.pack(file);
                    pharEntries.add(entry);
                }
            }
        }
    }

}
