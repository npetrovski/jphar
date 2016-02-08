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
import de.ailis.pherialize.Pherialize;
import java.io.Serializable;
import lombok.Data;

@Data
public class Metadata implements Readable, Writable {

    private Serializable meta;

    @Override
    public void read(PharInputStream is) throws IOException {
        int len = is.readRInt();

        if (len > 0) {
            byte[] metadataBytes = new byte[len];
            is.read(metadataBytes, 0, len);

            meta = Pherialize.unserialize(new String(metadataBytes));
        }
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        if (null != meta) {
            byte[] data = Pherialize.serialize(meta).getBytes("UTF-8");
            out.writeInt(data.length);
            out.write(data);
        } else {
            out.writeInt(0);
        }
    }

    @Override
    public String toString() {
        return (null != meta) ? Pherialize.serialize(meta) : "";
    }
}
