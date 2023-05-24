import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakartatck.jar2shrinkwrap.Jar2ShrinkWrap;
import jakartatck.jar2shrinkwrap.JarProcessor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

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

        assertEquals("initilizer.jar", war.getLibraries().get(0));
        assertEquals("META-INF/MANIFEST.MF", war.getMetainf().get(0));
        assertEquals("WEB-INF/web.xml",war.getWebinf().get(0));
        assertTrue(0 == war.getOtherFiles().size());
    }
}
