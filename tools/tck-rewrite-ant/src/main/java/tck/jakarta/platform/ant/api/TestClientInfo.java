package tck.jakarta.platform.ant.api;

import tck.jakarta.platform.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.List;

/**
 * Information about an Arquillian/Junit 5 test client that extends the JavaTest test client. If this is a client
 * that has multiple vehicle deployments, there should be multiple test client java classes created, one per
 * vehicle deployment method.
 */
public class TestClientInfo {
    private String className;
    private Class<?> baseTestClass;
    private List<String> testMethods;

    private DeploymentMethodInfo commonDeployment;
    private DeploymentMethodInfo testDeployment;
    private VehicleType vehicle;

    public TestClientInfo(String className, Class<?> baseTestClass, List<String> testMethods) {
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
        ArrayList<String> allImports = new ArrayList<>();
        if(commonDeployment != null) {
            allImports.addAll(commonDeployment.getImports());
        }
        allImports.addAll(testDeployment.getImports());
        // Make sure the base class is imported
        allImports.add(baseTestClass.getName());
        allImports.add("org.junit.jupiter.api.Test");
        return allImports;
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
    public boolean getHasVehicle() {
        return vehicle != VehicleType.none;
    }
    public VehicleType getVehicle() {
        return vehicle;
    }
    public void setVehicle(VehicleType vehicle) {
        this.vehicle = vehicle;
    }

    public List<String> getTestMethods() {
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
