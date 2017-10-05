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

import javax.xml.bind.annotation.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

@Data
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Signature implements Readable, Writable {

    private static final Logger LOGGER = Logger.getLogger(Signature.class.getName());

    private byte[] signature;

    @XmlAttribute(name = "signatureType")
    private Signature.Algorithm algorithm = Signature.Algorithm.SHA1;

    private final String magic = "GBMB";

    public enum Algorithm {

        @XmlEnumValue("MD5") MD5(0x0001, "MD5"),
        @XmlEnumValue("SHA-1") SHA1(0x0002, "SHA-1"),
        @XmlEnumValue("SHA-256") SHA256(0x0004, "SHA-256"),
        @XmlEnumValue("SHA-512") SHA512(0x0008, "SHA-512");

        private final int flag;

        private final String algorithm;

        Algorithm(final int flag, final String algorithm) {
            this.flag = flag;
            this.algorithm = algorithm;
        }

        public int getFlag() {
            return this.flag;
        }

        public String getAlgorithm() {
            return this.algorithm;
        }

        public static Algorithm getEnumByFlag(int code) {
            for (Algorithm e : Algorithm.values()) {
                if (code == e.getFlag()) {
                    return e;
                }
            }
            return null;
        }

        public static Algorithm getEnumByName(String name) {
            for (Algorithm e : Algorithm.values()) {
                if (name.equals(e.getAlgorithm())) {
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
        algorithm = Signature.Algorithm.getEnumByFlag(
                (typedata[3] << 24) + (typedata[2] << 16) + (typedata[1] << 8) + (typedata[0]));
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        out.write(signature);
        out.writeInt(algorithm.getFlag());
        out.write(magic.getBytes());
    }

    /**
     * Calculate file signature
     *
     */
    public void calcSignature(File file) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(algorithm.getAlgorithm());
            md.update(Files.readAllBytes(file.toPath()));
            signature = md.digest();
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
    }

}
