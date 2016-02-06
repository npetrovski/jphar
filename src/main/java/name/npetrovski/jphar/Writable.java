package name.npetrovski.jphar;

import java.io.IOException;

public interface Writable {

    void write(PharOutputStream out) throws IOException;
}
