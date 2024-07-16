package tck.jakarta.platform.ant;

import java.util.ArrayList;
import java.util.List;

public class Lib {
    String archiveName;
    List<TSFileSet> resources;

    public String getArchiveName() {
        return archiveName;
    }
    public void setArchiveName(String archiveName) {
        if(archiveName.endsWith(".jar")) {
            archiveName = archiveName.substring(0, archiveName.length() - 4);
        }
        this.archiveName = archiveName;
    }

    public String getTypedArchiveName() {
        return archiveName + "_lib";
    }
    public String getFullArchiveName() {
        return archiveName + ".jar";
    }

    public String getClassFilesString() {
        return Utils.getClassFilesString(resources);
    }

    public List<TSFileSet> getResources() {
        return resources;
    }
    public void addResources(TSFileSet fs) {
        if (resources == null) {
            resources = new ArrayList<>();
        }
        resources.add(fs);
    }
    public void addResources(List<TSFileSet> fs) {
        if (resources == null) {
            resources = new ArrayList<>();
        }
        resources.addAll(fs);
    }
    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append("Lib{archiveName=%s}".formatted(archiveName));
        tmp.append("resources:\n");
        for(TSFileSet fs : resources) {
            tmp.append('\n');
            tmp.append(fs);
        }
        return tmp.toString();
    }
}
