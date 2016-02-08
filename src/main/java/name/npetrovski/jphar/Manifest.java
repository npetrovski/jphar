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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Manifest implements Readable, Writable {

    private Integer len = 0;

    private Integer numberOfFiles = 0;

    private Version version = new Version();

    private Compression compression = new Compression();

    private Name alias = new Name();

    private Metadata metadata = new Metadata();

    private List<EntryManifest> entryManifest = new ArrayList<>();

    @Override
    public void read(PharInputStream is) throws IOException {
        len = is.readRInt() + 4;
        numberOfFiles = is.readRInt();

        version.read(is);

        compression.read(is);

        alias.read(is);

        metadata.read(is);

        for (int i = 0; i < numberOfFiles; i++) {
            EntryManifest em = new EntryManifest();
            em.read(is);
            entryManifest.add(em);
        }

    }

    @Override
    public void write(PharOutputStream out) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Global Phar bitmapped flags
        int globalZlibFlag = 0;
        int globalBzipFlag = 0;
        for (EntryManifest em : entryManifest) {
            if (em.getCompression().getType() == Compression.Type.BZIP) {
                globalBzipFlag = 0x00002000;
            }

            if (em.getCompression().getType() == Compression.Type.ZLIB) {
                globalZlibFlag = 0x00001000;
            }
        }
        int globalCompressionFlag = Compression.BITMAP_SIGNATURE_FLAG | globalZlibFlag | globalBzipFlag;

        try (PharOutputStream buffer = new PharOutputStream(byteArrayOutputStream)) {
            buffer.writeInt(entryManifest.size());
            buffer.write(version);
            buffer.writeInt(globalCompressionFlag);
            buffer.write(alias);
            buffer.write(metadata);
            for (EntryManifest em : entryManifest) {
                buffer.write(em);
            }

            buffer.flush();
        }

        byte[] data = byteArrayOutputStream.toByteArray();
        out.writeInt(data.length);
        out.write(data);

    }

}
