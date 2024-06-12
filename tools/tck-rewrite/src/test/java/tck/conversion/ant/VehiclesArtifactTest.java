package tck.conversion.ant;

import com.sun.ts.lib.harness.VehicleVerifier;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tck.jakarta.platform.ant.AttributeMap;
import tck.jakarta.platform.ant.Helper;
import tck.jakarta.platform.ant.Vehicles;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

/**
 * These tests only run if there is ts.home property set.
 */
@EnabledIfSystemProperty(named = "ts.home", matches = ".*")
public class VehiclesArtifactTest {
    static Path tsHome = Paths.get(System.getProperty("ts.home"));

    /**
     * Validate the expected locations in the EE 10 TCK dist pointed to by the ts.home system property
     */
    @Test
    public void validateTsHome() {
        String tsHomeProp = System.getProperty("ts.home");
        Assertions.assertNotNull(tsHomeProp);
        Path tsHome = Paths.get(tsHomeProp);
        Path bin = tsHome.resolve("bin");
        Path classes = tsHome.resolve("classes");
        Path vehicles = tsHome.resolve("classes/com/sun/ts/tests/common/vehicle");
        Path ejbliteServlet = tsHome.resolve("src/com/sun/ts/tests/common/vehicle/ejbliteservlet/EJBLiteServletVehicle.java.txt");
        Path dist = tsHome.resolve("dist");
        Path war = tsHome.resolve("dist/com/sun/ts/tests/ejb32/timer/service/stateless/ejb32_timer_service_stateless_ejbliteservlet_vehicle_web.war");
        // Validate that there are dist, src and src/vehicle.properties
        Assertions.assertTrue(Files.exists(tsHome), tsHome+" exists");
        Assertions.assertTrue(Files.exists(bin), bin+" exists");
        Assertions.assertTrue(Files.exists(classes), classes+" exists");
        Assertions.assertTrue(Files.exists(ejbliteServlet), ejbliteServlet+" exists");
        Assertions.assertTrue(Files.exists(vehicles), vehicles+" exists");
        Assertions.assertTrue(Files.exists(dist), dist+" exists");
        Assertions.assertTrue(Files.exists(war), war+" exists");
    }

    /**
     * Validate the war for the com/sun/ts/tests/ejb32/timer/service/stateless build.xml against
     * tshome/dist/com/sun/ts/tests/ejb32/timer/service/stateless/ejb32_timer_service_stateless_ejbliteservlet_vehicle_web.war
     */
    @Test
    public void test_ejb32_timer_service_stateless_ant() {
        // vehicle = ejbliteservlet
        // app.name = ejb32_timer_service_stateless
        /*
        <ts.vehicles name="${app.name}" buildleveloverride="2" classes="
	    com/sun/ts/tests/ejb32/timer/service/common/ClientBase.class,
	    com/sun/ts/tests/ejb32/timer/service/common/TimersSingletonBean.class,
	    com/sun/ts/tests/ejb32/timer/service/common/TimersStatelessBean.class,
	    com/sun/ts/tests/ejb32/timer/service/common/NoTimersStatefulBean.class,
	    com/sun/ts/tests/ejb32/timer/service/common/AutoTimerBeanBase.class,
	    com/sun/ts/tests/ejb32/timer/service/common/TimerIF.class,
	    com/sun/ts/tests/ejb30/timer/common/TimerUtil.class,
	    com/sun/ts/tests/ejb30/timer/common/TimerBeanBase.class,
	    com/sun/ts/tests/ejb30/timer/common/TimerBeanBaseWithoutTimeOutMethod.class,
	    com/sun/ts/tests/ejb30/timer/common/TimeoutStatusBean.class,
	    com/sun/ts/tests/ejb30/timer/common/TimerInfo.class
	    ">
        </ts.vehicles>
         */
        Path buildXml = tsHome.resolve("src/com/sun/ts/tests/ejb32/timer/service/stateless/build.xml");
        Project project = ParseBuildTree.parseBuildXml(buildXml);
        // Needed for the ts.javac classpath by way of ts.classpath
        System.out.printf("javaee.home.ri=%s\n", project.getProperty("javaee.home.ri"));
        //
        System.out.printf("class.dir=%s\n", project.getProperty("class.dir"));
        Target pkgTarget = project.getTargets().get("package");
        Location pkgLocation = pkgTarget.getLocation();

        VehicleVerifier verifier = VehicleVerifier.getInstance(new File(pkgLocation.getFileName()));
        Assertions.assertEquals("ejbliteservlet", verifier.getVehicleSet()[0]);

        Task tsVehicle = pkgTarget.getTasks()[0];
        Hashtable<String, Object> rcAttrs = tsVehicle.getRuntimeConfigurableWrapper().getAttributeMap();
        AttributeMap attributeMap = new AttributeMap(project, rcAttrs);
        String tsVehicleName = attributeMap.getAttribute("name");
        Assertions.assertEquals("ejb32_timer_service_stateless", tsVehicleName);
        String vehiclePkgDir = project.getProperty("vehicle.pkg.dir");
        Assertions.assertEquals("com/sun/ts/tests/common/vehicle", vehiclePkgDir);

        Vehicles vehicles = new Vehicles(tsVehicleName, vehiclePkgDir, attributeMap, tsVehicle.getRuntimeConfigurableWrapper(), pkgLocation);
        System.out.println(vehicles);
        List<VehicleType> toBuild = vehicles.determineVehicleToBuild();
        Assertions.assertEquals(VehicleType.ejbliteservlet, toBuild.get(0));

        tsVehicle.perform();
        Path ejbliteVehicleClass = tsHome.resolve("classes/com/sun/ts/tests/ejb32/timer/service/stateless/EJBLiteServletVehicle.class");
        File classFile = ejbliteVehicleClass.toFile();
        System.out.printf("ejbliteVehicleClass file: %s, exists: %s\n", classFile.getAbsolutePath(), classFile.exists());
    }

    /**
     * Validate a hand-coded shrinkwrap war against the
     * tshome/dist/com/sun/ts/tests/ejb32/timer/service/stateless/ejb32_timer_service_stateless_ejbliteservlet_vehicle_web.war
     [starksm@scottryzen stateless]$ jar -tf $TS_HOME/dist/com/sun/ts/tests/ejb32/timer/service/stateless/ejb32_timer_service_stateless_ejbliteservlet_vehicle_web.war
     META-INF/MANIFEST.MF
     WEB-INF/classes/com/sun/ts/lib/harness/EETest$Fault.class
     WEB-INF/classes/com/sun/ts/lib/harness/EETest$SetupException.class
     WEB-INF/classes/com/sun/ts/lib/harness/EETest.class
     WEB-INF/classes/com/sun/ts/lib/harness/ServiceEETest.class
     WEB-INF/classes/com/sun/ts/tests/common/vehicle/VehicleClient.class
     WEB-INF/classes/com/sun/ts/tests/common/vehicle/VehicleRunnable.class
     WEB-INF/classes/com/sun/ts/tests/common/vehicle/VehicleRunnerFactory.class
     WEB-INF/classes/com/sun/ts/tests/common/vehicle/ejbliteshare/EJBLiteClientIF.class
     WEB-INF/classes/com/sun/ts/tests/common/vehicle/ejbliteshare/ReasonableStatus.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/common/helper/Helper.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/common/lite/EJBLiteClientBase.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/common/lite/EJBLiteJsfClientBase.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/common/lite/NumberEnum.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/common/lite/NumberIF.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/timer/common/TimeoutStatusBean.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/timer/common/TimerBeanBase.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/timer/common/TimerBeanBaseWithoutTimeOutMethod.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/timer/common/TimerInfo.class
     WEB-INF/classes/com/sun/ts/tests/ejb30/timer/common/TimerUtil.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/AutoTimerBeanBase.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/ClientBase.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/NoTimersStatefulBean.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/TimerIF.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/TimersSingletonBean.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/common/TimersStatelessBean.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/stateless/Client.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/stateless/EJBLiteServletVehicle.class
     WEB-INF/classes/com/sun/ts/tests/ejb32/timer/service/stateless/HttpServletDelegate.class
     WEB-INF/web.xml
     */
    @Test
    public void test_ejb32_timer_service_stateless_byhand() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
    }
}
