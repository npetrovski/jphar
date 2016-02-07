package name.npetrovski.jphar;

import java.io.IOException;
import lombok.Data;

@Data
public class Stub implements Readable, Writable {

    static final String DEFAULT_STUB = "<?php\n__HALT_COMPILER(); ?>\n";

    private String code = "";

    public Stub() {
        this(DEFAULT_STUB);
    }

    public Stub(String code) {
        this.code = code;
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
        if (code.charAt(code.length() - 1) != '\r' && code.charAt(code.length() - 1) != '\n') {
            code = code + "\n";
        }
        out.write(code.getBytes("UTF-8"));
    }

    @Override
    public String toString() {
        return this.code;
    }
}
