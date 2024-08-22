package tck.jakarta.platform.ant.api;

import com.sun.ts.lib.harness.VehicleVerifier;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.Target;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import tck.jakarta.platform.ant.ClientJar;
import tck.jakarta.platform.ant.Ear;
import tck.jakarta.platform.ant.EjbJar;
import tck.jakarta.platform.ant.Lib;
import tck.jakarta.platform.ant.PackageTarget;
import tck.jakarta.platform.ant.Par;
import tck.jakarta.platform.ant.ProjectWrapper;
import tck.jakarta.platform.ant.Rar;
import tck.jakarta.platform.ant.TsArchiveInfo;
import tck.jakarta.platform.ant.TsArchiveInfoSet;
import tck.jakarta.platform.ant.TsFileSet;
import tck.jakarta.platform.ant.TsPackageInfo;
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
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Build the {@link TestPackageInfo} for a given test class/package. This contains the information needed to generate
 * a Arquillian/Junit5 based subclass of the test class. There can be multiple {@link TestClientInfo} instances
 * for a given test class if there are multiple vehicle types associated with the test package.
 */
public class TestPackageInfoBuilder {
    private static final Logger log = Logger.getLogger(TestPackageInfoBuilder.class.getName());
    private static final String[] ARQUILLIAN_IMPORTS = {
        "org.jboss.arquillian.test.api.ArquillianResource",
        "org.jboss.arquillian.container.test.api.Deployment",
        "org.jboss.arquillian.container.test.api.OperateOnDeployment",
        "org.jboss.arquillian.container.test.api.OverProtocol",
        "org.jboss.arquillian.container.test.api.TargetsContainer",
        "org.jboss.arquillian.junit5.ArquillianExtension",
        "tck.arquillian.protocol.common.TargetVehicle",
        "tck.arquillian.porting.lib.spi.TestArchiveProcessor",
        "org.jboss.shrinkwrap.api.ShrinkWrap",
        "org.jboss.shrinkwrap.api.asset.StringAsset",
        "org.jboss.shrinkwrap.api.exporter.ZipExporter",
        "org.jboss.shrinkwrap.api.spec.EnterpriseArchive",
        "org.jboss.shrinkwrap.api.spec.JavaArchive",
        "org.jboss.shrinkwrap.api.spec.WebArchive",
        "java.net.URL",
        "org.junit.jupiter.api.MethodOrderer",
        "org.junit.jupiter.api.Tag",
        "org.junit.jupiter.api.Test",
        "org.junit.jupiter.api.TestMethodOrder",
        "org.junit.jupiter.api.extension.ExtendWith",

    };

    // Path to EE10 TCK dist
    private final Path tsHome;

    /**
     * Create a DeploymentMethodInfoBuilder for the given EE10 TCK dist path
     * @param tsHome - A valid EE10 TCK dist path
     */
    public TestPackageInfoBuilder(Path tsHome) {
        this.tsHome = tsHome;
    }

    /**
     * Look for a TS_HOME environment variable or ts.home system property to obtain
     * the tsHome Path to a valid EE10 TCK distribution.
     * @return A DeploymentMethodInfoBuilder
     * @throws FileNotFoundException - if no valid env, or property maps to a valid EE10 TCK dist
     */
    public static TestPackageInfoBuilder fromSystemProperty() throws FileNotFoundException {
        String tsHomeProp = System.getProperty("ts.home");
        if(tsHomeProp == null) {
            tsHomeProp = System.getenv("TS_HOME");
        }
        if(tsHomeProp == null) {
            throw new FileNotFoundException("Neither a TS_HOME environment variable or ts.home system property were set.");
        }
        Path tsHome = Paths.get(tsHomeProp);
        Utils.validateTSHome(tsHome);
        return new TestPackageInfoBuilder(tsHome);
    }

    /**
     * Parses the ant build.xml file for the test directory associated with the pkg and returns the
     * Arquillian deployment methods for the test deployment artifacts that should be generated.

     * @param clazz - a test class in the EE10 TCK
     * @param testMethods - the test methods to include in the test client
     * @return the {@link TestPackageInfo} instance for the test class
     * @throws IOException - on failure to parse the build.xml file
     */
    public TestPackageInfo buildTestPackgeInfoEx(Class<?> clazz, List<TestMethodInfo> testMethods, EE11toEE10Mapping mapping) throws IOException {
        TestPackageInfo testPackageInfo = new TestPackageInfo(clazz, testMethods, mapping);
        List<TestClientInfo> testClientInfos = buildTestClientsEx(clazz, testMethods, mapping);
        testPackageInfo.setTestClients(testClientInfos);

        return testPackageInfo;
    }

    /**
     * Parses the ant build.xml file for the test directory associated with the pkg and returns the
     * Arquillian deployment methods for the test deployment artifacts that should be generated. This version
     * allows for different throws types per test method.
     *
     * @param clazz - a test class in the EE10 TCK
     * @param testMethods - the test method names and throws to include in the test client
     * @return the list of {@link TestClientInfo} instances for the test class
     * @throws IOException on failure to parse the build.xml file
     */
    public List<TestClientInfo> buildTestClientsEx(Class<?> clazz, List<TestMethodInfo> testMethods, EE11toEE10Mapping mapping) throws IOException {
        ArrayList<TestClientInfo> testClientInfos = new ArrayList<>();
        // Add the test class mapping
        mapping.addTestClassMapping(clazz, tsHome);
        // The simple name, e.g., MyTest for com.sun.*.MyTest
        String testClassSimpleName = clazz.getSimpleName();
        String pkg = clazz.getPackageName();
        pkg = mapping.getEE10TestPackageName(pkg);
        String pkgPath = pkg.replace('.', '/');
        Path srcDir = tsHome.resolve("src");
        Path buildXml = srcDir.resolve(pkgPath+"/build.xml");
        if(!buildXml.toFile().exists()) {
            throw new FileNotFoundException("The pkg path does not contain a build.xml file: "+buildXml);
        }

        VehicleVerifier verifier = VehicleVerifier.getInstance(buildXml.toFile());
        String[] vehicles = verifier.getVehicleSet();
        debug("Vehicles: %s\n", Arrays.asList(vehicles));

        // Does this test class have a common deployment?
        CommonApps commonApps = CommonApps.getInstance(tsHome);
        DeploymentMethodInfo commonDeployment = commonApps.getCommonDeployment(buildXml, testClassSimpleName);
        // Get any keyword tags
        KeywordTags keywordTags = KeywordTags.getInstance(tsHome);
        List<String> tags = keywordTags.getTags(Paths.get(pkgPath));
        // Load the deployment descriptor mapping
        DeploymentDescriptors.load();

        // Generate the test deployment method
        if(vehicles.length == 0) {
            Project project = initProject(buildXml);
            debug("Parsing(%s)\n", buildXml);
            ProjectHelper.configureProject(project, buildXml.toFile());
            Target antPackageTarget = project.getTargets().get("package");
            PackageTarget pkgTargetWrapper = new PackageTarget(new ProjectWrapper(project), antPackageTarget);

            DeploymentMethodInfo methodInfo = parseNonVehiclePackage(mapping, pkgTargetWrapper, clazz);
            // Add a tag for the protocol so one can filter tests by protocol
            String protocol = methodInfo.getDebugInfo().getProtocol();
            ArrayList<String> extraTags = new ArrayList<>(tags);
            extraTags.add("tck-"+protocol);
            // The class name of the generated clazz subclass
            String genTestClassName = "ClientTest";
            if(testClassSimpleName.equals("ClientTest")) {
                genTestClassName = "ClientExtTest";
            }
            TestClientInfo testClientInfo = new TestClientInfo(genTestClassName, clazz, testMethods);
            testClientInfo.setVehicle(VehicleType.none);
            testClientInfo.setTestDeployment(methodInfo);
            testClientInfo.setCommonDeployment(commonDeployment);
            testClientInfo.setTags(extraTags);
            testClientInfos.add(testClientInfo);
        } else {
            for(String vehicle : vehicles) {
                VehicleType vehicleType = VehicleType.valueOf(vehicle);
                // Skip unsupported vehicles
                if(vehicleType == VehicleType.ejbembed) {
                    continue;
                }
                // ejblitejsf vehicle only applies to the JsfClient class
                if (vehicleType == VehicleType.ejblitejsf && !testClassSimpleName.equals("JsfClient")) {
                    info("Skipping ejblitejsf vehicle for class: %s\n", testClassSimpleName);
                    continue;
                }
                if(testClassSimpleName.equals("JsfClient") && vehicleType != VehicleType.ejblitejsf) {
                    info("Skipping non-ejblitejsf vehicle for class: %s\n", testClassSimpleName);
                    continue;
                }
                Project project = initProject(buildXml);
                debug("Parsing(%s)\n", buildXml);
                ProjectHelper.configureProject(project, buildXml.toFile());
                Target antPackageTarget = project.getTargets().get("package");
                PackageTarget pkgTargetWrapper = new PackageTarget(new ProjectWrapper(project), antPackageTarget);
                DeploymentMethodInfo methodInfo = parseVehiclePackage(mapping, pkgTargetWrapper, clazz, vehicleType);
                // Add a tag for the protocol so one can filter tests by protocol
                String protocol = methodInfo.getDebugInfo().getProtocol();
                ArrayList<String> vehicleTags = new ArrayList<>(tags);
                vehicleTags.add("tck-"+protocol);
                // The class name of the generated clazz subclass
                String vehicleName = capitalizeFirst(vehicleType.name());
                String genTestClassName = testClassSimpleName+vehicleName+"Test";
                TestClientInfo testClientInfo = new TestClientInfo(genTestClassName, clazz, testMethods);
                testClientInfo.setVehicle(vehicleType);
                testClientInfo.setTestDeployment(methodInfo);
                testClientInfo.setCommonDeployment(commonDeployment);
                testClientInfo.setTags(vehicleTags);
                testClientInfos.add(testClientInfo);
            }
        }

        return testClientInfos;
    }

    /**
     * This is only used for testing purposes to generate the deployment method for a test class. If a mapping
     * needs to be done, the caller needs to do that.
     * @param testClass - EE10 test class
     * @param vehicleType - the vehicle type to generate the deployment method for
     * @return the deployment method info for the test class
     * @throws IOException - on failure to parse the build.xml file
     */
    public DeploymentMethodInfo forTestClassAndVehicle(Class<?> testClass, VehicleType vehicleType) throws IOException {
        return forTestClassAndVehicle(testClass, testClass.getPackageName(), testClass.getSimpleName(), vehicleType);
    }
    public DeploymentMethodInfo forTestClassAndVehicle(Class<?> testClass, String pkg, String simpleClassName, VehicleType vehicleType) throws IOException {
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
        project.setBaseDir(buildXml.getParent().toFile());
        project.setProperty(MagicNames.ANT_FILE, buildXml.toAbsolutePath().toString());

        debug("Parsing(%s)\n", buildXml);
        ProjectHelper.configureProject(project, buildXml.toFile());
        Target antPackageTarget = project.getTargets().get("package");
        PackageTarget pkgTargetWrapper = new PackageTarget(new ProjectWrapper(project), antPackageTarget);

        // Load the deployment descriptor mapping
        DeploymentDescriptors.load();

        DeploymentMethodInfo methodInfo;
        EE11toEE10Mapping mapping = DefaultEEMapping.getInstance();
        if(vehicleType == null || vehicleType == VehicleType.none) {
            methodInfo = parseNonVehiclePackage(mapping, pkgTargetWrapper, testClass, pkg, simpleClassName);
        } else {
            VehicleVerifier verifier = VehicleVerifier.getInstance(new File(antPackageTarget.getLocation().getFileName()));
            String[] vehicles = verifier.getVehicleSet();
            debug("Vehicles: %s\n", Arrays.asList(vehicles));

            methodInfo = parseVehiclePackage(mapping, pkgTargetWrapper, testClass, pkg, simpleClassName, vehicleType);
        }
        return methodInfo;
    }

    private Project initProject(Path buildXml) {
        Project project = new Project();
        project.init();
        // The location of the glassfish download for the jakarta api jars
        project.setProperty("ts.home", tsHome.toAbsolutePath().toString());
        project.setProperty("javaee.home.ri", "${ts.home}/../glassfish7/glassfish");
        project.setBaseDir(buildXml.getParent().toFile());
        project.setProperty(MagicNames.ANT_FILE, buildXml.toAbsolutePath().toString());
        return project;
    }

    /**
     * Execute the package target and parse the deployment information for a non-vehicle test class.
     * @param pkgTargetWrapper - the ant "package" target wrapper
     * @param clazz - the EE10 tck test class
     * @return the deployment method info for the test class
     */
    private DeploymentMethodInfo parseNonVehiclePackage(EE11toEE10Mapping mapping, PackageTarget pkgTargetWrapper, Class<?> clazz) {
        return parseNonVehiclePackage(mapping, pkgTargetWrapper, clazz, clazz.getPackageName(), clazz.getSimpleName());
    }
    private DeploymentMethodInfo parseNonVehiclePackage(EE11toEE10Mapping mapping, PackageTarget pkgTargetWrapper,
                                                        Class<?> clazz, String testClassPkg, String testClassSimpleName) {
        // Run the ant "package" target
        pkgTargetWrapper.execute();
        // Resolve any unprocessed TsArchiveInfoSets
        pkgTargetWrapper.resolveTsArchiveInfoSets();

        String protocol = pkgTargetWrapper.hasClientJarDef() ? "appclient" : "javatest";

        // Extract the information for the current deployment from the parsed ts.vehicles info
        DeploymentInfo deployment = new DeploymentInfo(testClassPkg, testClassSimpleName, pkgTargetWrapper.getDeploymentName(), protocol, VehicleType.none);
        deployment.setTestClass(clazz);
        populateDeployment(mapping, deployment, pkgTargetWrapper);

        // Generate the deployment method
        STGroup deploymentMethodGroup = new STGroupFile("DeploymentMethod.stg");
        deploymentMethodGroup.registerModelAdaptor(War.Content.class, new RecordAdaptor<War.Content>());
        ST template = deploymentMethodGroup.getInstanceOf("genMethodNonVehicle");
        template.add("pkg", pkgTargetWrapper);
        template.add("deployment", deployment);
        template.add("testClass", testClassSimpleName);
        String methodCode = template.render().trim();
        DeploymentMethodInfo methodInfo = new DeploymentMethodInfo(VehicleType.none, Arrays.asList(ARQUILLIAN_IMPORTS), methodCode);
        methodInfo.setName(deployment.getName());
        methodInfo.setDebugInfo(deployment);

        // Look

        return methodInfo;
    }

    /**
     * Execute the package target and parse the deployment information for a test class with a target vehicle.
     * @param mapping - the EE11 to EE10 mapping
     * @param pkgTargetWrapper - the ant "package" target wrapper
     * @param clazz - the EE10 tck test class
     * @param vehicleType - the TCK vehicle type
     * @return the deployment method info for the test class + vehicle
     */
    private DeploymentMethodInfo parseVehiclePackage(EE11toEE10Mapping mapping, PackageTarget pkgTargetWrapper, Class<?> clazz, VehicleType vehicleType) {
        return parseVehiclePackage(mapping, pkgTargetWrapper, clazz, clazz.getPackageName(), clazz.getSimpleName(), vehicleType);
    }
    private DeploymentMethodInfo parseVehiclePackage(EE11toEE10Mapping mapping, PackageTarget pkgTargetWrapper,
                                                     Class<?> clazz, String testClassPkg, String testClassSimpleName, VehicleType vehicleType) {
        pkgTargetWrapper.execute(vehicleType);
        String protocol = getProtocolForVehicle(vehicleType);
        // Extract the information for the current deployment from the parsed ts.vehicles info
        DeploymentInfo deployment = new DeploymentInfo(testClassPkg, testClassSimpleName, pkgTargetWrapper.getDeploymentName(), protocol, vehicleType);
        deployment.setTestClass(clazz);
        populateDeployment(mapping, deployment, pkgTargetWrapper);

        // Generate the deployment method
        STGroup deploymentMethodGroup = new STGroupFile("DeploymentMethod.stg");
        deploymentMethodGroup.registerModelAdaptor(War.Content.class, new RecordAdaptor<War.Content>());
        ST template = deploymentMethodGroup.getInstanceOf("genMethodVehicle");
        template.add("pkg", pkgTargetWrapper);
        template.add("deployment", deployment);
        template.add("testClass", testClassSimpleName);
        String methodCode = template.render().trim();
        DeploymentMethodInfo methodInfo = new DeploymentMethodInfo(vehicleType, Arrays.asList(ARQUILLIAN_IMPORTS), methodCode);
        methodInfo.setName(deployment.getName());
        methodInfo.setDebugInfo(deployment);

        return methodInfo;
    }

    /**
     * Called after completion of the ant "package" target to populate the deployment information for the test class.
     * @param deployment - deployment to populate with the information resulting from the "package" target execution
     * @param pkgTargetWrapper - the ant "package" target wrapper
     */
    private void populateDeployment(EE11toEE10Mapping mapping, DeploymentInfo deployment, PackageTarget pkgTargetWrapper) {
        Vehicles vehicleDef = pkgTargetWrapper.getVehiclesDef();
        ArrayList<String> foundDescriptors = new ArrayList<>();
        // Client
        if(pkgTargetWrapper.hasClientJarDef()) {
            ClientJar clientJarDef = pkgTargetWrapper.getClientJarDef();
            if(vehicleDef != null) {
                clientJarDef.addFileSet(vehicleDef.getClientElements());
                // common to all vehicles
                if (!vehicleDef.getJarElements().isEmpty()) {
                    List<TsFileSet> jarElements = vehicleDef.getJarElements();
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
            foundDescriptors.add("Client:\n");
            foundDescriptors.addAll(clientJarDef.getFoundDescriptors());
            info("Client jar added to deployment: %s\n", clientJarDef);
        }
        // EJB
        if(pkgTargetWrapper.hasEjbJarDef()) {
            EjbJar ejbJarDef = pkgTargetWrapper.getEjbJarDef();
            if(vehicleDef != null) {
                ejbJarDef.addFileSet(vehicleDef.getEjbElements());
                // common to all vehicles
                if (!vehicleDef.getJarElements().isEmpty()) {
                    List<TsFileSet> jarElements = vehicleDef.getJarElements();
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
            foundDescriptors.add("Ejb:\n");
            foundDescriptors.addAll(ejbJarDef.getFoundDescriptors());
            info("Ejb jar added to deployment: %s\n", ejbJarDef);
        }
        // War
        if(pkgTargetWrapper.hasWarDef()) {
            War warDef = pkgTargetWrapper.getWarDef();
            if(vehicleDef != null) {
                switch (deployment.getVehicle()) {
                    case servlet, ejbliteservlet, ejbliteservlet2, pmservlet, puservlet:
                        warDef.addFileSet(vehicleDef.getServletElements());
                        break;
                    case ejblitejsp, ejblitesecuredjsp:
                        // Add the
                        List<String> tld = List.of(deployment.getVehicle().name()+".tld");
                        TsFileSet tldFS = new TsFileSet(deployment.getTestClassPath().toString(), "WEB-INF/tlds", tld);
                        warDef.addFileSet(tldFS);
                        // Fall through to general case
                    case jsp, ejblitejsf:
                        warDef.addFileSet(vehicleDef.getJspElements());
                        break;
                }
                // common to all vehicles
                if (!vehicleDef.getJarElements().isEmpty()) {
                    List<TsFileSet> jarElements = vehicleDef.getJarElements();
                    warDef.addFileSet(jarElements);
                }
                //
                List<List<TsArchiveInfo>> pkgArchives = pkgTargetWrapper.getTargetArchives();
                if(!pkgArchives.isEmpty()) {
                    Collection<Lib> jarLibs = Utils.getJarLibs(mapping, pkgArchives);
                    for(Lib lib : jarLibs) {
                        String archiveName = lib.getFullArchiveName();
                        if(warDef.hasFile(archiveName)) {
                            warDef.addLib(lib);
                        }
                    }
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
            foundDescriptors.add("War:\n");
            foundDescriptors.addAll(warDef.getFoundDescriptors());
            info("War added to deployment: %s\n", warDef);
            info("War has content: %s\n", warDef.getWebContent());
            info("War has libs: %s\n", warDef.getLibs());
        }
        // Par
        if(pkgTargetWrapper.hasParDef()) {
            Par parDef = pkgTargetWrapper.getParDef();
            deployment.setPar(parDef);
            info("Par added to deployment: %s\n", parDef);
        }
        // Rar
        if(pkgTargetWrapper.hasRarDef()) {
            Rar rarDef = pkgTargetWrapper.getRarDef();
            deployment.setRar(rarDef);
            foundDescriptors.add("Rar:\n");
            foundDescriptors.addAll(rarDef.getFoundDescriptors());
            info("Rar added to deployment: %s\n", rarDef);
        }

        // Ear
        if(pkgTargetWrapper.hasEarDef()) {
            Ear earDef = pkgTargetWrapper.getEarDef();
            if(vehicleDef != null) {
                earDef.addFileSet(vehicleDef.getEarElements());
                // common to all vehicles
                if (!vehicleDef.getJarElements().isEmpty()) {
                    List<TsFileSet> jarElements = vehicleDef.getJarElements();
                    earDef.addFileSet(jarElements);
                }
            }
            deployment.setEar(earDef);
            foundDescriptors.add("Ear:\n");
            foundDescriptors.addAll(earDef.getFoundDescriptors());
            debug("Ear added to deployment: %s\n", earDef);
        }

        // Descriptor info and found descriptors
        String descriptors = DeploymentDescriptors.getDeploymentDescriptors(deployment.name);
        StringBuilder descriptorInfo = new StringBuilder();
        descriptorInfo.append("EE10 Deployment Descriptors:\n");
        descriptorInfo.append(descriptors);
        descriptorInfo.append("\nFound Descriptors:\n");
        descriptorInfo.append(String.join("\n", foundDescriptors));
        deployment.setDeploymentDescriptors(descriptorInfo.toString());
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
    private String capitalizeFirst(String word) {
        return word.substring(0, 1).toUpperCase()
                + word.substring(1).toLowerCase();
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
