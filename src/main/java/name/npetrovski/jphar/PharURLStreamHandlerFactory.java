package name.npetrovski.jphar;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;


public class PharURLStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("phar".equals(protocol)) {
            return new PharURLStreamHandler();
        }
        
        return null;
    }
    
}
