package tck.jakarta.platform.ant;

import org.apache.tools.ant.RuntimeConfigurable;

import java.util.Hashtable;

public class Ear extends BaseJar {
    boolean deletecomponentarchives;

    public Ear(RuntimeConfigurable taskRC) {
        super(taskRC);
        Hashtable<String,Object> attrs = taskRC.getAttributeMap();
        Object flag = attrs.get("deletecomponentarchives");
        if(flag != null) {
            this.deletecomponentarchives = Boolean.parseBoolean(flag.toString());
        }

    }

    public String getType() {
        return "EAR";
    }
    public boolean isDeleteComponentArchives() {
        return deletecomponentarchives;
    }
}
