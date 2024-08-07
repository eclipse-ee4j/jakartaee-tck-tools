package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

public class EjbJar extends BaseJar {
    public EjbJar(Project project, RuntimeConfigurable taskRC) {
        super(project, taskRC);
    }

    public String getType() {
        return "ejb";
    }
    public String getSunDescriptorSuffix() {
        return ".jar.sun-ejb-jar.xml";
    }
}
