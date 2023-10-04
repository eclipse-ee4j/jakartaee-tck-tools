import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakartatck.jar2shrinkwrap.Jar2ShrinkWrap;
import jakartatck.jar2shrinkwrap.JarProcessor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;


import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test that the com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes pkg
 * has the expected contents.
 */
public class Jar2ShrinkwrapPkgTest {

    @Test
    public void canLocateTestDefinitions() {
        String[] expectedClasses = {"com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TCKServletContainerInitializer",
                "com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TestListener",
                "com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TestServlet",
                "com.sun.ts.tests.servlet.common.servlets.GenericTCKServlet",
                "com.sun.ts.tests.servlet.common.util.Data",
                "com.sun.ts.tests.servlet.common.util.ServletTestUtil"};

        JarProcessor war = Jar2ShrinkWrap.fromPackage("com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes");
        ArrayList<String> classes = war.getClasses();
        System.out.printf("Classes: %s\n", classes);
        HashSet<String> classesSet = new HashSet<>(Arrays.asList(expectedClasses));
        HashSet<String> warClassesSet = new HashSet<>(classes);
        Iterator<String> iterator = classesSet.iterator();
        while(iterator.hasNext()) {
            String c = iterator.next();
            assertTrue(warClassesSet.contains(c));
            iterator.remove();
        }
        assertTrue(classesSet.size() == 0);

        System.out.printf("Libraries: %s\n", war.getLibraries());
        System.out.printf("LibDir: %s\n", war.getBaseDir());
        File initializeJar = new File(war.getBaseDir(), war.getLibraries().get(0));
        if(!initializeJar.exists()) {
            System.out.printf("initilizer.jar does not exist in war libDir: %s\n", initializeJar.getAbsolutePath());
            System.exit(2);
        }
        JavaArchive jar = ShrinkWrap.createFromZipFile(JavaArchive.class, initializeJar);
        System.out.printf("initilizer.jar contents: %s\n", jar.toString(true));
        System.out.println("---\n");

        // Sample tests code for what needs to be done in the deployment method to add the extracted war jars
        List<File> libraryFiles = new ArrayList<>();
        for (String jarName : war.getLibraries()) {
            File jarFile = new File(war.getBaseDir(), jarName);
            libraryFiles.add(jarFile);
        }
        List<JavaArchive> warJars =
                libraryFiles.stream().map(file -> ShrinkWrap.createFromZipFile(JavaArchive.class, file))
                        .collect(Collectors.toList());
        // WebArchive war = ...; war.addAsLibraries(warJars);

        System.out.printf("initilizer.jar size: %s\n", initializeJar.length());
        System.out.printf("Metainf: %s\n", war.getMetainf());
        System.out.printf("Webinf: %s\n", war.getWebinf());
        System.out.printf("OtherFiles: %s\n", war.getOtherFiles());

        assertEquals("initilizer.jar", war.getLibraries().get(0));
        assertEquals("web.xml",war.getWebinf().get(0));
        assertTrue(0 == war.getOtherFiles().size());

        // Write the java source to the console
        StringWriter src = new StringWriter();
        war.saveOutput(src, false);
        System.out.printf("\nJavaSource:\n%s\n", src.toString());
    }

    @Test
    public void testFindServletTestPkgs() throws IOException {
        Set<String> testPkgNames = Jar2ShrinkWrap.getTestPkgNames("com/sun/ts/tests/servlet");
        assertTrue(0 < testPkgNames.size());
        System.out.printf("Found %d test pkgs\n", testPkgNames.size());
        // Just a few random pkgs
        String[] expectedPkgs = {
                "com.sun.ts.tests.servlet.api.jakarta_servlet.filterconfig",
                "com.sun.ts.tests.servlet.api.jakarta_servlet_http.httpsessionbindingevent",
                "com.sun.ts.tests.servlet.api.jakarta_servlet.servletoutputstream",
                "com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.createfilter"
        };
        for(String pkg : expectedPkgs) {
            assertTrue(testPkgNames.contains(pkg), "Should contain: "+pkg);
        }

    }

    @Test
    public void testFindPersistenceTestPkgs() throws IOException {
        Set<String> testPkgNames = Jar2ShrinkWrap.getTestPkgNames("com/sun/ts/tests/jpa/ee/packaging/jar");

        assertTrue(0 < testPkgNames.size());

        String[] expectedPkgs = {
                "com.sun.ts.tests.jpa.ee.packaging.jar"
        };
        for(String pkg : expectedPkgs) {
            assertTrue(testPkgNames.contains(pkg), "Should contain: "+pkg);
        }

    }
}
