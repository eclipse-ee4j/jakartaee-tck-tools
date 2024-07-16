package tck.jakarta.platform.ant;

import org.apache.tools.ant.Target;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to hold information about the currently active package target dependencies that is being executed by
 * ant build.xml package target.
 */
public class TsPackageInfo {
    Target target;
    // The current Jar task archiveName
    private String archiveName;
    // The current Jar task FileSets resources
    private List<TSFileSet> resources;
    private List<TsTaskInfo> tsTaskInfos;

    public TsPackageInfo(Target target) {
        this.target = target;
    }

    public String getTargeName() {
        return target.getName();
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public List<TSFileSet> getResources() {
        return resources;
    }

    public void setResources(List<TSFileSet> resources) {
        this.resources = resources;
    }
    public void addResources(ArrayList<TSFileSet> fileSets) {
        if (resources == null) {
            resources = new ArrayList<>();
        }
        resources.addAll(fileSets);
    }

    public List<TsTaskInfo> getTsTaskInfos() {
        return tsTaskInfos;
    }

    public void addTaskInfo(TsTaskInfo tsTaskInfo) {
        if (tsTaskInfos == null) {
            tsTaskInfos = new ArrayList<>();
        }
        tsTaskInfos.add(tsTaskInfo);
    }

    public boolean hasTsVehicles() {
        boolean hasTsVehicles = false;
        if (tsTaskInfos != null) {
            for (TsTaskInfo tsTaskInfo : tsTaskInfos) {
                if (tsTaskInfo.getTaskName().equals("ts.vehicle")) {
                    hasTsVehicles = true;
                    break;
                }
            }
        }
        return hasTsVehicles;
    }
}
