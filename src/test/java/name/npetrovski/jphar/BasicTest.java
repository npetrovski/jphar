package name.npetrovski.jphar;

import java.io.File;
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

    public void testCreate() {
        try {
            
            File pharfile = new File(testPharFile);
            // Create
            Phar p = new Phar(pharfile, PharCompression.BZIP2);
            p.add(new File("src/test/resources/Image"));
            p.setStubFile(new File("src/test/resources/stub.php"));
            p.setMetadata(
                    new HashMap<String, String>() {
                {
                    put("Author", "Nikolay Petrovski");
                }
            }
            );

            p.write();
            
            assertTrue(pharfile.exists());
            assertTrue(pharfile.length() > 0);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void parseTest() {

        try {
            // READ
            Phar pa = new Phar(new File(testPharFile));

            PharCompression c = pa.getCompression();

            assertEquals(PharCompression.BZIP2, pa.getCompression());

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
