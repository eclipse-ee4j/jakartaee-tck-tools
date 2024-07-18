package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class Ear extends BaseJar {
    boolean deletecomponentarchives;
    TSFileSet modules;
    List<Lib> libs = new ArrayList<>();

    public Ear(Project project, RuntimeConfigurable taskRC) {
        super(project, taskRC);
        Hashtable<String,Object> attrs = taskRC.getAttributeMap();
        Object flag = attrs.get("deletecomponentarchives");
        if(flag != null) {
            this.deletecomponentarchives = Boolean.parseBoolean(flag.toString());
        }

    }

    public String getType() {
        return "ear";
    }
    public boolean isDeleteComponentArchives() {
        return deletecomponentarchives;
    }

    public void addJarResources(TsPackageInfo pkgInfo) {
        Lib lib = new Lib();
        lib.setArchiveName(pkgInfo.getArchiveName());
        lib.addResources(pkgInfo.getResources());
        libs.add(lib);
    }
    public List<Lib> getLibs() {
        return libs;
    }

    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append("%s{descriptor=%s, descriptorDir=%s, archiveName=%s, archivesuffix=%s, fullArchiveName=%s, excludedFiles=%s}".formatted(getType(),
                descriptor, descriptordir, archiveName, archiveSuffix, getFullArchiveName(), excludedFiles));
        tmp.append("module files:\n");
        for(TSFileSet fs : fileSets) {
            tmp.append('\n');
            tmp.append(fs);
        }
        tmp.append("lib files:\n");
        for (Lib lib : libs) {
            tmp.append('\n');
            tmp.append(lib);
        }
        return tmp.toString();

    }
}
