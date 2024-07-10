package tck.conversion.ant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Path resolution tests
 */
@EnabledIfSystemProperty(named = "ts.home", matches = ".*")
public class PathsTest {

    @Test
    public void testParent() throws IOException {
        String tsHome = System.getProperty("ts.home");
        Path sourceRoot = Paths.get(tsHome, "src");
        Path buildXml = sourceRoot.resolve("com/sun/ts/tests/jms/core/bytesMsgTopic/build.xml");
        Assertions.assertTrue(Files.exists(buildXml), buildXml.toString());
        Path target = buildXml.getParent();
        while (target != null && !target.equals(sourceRoot)) {
            target = target.getParent();
        }
        String path = "../";
        target = buildXml.getParent();
        while (!Files.isSameFile(sourceRoot, target)) {
            target = buildXml.getParent().resolve(path).normalize();
            path = "../" + path;
        }
        System.out.println("done1");

    }
    @Test
    public void testRelative() throws IOException {
        String tsHome = System.getProperty("ts.home");
        Path target = Paths.get(tsHome, "src/com/sun/ts/tests/ejb30/bb/session/stateful/concurrency/metadata/annotated/build.xml");
        Files.walk(target.getParent(), 1).forEach(System.out::println);
        long subdirs = Files.walk(target.getParent(), 1).filter(Files::isDirectory).count();
        System.out.printf("expect 1 subdir: %d\n", subdirs);
    }

    @Test
    public void testResolveTestPath() throws IOException {
        String tsHome = System.getProperty("ts.home");
        Path sourceRoot = Paths.get(tsHome, "src");
        Path ee10testpkg = Paths.get(tsHome, "src/com/sun/ts/tests/appclient/deploy/ejbref/scope/single/build.xml").getParent();
        Path pkgPath = sourceRoot.relativize(ee10testpkg);
        // Get the package name from the test directory path
        String pkg = pkgPath.toString().replace('/', '.');
        // The tckrefactor branch has separated the tests into modules. The module name is pkg[4]
        String moduleName = pkgPath.getName(4).toString();
        System.out.printf("maps to pkg: %s, module: %s\n", pkg, moduleName);
    }

    /**
     * Validate how to resolve relative paths with wildcards in them as used by the ant build.xml files
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testResolveWildcard() throws IOException, URISyntaxException {
        String tsHome = System.getProperty("ts.home");
        Path classesRoot = Paths.get(tsHome, "classes");

        Path jmsToolClass = classesRoot.resolve("com/sun/ts/tests/jms/common/JmsTool.class");
        int classesCount = classesRoot.getNameCount();
        System.out.printf("%s exists: %s\n", jmsToolClass.subpath(classesCount, classesCount+7).toString(), Files.exists(jmsToolClass));
        System.out.printf("jmsToolClass startsWith classesRoot: %s\n", jmsToolClass.startsWith(classesRoot));

        String[] exampleClassPatterns = {
                "com/sun/ts/tests/jms/common/JmsTool.class",
                "com/sun/ts/tests/common/vehicle/ejb/*.class",
                "com/sun/ts/tests/common/vehicle/*.class",
            "com/sun/ts/lib/harness/ServiceEETest*.class",
        };

        /*
        [starksm@scottryzen wildflytck-new]$ ls jakartaeetck/classes/com/sun/ts/tests/common/vehicle/ejb/
        EJBVehicle.class      EJBVehicleRemote.class
        EJBVehicleHome.class  EJBVehicleRunner.class
         */
        Path ejbVehicleClass = classesRoot.resolve("com/sun/ts/tests/common/vehicle/ejb/EJBVehicle.class");
        Path allEjbClasses = classesRoot.resolve("com/sun/ts/tests/common/vehicle/ejb/*.class");
        System.out.printf("ejbVehicleClass startsWith classesRoot: %s\n", ejbVehicleClass.startsWith(classesRoot));
        System.out.printf("ejbVehicleClass startsWith com/sun/ts/tests/common/vehicle/ejb/: %s\n", ejbVehicleClass.startsWith(classesRoot.resolve("com/sun/ts/tests/common/vehicle/ejb/")));
        System.out.printf("ejbVehicleClass startsWith allEjbClasses: %s\n", ejbVehicleClass.startsWith(allEjbClasses));
        System.out.printf("ejbVehicleClass.nameCount: %d, allEjbClasss.nameCount: %d\n", ejbVehicleClass.getNameCount(), allEjbClasses.getNameCount());
        System.out.printf("ejbVehicleClass.name: %s, allEjbClasss.name: %s\n", ejbVehicleClass.getFileName(), allEjbClasses.getFileName());

        for (String pattern : exampleClassPatterns) {
            Path patternPath = classesRoot.resolve(pattern);
            if(ejbVehicleClass.startsWith(patternPath.getParent())) {
                boolean sameDir = ejbVehicleClass.getNameCount() == patternPath.getNameCount();
                System.out.printf("ejbVehicleClass.startsWithPattern: %s, sameDir: %s\n", patternPath, sameDir);

            }
        }

        Stream<Path> classes = Files.walk(classesRoot, 1).filter(new PatternFilter(exampleClassPatterns));
        System.out.println(classes.collect(Collectors.toSet()));
    }
    static class PatternFilter implements Predicate<Path> {

        PatternFilter(String[] paths) {

        }
        @Override
        public boolean test(Path path) {
            if(path.startsWith("com/sun/ts/tests/jms/common")) {
                return true;
            }
            return false;
        }
    }
}
