import jakartatck.jar2shrinkwrap.Jar2ShrinkWrap;
import jakartatck.jar2shrinkwrap.JarProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Test that the com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes pkg
 * has the expected contents.
 */
public class Jar2ShrinkwrapPkgTest {
    public static void main(String[] args) {
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
        Iterator<String> iterator = classesSet.iterator();
        while(iterator.hasNext()) {
            String c = iterator.next();
            if(!classesSet.contains(c)) {
                System.err.printf("Failed to find class: %s\n", c);
            }
            iterator.remove();
        }
        if(classesSet.size() != 0) {
            System.out.printf("Not all expected classes were found: %s\n", classesSet);
            System.exit(1);
        }

        System.out.printf("Libraries: %s\n", war.getLibraries());
        System.out.printf("Metainf: %s\n", war.getMetainf());
        System.out.printf("Webinf: %s\n", war.getWebinf());
        System.out.printf("OtherFiles: %s\n", war.getOtherFiles());
    }
}
