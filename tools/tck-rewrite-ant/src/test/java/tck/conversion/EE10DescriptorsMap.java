package tck.conversion;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipFile;

/**
 * Parse the EE10 TCK distribution ${ts.home}/dist generate a map of descriptors for each test deployment
 * archive.
 */
public class EE10DescriptorsMap {
    public static void main(String[] args) throws IOException {
        String tsHome = System.getProperty("ts.home", "/home/starksm/Dev/Jakarta/wildflytck-new/jakartaeetck");
        Path dist = Paths.get(tsHome, "dist");
        if(!dist.toFile().exists()) {
            System.err.printf("ts.home(%s) has no dist subdirectory", tsHome);
            System.exit(1);
        }

        System.out.printf("Parsing ts.home/dist: %s\n", dist);
        HashMap<String, List<String>> deploymentMap = new HashMap<>();
        Files.walkFileTree(dist, new EE10DescriptorsMap.FindTestArchives(deploymentMap));
        System.out.printf("Read %d archives\n", deploymentMap.size());
        // Write out a mapping from the deployment archive to the descriptors
        Properties deploymentProps = new Properties();
        deploymentMap.forEach((k, v) -> {
            deploymentProps.setProperty(k, String.join(",", v));
        });
        try(FileWriter out = new FileWriter("deployment.properties")) {
            deploymentProps.store(out, "Deployment descriptors");
        }
        System.out.println("Wrote deployment.properties");
    }
    static class FindTestArchives extends SimpleFileVisitor<Path> {
        private final HashMap<String, List<String>> deploymentMap;
        public FindTestArchives(HashMap<String, List<String>> deploymentMap) {
            this.deploymentMap = deploymentMap;
        }
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            String fileName = file.getFileName().toString();
            if(fileName.endsWith(".ear") || fileName.endsWith(".war") || fileName.endsWith(".jar")) {
                // Remove the leading part x of x.[ear|war|jar]
                int lastDot = fileName.lastIndexOf('.');
                String deploymentName = fileName.substring(0, lastDot);
                List<String> descriptors = deploymentMap.computeIfAbsent(deploymentName, k -> new ArrayList<>());
                populateDescriptors(file, descriptors);
            } else if(fileName.endsWith(".xml")) {
                /*
                [starksm@scottryzen wildflytck-new]$ ls jakartaeetck/dist/com/sun/ts/tests/ejb30/timer/interceptor/business/mdb/
                ejb30_timer_interceptor_business_mdb_ejbliteservlet_vehicle_web.war
                ejb30_timer_interceptor_business_mdb_ejbliteservlet_vehicle_web.war.jboss-ejb3.xml
                ejb30_timer_interceptor_business_mdb_ejbliteservlet_vehicle_web.war.jboss-webservices.xml
                ejb30_timer_interceptor_business_mdb_ejbliteservlet_vehicle_web.war.jboss-web.xml
                ejb30_timer_interceptor_business_mdb_ejbliteservlet_vehicle_web.war.sun-ejb-jar.xml
                ejb30_timer_interceptor_business_mdb_ejbliteservlet_vehicle_web.war.sun-web.xml
                 */
                int firstDot = fileName.indexOf('.');
                String deploymentName = fileName.substring(0, firstDot);
                String descriptorName = fileName.substring(firstDot+1);
                if(descriptorName.contains("sun")) {
                    List<String> descriptors = deploymentMap.computeIfAbsent(deploymentName, k -> new ArrayList<>());
                    descriptors.add(descriptorName);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        void populateDescriptors(Path file, List<String> descriptors) {
            // Parse the deployment archive to find the descriptors
            try(ZipFile xar = new ZipFile(file.toFile())) {
                xar.stream().forEach(entry -> {
                    String entryName = entry.getName();
                    if(entryName.endsWith(".xml")) {
                        descriptors.add(entryName);
                    }
                });
            } catch(IOException e) {
                System.err.printf("Error parsing %s: %s\n", file, e);
            }
        }
    }

}
