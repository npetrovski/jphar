package name.npetrovski.jphar;

import java.io.IOException;


public final class PharMetadata implements PharWritable {

    private final String metadata;

    public PharMetadata(final String metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        this.metadata = metadata;
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        byte[] metadataBytes = new byte[0];
        if (!this.metadata.isEmpty()) {
            metadataBytes = this.metadata.getBytes(Phar.STRING_ENCODING);
        }
        out.writeInt(metadataBytes.length);
        out.write(metadataBytes);
    }

}
