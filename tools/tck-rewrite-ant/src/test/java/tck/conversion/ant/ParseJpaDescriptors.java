package tck.conversion.ant;

import com.sun.ts.lib.harness.VehicleVerifier;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import tck.jakarta.platform.ant.PackageTarget;
import tck.jakarta.platform.ant.ProjectWrapper;
import tck.jakarta.platform.ant.api.DefaultEEMapping;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * A one-off CLI app to parse the JPA tests build.xml files from the EE 10 TCK to collect the transformed
 * persistence.xml files and write them into the EE11 tree with package renaming.
 */
public class ParseJpaDescriptors {
    static String getArg(String name, String defaultValue, String[] args) {
        String value = defaultValue;
        for (String arg : args) {
            if (arg.startsWith(name)) {
                int equals = arg.indexOf('=');
                value = arg.substring(equals + 1);
            }
        }
        return value;
    }
    public static void main(String[] args) throws Exception {
        String tsHome = "/home/starksm/Dev/Jakarta/wildflytck/jakartaeetck";
        String tckRepo = "/home/starksm/Dev/Jakarta/sms-platform-tck";
        if(args.length != 0) {
            tsHome = getArg("ts.home", tsHome, args);
            tckRepo = getArg("tck.repo", tckRepo, args);
        }
        if(!Files.exists(Paths.get(tsHome))) {
            throw new FileNotFoundException(tsHome + " does not exist, pass in a valid EE 10 TCK root directory");
        }
        if(!Files.exists(Paths.get(tckRepo))) {
            throw new FileNotFoundException(tckRepo + " does not exist, pass in a valid EE 11 TCK src repository root");
        }

        System.setProperty("ts.home", tsHome);
        ParseJpaDescriptors parser = new ParseJpaDescriptors();
        parser.run(Path.of(tsHome), Path.of(tckRepo));
    }

    Path tsHome;
    Path tckRepo;
    DefaultEEMapping eeMapping = DefaultEEMapping.getInstance();

    public void run(Path tsHome, Path tckRepo) throws Exception {
        this.tsHome = tsHome;
        this.tckRepo = tckRepo;
        // Create/link directories that are needed to parse the build.xml files
        System.setProperty("deliverabledir", "tck");

        Path deliveryDir = tsHome.resolve("install/tck");
        Files.createDirectories(deliveryDir);
        Path binDir = tsHome.resolve("bin");
        Path installBinDir = tsHome.resolve("install/tck/bin");
        if(!Files.isSymbolicLink(installBinDir)) {
            Files.createSymbolicLink(installBinDir, binDir);
        }
        // Scans for all build.xml files under the jpa tree and parses them into ant project instances
        scanBuildFiles(tsHome.resolve("src/com/sun/ts/tests/jpa"));
    }
    /**
     * Walks the TCK src tree to collect and then parse the ant build.xml files
     *
     * @param sourceRoot
     * @throws IOException
     */
    void scanBuildFiles(Path sourceRoot) throws IOException {
        BuildXmlFilter filter = new BuildXmlFilter();
        Files.walkFileTree(sourceRoot, filter);
        List<Path> buildXmls = filter.getBuildFiles();
        System.out.printf("Found %d build.xml files\n", buildXmls.size());
        for(Path buildXml : buildXmls) {
            parseBuildXml(buildXml);
        }
    }
    Project initProject(Path buildXml) {
        Project project = new Project();
        project.init();
        // The location of the glassfish download for the jakarta api jars
        project.setProperty("ts.home", tsHome.toAbsolutePath().toString());
        project.setProperty("javaee.home.ri", "${ts.home}/../glassfish7/glassfish");
        project.setBaseDir(buildXml.getParent().toFile());
        project.setProperty(MagicNames.ANT_FILE, buildXml.toAbsolutePath().toString());
        return project;
    }

    /**
     * Given a build.xml file path, parse the file using the ant {@link Project}
     * and {@link ProjectHelper} and print a summary of the parsed {@link PackageTarget}
     *
     * @param buildXml
     */
    Project parseBuildXml(Path buildXml) throws IOException {
        Project project = initProject(buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        // The package targets are what build the test artifacts
        Target antPackageTarget = project.getTargets().get("package");
        if(antPackageTarget == null) {
            return null;
        }
        VehicleVerifier verifier = VehicleVerifier.getInstance(buildXml.toFile());
        String[] vehicles = verifier.getVehicleSet();

        Path modifiedPersistenceXml = Path.of("/tmp/modified.persistence.xml");
        // Remove any previously generated persistence.xml
        Files.deleteIfExists(modifiedPersistenceXml);
        System.out.printf("Parsing(%s)\n", buildXml);
        PackageTarget pkgTargetWrapper = new PackageTarget(new ProjectWrapper(project), antPackageTarget);
        if(vehicles.length == 0) {
            // Run the ant "package" target
            pkgTargetWrapper.execute();
        } else {
            VehicleType vehicleType = VehicleType.valueOf(vehicles[0]);
            pkgTargetWrapper.execute(vehicleType);
        }

        // If a modified persistence.xml was generated, write it to the EE 11 tree
        if(modifiedPersistenceXml.toFile().exists()) {
            System.out.printf("Modified persistence.xml: %s\n", modifiedPersistenceXml);
            mapAndWriteDescriptor(modifiedPersistenceXml, buildXml.getParent());
        }

        return project;
    }
    void mapAndWriteDescriptor(Path persistenceXml, Path testDir) throws IOException {
        List<String> lines = Files.readAllLines(persistenceXml);
        StringBuilder tmp = new StringBuilder();
        for (String line : lines) {
            // <class>com.sun.ts.tests.jpa.core.annotations.elementcollection.A</class>
            String mapped = line;
            if(line.matches("<class>.*</class>")) {
                int start = line.indexOf("<class>") + "<class>".length();
                int end = line.indexOf("</class>");
                String ee10Class = line.substring(start, end);
                String ee11Class = eeMapping.getEE11Name(ee10Class);
                mapped = line.replace(ee10Class, ee11Class);
            }
            tmp.append(mapped).append("\n");
        }
        String testPath = testDir.toString();
        int idx = testPath.indexOf("com/sun/ts/tests/jpa");
        String ee10Path = testPath.substring(idx);
        String ee10Pkg = ee10Path.replace('/', '.');
        String ee11Pkg = eeMapping.getEE11Name(ee10Pkg);
        String ee11Path = ee11Pkg.replace('.', '/');

        Path ee11PersistenceXml = null;
        // jpa/platform-tests/src/main/java
        Path ee11TestDir1 = tckRepo.resolve("jpa/platform-tests/src/main/java").resolve(ee11Path);
        if(ee11TestDir1.toFile().exists()) {
            ee11PersistenceXml = ee11TestDir1.resolve("persistence.xml");
        } else {
            // jpa/spec-tests/src/main/java
            Path ee11TestDir2 = tckRepo.resolve("jpa/spec-tests/src/main/java").resolve(ee11Path);
            if(!ee11TestDir2.toFile().exists()) {
                System.err.printf("Neither %s nor %s exist\n", ee11TestDir1, ee11TestDir1);
                return;
            }
            ee11PersistenceXml = ee11TestDir2.resolve("persistence.xml");
        }
        // If there is a persistence.xml in the EE 11 tree, write the modified one there with .new extension
        if(ee11PersistenceXml.toFile().exists()) {
            Path altPath = ee11PersistenceXml.resolveSibling("persistence.new.xml");
            Files.writeString(altPath, tmp.toString());
            System.out.printf("Wrote %s\n", altPath);
        } else {
            Files.writeString(ee11PersistenceXml, tmp.toString());
            System.out.printf("Wrote %s\n", ee11PersistenceXml);
        }

    }
}
