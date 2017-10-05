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

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlValue;
import java.io.IOException;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Compression implements Readable, Writable {

    static final int BITMAP_SIGNATURE_FLAG = 0x00010000;

    @XmlValue
    private Compression.Type type = Compression.Type.NONE;

    public Compression() {

    }

    public Compression(Type t) {
        this.type = t;
    }

    public Compression(String t) {
        Type found = Type.getEnumByName(t);
        if (null != found) {
            this.type = found;
        }
    }

    public enum Type {

        @XmlEnumValue("BZIP") BZIP(0x00002000, "BZIP"),
        @XmlEnumValue("ZLIB") ZLIB(0x00001000, "ZLIB"),
        @XmlEnumValue("NONE") NONE(0x00000000, "NONE");

        private final int flag;

        private final String compression;

        Type(final int i, final String name) {
            this.flag = i;
            this.compression = name;
        }

        public int getFlag() {
            return this.flag;
        }

        public String getType() {
            return this.compression;
        }

        public static Type getEnumByInt(int c) {
            for (Type e : Type.values()) {
                if (c == e.getFlag()) {
                    return e;
                }
            }
            return null;
        }

        public static Type getEnumByName(String n) {
            for (Type e : Type.values()) {
                if (n.equals(e.getType())) {
                    return e;
                }
            }
            return null;
        }
    }

    @Override
    public void read(PharInputStream is) throws IOException {
        int f = is.readRInt();
        type = Compression.Type.getEnumByInt(f & 0x0000f000);
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        out.writeInt(type.getFlag());
    }

}
