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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import lombok.Data;

@Data
public class Signature implements Readable, Writable {

    private byte[] signature;

    private Signature.Type type = Signature.Type.SHA1;

    private byte[] magic = "GBMB".getBytes();

    public enum Type {

        MD5(0x0001, "MD5"),
        SHA1(0x0002, "SHA-1"),
        SHA256(0x0004, "SHA-256"),
        SHA512(0x0008, "SHA-512");

        private final int flag;
        private final String algorithm;

        private Type(final int flag, final String algorithm) {
            this.flag = flag;
            this.algorithm = algorithm;
        }

        public int getFlag() {
            return this.flag;
        }

        public String getAlgorithm() {
            return this.algorithm;
        }

        public static Type getEnumByFlag(int code) {
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
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int b;
        while ((b = is.read()) != -1) {
            buffer.write(b);
        }
        buffer.flush();

        byte[] data = buffer.toByteArray();
        signature = Arrays.copyOfRange(data, 0, data.length - 8);

        byte[] typedata = Arrays.copyOfRange(data, data.length - 8, data.length - 4);
        type = Signature.Type.getEnumByFlag(
                (typedata[3] << 24) + (typedata[2] << 16)
                + (typedata[1] << 8) + (typedata[0] << 0));
    }

    public void calcSignature(File file) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(type.getAlgorithm());
            md.update(Files.readAllBytes(file.toPath()));
            signature = md.digest();
        } catch (NoSuchAlgorithmException ex) {
        }
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        out.write(signature);
        out.writeInt(type.getFlag());
        out.write(magic);
    }

}
