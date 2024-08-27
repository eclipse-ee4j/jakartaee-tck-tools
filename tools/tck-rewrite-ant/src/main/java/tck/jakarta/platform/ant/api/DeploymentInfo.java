package tck.jakarta.platform.ant.api;

import tck.jakarta.platform.ant.ClientJar;
import tck.jakarta.platform.ant.Ear;
import tck.jakarta.platform.ant.EjbJar;
import tck.jakarta.platform.ant.Par;
import tck.jakarta.platform.ant.Rar;
import tck.jakarta.platform.ant.War;
import tck.jakarta.platform.vehicles.VehicleType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A summary of the parsed information for a given test artifact deployment. This gets passed to the
 */
public class DeploymentInfo {
    final String name;
    final String protocol;
    final VehicleType vehicle;
    Class<?> testClass;
    final String testClassPkg;
    final String testClassSimpleName;
    ClientJar clientJarDef;
    EjbJar ejbJarDef;
    List<EjbJar> ejbJarDefs = new ArrayList<>();
    War warDef;
    Rar rarDef;
    Par parDef;
    Ear earDef;
    // The EE10 test archive deployment descriptors, {@link DeploymentDescriptors}
    String deploymentDescriptors;

    public DeploymentInfo(Class<?> testClass, String name, String protocol, VehicleType vehicle) {
        this(testClass.getPackageName(), testClass.getSimpleName(), name, protocol, vehicle);
        this.testClass = testClass;
    }
    public DeploymentInfo(String testClassPkg, String testClassSimpleName, String name, String protocol, VehicleType vehicle) {
        this.testClassPkg = testClassPkg;
        this.testClassSimpleName = testClassSimpleName;
        this.testClass = null;
        this.name = name;
        this.protocol = protocol;
        this.vehicle = vehicle;
    }

    public Class<?> getTestClass() {
        return testClass;
    }
    public void setTestClass(Class<?> testClass) {
        this.testClass = testClass;
    }
    public Path getTestClassPath() {
        return Path.of(testClassPkg.replace('.', '/'));
    }
    public String getTestClassSimpleName() {
        return testClassSimpleName;
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
    public List<EjbJar> getEjbJars() {
        return ejbJarDefs;
    }
    public void setEjbJar(EjbJar ejbJarDef) {
        this.ejbJarDef = ejbJarDef;
        this.ejbJarDefs.add(ejbJarDef);
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
