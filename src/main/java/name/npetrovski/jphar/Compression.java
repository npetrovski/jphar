package name.npetrovski.jphar;

import java.io.IOException;
import lombok.Data;

@Data
public class Compression implements Parsable, Writable {

    static final int BITMAP_SIGNATURE_FLAG = 0x00010000;
    
    private Compression.Type type = Compression.Type.NONE;

    public enum Type {

        BZIP(0x00002000),
        ZLIB(0x00001000),
        NONE(0x00000000);

        public final int flag;

        private Type(final int i) {
            this.flag = i;
        }
        
        public int getFlag() {
            return this.flag;
        }

        public static Type getEnumByInt(int code) {
            for (Type e : Type.values()) {
                if (code == e.getFlag()) {
                    return e;
                }
            }
            return null;
        }
    }

    @Override
    public void parse(PharInputStream is) throws IOException {
        int f = is.readRInt();
        type = Compression.Type.getEnumByInt(f & 0x0000f000);
    }
    
    @Override
    public void write(PharOutputStream out) throws IOException {
        out.writeInt(type.getFlag());
    }
    
    @Override
    public String toString() {
        return new String(type.name());
    }
}
