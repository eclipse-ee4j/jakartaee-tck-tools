package tck.conversion.ant.api;

import com.sun.ts.tests.jms.core.bytesMsgTopic.BytesMsgTopicTests;
import com.sun.ts.tests.ejb30.assembly.appres.appclientejb.Client;
import com.sun.ts.tests.signaturetest.javaee.JavaEESigTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tck.jakarta.platform.ant.api.DeploymentMethodInfo;
import tck.jakarta.platform.ant.api.TestClientFile;
import tck.jakarta.platform.ant.api.TestClientInfo;
import tck.jakarta.platform.ant.api.TestMethodInfo;
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
        List<TestMethodInfo> testMethods = Arrays.asList(new TestMethodInfo("bytesMsgNullStreamTopicTest", "Exception"),
                new TestMethodInfo("bytesMessageTopicTestsFullMsg", "Exception"),
                new TestMethodInfo("bytesMessageTNotWriteable", "Exception"));
        TestPackageInfo pkgInfo = builder.buildTestPackgeInfoEx(BytesMsgTopicTests.class, testMethods);
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
     * src/com/sun/ts/tests/ejb30/misc/sameejbclass/build.xml
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

    /**
     * src/com/sun/ts/tests/ejb30/zombie/build.xml
     * @throws IOException
     */
    @Test
    public void testEjb30Zombie_ClientTest() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<String> testMethods = Arrays.asList("test1");
        Class<?> baseTestClass = com.sun.ts.tests.ejb30.zombie.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfo(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    /**
     * com/sun/ts/tests/ejb30/assembly/librarydirectory/custom/build.xml
     *
     * [starksm@scottryzen wildflytck-new]$ ls jakartaeetck/dist/com/sun/ts/tests/ejb30/assembly/librarydirectory/custom/
     * ejb3_assembly_librarydirectory_custom_client.jar
     * ejb3_assembly_librarydirectory_custom_client.jar.jboss-client.xml
     * ejb3_assembly_librarydirectory_custom_client.jar.sun-application-client.xml
     * ejb3_assembly_librarydirectory_custom.ear
     * ejb3_assembly_librarydirectory_custom_ejb.jar
     * ejb3_assembly_librarydirectory_custom_ejb.jar.jboss-ejb3.xml
     * ejb3_assembly_librarydirectory_custom_ejb.jar.jboss-webservices.xml
     * ejb3_assembly_librarydirectory_custom_ejb.jar.sun-ejb-jar.xml
     * hello-client-view.jar
     * lib-shared.jar
     * second-level-jar.jar
     * shared.jar

     * [starksm@scottryzen wildflytck-new]$ jar -tf jakartaeetck/dist/com/sun/ts/tests/ejb30/assembly/librarydirectory/custom/ejb3_assembly_librarydirectory_custom.ear
     * META-INF/MANIFEST.MF
     * 1/2/3/hello-client-view.jar
     * 1/2/3/shared.jar
     * lib/lib-shared.jar
     * ejb3_assembly_librarydirectory_custom_client.jar
     * ejb3_assembly_librarydirectory_custom_ejb.jar
     * 1/2/3/4/second-level-jar.jar
     * 1/2/3/second-level-dir/com/sun/ts/tests/ejb30/assembly/librarydirectory/custom/second-level-dir.txt
     * META-INF/application.xml
     * @throws IOException
     */
    @Test
    public void testEjb30LibDirCustom_ClientTest() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<String> testMethods = Arrays.asList("libDirNotUsed", "libDirNotUsedEJB", "secondLevelJar",
                "secondLevelJarEJB", "secondLevelDir", "secondLevelDirEJB", "postConstructInvokedInSuperElseWhere",
                "remoteAdd", "remoteAddByHelloEJB", "remoteAddByHelloEJBFromAssemblyBean");
        Class<?> baseTestClass = com.sun.ts.tests.ejb30.assembly.librarydirectory.custom.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfo(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void testEjb_tx_session_stateless_bm_Tx_Multi_ClientTest() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("test1", "com.sun.ts.lib.harness.EETest.Fault"),
                new TestMethodInfo("test2", "com.sun.ts.lib.harness.EETest.Fault"),
                new TestMethodInfo("test4", "com.sun.ts.lib.harness.EETest.Fault"),
                new TestMethodInfo("test5", "com.sun.ts.lib.harness.EETest.Fault")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb.ee.tx.session.stateless.bm.Tx_Multi.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void testConnector_localTx_msginflow_ClientTest() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("testReceiveMessage", "Exception"),
                new TestMethodInfo("testProxyInterfaceImp", "Exception"),
                new TestMethodInfo("testUniqueMessageEndpoint", "Exception"),
                new TestMethodInfo("testMessageEndpointFactoryForEquals", "Exception"),
                new TestMethodInfo("testUniqueMessageEndpointFactory", "Exception"),
                new TestMethodInfo("testEndpointActivationName", "Exception"),
                new TestMethodInfo("testGetEndpoinClass", "Exception"),
                new TestMethodInfo("testMessageDeliveryTransacted", "Exception"),
                new TestMethodInfo("testMessageDeliveryNonTransacted", "Exception"),
                new TestMethodInfo("testMessageDeliveryTransactedUsingXid", "Exception"),
                new TestMethodInfo("testActivationSpeccalledOnce", "Exception"),
                new TestMethodInfo("testEJBExceptionNotSupported", "Exception"),
                new TestMethodInfo("testEJBExceptionRequired", "Exception"),
                new TestMethodInfo("testAppExceptionNotSupported", "Exception"),
                new TestMethodInfo("testAppExceptionRequired", "Exception"),
                new TestMethodInfo("testSICMsgPrincipal", "Exception"),
                new TestMethodInfo("testIBAnnoMsgTransactedUsingXid", "Exception"),
                new TestMethodInfo("testActivationSpecImplRAA", "Exception"),
                new TestMethodInfo("testIBAnnoASConfigProp", "Exception"),
                new TestMethodInfo("testContextSetupCompleted", "Exception")
        );
        Class<?> baseTestClass = com.sun.ts.tests.connector.localTx.msginflow.MDBClient.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    /**
     * For example, I see this in the EE10 dist:
     * starksm@Scotts-Mac-Studio jakartaeetck % ls src/com/sun/ts/tests/jpa/ee/entityManager
     * Client.java Order.java build.xml
     *
     * and in the master platform-tck it is now:
     * jpa/platform-tests/src/main/java/ee/jakarta/tck/persistence/ee/entityManager/Client.java
     */
    @Test
    public void testJpa() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<String> testMethods = Arrays.asList("createEntityManagerSynchronizationTypeMapTest",
                "createEntityManagerSynchronizationTypeTest",
                "joinTransactionTransactionRequiredExceptionTest"
                );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.ee.entityManager.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfo(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void testejb32_lite_timer_basic_concurrency() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("lookupTimerService", "InterruptedException, java.util.concurrent.ExecutionException"),
                new TestMethodInfo("writeLockTimeout", "")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb32.lite.timer.basic.concurrency.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }
    @Test
    public void testejb32_lite_timer_basic_sharing() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("createTimerRollbackStateless", ""),
                new TestMethodInfo("createTimerRollbackSingleton", ""),
                new TestMethodInfo("createVerifyRecurringTimerStateless", ""),
                new TestMethodInfo("createVerifyRecurringTimerSingleton", ""),
                new TestMethodInfo("accessTimersStateless", ""),
                new TestMethodInfo("accessTimersSingleton", "")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb32.lite.timer.basic.sharing.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void testEjb32_relaxedclientview_singleton() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("noAnnotationTest", "com.sun.ts.tests.ejb30.common.helper.TestFailedException")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb32.relaxedclientview.singleton.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods);
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }
}
