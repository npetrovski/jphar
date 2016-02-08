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

@Data
public class Compression implements Readable, Writable {

    static final int BITMAP_SIGNATURE_FLAG = 0x00010000;
    
    private Compression.Type type = Compression.Type.NONE;

    public enum Type {

        BZIP(0x00002000),
        ZLIB(0x00001000),
        NONE(0x00000000);

        public final int flag;

        private Type(final int i) {
            this.flag = i;
        }
        
        public int getFlag() {
            return this.flag;
        }

        public static Type getEnumByInt(int code) {
            for (Type e : Type.values()) {
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
        type = Compression.Type.getEnumByInt(f & 0x0000f000);
    }
    
    @Override
    public void write(PharOutputStream out) throws IOException {
        out.writeInt(type.getFlag());
    }
    
    @Override
    public String toString() {
        return new String(type.name());
    }
}
