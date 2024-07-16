package tck.jakarta.platform.ant;

import com.sun.ts.lib.harness.VehicleVerifier;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Task;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

public class Utils {
    private static Logger log = Logger.getLogger(Utils.class.getName());

    public static <T> List<T> asList(final Enumeration<T> e) {
        return Collections.list(e);
    }

    public static List<String> toList(Enumeration<String> iter) {
        ArrayList<String> list = new ArrayList<>();
        while (iter.hasMoreElements()) {list.add(iter.nextElement());}
        return list;
    }

    public static String toString(Task[] tasks) {
        StringBuilder tmp = new StringBuilder();
        tmp.append('[');
        for (Task task : tasks) {
            tmp.append(toString(task));
            tmp.append("; ");
        }
        tmp.append(']');
        return tmp.toString();
    }
    public static String toString(Task task) {
        return String.format("%s, type=%s, attrs: %s", task.getTaskName(), task.getTaskType(),
                task.getRuntimeConfigurableWrapper().getAttributeMap());
    }
    public static String toDotClassList(String classes) {
        return classes.replace(", ", "\n").replace('$', '.').replace('/', '.');
    }

    /**
     *
     * @param testClass
     * @param vehicleType
     * @param clientType
     * @return
     * @throws MalformedURLException
     */
    public static String getVehicleArchiveDescriptor(Class<?> testClass,
                                                   VehicleType vehicleType, String clientType) throws MalformedURLException {
        String vehicleDescriptor = vehicleType.name() + "_vehicle_" + clientType + ".xml";
        String resPath = vehicleDescriptor;
        // Look in the tckrefactor test src tree for an override
        URL resURL = testClass.getResource(resPath);
        debug("%s -> %s\n", resPath, resURL);
        if (resURL == null) {
            String pkgPath = testClass.getPackage().getName();
            pkgPath = pkgPath.replace('.', '/');
            resPath = "/" + pkgPath + "/" + vehicleDescriptor;
            resURL = testClass.getResource(resPath);
            debug("%s -> %s\n", resPath, resURL);
        }
        // Look in the tckrefactor vehicle common module
        if(resURL == null) {
            resPath = "/com/sun/ts/tests/common/vehicle/"+vehicleType.name()+"/"+vehicleDescriptor;
            resURL = testClass.getResource(resPath);
            debug("%s -> %s\n", resPath, resURL);
        }

        // Use the test class src tree for a local override in a src/resource/vehicle/ tree
        if(resURL == null) {
            resPath = "/vehicle/"+vehicleType.name()+"/"+vehicleDescriptor;
            resURL = testClass.getResource(resPath);
            debug("%s -> %s\n", resPath, resURL);
        }
        return resPath;
    }

    private static void debug(String format, Object ... args) {
        String msg = String.format(format, args);
        log.fine(msg);
    }

    /**
     * Validate that a tsHome path contains:
     * tsHome/bin
     * tsHome/classes
     * tsHome/classes/com/sun/ts/tests/common/vehicle
     * tsHome/dist
     * tsHome/src/vehicle.properties
     * tsHome/../glassfish7/glassfish/modules
     *
     * @param tsHome - path to EE10 TCK dist
     */
    public static void validateTSHome(Path tsHome) throws FileNotFoundException {
        if(!tsHome.toFile().exists()) {
            throw new FileNotFoundException("The tsHome path does not exist: "+tsHome);
        }
        Path bin = tsHome.resolve("bin");
        if(!bin.toFile().exists()) {
            throw new FileNotFoundException("The tsHome path does not contain a bin directory: "+bin);
        }
        Path classes = tsHome.resolve("classes");
        if(!classes.toFile().exists()) {
            throw new FileNotFoundException("The tsHome path does not contain a classes directory: "+classes);
        }
        Path vehicles = tsHome.resolve("classes/com/sun/ts/tests/common/vehicle");
        if(!classes.toFile().exists()) {
            throw new FileNotFoundException("The tsHome path does not contain a vehicle classes directory: "+vehicles);
        }
        Path dist = tsHome.resolve("dist");
        if(!dist.toFile().exists()) {
            throw new FileNotFoundException("The tsHome path does not contain a dist directory: "+dist);
        }
        Path vehicleProps = tsHome.resolve("src/vehicle.properties");
        if(!vehicleProps.toFile().canRead()) {
            throw new FileNotFoundException("The tsHome path does not contain a readable vehicle.properties: "+vehicleProps);
        }
        Path glassfish = tsHome.resolve("../glassfish7/glassfish/modules");
        if(!vehicleProps.toFile().canRead()) {
            String msg = "The tsHome path does not have a peer glassfish7 dist with a glassfish/modules directory.\n"+
                    "The expected path is: "+glassfish.toAbsolutePath();
            throw new FileNotFoundException(msg);
        }
    }

    /**
     * Build a URLClassLoader from the tsHarness.classpath property in the tsHome/bin/build.xml file.
     * @param tsHome - path to EE10 TCK dist
     * @return URLClassLoader
     * @throws FileNotFoundException if tsHome fails {@link #validateTSHome(Path)}
     */
    public static ClassLoader getTSClassLoader(Path tsHome) throws FileNotFoundException {
        validateTSHome(tsHome);
        Path buildXml = tsHome.resolve("bin/build.xml");
        Project project = new Project();
        project.init();
        // The location of the glassfish download for the jakarta api jars
        project.setProperty("javaee.home.ri", "${ts.home}/../glassfish7/glassfish");

        log.info("Parsing: " + buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        String tsHarnessCP = project.getProperty("ts.harness.classpath");
        String[] paths = tsHarnessCP.split(File.pathSeparator);
        ArrayList<URL> urls = new ArrayList<>();
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException e) {
                }
            }
        }
        Path classes = tsHome.resolve("classes");
        try {
            urls.add(classes.toUri().toURL());
        } catch (MalformedURLException e) {

        }
        return URLClassLoader.newInstance(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
    }

    /**
     * Get the list of vehicle types from the vehicle.properties file in the tsHome/src directory.
     * @param testFilePath - path to the test java file
     * @return non-empty list of vehicle types. If there are no vehicles the list will contain VehicleType#none
     */
    public static List<VehicleType> getVehicleTypes(Path testFilePath) {
        VehicleVerifier verifier = VehicleVerifier.getInstance(testFilePath.toFile());
        ArrayList<VehicleType> vehicleTypes = new ArrayList<>();
        for (String vehicle : verifier.getVehicleSet()) {
            vehicleTypes.add(VehicleType.valueOf(vehicle));
        }
        if(vehicleTypes.isEmpty()) {
            vehicleTypes.add(VehicleType.none);
        }
        return vehicleTypes;
    }

    public static String getClassFilesString(List<TSFileSet> fileSets) {
        // Capture unique classes
        HashSet<String> classes = new HashSet<>();
        for(TSFileSet fs : fileSets) {
            String dir = fs.dir + '/';
            for(String f : fs.includes) {
                // Skip the obsolete EJBHomes
                if(f.endsWith(".class") && !f.endsWith("Home.class")) {
                    f = f.replace(dir, "");
                    String clazz = f.replace('/', '.').replace('$', '.');
                    classes.add(clazz);
                }
            }
        }
        // Build up a string that can be passed to an archive addClasses method
        StringBuilder sb = new StringBuilder();
        for(String clazz : classes) {
            sb.append(clazz);
            sb.append(",\n");
        }
        if(sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        return sb.toString();
    }
}
