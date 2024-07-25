package tck.conversion.ant.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tck.jakarta.platform.ant.api.CommonApps;
import tck.jakarta.platform.ant.api.DeploymentMethodInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Tests that validate if a test package has the expected common app
 */
@EnabledIfSystemProperty(named = "ts.home", matches = ".*")
public class CommonAppsTest {
    static Path tsHome = Paths.get(System.getProperty("ts.home"));


    // +++ Begin insert new common app template tests here

    @Test
    public void test_replace_me() {

    }

    /**
     * #tx session tests' common apps
     * commonarchives.com/sun/ts/tests/ejb/ee/tx/session=com/sun/ts/tests/ejb/ee/tx/txbean
     */
    @Test
    public void test_mdb() throws IOException {
        CommonApps commonApps = CommonApps.getInstance(tsHome);
        Path testPath = tsHome.resolve("src/com/sun/ts/tests/connector/localTx/msginflow/build.xml");
        DeploymentMethodInfo methodInfo = commonApps.getCommonDeployment(testPath, "ClientTest");
        Assertions.assertNotNull(methodInfo, "com/sun/ts/tests/connector/localTx/msginflow/build.xml has common app");
        String methodCode = methodInfo.getMethodCode();
        int index = methodCode.indexOf("@Deployment(name = \"msginflow_mdb\"");
        Assertions.assertTrue(index >= 0, "method contains expected name");
        System.out.println(methodInfo);
    }

    /**
     * #tx session tests' common apps
     * commonarchives.com/sun/ts/tests/ejb/ee/tx/session=com/sun/ts/tests/ejb/ee/tx/txbean
     */
    @Test
    public void test_txbean() throws IOException {
        CommonApps commonApps = CommonApps.getInstance(tsHome);
        Path testPath = tsHome.resolve("src/com/sun/ts/tests/ejb/ee/tx/session/stateless/bm/Tx_Multi/build.xml");
        DeploymentMethodInfo methodInfo = commonApps.getCommonDeployment(testPath, "ClientTest");
        Assertions.assertNotNull(methodInfo, "com/sun/ts/tests/ejb/ee/tx/session/stateless/bm/Tx_Multi/build.xml has common app");
        String methodCode = methodInfo.getMethodCode();
        int index = methodCode.indexOf("@Deployment(name = \"ejb_tx_txbean\"");
        Assertions.assertTrue(index >= 0, "method contains expected name");
        System.out.println(methodInfo);
    }

    /**
     * Validate the get_ejb3_common_helloejbjar_standalone_component common app
     * test packages under these path should have this common app:
     * com/sun/ts/tests/ejb30/assembly
     * com/sun/ts/tests/ejb30/misc/jndi/earjar
     * @throws IOException
     */
    @Test
    public void validate_helloejbjar() throws IOException {
        CommonApps commonApps = CommonApps.getInstance(tsHome);
        Path testPath = tsHome.resolve("src/com/sun/ts/tests/ejb30/assembly/initorder/warejb/build.xml");
        DeploymentMethodInfo methodInfo = commonApps.getCommonDeployment(testPath, "ClientTest");
        Assertions.assertNotNull(methodInfo, "com/sun/ts/tests/ejb30/assembly/initorder/warejb/build.xml has common app");
        String methodCode = methodInfo.getMethodCode();
        int index = methodCode.indexOf("@Deployment(name = \"ejb3_common_helloejbjar_standalone_component\"");
        Assertions.assertTrue(index >= 0, "method contains expected name");
        System.out.println(methodInfo);
    }

    // End

    // Other internal tests
    @Test
    public void validate_no_common_app() throws IOException {
        CommonApps commonApps = CommonApps.getInstance(tsHome);
        Path testPath = tsHome.resolve("src/com/sun/ts/tests/jms/core/appclient/closedQueueSession/ClosedQueueSessionTestsIT.java");
        DeploymentMethodInfo methodInfo = commonApps.getCommonDeployment(testPath, "ClientTest");
        Assertions.assertNull(methodInfo, "com/sun/ts/tests/jms/* has no common app");
        System.out.println(methodInfo);
    }
    @Test
    public void validate_two_common_app_archives() throws IOException {
        CommonApps commonApps = CommonApps.getInstance(tsHome);
        Path testPath = tsHome.resolve("src/main/java/com/sun/ts/tests/ejb/ee/tx/session/stateless/cm/Tx_SetRollbackOnly/Client.java");
        DeploymentMethodInfo methodInfo = commonApps.getCommonDeployment(testPath, "ClientTest");
        System.out.println(methodInfo);
    }

    @Test
    public void validateCommonAppsMap() throws IOException {
        Map<Path, CommonApps.CommonAppInfo> infoMap = CommonApps.getCommonAppInfos(tsHome);
        for (Map.Entry<Path, CommonApps.CommonAppInfo> entry : infoMap.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

    }
}
