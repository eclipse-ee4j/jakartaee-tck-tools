package tck.jakarta.platform.ant;

import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Used to hold information about the currently active ts.* task that is being executed by
 * ant build.xml package target.
 */
public class TsTaskInfo extends TsBaseInfo {
    // current ts.* task on the stack
    private Task task;
    private AttributeMap componetAttributes;

    public TsTaskInfo(Task task) {
        this.task = task;
    }
    public Task getTask() {
        return task;
    }

    public String getTaskName() {
        return task.getTaskName();
    }

    public AttributeMap getComponetAttributes() {
        return componetAttributes;
    }

    public void setComponentAttributes(RuntimeConfigurable rc) {
        this.componetAttributes = new AttributeMap(task.getProject(), rc.getAttributeMap());
    }

}
