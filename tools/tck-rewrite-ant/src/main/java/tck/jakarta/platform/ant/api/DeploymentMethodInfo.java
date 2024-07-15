package tck.jakarta.platform.ant.api;

import tck.jakarta.platform.vehicles.VehicleType;

import java.util.Collections;
import java.util.List;

public class DeploymentMethodInfo {
    private VehicleType vehicle;
    private List<String> imports;
    private String methodCode;

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
