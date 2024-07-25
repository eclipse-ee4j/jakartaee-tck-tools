package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

public class Rar extends BaseJar {
    private Lib rarLib;

    public Rar() {
        this(null, null);
    }
    public Rar(Project project) {
        this(project, null);
    }
    public Rar(Project project, RuntimeConfigurable taskRC) {
        super(project, taskRC);
        if(archiveSuffix == null) {
            setArchiveSuffix(".rar");
        }
    }

    @Override
    public String getType() {
        return "ra";
    }

    /**
     * Return the lib classes
     * @return
     */
    @Override
    public String getClassFilesString() {
        anonymousClasses.clear();
        return Utils.getClassFilesString(rarLib.resources, anonymousClasses);
    }

    public Lib getRarLib() {
        return rarLib;
    }

    public void setRarLib(Lib rarLib) {
        this.rarLib = rarLib;
    }
}
