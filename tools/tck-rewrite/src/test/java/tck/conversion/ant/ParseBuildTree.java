package tck.conversion.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import tck.jakarta.platform.ant.EjbJar;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * Parses the build.xml files in the Jakarta EE 10 platform TCK dist src directory. One way
 * to get the src is to run the tools/jar2shrinkwrap/src/test/java/Jar2ShrinkwrapPkgTest.java#canLocateTestDefinitions
 * method. It will download the source to /tmp/legacytck/LegacyTCKFolderName/jakartaeetck
 */
public class ParseBuildTree {
    /**
     * The required arguments include:
     * ts.home = points to an EE10 source tree
     * deliverabledir = ?
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String tsHome = "/tmp/legacytck/LegacyTCKFolderName/jakartaeetck";
        if(args.length != 0) {
            tsHome = getArg("ts.home", tsHome, args);

        }
        System.setProperty("ts.home", tsHome);
        //
        System.setProperty("deliverabledir", "tck");

        File deliveryDir = new File(tsHome+"/install/tck");
        deliveryDir.mkdirs();
        Path binDir = Paths.get(tsHome+"/bin");
        Path installBinDir = Paths.get(tsHome+"/install/tck/bin");
        if(!Files.isSymbolicLink(installBinDir)) {
            Files.createSymbolicLink(installBinDir, binDir);
        }
        // Scans for all build.xml files and parses them into ant project instances
        scanBuildFiles(Paths.get(tsHome+"/src"));
    }
    static String getArg(String name, String defaultValue, String[] args) {
        String value = defaultValue;
        for (String arg : args) {
            if(arg.startsWith(name)) {
                int equals = arg.indexOf('=');
                value = arg.substring(equals+1);
            }
        }
        return value;
    }

    /**
     * Walks the TCK src tree to collect and then parse the ant build.xml files
     *
     * @param sourceRoot
     * @throws IOException
     */
    static void scanBuildFiles(Path sourceRoot) throws IOException {
        BuildXmlFilter filter = new BuildXmlFilter();
        Files.walkFileTree(sourceRoot, filter);
        List<Path> buildXmls = filter.getBuildFiles();
        for(Path buildXml : buildXmls) {
            parseBuildXml(buildXml);
        }
    }

    /**
     * A {@link java.nio.file.SimpleFileVisitor} to look for build.xml files while skipping
     * any target dirs
     */
    static class BuildXmlFilter extends SimpleFileVisitor<Path> {
        private ArrayList<Path> buildXmls = new ArrayList<>();
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            // Ignore any target directories
            if(dir.getFileName().endsWith("target")) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if(file.getFileName().endsWith("build.xml")) {
                buildXmls.add(file);
            }
            return FileVisitResult.CONTINUE;
        }
        public List<Path> getBuildFiles() {
            return buildXmls;
        }
    }

    /**
     * Given a build.xml file path, parse the file using the ant {@link Project}
     * and {@link ProjectHelper}. This currently is just run for the side effect
     * of printing out the ts.ejbjar tasks found as a starting point for understanding
     * the CTS ant structure and whether this parsing provides a complete description.
     *
     * @param buildXml
     */
    static Project parseBuildXml(Path buildXml) {
        Project project = new Project();
        project.init();
        System.out.printf("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        System.out.println(project.getName());
        System.out.println(project.getBaseDir());
        // The package targets are what build the test artifacts
        Target pkg = project.getTargets().get("package");
        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        System.out.printf("--- Dependencies: %s", asList(pkg.getDependencies()));
        System.out.println("--- Tasks in package target:");

        for(Task t : pkg.getTasks()) {
            System.out.printf("--- ---%s/%s\n", t.getTaskName(), t.getTaskType());
            System.out.println(t.getRuntimeConfigurableWrapper().getAttributeMap());
            Enumeration<RuntimeConfigurable> children = t.getRuntimeConfigurableWrapper().getChildren();
            if(t.getTaskName().equals("ts.ejbjar")) {
                printEjbJarTask(t.getRuntimeConfigurableWrapper());
            } else {
                for (RuntimeConfigurable rc : asIterable(children)) {
                    System.out.printf("\t%s:\n%s\n", rc.getElementTag(), rc.getAttributeMap());
                }
            }
        }
        return project;
    }
    static void printEjbJarTask(RuntimeConfigurable taskRC) {
        EjbJar ejbJar = new EjbJar(taskRC);
        System.out.println(ejbJar);
    }
    public static <T> List<T> asList(final Enumeration<T> e) {
        return Collections.list(e);
    }
    public static <T> Iterable<T> asIterable(final Enumeration<T> e) {
        if (e == null)
            return null;
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    @Override
                    public boolean hasNext() {
                        return e.hasMoreElements();
                    }
                    @Override
                    public T next() {
                        return e.nextElement();
                    }
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
