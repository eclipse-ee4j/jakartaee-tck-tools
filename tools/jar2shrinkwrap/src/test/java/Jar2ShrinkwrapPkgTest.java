import jakartatck.jar2shrinkwrap.Jar2ShrinkWrap;
import jakartatck.jar2shrinkwrap.JarProcessor;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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
        System.out.printf("LibDir: %s\n", war.getLibDir());
        File initializeJar = new File(war.getLibDir(), war.getLibraries().get(0));
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
            File jarFile = new File(war.getLibDir(), jarName);
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

        // Write the java source to the console
        StringWriter src = new StringWriter();
        war.saveOutput(src, false);
        System.out.printf("\nJavaSource:\n%s\n", src.toString());
    }
}
