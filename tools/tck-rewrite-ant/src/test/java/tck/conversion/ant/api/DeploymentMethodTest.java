package tck.conversion.ant.api;

import com.sun.ts.tests.jms.core.bytesMsgTopic.BytesMsgTopicTests;
import com.sun.ts.tests.ejb30.assembly.appres.appclientejb.Client;
import com.sun.ts.tests.signaturetest.javaee.JavaEESigTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tck.jakarta.platform.ant.api.DeploymentMethodInfo;
import tck.jakarta.platform.ant.api.TestClientFile;
import tck.jakarta.platform.ant.api.TestClientInfo;
import tck.jakarta.platform.ant.api.TestPackageInfo;
import tck.jakarta.platform.ant.api.TestPackageInfoBuilder;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

/**
 * These tests only run if there is ts.home property set. The ts.home property needs to be set to an EE10 TCK
 * distribution and a glassfish7 distribution unbundled as a peer directory:
 * workingdir/glassfish7
 * workingdir/jakartaeetck
 *
 * They also require the EE 11 TCK tests, so they need to be run with the dependencies defined in the
 * deploymentmethod-tests profile.
 */
@EnabledIfSystemProperty(named = "ts.home", matches = ".*")
public class DeploymentMethodTest {
    static Path tsHome = Paths.get(System.getProperty("ts.home"));

    @Test
    public void testBytesMsgTopicTest_deployAppclientMethod() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        DeploymentMethodInfo deployMethod = builder.forTestClassAndVehicle(BytesMsgTopicTests.class, VehicleType.appclient);
        System.out.printf("--- DeployMethod(%s):\n%s\n---\n", deployMethod.getVehicle(), deployMethod.getMethodCode());
    }
    @Test
    public void testBytesMsgTopicTest_writeTestClasses() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<String> testMethods = Arrays.asList("bytesMsgNullStreamTopicTest", "bytesMessageTopicTestsFullMsg", "bytesMessageTNotWriteable");
        TestPackageInfo pkgInfo = builder.buildTestPackgeInfo(BytesMsgTopicTests.class, testMethods);
        System.out.println(pkgInfo);

        System.out.println("TestClasses:");
        // The test module src/main/java directory
        Path srcDir = Paths.get("/tmp");
        for (TestClientFile testClient : pkgInfo.getTestClientFiles()) {
            // The test package dir under the test module src/main/java directory
            Path testPkgDir = srcDir.resolve(testClient.getPackage().replace(".", "/"));
            Files.createDirectories(testPkgDir);
            // The test client .java file
            Path tetClientJavaFile = testPkgDir.resolve(testClient.getName() + ".java");
            // Write out the test client .java file content
            Files.writeString(tetClientJavaFile, testClient.getContent(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        }
    }

    @Test
    public void testBytesMsgTopicTest_deployEjbMethod() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        DeploymentMethodInfo deployMethod = builder.forTestClassAndVehicle(BytesMsgTopicTests.class, VehicleType.ejb);
        System.out.printf("--- DeployMethod(%s):\n%s\n---\n", deployMethod.getVehicle(), deployMethod.getMethodCode());
    }

    @Test
    public void testBytesMsgTopicTest_deployMethods() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<String> testMethods = Arrays.asList("bytesMsgNullStreamTopicTest", "bytesMessageTopicTestsFullMsg", "bytesMessageTNotWriteable");
        List<TestClientInfo> testClientInfos = builder.buildTestClients(BytesMsgTopicTests.class, testMethods);
        for (TestClientInfo clientInfo : testClientInfos) {
            System.out.println(clientInfo);
        }
    }

    @Test
    public void testJavaEESigTest_deployAppclientMethod() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        DeploymentMethodInfo deployMethod = builder.forTestClassAndVehicle(JavaEESigTest.class, VehicleType.appclient);
        for(String imp : deployMethod.getImports()) {
            System.out.printf("import %s;\n", imp);
        }
        System.out.printf("--- DeployMethod(%s):\n%s\n---\n", deployMethod.getVehicle(), deployMethod.getMethodCode());
    }

    @Test
    public void testJavaEESigTest_deployServletMethod() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        DeploymentMethodInfo deployMethod = builder.forTestClassAndVehicle(JavaEESigTest.class, VehicleType.servlet);
        System.out.println(deployMethod);
    }

    @Test
    public void testJavaEESigTest_deployJspMethod() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        DeploymentMethodInfo deployMethod = builder.forTestClassAndVehicle(JavaEESigTest.class, VehicleType.jsp);
        System.out.println(deployMethod);
    }

    @Test
    public void testJavaEESigTest_deployEjbMethod() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        DeploymentMethodInfo deployMethod = builder.forTestClassAndVehicle(JavaEESigTest.class, VehicleType.ejb);
        System.out.println(deployMethod);
    }

    /**
     * TODO
     * A test with no vehicles com.sun.ts.tests.ejb30.assembly.appres.appclientejb.Client
     * this has ear lib with persistence.xml
     * com/sun/ts/tests/ejb30/assembly/appres/appclientejb/build.xml
     * @throws IOException
     */
    @Test
    public void testClient_deployNoneMethod() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        DeploymentMethodInfo deployMethod = builder.forTestClassAndVehicle(Client.class, VehicleType.none);
        System.out.println(deployMethod);
    }

    /**
     * TODO
     * com.sun.ts.tests.ejb30.assembly.initorder.appclientejb.Client has two ejb jars
     * @throws IOException
     */
    @Test
    public void testClient2EjbJars_deployNoneMethod() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        DeploymentMethodInfo deployMethod = builder.forTestClassAndVehicle(com.sun.ts.tests.ejb30.assembly.initorder.appclientejb.Client.class, VehicleType.none);
        System.out.println(deployMethod);
    }

    /**
     * app.name="ejb3_assembly_initorder_warejb"
     * [starksm@scottryzen jakartaeetck]$ ls dist/com/sun/ts/tests/ejb30/assembly/initorder/warejb/
     * ejb3_assembly_initorder_warejb.ear
     * ejb3_assembly_initorder_warejb_ejb.jar
     * ejb3_assembly_initorder_warejb_ejb.jar.jboss-ejb3.xml
     * ejb3_assembly_initorder_warejb_ejb.jar.jboss-webservices.xml
     * ejb3_assembly_initorder_warejb_ejb.jar.sun-ejb-jar.xml
     * ejb3_assembly_initorder_warejb_web.war
     * shared.jar
     * @throws IOException
     */
    @Test
    public void testClientInitOrderWarejb_deployNoneMethod() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        DeploymentMethodInfo deployMethod = builder.forTestClassAndVehicle(com.sun.ts.tests.ejb30.assembly.initorder.warejb.Client.class, VehicleType.none);
        System.out.println(deployMethod);
    }

    /**
     * The full com/sun/ts/tests/ejb30/assembly/initorder/warejb/ test class which includes a common deployment
     * @throws IOException
     */
    @Test
    public void testClientInitOrderWarejb_ClientTest() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<String> testMethods = Arrays.asList("initOrder", "appName");
        Class<?> baseTestClass = com.sun.ts.tests.ejb30.assembly.initorder.warejb.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfo(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    /**
     * The full com/sun/ts/tests/ejb32/mdb/modernconnector test class which includes a rar deployment built
     * in a pre.package dependency
     * @throws IOException
     */
    @Test
    public void testEjb32MdbModernconnector_ClientTest() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<String> testMethods = Arrays.asList("testModernConnector");
        Class<?> baseTestClass = com.sun.ts.tests.ejb32.mdb.modernconnector.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfo(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    /**
     * The full com/sun/ts/tests/ejb32/mdb/modernconnector test class which includes a rar deployment built
     * in a pre.package dependency
     * @throws IOException
     */
    @Test
    public void testEjb30MiscSameEjbClass_ClientTest() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<String> testMethods = Arrays.asList("checkEnvEntry", "testDTO");
        Class<?> baseTestClass = com.sun.ts.tests.ejb30.misc.sameejbclass.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfo(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }
}
