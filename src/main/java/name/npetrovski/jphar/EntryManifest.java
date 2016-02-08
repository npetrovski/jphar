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
public class EntryManifest implements Readable, Writable {

    private Name path = new Name();
    private Integer uncompressedSize = 0;
    private Integer timestamp = 0;
    private Integer compressedSize = 0;
    private Integer CRC32 = 0;
    private Compression compression = new Compression();
    private Metadata metadata = new Metadata();

    @Override
    public void read(PharInputStream is) throws IOException {

        path.read(is);

        uncompressedSize = is.readRInt();

        timestamp = is.readRInt();

        compressedSize = is.readRInt();

        CRC32 = is.readRInt();

        compression.read(is);

        metadata.read(is);
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        out.write(path);
        out.writeInt(uncompressedSize);
        out.writeInt(timestamp);
        out.writeInt(compressedSize);
        out.writeInt(CRC32);
        out.write(compression);
        out.write(metadata);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(path).append("\n")
            .append("UncompressedSize: ").append(uncompressedSize).append("\n")
            .append("Timestamp: ").append(timestamp).append("\n")
            .append("CompressedSize: ").append(compressedSize).append("\n")
            .append("CRC32: ").append(CRC32).append("\n")
            .append("Compression: ").append(compression).append("\n")
            .append("Metadata: ").append(metadata).append("\n");

        return sb.toString();
    }

}
