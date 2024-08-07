package tck.jakarta.platform.ant.api;

import tck.jakarta.platform.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Information about an Arquillian/Junit 5 test client that extends the JavaTest test client. If this is a client
 * that has multiple vehicle deployments, there should be multiple test client java classes created, one per
 * vehicle deployment method.
 */
public class TestClientInfo {
    // The simple class name of the test client
    private String className;
    // The EE10 base test class that the test client extends
    private Class<?> baseTestClass;
    // The test methods that are overridden in the test client
    private List<TestMethodInfo> testMethods;
    // Junit5 tags for the test client
    private List<String> tags;


    // An optional common deployment method for an addition deployment archive
    private DeploymentMethodInfo commonDeployment;
    // The Arquillian deployment method for the test client
    private DeploymentMethodInfo testDeployment;
    // The vehicle type for the test client, if any, none otherwise
    private VehicleType vehicle;

    /**
     * Capture information about a test client that extends a base test class and overrides test methods.
     * @param className - simple class name of the test client
     * @param baseTestClass - the base test class that the test client extends
     * @param testMethods - the test methods to be overriden in the test client
     */
    public TestClientInfo(String className, Class<?> baseTestClass, List<TestMethodInfo> testMethods) {
        this.className = className;
        this.baseTestClass = baseTestClass;
        this.testMethods = testMethods;
    }
    public String getClassName() {
        return className;
    }
    public String getBaseClassName() {
        return baseTestClass.getName();
    }
    public String getPackageName() {
        return baseTestClass.getPackageName();
    }
    public List<String> getAllImports() {
        HashSet<String> allImports = new HashSet<>();
        if(commonDeployment != null) {
            // This can be empty so filter it
            for (String imp : commonDeployment.getImports()) {
                if (!imp.isEmpty()) {
                    allImports.add(imp);
                }
            }
        }
        allImports.addAll(testDeployment.getImports());
        // Make sure the base class is imported
        allImports.add(baseTestClass.getName());
        List<String> imports = new ArrayList<>(allImports);
        Collections.sort(imports);
        return imports;
    }
    public boolean getHasCommonDeployment() {
        return commonDeployment != null;
    }
    public DeploymentMethodInfo getCommonDeployment() {
        return commonDeployment;
    }
    public void setCommonDeployment(DeploymentMethodInfo commonDeployment) {
        this.commonDeployment = commonDeployment;
    }
    public String getCommonDeploymentName() {
        return commonDeployment.getName();
    }
    public DeploymentMethodInfo getTestDeployment() {
        return testDeployment;
    }
    public void setTestDeployment(DeploymentMethodInfo testDeployment) {
        this.testDeployment = testDeployment;
    }
    public String getTestDeploymentName() {
        return testDeployment.getName();
    }
    public boolean getHasVehicle() {
        return vehicle != VehicleType.none;
    }
    public VehicleType getVehicle() {
        return vehicle;
    }
    public void setVehicle(VehicleType vehicle) {
        this.vehicle = vehicle;
    }

    public List<String> getTags() {
        return tags;
    }
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<TestMethodInfo> getTestMethods() {
        return testMethods;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("------ TestClientInfo[%s]\n", className));
        sb.append("BaseTestClass: ").append(baseTestClass.getName()).append('\n');
        sb.append("TestMethods: ").append(testMethods).append('\n');
        if (commonDeployment != null) {
            sb.append("CommonDeployment: ").append(commonDeployment.getName()).append('\n');
        }
        sb.append("TestDeployment: ").append(testDeployment.getName()).append('\n');
        sb.append("Vehicle: ").append(vehicle).append('\n');
        sb.append("------ End\n");
        return sb.toString();
    }
}
