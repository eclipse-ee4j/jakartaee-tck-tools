package tck.jakarta.platform.ant;

import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to hold information about the currently active ts.* task that is being executed by
 * ant build.xml package target.
 */
public class TsTaskInfo {
    // current ts.* task on the stack
    private Task task;
    // The current Jar task archiveName
    private String archiveName;
    // The current Jar task FileSets resources
    private List<TSFileSet> resources;

    public TsTaskInfo(Task task) {
        this.task = task;
    }
    public Task getTask() {
        return task;
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

    public String getTaskName() {
        return task.getTaskName();
    }

}
