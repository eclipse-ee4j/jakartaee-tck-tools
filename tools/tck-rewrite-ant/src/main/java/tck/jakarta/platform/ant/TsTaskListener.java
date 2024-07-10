package tck.jakarta.platform.ant;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * An ant BuildListener used to capture the ts.* task information when executing the package target
 * of the TCK build.xml
 */
public class TsTaskListener implements BuildListener {
    private PackageTarget packageTarget;
    private LinkedList<TsTaskInfo> tsTaskStack = new LinkedList<>();

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
        Target target = event.getTarget();
        System.out.printf("--- targetFinished %s\n", target.getName(), target.getLocation());

    }

    @Override
    public void taskStarted(BuildEvent event) {
        Task task = event.getTask();
        String name = task.getTaskName();
        if(name.startsWith("ts.") && !name.startsWith("ts.verbose")) {
            System.out.printf("+++ Started %s: %s\n", task.getTaskName(), task.getRuntimeConfigurableWrapper().getAttributeMap());
            tsTaskStack.push(new TsTaskInfo(task));
        }
    }

    @Override
    public void taskFinished(BuildEvent event) {
        Task task = event.getTask();
        String name = task.getTaskName();
        if(name.startsWith("ts.")) {
            if (name.equals("ts.verbose")) {
                String msg = task.getRuntimeConfigurableWrapper().getAttributeMap().get("message").toString();
                Project project = event.getProject();
                PropertyHelper helper = PropertyHelper.getPropertyHelper(project);
                msg = helper.parseProperties(msg).toString();

                System.out.printf("ts.verbose: %s\n", msg);
            } else {
                TsTaskInfo taskInfo = tsTaskStack.pop();
                boolean isTsVehicles = name.equals("ts.vehicles");
                System.out.printf("+++ Finished %s: %s\n", task.getTaskName(), task.getRuntimeConfigurableWrapper().getAttributeMap());
                if(event.getMessage() != null) {
                    System.out.printf("\t%s\n", event.getMessage());
                }
                if(event.getException() != null) {
                    System.out.printf("\t%s\n", event.getException());
                }
                BaseJar taskJar = packageTarget.addTask(task);
                if (taskJar == null && !isTsVehicles) {
                    System.out.printf("Unhandled task: %s\n", name);
                } else if(isTsVehicles) {

                } else if(!taskInfo.getResources().isEmpty()) {
                    if(isTsVehicles) {
                        Vehicles vehiclesDef = packageTarget.getVehiclesDef();
                        vehiclesDef.addJarResources(taskInfo);
                    } else {
                        taskJar.addJarResources(taskInfo);
                    }
                }
            }
        } else if(name.contains("fileset")) {
            System.out.printf("Finished %s: %s\n", task.getTaskName(), task.getRuntimeConfigurableWrapper().getAttributeMap());
        } else if(name.contains("jar")) {
            System.out.printf("%s: %s\n", task.getTaskName(), task.getRuntimeConfigurableWrapper().getAttributeMap());
            if(task instanceof UnknownElement) {
                UnknownElement ue = (UnknownElement) task;
                ue.maybeConfigure();
                Object realThing = ue.getRealThing();
                if(realThing instanceof Jar) {
                    Jar jar = (Jar) realThing;
                    System.out.printf("+++ jar: %s\n", jar.getDestFile());
                    ArrayList<TSFileSet> fileSets = new ArrayList<>();
                    for (UnknownElement uec : ue.getChildren()) {
                        Object proxy = uec.getWrapper().getProxy();
                        if(proxy instanceof FileSet) {
                            ArrayList<String> files = new ArrayList<>();
                            FileSet fileSet = (FileSet) proxy;
                            File dir = fileSet.getDir();
                            String prefix = null;
                            if(fileSet instanceof ZipFileSet) {
                                ZipFileSet zipFileSet = (ZipFileSet) fileSet;
                                prefix = zipFileSet.getPrefix(packageTarget.getProject().getProject());
                            }
                            fileSet.iterator().forEachRemaining(r -> files.add(r.toString()));
                            TSFileSet tsFileSet = new TSFileSet(dir.getAbsolutePath(), prefix, files);
                            fileSets.add(tsFileSet);
                        }
                    }
                    System.out.printf("\tfiles: %s\n", fileSets);
                    TsTaskInfo lastTsTask = tsTaskStack.peek();
                    lastTsTask.addResources(fileSets);
                    lastTsTask.setArchiveName(jar.getDestFile().getName());
                    System.out.printf("--- jar(%s): %s\n", lastTsTask.getTaskName(), jar.getDestFile());
                }
            }
        }
    }

    @Override
    public void messageLogged(BuildEvent event) {
        System.out.println(event.getMessage());
    }
}
