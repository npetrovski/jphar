package name.npetrovski.jphar;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BasicTest extends TestCase {

    private static final String testPharFile = "test-" + String.valueOf((System.currentTimeMillis() / 1000L)) + ".phar";

    /**
     * Returns the test suite.
     *
     * @return The test suite
     */
    public static Test suite() {
        return new TestSuite(BasicTest.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testCreated() throws IOException, NoSuchAlgorithmException {

        // Create
        final Phar p = new Phar(testPharFile);
        p.add(new File("src/test/resources/Image"), Compression.Type.ZLIB);
        p.setStub(new File("src/test/resources/stub.php"));
        p.setMetadata(new HashMap<String, String>() {
            {
                put("Author", "Nikolay Petrovski");
            }
        }
        );

        p.write();

        assertTrue(p.exists());
        assertTrue(p.canRead());
        assertTrue(p.length() > 0);

    }

    public void testParse() throws IOException {

        // READ
        final Phar pa = new Phar(testPharFile);

        assertEquals(Compression.Type.ZLIB, pa.getManifest().getCompression().getType());
    }

    @Override
    public void tearDown() {

    }
}
