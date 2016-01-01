package name.npetrovski.jphar;

public enum PharCompression {
        
    /**
     * File is compressed with zlib compression - 0 x 00 00 10 00.
     */
    GZIP(new byte[]{0, 0x10, 0, 0}),
    /**
     * File is compressed with bzip compression - 0 x 00 00 20 00.
     */
    BZIP2(new byte[]{0, 0x20, 0, 0}),
    /**
     * No compression
     */
    NONE(new byte[]{0, 0, 0, 0});

    public final byte[] bitmapFlag;

    private PharCompression(final byte[] bitmapFlag) {
        this.bitmapFlag = bitmapFlag;
    }

    public byte[] getBitmapFlag() {
        return this.bitmapFlag;
    }

    public static PharCompression getEnumByInt(int code) {
        for (PharCompression e : PharCompression.values()) {
            int i = (e.bitmapFlag[3] << 24) & 0xff000000 | 
                    (e.bitmapFlag[2] << 16) & 0x00ff0000 | 
                    (e.bitmapFlag[1] << 8) & 0x0000ff00 | 
                    (e.bitmapFlag[0] /*<< 0*/) & 0x000000ff;
            if (code == i) {
                return e;
            }
        }
        return null;
    }
}
