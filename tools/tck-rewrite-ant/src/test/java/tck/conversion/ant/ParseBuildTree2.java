package tck.conversion.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import tck.jakarta.platform.ant.PackageTarget;
import tck.jakarta.platform.ant.ProjectWrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the build.xml files in the Jakarta EE 10 platform TCK dist src directory. One way
 * to get the src is to run the tools/jar2shrinkwrap/src/test/java/Jar2ShrinkwrapPkgTest.java#canLocateTestDefinitions
 * method. It will download the source to /tmp/legacytck/LegacyTCKFolderName/jakartaeetck.
 * Also available from the downloads page:
 * https://www.eclipse.org/downloads/download.php?file=/ee4j/jakartaee-tck/jakartaee10/promoted/eftl/jakarta-jakartaeetck-10.0.0.zip
 *
 * This version attempts to parse all build.xml files into a {@link tck.jakarta.platform.ant.PackageTarget}
 */
public class ParseBuildTree2 {
    static ArrayList<PackageTarget> targetsWithUnhandledTasks = new ArrayList<>();
    /**
     * The required arguments include:
     * ts.home = points to an EE10 source tree
     * deliverabledir = ?
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        String tsHome = "/home/starksm/Dev/Jakarta/wildflytck/jakartaeetck";
        if(args.length != 0) {
            tsHome = getArg("ts.home", tsHome, args);
        }
        if(!Files.exists(Paths.get(tsHome))) {
            throw new FileNotFoundException(tsHome + " does not exist, pass in a valid EE 10 TCK root directory");
        }
        System.setProperty("ts.home", tsHome);
        // Create/link directories that are needed to parse the build.xml files
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
        // Write out build.xml paths for package targets with unhandled tasks
        targetsWithUnhandledTasks.forEach(pt -> System.out.println(pt.getPkgTarget().getLocation()));
    }

    /**
     * Given a build.xml file path, parse the file using the ant {@link Project}
     * and {@link ProjectHelper} and print a summary of the parsed {@link PackageTarget}
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
        if(pkg == null) {
            return null;
        }

        System.out.printf("Target 'package' location: %s\n", pkg.getLocation());
        PackageTarget pkgTarget = new PackageTarget(new ProjectWrapper(project), pkg);
        pkgTarget.parse();
        System.out.println(pkgTarget.toSummary());
        if(pkgTarget.getUnhandledTaks().size() > 0) {
            targetsWithUnhandledTasks.add(pkgTarget);
        }
        return project;
    }

}
