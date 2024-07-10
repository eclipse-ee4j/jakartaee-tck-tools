package tck.conversion;

import com.sun.ts.lib.harness.VehicleVerifier;
import tck.conversion.ant.BuildXmlFilter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * One off program to take the vehicle templates that use the @package@ variable and move them into
 * the tckrefactor source tree in the correct test package.
 */
public class FixTCKTemplates {
    static Path tsHome = Paths.get("/home/starksm/Dev/Jakarta/wildflytck-new/jakartaeetck");
    static Path refactorRoot = Paths.get("/home/starksm/Dev/Jakarta/sms-platform-tck");
    static final String[] TEMPLATES = {
            "src/com/sun/ts/tests/common/vehicle/ejbliteservlet2/ejbliteservlet2_vehicle_web.xml",
            "src/com/sun/ts/tests/common/vehicle/ejbliteservlet2/EJBLiteServlet2Filter.java.txt",
            "src/com/sun/ts/tests/common/vehicle/ejblitejsp/EJBLiteJSPTag.java.txt",
            "src/com/sun/ts/tests/common/vehicle/ejblitesecuredjsp/ejblitesecuredjsp.tld",
            "src/com/sun/ts/tests/common/vehicle/ejblitesecuredjsp/EJBLiteSecuredJSPTag.java.txt",
            "src/com/sun/ts/tests/common/vehicle/ejbliteservlet/ejbliteservlet_vehicle_web.xml",
            "src/com/sun/ts/tests/common/vehicle/ejbliteservlet/EJBLiteServletVehicle.java.txt",
            "src/com/sun/ts/tests/common/vehicle/ejbliteservlet/HttpServletDelegate.java.txt",
            "src/com/sun/ts/tests/common/vehicle/ejbliteservletcal/ejbliteservletcal_vehicle_web.xml",
            "src/com/sun/ts/tests/common/vehicle/ejbliteservletcal/EJBLiteServletContextAttributeListener.java.txt"
    };
    record TemplateInfo(
        String srcPath,
        List<String> templateLines,
        String fileName) {
    }
    public static void main(String[] args) throws IOException {
        if(!Files.exists(tsHome)) {
            throw new FileNotFoundException("Given ts.home does not exist: "+tsHome);
        }
        if(!Files.exists(refactorRoot)) {
            throw new FileNotFoundException("Given tckrefactor source repo does not exist: "+refactorRoot);
        }
        System.setProperty("ts.home", "/home/starksm/Dev/Jakarta/wildflytck-new/jakartaeetck");

        HashMap<String, List<TemplateInfo>> templates = new HashMap<>();
        for (String template : TEMPLATES) {
            Path templatePath = tsHome.resolve(template);
            if(!templatePath.toFile().exists()) {
                System.err.println("Template " + template + " not found");
                System.exit(1);
            }

            List<String> lines = Files.readAllLines(templatePath);
            String fileName = templatePath.getName(templatePath.getNameCount() - 1).toString();
            if(fileName.endsWith(".java.txt")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }
            String vehicle = templatePath.getName(templatePath.getNameCount() - 2).toString();
            TemplateInfo info = new TemplateInfo(template, lines, fileName);
            List<TemplateInfo> existing = templates.get(vehicle);
            if(existing == null) {
                existing = new ArrayList<>();
                templates.put(vehicle, existing);
            }
            existing.add(info);
        }

        List<Path> buildXmls = scanBuildFiles(tsHome);
        int fixCount = 0;
        for (Path buildXml : buildXmls) {
            VehicleVerifier verifier = VehicleVerifier.getInstance(buildXml.toFile());
            String[] vehicles = verifier.getVehicleSet();
            if(vehicles.length > 0) {
                System.out.printf("%s: %s\n", buildXml.getParent().toString(), Arrays.asList(vehicles));
                if(Arrays.stream(vehicles).anyMatch(v -> v.equals("ejbliteservlet"))) {
                    System.out.printf("+++ Fixing ejbliteservlet templates.\n");
                    fixCount++;
                    List<TemplateInfo> infos = templates.get("ejbliteservlet");
                    writeTemplateToPath(buildXml, infos);
                }
                if(Arrays.stream(vehicles).anyMatch(v -> v.equals("ejbliteservlet2"))) {
                    System.out.printf("+++ Fixing ejbliteservlet2 templates.\n");
                    List<TemplateInfo> infos = templates.get("ejbliteservlet2");
                    writeTemplateToPath(buildXml, infos);
                    fixCount++;
                }
                if(Arrays.stream(vehicles).anyMatch(v -> v.equals("ejblitejsp"))) {
                    System.out.printf("+++ Fixing ejblitejsp templates.\n");
                    List<TemplateInfo> infos = templates.get("ejblitejsp");
                    writeTemplateToPath(buildXml, infos);
                    fixCount++;
                }
                if(Arrays.stream(vehicles).anyMatch(v -> v.equals("ejbliteservletcal"))) {
                    System.out.printf("+++ Fixing ejbliteservletcal templates.\n");
                    fixCount++;
                }
                if(Arrays.stream(vehicles).anyMatch(v -> v.equals("ejblitesecuredjsp"))) {
                    System.out.printf("+++ Fixing ejblitesecuredjsp templates.\n");
                    fixCount++;
                }
            }
        }
        System.out.printf("Fix count: %d\n", fixCount);
    }
    static List<Path> scanBuildFiles(Path sourceRoot) throws IOException {
        BuildXmlFilter filter = new BuildXmlFilter();
        Files.walkFileTree(sourceRoot, filter);
        List<Path> buildXmls = filter.getBuildFiles();
        return buildXmls;
    }

    /**
     * Given a path to a tck test package that has one or more of the vehicles that use templates,
     * apply the templates to generate the files in the test package.
     * @param buildXml - an EE10 TCK test directory build.xml file path
     * @param templates
     * @throws IOException
     */
    static void writeTemplateToPath(Path buildXml, List<TemplateInfo> templates) throws IOException {
        // Get the package name from the test directory path
        Path srcDir = tsHome.resolve("src");
        Path testDir = buildXml.getParent();
        // This gives a relative path like com/sun/ts/tests/appclient/deploy/ejbref/single
        Path pkgPath = srcDir.relativize(testDir);
        // Get the package name from the test directory path
        String pkg = pkgPath.toString().replace('/', '.');
        // The tckrefactor branch has separated the tests into modules. The module name is pkg[4]
        String moduleName = pkgPath.getName(4).toString();
        Path refactorModule = refactorRoot.resolve(moduleName);
        Path moduleSrcDir = refactorModule.resolve("src/main/java");

        for (TemplateInfo info : templates) {

            // Check for an existing override and do not overwrite it
            Path destDir = moduleSrcDir.resolve(pkgPath);
            Path dest = destDir.resolve(info.fileName);
            if(Files.exists(dest)) {
                System.out.printf("Skipping existing: %s\n", dest);
            }
            // Write out the file line by line replacing all @package@ references
            StringBuilder contents = new StringBuilder();
            for(String line : info.templateLines) {
                String updated = line.replace("@package@", pkg);
                contents.append(updated);
                contents.append('\n');
            }
            Files.writeString(dest, contents);
            System.out.printf("Wrote: %s\n", dest);
        }
    }
}
