package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

public class Par extends BaseJar {

    public Par(Project project, RuntimeConfigurable taskRC) {
        super(project, taskRC);
    }

    @Override
    public String getType() {
        return "par";
    }

    /**
     * PARs are just jars and the EE10 TCK does not add a suffix to the archive name
     * @return
     */
    @Override
    public String getTypedArchiveName() {
        return archiveName;
    }
}
