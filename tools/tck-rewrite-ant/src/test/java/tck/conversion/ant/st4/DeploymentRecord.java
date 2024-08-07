package tck.conversion.ant.st4;

import tck.jakarta.platform.ant.Ear;
import tck.jakarta.platform.ant.Rar;
import tck.jakarta.platform.ant.War;

public class DeploymentRecord {
    String name;
    String protocol;
    String vehcile;
    War warDef;
    Rar rarDef;
    Ear earDef;


    public DeploymentRecord(String name, String protocol, String vehcile) {
        this.name = name;
        this.protocol = protocol;
        this.vehcile = vehcile;
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getVehcile() {
        return vehcile;
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

    public boolean getHasWar() {
        return warDef != null;
    }
    public War getWar() {
        return warDef;
    }
    public void setWar(War warDef) {
        this.warDef = warDef;
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
}
