package tck.jakarta.platform.ant.api;

import com.sun.ts.lib.harness.VehicleVerifier;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import tck.jakarta.platform.ant.ClientJar;
import tck.jakarta.platform.ant.Ear;
import tck.jakarta.platform.ant.EjbJar;
import tck.jakarta.platform.ant.PackageTarget;
import tck.jakarta.platform.ant.ProjectWrapper;
import tck.jakarta.platform.ant.TSFileSet;
import tck.jakarta.platform.ant.Utils;
import tck.jakarta.platform.ant.Vehicles;
import tck.jakarta.platform.ant.War;
import tck.jakarta.platform.ant.st4.RecordAdaptor;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Build the DeploymentMethodInfo(s) for a given test class/package that corresponds to an Arquillian
 * annotated {@link org.jboss.arquillian.container.test.api.Deployment} method. There can be multiple deployments
 * for a given test class if there are multiple vehicle types associated with the test package.
 */
public class DeploymentMethodInfoBuilder {
    private static final Logger log = Logger.getLogger(DeploymentMethodInfoBuilder.class.getName());
    private static final String[] ARQUILLIAN_IMPORTS = {
        "org.jboss.arquillian.container.test.api.Deployment",
        "org.jboss.arquillian.container.test.api.OperateOnDeployment",
        "org.jboss.arquillian.container.test.api.OverProtocol",
        "org.jboss.arquillian.container.test.api.TargetsContainer",
        "org.jboss.arquillian.junit5.ArquillianExtension",
        "tck.arquillian.protocol.common.TargetVehicle",
        "org.jboss.shrinkwrap.api.asset.StringAsset",
        "org.jboss.shrinkwrap.api.ShrinkWrap",
        "org.jboss.shrinkwrap.api.spec.EnterpriseArchive",
        "org.jboss.shrinkwrap.api.spec.JavaArchive",
        "org.jboss.shrinkwrap.api.spec.WebArchive",
        "java.net.URL",
    };
    // Path to EE10 TCK dist
    private Path tsHome;

    /**
     * Create a DeploymentMethodInfoBuilder for the given EE10 TCK dist path
     * @param tsHome - A valid EE10 TCK dist path
     */
    public DeploymentMethodInfoBuilder(Path tsHome) {
        this.tsHome = tsHome;
    }

    /**
     * Look for a TS_HOME environment variable or ts.home system property to obtain
     * the tsHome Path to a valid EE10 TCK distribution.
     * @return A DeploymentMethodInfoBuilder
     * @throws FileNotFoundException - if no valid env, or property maps to a valid EE10 TCK dist
     */
    public static DeploymentMethodInfoBuilder fromSystemProperty() throws FileNotFoundException {
        String tsHomeProp = System.getProperty("ts.home");
        if(tsHomeProp == null) {
            tsHomeProp = System.getenv("TS_HOME");
        }
        if(tsHomeProp == null) {
            throw new FileNotFoundException("Neither a TS_HOME environment variable or ts.home system property were set.");
        }
        Path tsHome = Paths.get(tsHomeProp);
        Utils.validateTSHome(tsHome);
        return new DeploymentMethodInfoBuilder(tsHome);
    }

    /**
     * Parses the ant build.xml file for the test directory associated with the pkg and returns the
     * Arquillian deployment methods for the test deployment artifacts that should be generated.

     * @param clazz - a test class in the EE10 TCK
     * @return
     * @throws IOException - on failure to parse the build.xml file
     */
    public List<DeploymentMethodInfo> forTestClass(Class<?> clazz) throws IOException {
        String testClass = clazz.getPackage().getName();
        int lastDot = testClass.lastIndexOf('.');
        // The simple name, e.g., MyTest for com.sun.*.MyTest
        String testClassSimpleName = testClass.substring(lastDot + 1);
        String pkg = testClass.substring(0, lastDot);
        String pkgPath = pkg.replace('.', '/');
        Path srcDir = tsHome.resolve("src");
        Path buildXml = srcDir.resolve(pkgPath+"/build.xml");
        if(!buildXml.toFile().exists()) {
            throw new FileNotFoundException("The pkg path does not contain a build.xml file: "+buildXml);
        }

        Project project = new Project();
        project.init();
        // The location of the glassfish download for the jakarta api jars
        project.setProperty("ts.home", tsHome.toAbsolutePath().toString());
        project.setProperty("javaee.home.ri", "${ts.home}/../glassfish7/glassfish");
        debug("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target antPackageTarget = project.getTargets().get("package");
        PackageTarget pkgTargetWrapper = new PackageTarget(new ProjectWrapper(project), antPackageTarget);

        VehicleVerifier verifier = VehicleVerifier.getInstance(new File(antPackageTarget.getLocation().getFileName()));
        String[] vehicles = verifier.getVehicleSet();
        debug("Vehicles: %s\n", Arrays.asList(vehicles));

        ArrayList<DeploymentMethodInfo> deploymentMethodInfos = new ArrayList<>();
        if(vehicles.length == 0) {
            DeploymentMethodInfo methodInfo = parseNonVehiclePackage(pkgTargetWrapper, clazz);
            deploymentMethodInfos.add(methodInfo);
        } else {
            for(String vehicle : vehicles) {
                VehicleType vehicleType = VehicleType.valueOf(vehicle);
                DeploymentMethodInfo methodInfo = parseVehiclePackage(pkgTargetWrapper, clazz, vehicleType);
                deploymentMethodInfos.add(methodInfo);
            }
        }

        return deploymentMethodInfos;
    }
    public DeploymentMethodInfo forTestClassAndVehicle(Class<?> testClass, VehicleType vehicleType) throws IOException {
        String pkg = testClass.getPackageName();
        String pkgPath = pkg.replace('.', '/');
        Path srcDir = tsHome.resolve("src");
        Path buildXml = srcDir.resolve(pkgPath+"/build.xml");
        if(!buildXml.toFile().exists()) {
            throw new FileNotFoundException("The pkg path does not contain a build.xml file: "+buildXml);
        }

        Project project = new Project();
        project.init();
        // The location of the glassfish download for the jakarta api jars
        project.setProperty("ts.home", tsHome.toAbsolutePath().toString());
        project.setProperty("javaee.home.ri", "${ts.home}/../glassfish7/glassfish");
        debug("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target antPackageTarget = project.getTargets().get("package");
        PackageTarget pkgTargetWrapper = new PackageTarget(new ProjectWrapper(project), antPackageTarget);

        DeploymentMethodInfo methodInfo;
        if(vehicleType == null || vehicleType == VehicleType.none) {
            methodInfo = parseNonVehiclePackage(pkgTargetWrapper, testClass);
        } else {
            VehicleVerifier verifier = VehicleVerifier.getInstance(new File(antPackageTarget.getLocation().getFileName()));
            String[] vehicles = verifier.getVehicleSet();
            debug("Vehicles: %s\n", Arrays.asList(vehicles));

            methodInfo = parseVehiclePackage(pkgTargetWrapper, testClass, vehicleType);
        }
        return methodInfo;
    }

    private DeploymentMethodInfo parseNonVehiclePackage(PackageTarget pkgTargetWrapper, Class<?> clazz) {
        pkgTargetWrapper.execute();
        String protocol = pkgTargetWrapper.hasClientJarDef() ? "appclient" : "javatest";
        String testClassSimpleName = clazz.getSimpleName();

        // Extract the information for the current deployment from the parsed ts.vehicles info
        DeploymentInfo deployment = new DeploymentInfo(clazz, pkgTargetWrapper.getDeploymentName(), protocol, VehicleType.none);
        populateDeployment(deployment, pkgTargetWrapper);

        // Generate the deployment method
        STGroup deploymentMethodGroup = new STGroupFile("DeploymentMethod.stg");
        deploymentMethodGroup.registerModelAdaptor(War.Content.class, new RecordAdaptor<War.Content>());
        ST template = deploymentMethodGroup.getInstanceOf("genMethodNonVehicle");
        template.add("pkg", pkgTargetWrapper);
        template.add("deployment", deployment);
        template.add("testClass", testClassSimpleName);
        String methodCode = template.render().trim();
        DeploymentMethodInfo methodInfo = new DeploymentMethodInfo();
        methodInfo.setVehicle(VehicleType.none);
        methodInfo.setMethodCode(methodCode);
        methodInfo.setImports(Arrays.asList(ARQUILLIAN_IMPORTS));

        return methodInfo;
    }
    private DeploymentMethodInfo parseVehiclePackage(PackageTarget pkgTargetWrapper, Class<?> clazz, VehicleType vehicleType) {
        pkgTargetWrapper.execute(vehicleType);
        String protocol = getProtocolForVehicle(vehicleType);
        String testClassSimpleName = clazz.getSimpleName();
        // Extract the information for the current deployment from the parsed ts.vehicles info
        DeploymentInfo deployment = new DeploymentInfo(clazz, pkgTargetWrapper.getDeploymentName(), protocol, vehicleType);
        populateDeployment(deployment, pkgTargetWrapper);

        // Generate the deployment method
        STGroup deploymentMethodGroup = new STGroupFile("DeploymentMethod.stg");
        deploymentMethodGroup.registerModelAdaptor(War.Content.class, new RecordAdaptor<War.Content>());
        ST template = deploymentMethodGroup.getInstanceOf("genMethodVehicle");
        template.add("pkg", pkgTargetWrapper);
        template.add("deployment", deployment);
        template.add("testClass", testClassSimpleName);
        String methodCode = template.render().trim();
        DeploymentMethodInfo methodInfo = new DeploymentMethodInfo();
        methodInfo.setVehicle(vehicleType);
        methodInfo.setMethodCode(methodCode);
        methodInfo.setImports(Arrays.asList(ARQUILLIAN_IMPORTS));

        return methodInfo;
    }

    private void populateDeployment(DeploymentInfo deployment, PackageTarget pkgTargetWrapper) {
        Vehicles vehicleDef = pkgTargetWrapper.getVehiclesDef();
        // Client
        if(pkgTargetWrapper.hasClientJarDef()) {
            ClientJar clientJarDef = pkgTargetWrapper.getClientJarDef();
            if(vehicleDef != null) {
                clientJarDef.addFileSet(vehicleDef.getClientElements());
                // common to all vehicles
                if (vehicleDef.getJarElements() != null) {
                    TSFileSet jarElements = vehicleDef.getJarElements();
                    clientJarDef.addFileSet(jarElements);
                }
                // Look for a *_vehicle_client.xml descriptor since this get overriden to tsHome/tmp
                try {
                    String resPath = Utils.getVehicleArchiveDescriptor(deployment.testClass, deployment.vehicle, "client");
                    clientJarDef.setVehicleDescriptor(resPath);
                } catch (MalformedURLException e) {
                    info("Failed to locate client jar descriptor for vehicle: %s\n%s" + deployment.vehicle, e);
                }
            }
            deployment.setClientJar(clientJarDef);
            info("Client jar added to deployment: %s\n", clientJarDef);
        }
        // EJB
        if(pkgTargetWrapper.hasEjbJarDef()) {
            EjbJar ejbJarDef = pkgTargetWrapper.getEjbJarDef();
            if(vehicleDef != null) {
                ejbJarDef.addFileSet(vehicleDef.getEjbElements());
                // common to all vehicles
                if (vehicleDef.getJarElements() != null) {
                    TSFileSet jarElements = vehicleDef.getJarElements();
                    ejbJarDef.addFileSet(jarElements);
                }
                // Look for a *_vehicle_ejb.xml descriptor since this get overriden to tsHome/tmp
                try {
                    String resPath = Utils.getVehicleArchiveDescriptor(deployment.testClass, deployment.vehicle, "ejb");
                    ejbJarDef.setVehicleDescriptor(resPath);
                } catch (MalformedURLException e) {
                    info("Failed to locate ejb jar descriptor for vehicle: %s\n%s" + deployment.vehicle, e);
                }
            }
            deployment.setEjbJar(ejbJarDef);
            info("Ejb jar added to deployment: %s\n", ejbJarDef);
        }
        // War
        if(pkgTargetWrapper.hasWarDef()) {
            War warDef = pkgTargetWrapper.getWarDef();
            if(vehicleDef != null) {
                switch (deployment.getVehicle()) {
                    case servlet:
                        warDef.addFileSet(vehicleDef.getServletElements());
                        break;
                    case jsp:
                        warDef.addFileSet(vehicleDef.getJspElements());
                        break;
                }
                // common to all vehicles
                if (vehicleDef.getJarElements() != null) {
                    TSFileSet jarElements = vehicleDef.getJarElements();
                    warDef.addFileSet(jarElements);
                }
                // Look for a *_vehicle_web.xml descriptor since this get overriden to tsHome/tmp
                try {
                    String resPath = Utils.getVehicleArchiveDescriptor(deployment.testClass, deployment.vehicle, "web");
                    warDef.setVehicleDescriptor(resPath);
                } catch (MalformedURLException e) {
                    info("Failed to locate war descriptor for vehicle: %s\n%s" + deployment.vehicle, e);
                }
            }
            deployment.setWar(warDef);
            info("War added to deployment: %s\n", warDef);
            info("War has content: %s\n", warDef.getWebContent());
        }
        // Ear
        if(pkgTargetWrapper.hasEarDef()) {
            Ear earDef = pkgTargetWrapper.getEarDef();
            if(vehicleDef != null) {
                earDef.addFileSet(vehicleDef.getEarElements());
                // common to all vehicles
                if (vehicleDef.getJarElements() != null) {
                    TSFileSet jarElements = vehicleDef.getJarElements();
                    earDef.addFileSet(jarElements);
                }
            }
            deployment.setEar(earDef);
            debug("Ear added to deployment: %s\n", earDef);
        }
    }

    /**
     * Map from the vehicle type to the arquillian protocol. This is based on which ts.vehicles create a
     * client jar with the com.sun.ts.tests.common.vehicle.VehicleClient as the mainClass.
     *
     * @param vehicleType - vehicle
     * @return either appclient or javatest
     */
    private String getProtocolForVehicle(VehicleType vehicleType) {
        String protocol = switch (vehicleType) {
            // This is all the types in ts.vehicles.xml that use an appclient
            case appclient, ejb, wsappclient, wsejb, appmanaged, appmanagedNoTx, stateless3, stateful3 -> "appclient";
            default -> "javatest";
        };
        return protocol;
    }

    private void info(String format, Object ... args) {
        String msg = String.format(format, args);
        log.info(msg);
    }
    private void debug(String format, Object ... args) {
        String msg = String.format(format, args);
        log.info(msg);
    }
}
