package tck.jakarta.platform.ant;

import tck.jakarta.platform.ant.api.DefaultEEMapping;
import tck.jakarta.platform.ant.api.EE11toEE10Mapping;

import java.util.ArrayList;
import java.util.List;

public class Lib {
    String archiveName;
    List<TSFileSet> resources;
    List<String> anonymousClasses = new ArrayList<>();
    EE11toEE10Mapping mapping;

    public Lib() {
        this(new DefaultEEMapping());
    }
    public Lib(EE11toEE10Mapping mapping) {
        this.mapping = mapping;
    }
    public String getArchiveName() {
        return archiveName;
    }
    public void setArchiveName(String archiveName) {
        if(archiveName.endsWith(".jar")) {
            archiveName = archiveName.substring(0, archiveName.length() - 4);
        }
        this.archiveName = archiveName;
    }

    /**
     * Used in the code as the variable name for the archive
     * @return
     */
    public String getTypedArchiveName() {
        // archiveName can contain '-' characters, to map to '_' in the variable name
        String varName = archiveName.replace('-', '_');
        return varName + "_lib";
    }
    public String getFullArchiveName() {
        return archiveName + ".jar";
    }

    public boolean getHasClassFiles() {
        anonymousClasses.clear();
        return !Utils.getClassFilesString(mapping, resources, anonymousClasses).trim().isEmpty();
    }
    public String getClassFilesString() {
        anonymousClasses.clear();
        return Utils.getClassFilesString(mapping, resources, anonymousClasses);
    }
    public boolean getHasAnonymousClasses() {
        return !anonymousClasses.isEmpty();
    }
    public List<String> getAnonymousClasses() {
        return anonymousClasses;
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
    public boolean getHasResources() {
        return !getResourceStrings().isEmpty();
    }
    public List<String> getResourceStrings() {
        ArrayList<String> tmp = new ArrayList<>();
        for(TSFileSet fs : resources) {
            String dir = fs.dir + '/';
            for(String f : fs.includes) {
                if(!f.endsWith(".class")) {
                    f = f.replace(dir, "");
                    tmp.add(f);
                }
            }
        }
        return tmp;
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
