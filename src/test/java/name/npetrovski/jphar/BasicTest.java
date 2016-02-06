package name.npetrovski.jphar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.compress.utils.IOUtils;

public class BasicTest extends TestCase {

    private static final String testPharFile = "src/test/resources/test-" + String.valueOf((System.currentTimeMillis() / 1000L)) + ".phar";

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

    public void testCreateNew() throws IOException, NoSuchAlgorithmException {

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

    public void testPhpunit() throws IOException {
        final Phar p = new Phar("src/test/resources/phpunit-5.2.1.phar");
        assertEquals(542, p.getEntries().size());
        assertEquals(Compression.Type.NONE, p.getManifest().getCompression().getType());
        assertEquals("1.1.0", p.getManifest().getVersion().getVersion());
    }

    public void testCodeception() throws IOException {
        final Phar p = new Phar("src/test/resources/codecept.phar");
        assertEquals(965, p.getEntries().size());
        assertEquals(Compression.Type.ZLIB, p.getManifest().getCompression().getType());
        assertEquals("1.1.0", p.getManifest().getVersion().getVersion());
        
        Entry entry = p.findEntry("src/Codeception/Events.php");
   
        assertNotNull(entry);
        
        String data = new String(IOUtils.toByteArray(entry.getInputStream()));
        
        assertTrue(data.contains("SUITE_INIT"));
    }

    @Override
    public void tearDown() {

    }
}
