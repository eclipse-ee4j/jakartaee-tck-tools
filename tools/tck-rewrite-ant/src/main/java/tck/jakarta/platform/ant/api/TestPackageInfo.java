package tck.jakarta.platform.ant.api;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about a TCK test package. A TestPackageInfo contains:
 * - a base test class
 * - list of test methods
 * - list of test clients, one for each unit test type
 */
public class TestPackageInfo {
    // TCK test class
    private final Class<?> baseTestClass;
    // Names of the test methods in the baseTestClass
    private List<TestMethodInfo> testMethods;
    private final EE11toEE10Mapping mapping;
    // One for a non-vehicle client, and one or more for vehicle clients
    List<TestClientInfo> testClients;
    // Generated test client java files, not created until #getTestClientFiles is called
    private final ArrayList<TestClientFile> testClientFiles = new ArrayList<>();

    public TestPackageInfo(Class<?> baseTestClass, List<TestMethodInfo> testMethods, EE11toEE10Mapping mapping) {
        this.baseTestClass = baseTestClass;
        this.testMethods = testMethods;
        this.mapping = mapping;
    }

    public Class<?> getBaseTestClass() {
        return baseTestClass;
    }
    public EE11toEE10Mapping getMapping() {
        return mapping;
    }

    public List<TestMethodInfo> getTestMethods() {
        return testMethods;
    }
    public void setTestMethods(List<TestMethodInfo> testMethods) {
        this.testMethods = testMethods;
    }

    public List<TestClientInfo> getTestClients() {
        return testClients;
    }
    public void setTestClients(List<TestClientInfo> testClients) {
        this.testClients = testClients;
    }

    public List<TestClientFile> getTestClientFiles() {
         synchronized (testClientFiles) {
             if(testClientFiles.isEmpty()) {
                 STGroup arqClientGroup = new STGroupFile("ArqClientTest.stg");
                 ST clientTemplate = arqClientGroup.getInstanceOf("/genClientTestClass");
                 for (TestClientInfo testClient : testClients) {
                     clientTemplate.remove("testClient");
                     clientTemplate.add("testClient", testClient);
                     String content = clientTemplate.render();
                     TestClientFile testClientFile = new TestClientFile(testClient.getClassName(), testClient.getPackageName(), content);
                     testClientFiles.add(testClientFile);
                 }
             }
         }
        return testClientFiles;
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
