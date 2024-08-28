package tck.conversion.ant.api;

import com.sun.ts.tests.jms.core.bytesMsgTopic.BytesMsgTopicTests;
import com.sun.ts.tests.ejb30.assembly.appres.appclientejb.Client;
import com.sun.ts.tests.signaturetest.javaee.JavaEESigTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tck.jakarta.platform.ant.api.DefaultEEMapping;
import tck.jakarta.platform.ant.api.DeploymentInfo;
import tck.jakarta.platform.ant.api.DeploymentMethodInfo;
import tck.jakarta.platform.ant.api.EE11toEE10Mapping;
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
import java.util.ArrayList;
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


    private List<TestMethodInfo> toExMethods(List<String> testMethods) {
        ArrayList<TestMethodInfo> exMethods = new ArrayList<>();
        for (String testMethod : testMethods) {
            exMethods.add(new TestMethodInfo(testMethod, "Exception"));
        }
        return exMethods;
    }

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
        TestPackageInfo pkgInfo = builder.buildTestPackgeInfoEx(BytesMsgTopicTests.class, testMethods, DefaultEEMapping.getInstance());
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
        List<TestMethodInfo> testMethodsEx = toExMethods(testMethods);
        List<TestClientInfo> testClientInfos = builder.buildTestClientsEx(BytesMsgTopicTests.class, testMethodsEx, DefaultEEMapping.getInstance());
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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, toExMethods(testMethods), DefaultEEMapping.getInstance());
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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, toExMethods(testMethods), DefaultEEMapping.getInstance());
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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, toExMethods(testMethods), DefaultEEMapping.getInstance());
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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, toExMethods(testMethods), DefaultEEMapping.getInstance());
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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, toExMethods(testMethods), DefaultEEMapping.getInstance());
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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, toExMethods(testMethods), DefaultEEMapping.getInstance());
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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }
    @Test
    public void testejb32_lite_timer_basic_concurrency_ejblitejsf() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("lookupTimerService", "InterruptedException, java.util.concurrent.ExecutionException"),
                new TestMethodInfo("writeLockTimeout", "")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb32.lite.timer.basic.concurrency.JsfClient.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }
    @Test
    public void testejb32_lite_timer_basic_concurrency_ejblitejsf2() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        Class<?> baseTestClass = com.sun.ts.tests.ejb32.lite.timer.basic.concurrency.JsfClient.class;
        DeploymentMethodInfo deploymentMethodInfo = builder.forTestClassAndVehicle(baseTestClass, VehicleType.ejblitejsf);
        System.out.println(deploymentMethodInfo);
        System.out.printf("War.content: %s\n", deploymentMethodInfo.getDebugInfo().getWar().getWebContent());
    }
    @Test
    public void testejb32_lite_timer_basic_concurrency_ejblitejsp() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        Class<?> baseTestClass = com.sun.ts.tests.ejb32.lite.timer.basic.concurrency.Client.class;
        DeploymentMethodInfo deploymentMethodInfo = builder.forTestClassAndVehicle(baseTestClass, VehicleType.ejblitejsp);
        System.out.println(deploymentMethodInfo);
        System.out.printf("War.content: %s\n", deploymentMethodInfo.getDebugInfo().getWar().getWebContent());
    }

    @Test
    public void testejb32_lite_timer_interceptor_lifecycle_singleton() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        Class<?> baseTestClass = com.sun.ts.tests.ejb32.lite.timer.interceptor.lifecycle.singleton.Client.class;
        DeploymentMethodInfo deploymentMethodInfo = builder.forTestClassAndVehicle(baseTestClass, VehicleType.ejbliteservlet);
        System.out.println(deploymentMethodInfo);
        System.out.printf("War.content: %s\n", deploymentMethodInfo.getDebugInfo().getWar().getWebContent());
        System.out.printf("War.libs: %s\n", deploymentMethodInfo.getDebugInfo().getWar().getLibs());

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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }
    @Test
    public void testejb32_lite_timer_basic_xa() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("persistCoffeeCreateTimerRollbackStateless", ""),
                new TestMethodInfo("persistCoffeeCreateTimerRollbackSingleton", "")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb32.lite.timer.basic.xa.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
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
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void testEjb32_relaxedclientview_stateful() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("noAnnotationTest", "com.sun.ts.tests.ejb30.common.helper.TestFailedException")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb32.relaxedclientview.stateful.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_jpa_core_callback_listener() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("prePersistTest", "Exception"),
                new TestMethodInfo("prePersistMultiTest", "Exception"),
                new TestMethodInfo("prePersistCascadeTest", "Exception")

        );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.callback.listener.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_ejb30_assembly_metainfandlibdir() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("remoteAdd", "Exception"),
                new TestMethodInfo("remoteAddByHelloEJB", "Exception"),
                new TestMethodInfo("remoteAddByHelloEJBFromAssemblyBean", "Exception"),
                new TestMethodInfo("ejbInjectionInFilterTest", "Exception"),
                new TestMethodInfo("libSubdirNotScanned", "Exception")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb30.assembly.metainfandlibdir.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_connector_xa_event() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("testConnectionEventListener", "Exception")
        );
        Class<?> baseTestClass = com.sun.ts.tests.connector.xa.event.eventClient1.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_jpa_ee_propagation_cm_extended() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("test1", "Exception"),
                new TestMethodInfo("test2", "Exception"),
                new TestMethodInfo("test3", "Exception"),
                new TestMethodInfo("test4", "Exception"),
                new TestMethodInfo("test5", "Exception"),
                new TestMethodInfo("test6", "Exception"),
                new TestMethodInfo("test7", "Exception")
        );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.ee.propagation.cm.extended.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_jpa_core_entityManager() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("persistExceptionsTest", "Exception")
        );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.entityManager.Client2.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }
    @Test
    public void test_jpa_core_basic() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
            new TestMethodInfo("updateOrderTest", "Exception"),
            new TestMethodInfo("newEntityTest", "Exception")
        );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.basic.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }
    @Test
    public void test_jpa_core_basic_puservlet() throws Exception {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.basic.Client.class;
        EE11toEE10Mapping mapping = DefaultEEMapping.getInstance();
        String ee10Pkg = mapping.getEE10TestPackageName(baseTestClass.getPackageName());
        DeploymentMethodInfo methodInfo = builder.forTestClassAndVehicle(null, ee10Pkg, "Client", VehicleType.puservlet);
        System.out.println(methodInfo);
        System.out.println(methodInfo.getDebugInfo());
    }

    @Test
    public void test_jpa_core_StoredProcedureQuery() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("executeTest", "Exception"),
                new TestMethodInfo("getOutputParameterValueIntIllegalArgumentExceptionTest", "Exception"),
            new TestMethodInfo("getFirstResultTest", "Exception")

        );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.StoredProcedureQuery.Client1.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_jpa_core_annotations_elementcollection() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("executeTest", "Exception"),
                new TestMethodInfo("getOutputParameterValueIntIllegalArgumentExceptionTest", "Exception"),
                new TestMethodInfo("getFirstResultTest", "Exception")

        );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.annotations.elementcollection.Client1.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }
    @Test
    public void test_jpa_core_entitytest_apitests() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("executeTest", "Exception"),
                new TestMethodInfo("getOutputParameterValueIntIllegalArgumentExceptionTest", "Exception"),
                new TestMethodInfo("getFirstResultTest", "Exception")

        );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.entitytest.apitests.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_ejb30_bb_asynch_singleton_annotated() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("addAway", "Exception"),
                new TestMethodInfo("voidRuntimeException", "InterruptedException, ExecutionException"),
                new TestMethodInfo("futureError", "InterruptedException, ExecutionException"),
                new TestMethodInfo("futureException", "InterruptedException, ExecutionException"),
                new TestMethodInfo("futureValueList", "InterruptedException, ExecutionException")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb30.bb.async.singleton.annotated.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }
    @Test
    public void test_ejb30_bb_session_stateful_timeout_annotated() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("defaultUnitRemote", ""),
                new TestMethodInfo("defaultUnitLocal", ""),
                new TestMethodInfo("defaultUnitNoInterface", ""),
                new TestMethodInfo("secondUnitLocal", ""),
                new TestMethodInfo("secondUnitNoInterface", ""),
                new TestMethodInfo("secondUnitRemote", "")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb30.bb.session.stateful.timeout.annotated.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_ejb30_bb_session_stateless_migration_threetwo_annotated() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("addAway", "Exception"),
                new TestMethodInfo("voidRuntimeException", "InterruptedException, ExecutionException"),
                new TestMethodInfo("futureError", "InterruptedException, ExecutionException"),
                new TestMethodInfo("futureException", "InterruptedException, ExecutionException"),
                new TestMethodInfo("futureValueList", "InterruptedException, ExecutionException")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb30.bb.session.stateless.migration.threetwo.annotated.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_ejb30_lite_packaging_war_datasource_global() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("postConstructRecords", ""),
                new TestMethodInfo("postConstructRecordsEJB", ""),
                new TestMethodInfo("getConnectionEJB", "")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb30.lite.packaging.war.datasource.global.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_jms_core_closedTopicPublisher() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("closedTopicPublisherCloseTest", "Exception"),
                new TestMethodInfo("closedTopicPublisherGetDeliveryModeTest", "Exception"),
                new TestMethodInfo("closedTopicPublisherGetDisableMessageIDTest", "Exception")
        );
        Class<?> baseTestClass = com.sun.ts.tests.jms.core.closedTopicPublisher.ClosedTopicPublisherTestsAppclientTest.class;
        DeploymentMethodInfo deploymentMethodInfo = builder.forTestClassAndVehicle(baseTestClass, VehicleType.appclient);
        DeploymentInfo deploymentInfo = deploymentMethodInfo.getDebugInfo();
        String appDescriptor = deploymentInfo.getEar().getRelativeDescriptorPath();
        System.out.printf("AppDescriptor: %s\n", appDescriptor);
        System.out.println(deploymentMethodInfo.getMethodCode());
    }

    @Test
    public void test_ejb_ee_deploy_mdb_ejbref_single() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("testStatelessInternal", "Fault"),
                new TestMethodInfo("testStatelessExternal", "Fault"),
                new TestMethodInfo("testStatefulInternal", "Fault")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb.ee.deploy.mdb.ejbref.single.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        System.out.println(packageInfo.getTestClientFiles());
    }


    @Test
    public void test_ejb_ee_bb_localaccess_sbaccesstest() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("test2", "Fault"),
                new TestMethodInfo("test4", "Fault")
        );
        Class<?> baseTestClass = com.sun.ts.tests.ejb.ee.bb.localaccess.sbaccesstest.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        DeploymentInfo deploymentInfo = packageInfo.getTestClients().get(0).getTestDeployment().getDebugInfo();
        System.out.printf("Ejbs: %s\n", deploymentInfo.getEjbJars());
        System.out.printf("Ejb1.classes: %s\n", deploymentInfo.getEjbJar().getClassFilesString());

        System.out.println("---- TestClientFiles ----");
        System.out.println(packageInfo.getTestClientFiles());
    }


    @Test
    public void test_jpa_core_types_generator() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("sequenceGeneratorOnPropertyTest", "Exception")
        );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.types.generator.Client4.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        DeploymentInfo deploymentInfo = packageInfo.getTestClients().get(0).getTestDeployment().getDebugInfo();
        System.out.printf("Ejbs: %s\n", deploymentInfo.getEjbJars());
        System.out.printf("Ejb1.classes: %s\n", deploymentInfo.getEjbJar().getClassFilesString());

        System.out.println("---- TestClientFiles ----");
        System.out.println(packageInfo.getTestClientFiles());
    }

    @Test
    public void test_jdbc_ee_batchUpdate() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("sequenceGeneratorOnPropertyTest", "Exception")
        );
        Class<?> baseTestClass = com.sun.ts.tests.jdbc.ee.batchUpdate.batchUpdateClient.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);

        DeploymentInfo deploymentInfo = packageInfo.getTestClients().get(0).getTestDeployment().getDebugInfo();
        System.out.printf("Client: %s\n", deploymentInfo.getClientJar());
        System.out.printf("Client.mainClass: %s\n", deploymentInfo.getClientJar().getMainClass());
        System.out.printf("Client.classes: %s\n", deploymentInfo.getClientJar().getClassFilesString());
        System.out.printf("Ejbs: %s\n", deploymentInfo.getEjbJars());
        System.out.printf("Ejb1.classes: %s\n", deploymentInfo.getEjbJar().getClassFilesString());

        System.out.println("---- TestClientFiles ----");
        System.out.println(packageInfo.getTestClientFiles());
    }


    @Test
    public void test_jpa_core_annotations_orderby() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("test1", "Exception")
        );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.annotations.orderby.Client1.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);

        DeploymentInfo deploymentInfo = packageInfo.getTestClients().get(0).getTestDeployment().getDebugInfo();
        System.out.printf("Client: %s\n", deploymentInfo.getClientJar());
        System.out.printf("Client.mainClass: %s\n", deploymentInfo.getClientJar().getMainClass());
        System.out.printf("Client.classes: %s\n", deploymentInfo.getClientJar().getClassFilesString());
      
        System.out.println("---- TestClientFiles ----");
        System.out.println(packageInfo.getTestClientFiles());
    }
  
    @Test
    public void test_jpa_core_entitytest_persist_oneXmany() throws IOException {
        TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
        List<TestMethodInfo> testMethods = Arrays.asList(
                new TestMethodInfo("persist1XMTest1", "Exception"),
                new TestMethodInfo("persist1XMTest3", "Exception"),
            new TestMethodInfo("persist1XMTest4", "Exception")
        );
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.entitytest.persist.oneXmany.Client.class;
        TestPackageInfo packageInfo = builder.buildTestPackgeInfoEx(baseTestClass, testMethods, DefaultEEMapping.getInstance());
        System.out.println(packageInfo);
        DeploymentInfo deploymentInfo = packageInfo.getTestClients().get(0).getTestDeployment().getDebugInfo();
        System.out.printf("Ejbs: %s\n", deploymentInfo.getEjbJars());
        System.out.printf("Ejb1.classes: %s\n", deploymentInfo.getEjbJar().getClassFilesString());

        System.out.println("---- TestClientFiles ----");
        System.out.println(packageInfo.getTestClientFiles());
    }
}
