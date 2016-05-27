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
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class Compression implements Readable, Writable {

    static final int BITMAP_SIGNATURE_FLAG = 0x00010000;

    @XmlValue
    private Compression.Sort type = Compression.Sort.NONE;

    @XmlType
    @XmlEnum(String.class)
    public enum Sort {

        @XmlEnumValue("BZIP") BZIP(0x00002000),
        @XmlEnumValue("ZLIB") ZLIB(0x00001000),
        @XmlEnumValue("NONE") NONE(0x00000000);

        public final int flag;

        private Sort(final int i) {
            this.flag = i;
        }

        public int getFlag() {
            return this.flag;
        }

        public static Sort getEnumByInt(int code) {
            for (Sort e : Sort.values()) {
                if (code == e.getFlag()) {
                    return e;
                }
            }
            return null;
        }
    }

    @Override
    public void read(PharInputStream is) throws IOException {
        int f = is.readRInt();
        type = Compression.Sort.getEnumByInt(f & 0x0000f000);
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        out.writeInt(type.getFlag());
    }

    @Override
    public String toString() {
        return type.name();
    }
}
