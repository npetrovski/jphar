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
