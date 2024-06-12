package tck.conversion.ant;

import com.sun.ts.lib.harness.VehicleVerifier;
import org.apache.tools.ant.IntrospectionHelper;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.MacroDef;
import org.apache.tools.ant.taskdefs.MacroInstance;
import tck.jakarta.platform.ant.AttributeMap;
import tck.jakarta.platform.ant.Helper;
import tck.jakarta.platform.ant.ProjectWrapper;
import tck.jakarta.platform.ant.Vehicles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * Parse and validate the
 * src/com/sun/ts/tests/ejb32/timer/service/stateless/build.xml
 */
public class ParseEjb32TimerServiceStateless {
    public static void main(String[] args) {
        String tsHome = "/tmp/legacytck/LegacyTCKFolderName/jakartaeetck";
        System.setProperty("ts.home", tsHome);
        Path buildXml = Paths.get(tsHome, "src/com/sun/ts/tests/ejb32/timer/service/stateless/build.xml");
        Project project = ParseBuildTree.parseBuildXml(buildXml);
        Target pkgTarget = project.getTargets().get("package");
        Location pkgLocation = pkgTarget.getLocation();
        System.out.println(Helper.getAllVehicles());

        VehicleVerifier verifier = VehicleVerifier.getInstance(new File(pkgLocation.getFileName()));
        System.out.println(Arrays.asList(verifier.getVehicleSet()));
        Task tsVehicle = pkgTarget.getTasks()[0];
        Hashtable<String, Object> rcAttrs = tsVehicle.getRuntimeConfigurableWrapper().getAttributeMap();
        AttributeMap attributeMap = new AttributeMap(project, rcAttrs);
        String tsVehicleName = attributeMap.getAttribute("name");
        String vehiclePkgDir = project.getProperty("vehicle.pkg.dir");
        Vehicles vehicles = new Vehicles(tsVehicleName, vehiclePkgDir, attributeMap, tsVehicle.getRuntimeConfigurableWrapper(), pkgLocation);
        System.out.println(vehicles);

        System.out.printf("Project info, name=%s, baseDir=%s\n", project.getName(), project.getBaseDir());
        System.out.printf(" + references=%s\n\n", project.getReferences().keySet());
        System.out.printf(" + targets=%s\n\n", project.getTargets().keySet());
        System.out.printf(" + propertyNames=%s\n\n", project.getPropertyNames());
        System.out.printf(" + userProperties=%s\n\n", project.getUserProperties());

        ProjectWrapper wrapper = new ProjectWrapper(project);
        System.out.printf("imported names: %s\n", wrapper.getImportNames());
        System.out.printf("imported files: %s\n", wrapper.getImports());
        Vector<Target> allPkgTargets = project.topoSort("package", project.getTargets(), true);
        System.out.printf("All package targets: %s\n", allPkgTargets);

    }
}
