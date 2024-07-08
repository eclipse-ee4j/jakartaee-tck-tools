package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

public class EjbJar extends BaseJar {
    public EjbJar(Project project, RuntimeConfigurable taskRC) {
        super(project, taskRC);
    }

    public String getType() {
        return "ejb";
    }
}
