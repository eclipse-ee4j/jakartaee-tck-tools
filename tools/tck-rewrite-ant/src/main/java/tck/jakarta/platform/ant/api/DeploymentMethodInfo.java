package tck.jakarta.platform.ant.api;

import tck.jakarta.platform.vehicles.VehicleType;

import java.util.Collections;
import java.util.List;

/**
 * An arquillian deployment method in a test client that produces a test artifact.
 */
public class DeploymentMethodInfo {
    private String name;
    private VehicleType vehicle = VehicleType.none;
    private List<String> imports = Collections.emptyList();
    private String methodCode;
    // the lower level deployment info from the ant build
    private DeploymentInfo debugInfo;

    public DeploymentMethodInfo(VehicleType vehicle, List<String> imports, String methodCode) {
        this.vehicle = vehicle;
        this.imports = imports;
        this.methodCode = methodCode;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public VehicleType getVehicle() {
        return vehicle;
    }
    public void setVehicle(VehicleType vehicle) {
        this.vehicle = vehicle;
    }

    public List<String> getImports() {
        return imports;
    }
    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public String getMethodCode() {
        return methodCode;
    }
    public void setMethodCode(String methodCode) {
        this.methodCode = methodCode;
    }

    public DeploymentInfo getDebugInfo() {
        return debugInfo;
    }

    public void setDebugInfo(DeploymentInfo debugInfo) {
        this.debugInfo = debugInfo;
    }

    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append("DeploymentMethodInfo[");
        tmp.append("vehicle=").append(vehicle);
        tmp.append("]\n");
        for (String imp : imports) {
            tmp.append("import ").append(imp).append(";\n");
        }
        tmp.append("methodCode: ----\n");
        tmp.append(methodCode);
        tmp.append("\n----");
        return tmp.toString();
    }
}
