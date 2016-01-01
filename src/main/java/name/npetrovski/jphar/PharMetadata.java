package name.npetrovski.jphar;

import de.ailis.pherialize.Mixed;
import de.ailis.pherialize.Pherialize;
import java.io.IOException;

public final class PharMetadata implements PharWritable {

    private final String serialized;

    public PharMetadata(final String phpserialized) {
        if (phpserialized == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        serialized = phpserialized;
    }

    public PharMetadata(final Object data) {
        if (data == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        serialized = Pherialize.serialize(data);
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        byte[] metadataBytes = new byte[0];
        if (!serialized.isEmpty()) {
            metadataBytes = this.serialized.getBytes(Phar.STRING_ENCODING);
        }
        out.writeInt(metadataBytes.length);
        out.write(metadataBytes);
    }

    public Mixed unserialize() {
        if (null != serialized) {
            return Pherialize.unserialize(serialized);
        }

        return null;
    }
}
