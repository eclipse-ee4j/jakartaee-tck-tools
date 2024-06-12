package tck.jakarta.platform.ant;

import com.sun.ts.lib.harness.VehicleVerifier;
import com.sun.ts.lib.util.ConfigUtil;
import com.sun.ts.tests.ejb.ee.bb.session.lrapitest.A;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

public class Helper {
    /**
     * Parse the ts.home/src/vehicles.properties file to see what unique set of vehicles is being used
     * @return name sorted list of unique vehicle names
     */
    public static List<String> getAllVehicles() {
        Properties mapping = ConfigUtil.loadPropertiesFor(VehicleVerifier.VEHICLE_PROP_FILE_NAME);
        HashSet<String> allVehicles = new HashSet<>();
        for (String key : mapping.stringPropertyNames()) {
            if(key.equalsIgnoreCase("exclude.dir")) {
                continue;
            }
            String vehicles = mapping.getProperty(key);
            String[] vehicleArray = vehicles.trim().split(" ");
            allVehicles.addAll(Arrays.asList(vehicleArray));
        }
        List<String> sorted = new ArrayList<>(allVehicles);
        Collections.sort(sorted);
        return sorted;
    }
}
