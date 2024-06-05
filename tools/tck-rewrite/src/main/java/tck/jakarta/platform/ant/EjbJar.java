package tck.jakarta.platform.ant;

import org.apache.tools.ant.RuntimeConfigurable;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;

public class EjbJar extends BaseJar {
    public EjbJar(RuntimeConfigurable taskRC) {
        super(taskRC);
    }

    public String getType() {
        return "EjbJar";
    }
}
