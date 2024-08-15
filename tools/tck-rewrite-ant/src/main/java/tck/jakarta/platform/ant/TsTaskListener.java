package tck.jakarta.platform.ant;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.Jar;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.ZipFileSet;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * An ant BuildListener used to capture the ts.* task information when executing the package target
 * of the TCK build.xml
 */
public class TsTaskListener implements BuildListener {
    private static Logger log = Logger.getLogger(TsTaskListener.class.getName());
    // The ant 'package' target wrapper
    private PackageTarget packageTarget;
    // A stack to track information for the active ts.* task
    private LinkedList<TsTaskInfo> tsTaskStack = new LinkedList<>();
    // A stack to track information for the active package and dependent targets
    private LinkedList<TsPackageInfo> tsTargetStack = new LinkedList<>();
    private int archiveOrder = 0;

    public TsTaskListener(PackageTarget packageTarget) {
        this.packageTarget = packageTarget;
    }

    public void reset() {
        tsTaskStack.clear();
        tsTargetStack.clear();
        archiveOrder = 0;
    }

    @Override
    public void buildStarted(BuildEvent event) {
    }

    @Override
    public void buildFinished(BuildEvent event) {
    }

    /**
     * Notification that a target is starting. We push a TsPackageInfo onto the tsTargetStack. This is only called
     * when target#performTasks(). Directly calling target#execute() bypasses the listener notification.
     * @param event An event with any relevant extra information.
     */
    @Override
    public void targetStarted(BuildEvent event) {
        Target target = event.getTarget();
        debug("--- targetStarted %s, %s\n", target.getName(), target.getLocation());
        TsPackageInfo tsPackageInfo = new TsPackageInfo(target);
        tsTargetStack.push(tsPackageInfo);
    }

    /**
     * Notification that a target has finished. Pops the TsPackageInfo from the tsTargetStack and updates the target
     * tasks with any artifact information created outside the task.
     *
     * @param event An event with any relevant extra information.
     */
    @Override
    public void targetFinished(BuildEvent event) {
        Target target = event.getTarget();
        TsPackageInfo tsPackageInfo = tsTargetStack.pop();
        debug("--- targetFinished %s, %s, %s\n", target.getName(), target.getLocation(), tsPackageInfo);
        // We need to see if there are jars associated with the target or its task
        List<TsTaskInfo> taskInfos = tsPackageInfo.getTsTaskInfos();
        boolean isTsVehicles = tsPackageInfo.hasTsVehicles();
        if(isTsVehicles && !tsPackageInfo.getArchives().isEmpty()){
            Vehicles vehiclesDef = packageTarget.getVehiclesDef();
            vehiclesDef.addJarResources(tsPackageInfo);
        } else {
            BaseJar taskJar = packageTarget.getTaskJar(tsPackageInfo.getTargetName());
            if(taskJar == null) {
                if(!tsPackageInfo.getArchives().isEmpty()) {
                    packageTarget.addTargetJar(tsPackageInfo);
                } else {
                    debug("Target(%s) had no jars or taskJar\n", tsPackageInfo.getTargetName());
                }
            } else {
                if(!tsPackageInfo.getArchives().isEmpty()) {
                    taskJar.addJarResources(tsPackageInfo);
                } else {
                    debug("Target(%s) had no jars\n", tsPackageInfo.getTargetName());
                }
            }
        }
    }

    /**
     * Here we capture the start of a ts.* task if it is not ts.verbose and push a TsTaskInfo onto
     * the tsTaskStack
     *
     * @param event An event for the start of an ant Task
     */
    @Override
    public void taskStarted(BuildEvent event) {
        Task task = event.getTask();
        String name = task.getTaskName();
        if(name.startsWith("ts.") && !name.startsWith("ts.verbose")) {
            debug("+++ Started %s: %s\n", task.getTaskName(), task.getRuntimeConfigurableWrapper().getAttributeMap());
            TsTaskInfo taskInfo = new TsTaskInfo(task);
            tsTaskStack.push(taskInfo);
            TsPackageInfo lastTsPackage = tsTargetStack.peek();
            lastTsPackage.addTaskInfo(taskInfo);
        } else {
            trace("+++ Started %s: %s\n", task.getTaskName(), task.getLocation());
        }
    }

    /**
     * Notification that the Task has complated. If it is a ts.* task, we print information about, pop the
     * matching TsTaskInfo from the stack if this is not ts.verbose and update the BaseJar or Vehicles
     * information.
     *
     * @param event An event for the start of an ant Task
     */
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

                debug("ts.verbose: %s\n", msg);
            } else {
                TsTaskInfo taskInfo = tsTaskStack.pop();
                boolean isTsVehicles = name.equals("ts.vehicles");
                debug("+++ Finished %s: %s\n", task.getTaskName(), task.getRuntimeConfigurableWrapper().getAttributeMap());
                if(event.getMessage() != null) {
                    debug("\t%s\n", event.getMessage());
                }
                if(event.getException() != null) {
                    debug("\t%s\n", event.getException());
                }
                // Add the task to the package target wrapper
                BaseJar taskJar = packageTarget.addTask(task);
                if(isTsVehicles) {
                    Vehicles vehiclesDef = packageTarget.getVehiclesDef();
                    if(!taskInfo.getArchives().isEmpty()) {
                        vehiclesDef.addJarResources(taskInfo);
                    }
                    // Copy tasks
                    packageTarget.mergeCopyFS(taskInfo.getCopyFSSets());
                } else if(taskJar == null) {
                    debug("Unhandled task: %s\n", name);
                } else {
                    taskJar.addJarResources(taskInfo);
                    // Update attributes from the component info
                    if(taskInfo.getComponetAttributes() != null) {
                        debug("Updating attributes from component map: %s\n", taskInfo.getComponetAttributes());
                        taskJar.updateFromComponentAttrs(taskInfo.getComponetAttributes());
                    }
                }
            }
        } else if(name.contains("jar")) {
            /* Here we capture information about a test archive that uses the fully resolved files and attributes.
            This avoids having to try to resolve wildcards in the original parsed task attributes.
            */
            debug("%s: %s\n", task.getTaskName(), task.getRuntimeConfigurableWrapper().getAttributeMap());
            if(task instanceof UnknownElement) {
                UnknownElement ue = (UnknownElement) task;
                ue.maybeConfigure();
                Object realThing = ue.getRealThing();
                if(realThing instanceof Jar) {
                    // This is any archive, client, ejb, ear, par, rar, war
                    Jar jar = (Jar) realThing;
                    File destFile =  jar.getDestFile();
                    String jarName = destFile.getName();
                    debug("+++ jar: %s\n", jar.getDestFile());
                    TsArchiveInfo archiveInfo = new TsArchiveInfo(jarName, archiveOrder ++);
                    if(ue.getChildren() != null) {
                        for (UnknownElement uec : ue.getChildren()) {
                            Object proxy = uec.getWrapper().getProxy();
                            if (proxy instanceof FileSet) {
                                ArrayList<String> files = new ArrayList<>();
                                FileSet fileSet = (FileSet) proxy;
                                File dir = fileSet.getDir();
                                String prefix = null;
                                if (fileSet instanceof ZipFileSet) {
                                    ZipFileSet zipFileSet = (ZipFileSet) fileSet;
                                    prefix = zipFileSet.getPrefix(packageTarget.getProject().getProject());
                                }
                                fileSet.iterator().forEachRemaining(r -> files.add(r.toString()));
                                TsFileSet tsFileSet = new TsFileSet(dir.getAbsolutePath(), prefix, files);
                                archiveInfo.addResource(tsFileSet);
                            }
                        }
                    } else {
                        AttributeMap attrMap = new AttributeMap(packageTarget.getProject().getProject(), jar.getRuntimeConfigurableWrapper().getAttributeMap());
                        TsFileSet tsFileSet = new TsFileSet(attrMap);
                        archiveInfo.addResource(tsFileSet);
                    }

                    debug("jarLib: %s\n", archiveInfo);
                    TsTaskInfo lastTsTask = tsTaskStack.peek();
                    TsPackageInfo lastTsPackage = tsTargetStack.peek();
                    if(lastTsTask != null) {
                        // If there was a ts.* task, add the archive info to it
                        lastTsTask.addArchive(archiveInfo);
                        debug("--- jar(%s): %s\n", lastTsTask.getTaskName(), archiveInfo);
                    }
                    else if(lastTsPackage != null) {
                        lastTsPackage.addArchive(archiveInfo);
                        debug("--- jar(%s): %s\n", lastTsPackage.getTargetName(), archiveInfo);
                    }
                } else if(name.startsWith("component.")) {
                    // This is an instance of the ts.common.xml _component macrodef that has jar in the name, e.g., component.clientjar
                    RuntimeConfigurable rc = task.getRuntimeConfigurableWrapper();
                    TsTaskInfo lastTsTask = tsTaskStack.peek();
                    if(lastTsTask != null) {
                        lastTsTask.setComponentAttributes(rc);
                    }
                }

            }
        } else if(name.startsWith("component.")) {
            // This is an instance of the ts.common.xml _component macrodef that does not have jar in the name, e.g., component.ear
            RuntimeConfigurable rc = task.getRuntimeConfigurableWrapper();
            TsTaskInfo lastTsTask = tsTaskStack.peek();
            lastTsTask.setComponentAttributes(rc);
        } else if(name.equals("copy")) {
            /* The copy task is used to copy descriptors from the source tree to the ts.home/tmp directory.
            Here we try to capture the source directory being used as the ts.* task often just has the
            ts.home/tmp directory as the fileset dir for the descriptor, and this does not work with arquillian.
            */
            UnknownElement ue = (UnknownElement) task;
            ue.maybeConfigure();
            RuntimeConfigurable rc = task.getRuntimeConfigurableWrapper();
            if(rc.getChildren().hasMoreElements()) {
                FileSet fs = (FileSet) rc.getChildren().nextElement().getProxy();
                String dir = fs.getDir().getAbsolutePath();
                String[] includes = fs.getDirectoryScanner().getIncludedFiles();
                if(includes.length > 0) {
                    TsTaskInfo lastTsTask = tsTaskStack.peek();
                    TsFileSet copyFS = new TsFileSet(dir, null, new ArrayList<>(List.of(includes)));
                    if(lastTsTask == null) {
                        log.warning("No ts.* task for copy task for: "+copyFS);
                    } else {
                        lastTsTask.addCopyFS(copyFS);
                    }
                }
            }
        } else {
            trace("Finished other(%s): %s\n", task.getTaskName(), task.getRuntimeConfigurableWrapper().getAttributeMap());
        }
    }

    @Override
    public void messageLogged(BuildEvent event) {
        Task task = event.getTask();
        if(task == null) {
            log.finer(event.getMessage()+"\n");
        } else {
            debug("%s msg: %s\n", task.getTaskName(), event.getMessage());
        }
    }
    private void info(String format, Object ... args) {
        String msg = String.format(format, args);
        log.info(msg);
    }
    private void debug(String format, Object ... args) {
        String msg = String.format(format, args);
        log.fine(msg);
    }
    private void trace(String format, Object ... args) {
        String msg = String.format(format, args);
        log.finer(msg);
    }
}
