import jakartatck.jar2shrinkwrap.Jar2ShrinkWrap;
import jakartatck.jar2shrinkwrap.JarProcessor;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple test of displaying the contents of a war including the libary
 */
public class DumpWarTest {
    @Test
    public void displayWarStructure() {
        JarProcessor war = Jar2ShrinkWrap.fromPackage("com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes");
        ArrayList<String> classes = war.getClasses();

        List<File> libraryFiles = new ArrayList<>();
        for (String jarName : war.getLibraries()) {
            File jarFile = new File(war.getLibDir(), jarName);
            libraryFiles.add(jarFile);
        }
        List<JavaArchive> warJars = libraryFiles.stream()
                .map(file -> ShrinkWrap.createFromZipFile(JavaArchive.class, file))
                .collect(Collectors.toList());

        System.out.println(war.getName());
        String indent = "  ";
        for (String c : war.getClasses()) {
            System.out.printf("%sWEB-INF/classes/%s\n", indent.repeat(1), c);
        }
        for (JavaArchive jar : warJars) {
            System.out.printf("%sWEB-INF/lib/%s\n", indent.repeat(1), jar.getName());
            Map<ArchivePath, Node> content = jar.getContent();
            for (ArchivePath path : content.keySet()) {
                Asset asset = content.get(path).getAsset();
                if(asset != null) {
                    System.out.printf("%s%s\n", indent.repeat(2), path.get());
                }
            }
        }
    }
}
