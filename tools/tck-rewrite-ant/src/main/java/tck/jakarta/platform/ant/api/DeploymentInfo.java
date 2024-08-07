package tck.jakarta.platform.ant.api;

import tck.jakarta.platform.ant.ClientJar;
import tck.jakarta.platform.ant.Ear;
import tck.jakarta.platform.ant.EjbJar;
import tck.jakarta.platform.ant.Par;
import tck.jakarta.platform.ant.Rar;
import tck.jakarta.platform.ant.War;
import tck.jakarta.platform.vehicles.VehicleType;

/**
 * A summary of the parsed information for a given test artifact deployment. This gets passed to the
 */
public class DeploymentInfo {
    final String name;
    final String protocol;
    final VehicleType vehicle;
    Class<?> testClass;
    ClientJar clientJarDef;
    EjbJar ejbJarDef;
    War warDef;
    Rar rarDef;
    Par parDef;
    Ear earDef;
    // The EE10 test archive deployment descriptors, {@link DeploymentDescriptors}
    String deploymentDescriptors;

    public DeploymentInfo(Class<?> testClass, String name, String protocol, VehicleType vehicle) {
        this.testClass = testClass;
        this.name = name;
        this.protocol = protocol;
        this.vehicle = vehicle;
    }

    public Class<?> getTestClass() {
        return testClass;
    }
    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    public VehicleType getVehicle() {
        return vehicle;
    }

    public boolean getHasWar() {
        return warDef != null;
    }
    public War getWar() {
        return warDef;
    }

    public void setWar(War warDef) {
        this.warDef = warDef;
    }

    public boolean getHasPar() {
        return parDef != null;
    }
    public Par getPar() {
        return parDef;
    }
    public void setPar(Par parDef) {
        this.parDef = parDef;
    }

    public boolean getHasRar() {
        return rarDef != null;
    }
    public Rar getRar() {
        return rarDef;
    }
    public void setRar(Rar rarDef) {
        this.rarDef = rarDef;
    }

    public boolean getHasEar() {
        return earDef != null;
    }
    public Ear getEar() {
        return earDef;
    }

    public void setEar(Ear earDef) {
        this.earDef = earDef;
    }

    public boolean getHasClientJar() {
        return clientJarDef != null;
    }
    public ClientJar getClientJar() {
        return clientJarDef;
    }

    public void setClientJar(ClientJar clientJarDef) {
        this.clientJarDef = clientJarDef;
    }

    public boolean getHasEjbJar() {
        return ejbJarDef != null;
    }
    public EjbJar getEjbJar() {
        return ejbJarDef;
    }

    public void setEjbJar(EjbJar ejbJarDef) {
        this.ejbJarDef = ejbJarDef;
    }

    public boolean getHasDeploymentDescriptors() {
        return deploymentDescriptors != null && !deploymentDescriptors.isEmpty();
    }
    public String getDeploymentDescriptors() {
        return deploymentDescriptors;
    }
    public void setDeploymentDescriptors(String deploymentDescriptors) {
        this.deploymentDescriptors = deploymentDescriptors;
    }

    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append(String.format("DeploymentInfo [name=%s, protocol=%s, vehicle=%s, testClass=%s]\n", name, protocol, vehicle, testClass));
        tmp.append(clientJarDef == null ? "No client" : clientJarDef.toString());
        tmp.append('\n');
        tmp.append(ejbJarDef == null ? "No ejb" : ejbJarDef.toString());
        tmp.append('\n');
        tmp.append(rarDef == null ? "No rar" : rarDef.toString());
        tmp.append('\n');
        tmp.append(warDef == null ? "No war" : warDef.toString());
        tmp.append('\n');
        tmp.append(earDef == null ? "No ear" : earDef.toString());
        tmp.append('\n');
        return tmp.toString();
    }
}
