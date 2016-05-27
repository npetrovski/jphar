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

import java.io.IOException;
import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

@Data
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Version implements Readable, Writable {

    @XmlValue
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
