package tck.jakarta.platform.ant;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Task;

/**
 * An ant BuildListener used to capture the ts.* task information when executing the package target
 * of the TCK build.xml
 */
public class TsTaskListener implements BuildListener {
    private PackageTarget packageTarget;
    public TsTaskListener(PackageTarget packageTarget) {
        this.packageTarget = packageTarget;
    }
    @Override
    public void buildStarted(BuildEvent event) {
    }

    @Override
    public void buildFinished(BuildEvent event) {

    }

    @Override
    public void targetStarted(BuildEvent event) {

    }

    @Override
    public void targetFinished(BuildEvent event) {

    }

    @Override
    public void taskStarted(BuildEvent event) {

    }

    @Override
    public void taskFinished(BuildEvent event) {
        Task task = event.getTask();
        String name = task.getTaskName();
        if(name.startsWith("ts.")) {
            if (name.equals("ts.verbose")) {
                System.out.printf("ts.verbose: %s\n", task.getRuntimeConfigurableWrapper().getAttributeMap().get("message"));
            } else {
                boolean added = packageTarget.addTask(task);
                if (!added) {
                    System.out.printf("Unhandled task: %s\n", name);
                }
            }
        }
    }

    @Override
    public void messageLogged(BuildEvent event) {

    }
}
