package tck.jakarta.platform.ant;

import org.apache.tools.ant.Task;
import tck.jakarta.platform.vehicles.VehicleType;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
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
     * @param tckrefactor
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
}
