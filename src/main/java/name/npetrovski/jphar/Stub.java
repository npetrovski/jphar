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
import java.io.*;
import java.nio.file.Files;

@Data
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Stub implements Entry, Readable, Writable {

    static final String DEFAULT_STUB = "<?php\n__HALT_COMPILER(); ?>\n";

    static final String DEFAULT_PATH = ".phar/stub.php";

    @XmlValue
    private String code = DEFAULT_STUB;

    @XmlTransient
    private Integer lastModified = (int) (System.currentTimeMillis() / 1000);

    public Stub() {
        this(DEFAULT_STUB);
    }

    public Stub(final String code) {
        assert (code.contains("__HALT_COMPILER"));
        this.code = code;
    }

    public Stub(final File path) throws IOException {
        this(new String(Files.readAllBytes(path.toPath())));
        lastModified = (int) path.lastModified() / 1000;
    }

    @Override
    public String getName() {
        return DEFAULT_PATH;
    }

    @Override
    public Integer getSize() {
        return code.length();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public Integer getLastModified() {
        return lastModified;
    }

    @Override
    public InputStream getInputStream() {
        if (code.charAt(code.length() - 1) != '\r' && code.charAt(code.length() - 1) != '\n') {
            code = code + "\n";
        }
        return new ByteArrayInputStream(code.getBytes());
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        try (InputStream is = getInputStream()) {
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int n = 0; n >= 0; n = is.read(buffer)) {
                data.write(buffer, 0, n);
            }
            data.close();

            return data;
        }
    }

    @Override
    public void read(PharInputStream is) throws IOException {
        int c;
        code = "";
        while ((c = is.read()) != -1) {
            code = code.concat(Character.toString((char) c));
            if (code.length() >= 3 && (code.endsWith("?>\r\n") || code.endsWith("?>\n"))) {
                break;
            }
        }
    }

    @Override
    public void write(PharOutputStream out) throws IOException {
        ByteArrayOutputStream data = (ByteArrayOutputStream) getOutputStream();
        out.write(data.toByteArray());
    }

    @Override
    public String toString() {
        return this.code;
    }
}
