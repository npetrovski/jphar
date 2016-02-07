package name.npetrovski.jphar;

import java.io.IOException;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import lombok.Data;

@Data
public class Version implements Readable, Writable {

    private String version = "1.1.1";

    public static String getVersionString(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a) {
            sb.append(String.format("%02x", b & 0xff));
        }
        char[] version = new char[]{sb.charAt(0), '.', sb.charAt(1), '.', sb.charAt(2)};

        return new String(version);
    }

    public static byte[] getVersionNibbles(final String version) throws NumberFormatException {
        String[] splitted = version.split("\\.");
        if (splitted.length != 3) {
            throw new IllegalArgumentException("Version must contains 3 parts");
        }

        String[] hex = new String[4];

        for (int i = 0; i < splitted.length; i++) {
            int versionPartInt = Integer.parseInt(splitted[i]);
            if (versionPartInt < 0) {
                throw new NumberFormatException("Version cannot contains negative numbers");
            }
            if (versionPartInt > 15) {
                throw new NumberFormatException("Version cannot contains part over 15");
            }

            hex[i] = Integer.toHexString(versionPartInt);
        }

        hex[3] = "0"; // last nibble is not used

        HexBinaryAdapter adapter = new HexBinaryAdapter();

        byte[] nibbles = new byte[2];
        nibbles[0] = adapter.unmarshal(hex[0] + hex[1])[0];
        nibbles[1] = adapter.unmarshal(hex[2] + hex[3])[0];

        return nibbles;
    }

    @Override
    public void read(PharInputStream is) throws IOException {
        byte[] v = new byte[2];
        is.read(v, 0, 2);
        version = getVersionString(v);
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        out.write(getVersionNibbles(version));
    }
    
    @Override
    public String toString() {
        return this.version;
    }
}
