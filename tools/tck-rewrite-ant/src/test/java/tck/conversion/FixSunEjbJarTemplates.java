package tck.conversion;

import com.sun.ts.lib.harness.VehicleVerifier;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * One off program to take the ejb30 module sun-ejb-jar.xml.template that use the @vehicle@ variable and generate
 * the vehicle specific sun-ejb-jar.xml files in the EE11 source tree in the correct test package.
 */
public class FixSunEjbJarTemplates {
    static Path tsHome = Paths.get("/home/starksm/Dev/Jakarta/wildflytck-new/jakartaeetck");
    static Path refactorRoot = Paths.get("/home/starksm/Dev/Jakarta/sms-platform-tck");

    public static void main(String[] args) throws IOException {
        if(!Files.exists(tsHome)) {
            throw new FileNotFoundException("Given ts.home does not exist: "+tsHome);
        }
        if(!Files.exists(refactorRoot)) {
            throw new FileNotFoundException("Given EE11 source repo does not exist: "+refactorRoot);
        }
        System.setProperty("ts.home", tsHome.toString());

        Path ee10Src = tsHome.resolve("src");
        List<Path> templatePaths = scanTemplateFiles(refactorRoot.resolve("ejb30"));
        for (Path templatePath : templatePaths) {
            Path ee10Pkg = templatePath.subpath(9, templatePath.getNameCount() - 1);
            Path buildXml = ee10Src.resolve(ee10Pkg).resolve("build.xml");
            Project project = initProject(buildXml);
            String appName = project.getProperty("app.name");

            VehicleVerifier verifier = VehicleVerifier.getInstance(buildXml.toFile());
            String[] vehicles = verifier.getVehicleSet();
            List<String> lines = Files.readAllLines(templatePath);
            for (String vehicle : vehicles) {
                if(vehicle.equals("ejbembed")) {
                    continue;
                }
                String fileName = "%s_%s_vehicle_web.war.sun-ejb-jar.xml".formatted(appName, vehicle);
                Path dest = templatePath.resolveSibling(fileName);
                // Write out the file line by line replacing all @vehicle@ references
                StringBuilder contents = new StringBuilder();
                for(String line : lines) {
                    String updated = line.replace("@vehicle@", vehicle);
                    contents.append(updated);
                    contents.append('\n');
                }
                Files.writeString(dest, contents);
                System.out.printf("Wrote: %s\n", dest);
            }
        }

    }
    static Project initProject(Path buildXml) {
        Project project = new Project();
        project.init();

        // The location of the glassfish download for the jakarta api jars
        project.setProperty("ts.home", tsHome.toAbsolutePath().toString());
        project.setProperty("javaee.home.ri", "${ts.home}/../glassfish7/glassfish");
        project.setBaseDir(buildXml.getParent().toFile());
        project.setProperty(MagicNames.ANT_FILE, buildXml.toAbsolutePath().toString());
        ProjectHelper.configureProject(project, buildXml.toFile());
        return project;
    }

    static class TemplateFilter extends SimpleFileVisitor<Path> {
        private ArrayList<Path> ejbXmls = new ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.getFileName().toString().equals("sun-ejb-jar.xml.template")) {
                 ejbXmls.add(file);
            }
            return FileVisitResult.CONTINUE;
        }
        public List<Path> getTemplates() {
            return ejbXmls;
        }
    }
    static List<Path> scanTemplateFiles(Path sourceRoot) throws IOException {
        TemplateFilter filter = new TemplateFilter();
        Files.walkFileTree(sourceRoot, filter);
        List<Path> ejbXmls = filter.getTemplates();
        return ejbXmls;
    }
}
