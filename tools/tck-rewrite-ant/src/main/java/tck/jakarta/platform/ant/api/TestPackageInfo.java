package tck.jakarta.platform.ant.api;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about a TCK test package.
 */
public class TestPackageInfo {
    // TCK test class
    private Class<?> baseTestClass;
    // Names of the test methods in the baseTestClass
    private List<String> testMethods;
    // One for a non-vehicle client, and one or more for vehicle clients
    List<TestClientInfo> testClients;

    public TestPackageInfo(Class<?> baseTestClass, List<String> testMethods) {
        this.baseTestClass = baseTestClass;
        this.testMethods = testMethods;
    }

    public Class<?> getBaseTestClass() {
        return baseTestClass;
    }

    public List<String> getTestMethods() {
        return testMethods;
    }
    public void setTestMethods(List<String> testMethods) {
        this.testMethods = testMethods;
    }

    public List<TestClientInfo> getTestClients() {
        return testClients;
    }
    public void setTestClients(List<TestClientInfo> testClients) {
        this.testClients = testClients;
    }

    public List<String> getTestClientsCode() {
        ArrayList<String> testCode = new ArrayList<>();
        STGroup arqClientGroup = new STGroupFile("ArqClientTest.stg");
        ST clientTemplate = arqClientGroup.getInstanceOf("/genClientTestClass");
        for (TestClientInfo testClient : testClients) {
            clientTemplate.remove("testClient");
            clientTemplate.add("testClient", testClient);
            testCode.add(clientTemplate.render());
        }
        return testCode;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("--- TestPackageInfo[%s]\n", baseTestClass));
        for (TestClientInfo testClient : testClients) {
            sb.append(testClient);
            sb.append('\n');
        }
        sb.append("--- End\n");
        return sb.toString();
    }
}
