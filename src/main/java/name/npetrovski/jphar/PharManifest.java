package name.npetrovski.jphar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public final class PharManifest implements PharWritable {

    private static final int BITMAP_SIGNATURE_FLAG = 0x00010000;

    private final String alias;
    
    private final int pharFilesLength;
    
    private final int pharEntriesCount;
    
    private PharMetadata pharMetadata;


    public PharManifest(
            final String alias,
            final int pharFilesLength,
            final int pharEntriesCount,
            final PharMetadata pharMetadata) {

        if (alias == null) {
            throw new IllegalArgumentException("Phar file cannot be null");
        }
        if (pharFilesLength == 0) {
            throw new IllegalArgumentException("Phar files length cannot be zero");
        }
        if (pharEntriesCount == 0) {
            throw new IllegalArgumentException("Phar entries count cannot be zero");
        }
        if (pharMetadata == null) {
            throw new IllegalArgumentException("Phar metadata cannot be null");
        }
        this.alias = alias;
        this.pharFilesLength = pharFilesLength;
        this.pharEntriesCount = pharEntriesCount;
        this.pharMetadata = pharMetadata;
    }

    @Override
    public void write(final PharOutputStream out) throws IOException {
        
        ByteArrayOutputStream metadataOutputStream = new ByteArrayOutputStream();
        
        try (PharOutputStream pharOutputStream = new PharOutputStream(metadataOutputStream)) {
            pharOutputStream.write(pharMetadata);
            pharOutputStream.flush();
        }

        byte[] metadataBytes = metadataOutputStream.toByteArray();
        
        
        byte[] pharAlias = alias.getBytes(Phar.STRING_ENCODING);

        // Length of manifest in bytes (1 MB limit)
        if (metadataBytes.length > 1024 * 1024) {
            throw new IOException("Phar manifest too large.");
        }
        out.writeInt(metadataBytes.length + pharFilesLength + pharAlias.length + 14);
        
        // Number of files in the Phar
        out.writeInt(pharEntriesCount); 

        // API version of the Phar manifest (currently 1.1.1)
        out.write(PharVersion.getVersionNibbles(Phar.DEFAULT_PHAR_VERSION));

        // Global Phar bitmapped flags
        out.writeInt(BITMAP_SIGNATURE_FLAG);

        // Length of Phar alias
        out.writeInt(pharAlias.length);
        
        // Phar alias (length based on previous)
        out.write(pharAlias);

        // write serializedMeta
        out.write(metadataBytes);
    }


}
