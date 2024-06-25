package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

public class War extends BaseJar {

    public War(Project project, RuntimeConfigurable taskRC) {
        super(project, taskRC);
    }

    @Override
    public String getType() {
        return "War";
    }
}
