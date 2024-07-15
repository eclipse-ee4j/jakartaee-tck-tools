package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

import java.util.Hashtable;

/**
 * ts.clientar representation
 */
public class ClientJar extends BaseJar {
    // main-class MANIFEST entry for the appclient archive
    String mainClass;

    public ClientJar(Project project, RuntimeConfigurable taskRC) {
        super(project, taskRC);
        Hashtable<String,Object> attrs = taskRC.getAttributeMap();
        Object mclass = attrs.get("mainclass");
        if(mclass != null) {
            this.mainClass = mclass.toString();
        }
    }

    public String getType() {
        return "client";
    }
    public String getMainClass() {
        return mainClass;
    }
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }
}
