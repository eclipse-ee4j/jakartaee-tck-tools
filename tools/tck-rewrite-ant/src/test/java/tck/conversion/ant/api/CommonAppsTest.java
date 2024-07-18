package tck.conversion.ant.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tck.jakarta.platform.ant.api.CommonApps;
import tck.jakarta.platform.ant.api.DeploymentMethodInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@EnabledIfSystemProperty(named = "ts.home", matches = ".*")
public class CommonAppsTest {
    static Path tsHome = Paths.get(System.getProperty("ts.home"));

    @Test
    public void validateCommonApps() throws IOException {
        CommonApps commonApps = CommonApps.getInstance(tsHome);
        Path testPath = tsHome.resolve("src/com/sun/ts/tests/ejb30/assembly/initorder/warejb/build.xml");
        DeploymentMethodInfo methodInfo = commonApps.getCommonDeployment(testPath);
        System.out.println(methodInfo);
    }
}
