package tck.conversion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tck.jakarta.platform.ant.Utils;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@EnabledIfSystemProperty(named = "ts.home", matches = ".*")
public class UtilsTest {
    @Test
    public void testValidateTsHome() throws FileNotFoundException {
        String tsHome = System.getenv("ts.home");
        if(tsHome != null) {
            System.out.println("TS_HOME is set to: " + tsHome);
            Utils.validateTSHome(Paths.get(tsHome));
        } else {
            System.out.println("TS_HOME is not set");
        }
    }

    @Test
    public void validateTsClassLoader() throws ClassNotFoundException, FileNotFoundException {
        String tsHome = System.getProperty("ts.home");
        ClassLoader classLoader = Utils.getTSClassLoader(Paths.get(tsHome));
        Class<?> clazz = classLoader.loadClass("com.sun.ts.lib.harness.EETest");
        System.out.println("Loaded class: " + clazz.getName());
        clazz = classLoader.loadClass("com.sun.ts.tests.signaturetest.javaee.JavaEESigTest");
        System.out.println("Loaded class: " + clazz.getName());
        clazz = classLoader.loadClass("com.sun.ts.tests.ejb30.lite.view.singleton.annotated.Client");
        System.out.println("Loaded class: " + clazz.getName());
    }

    @Test
    public void validateVehiclTypes() {
        String tsHome = System.getProperty("ts.home");
        Path client = Paths.get(tsHome, "src/com/sun/ts/tests/ejb30/lite/view/singleton/annotated/Client.java");
        List<VehicleType> vehicles = Utils.getVehicleTypes(client);
        System.out.printf("Vehicles(%s): %s\n", client, vehicles);

        client = Paths.get("/home/starksm/Dev/Jakarta/wildflytck-new/jakartaeetck/src/com/sun/ts/tests/ejb30/lite/view/singleton/annotated/Client.java");
        vehicles = Utils.getVehicleTypes(client);
        System.out.printf("Vehicles(%s): %s\n", client, vehicles);
    }
}
