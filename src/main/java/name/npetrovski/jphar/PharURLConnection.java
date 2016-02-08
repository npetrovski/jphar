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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class PharURLConnection extends URLConnection {

    private final URL pharFileURL;

    private final String entryName;

    protected PharURLConnection(URL url) throws MalformedURLException {
        super(url);

        if (!url.getProtocol().equals("phar")) {
            throw new MalformedURLException(url + ": Not phar protocol.");
        }

        String spec = url.getFile();
        int bang = spec.indexOf("!/");
        if (bang == -1) {
            throw new MalformedURLException(url + ": No `!/' in spec.");
        }

        pharFileURL = new URL(spec.substring(0, bang));

        entryName = spec.length() == (bang + 2) ? null : spec.substring(bang + 2);
    }

    @Override
    public void connect() throws IOException {
    }

    public URL getPharFileURL() {
        return pharFileURL;
    }

    public String getEntryName() {
        return entryName;
    }

    public Phar getPharFile() {
        return new Phar(pharFileURL.getPath());
    }

    public Entry getPharEntry() {
        if (null == entryName) {
            return null;
        }
        Phar pharFile = getPharFile();
        return pharFile != null ? pharFile.findEntry(entryName) : null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return getPharEntry().getInputStream();
    }

}
