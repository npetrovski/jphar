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

    public DirectoryPharEntryProvider(final File directory, final String localPath) {
        if (directory == null) {
            throw new IllegalArgumentException("Directory cannot be null");
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Directory must be a valid directory");
        }
        if (localPath == null || localPath.length() == 0) {
            throw new IllegalArgumentException("Local path cannot be empty");
        }

        this.rootPath = directory.toPath();
        this.localPath = localPath;

    }

    @Override
    public List<PharEntry> getPharEntries() throws IOException {
        List<PharEntry> pharEntries = new ArrayList<PharEntry>();

        addPharEntriesRecursively(pharEntries, this.rootPath);
        return pharEntries;

    }

    private void addPharEntriesRecursively(final List<PharEntry> pharEntries, final Path directory) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory)) {
            for (Path element : directoryStream) {
                File file = element.toFile();
                if (file.isDirectory()) {
                    addPharEntriesRecursively(pharEntries, element);
                } else {
                    String relativePath = this.rootPath.relativize(element).toString();
                    pharEntries.add(new PharEntry(file, this.localPath + "/" + relativePath, PharCompression.NONE));
                }
            }
        }
    }

}
