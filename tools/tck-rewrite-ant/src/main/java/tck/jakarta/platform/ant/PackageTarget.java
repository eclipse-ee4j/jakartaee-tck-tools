package tck.jakarta.platform.ant;

import org.apache.tools.ant.Location;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * An encapsulation of a build.xml package target used to create test deployment artifacts.
 */
public class PackageTarget {
    ProjectWrapper project;
    Target pkgTarget;
    ClientJar clientJarDef;
    List<ClientJar> clientJars;
    Ear earDef;
    List<Ear> ears;
    EjbJar ejbJarDef;
    List<EjbJar> ejbJars;
    Par parDef;
    List<Par> pars;
    War warDef;
    List<War> wars;
    Rar rarDef;
    List<Rar> rars;
    Vehicles vehiclesDef;
    List<TaskInfo> unhandledTaks = new ArrayList<>();

    public PackageTarget(ProjectWrapper project, Target pkgTarget) {
        this.project = project;
        this.pkgTarget = pkgTarget;
    }

    public ProjectWrapper getProject() {
        return project;
    }

    public Target getPkgTarget() {
        return pkgTarget;
    }

    public ClientJar getClientJarDef() {
        return clientJarDef;
    }
    public boolean hasClientJarDef() {
        return clientJarDef != null;
    }

    public Ear getEarDef() {
        return earDef;
    }
    public boolean hasEarDef() {
        return earDef != null;
    }

    public EjbJar getEjbJarDef() {
        return ejbJarDef;
    }

    public List<EjbJar> getEjbJarDefs() {
        return ejbJars;
    }

    public boolean hasEjbJarDef() {
        return ejbJarDef != null;
    }

    public Par getParDef() {
        return parDef;
    }
    public boolean hasParDef() {
        return parDef != null;
    }
    public List<Par> getPars() {
        return pars;
    }
    public War getWarDef() {
        return warDef;
    }
    public boolean hasWarDef() {
        return warDef != null;
    }

    public Rar getRarDef() {
        return rarDef;
    }
    public boolean hasRarDef() {
        return rarDef != null;
    }

    public Vehicles getVehiclesDef() {
        return vehiclesDef;
    }
    public boolean hasVehiclesDef() {
        return vehiclesDef != null;
    }

    public List<TaskInfo> getUnhandledTaks() {
        return unhandledTaks;
    }

    public void parse() {
        Enumeration<String> dependencies = pkgTarget.getDependencies();
        // If the target has dependencies check those for ts.* tasks
        while(dependencies.hasMoreElements()) {
            String dep = dependencies.nextElement();
            Target depTarget = project.getTargets().get(dep);
            parseTarget(depTarget);
        }
        //
        parseTarget(pkgTarget);
    }

    /**
     * Go through all archives other than ears and collect the archive names as the {@link BaseJar#archiveName} for
     * use in code generation.
     * @return List of archive variable names using the archiveName
     */
    public List<String> getModuleNames() {
        ArrayList<String> moduleNames = new ArrayList<>();
        addModuleNames(moduleNames, ejbJarDef, ejbJars);
        addModuleNames(moduleNames, clientJarDef, clientJars);
        addModuleNames(moduleNames, parDef, pars);
        addModuleNames(moduleNames, warDef, wars);
        addModuleNames(moduleNames, rarDef, rars);
        return moduleNames;
    }
    private void addModuleNames(ArrayList<String> moduleNames, BaseJar jar, List<? extends BaseJar> jars) {
        // If there are multiple, they will all be in the jars list, a single value will be in jar
        if(jars != null) {
            for(BaseJar baseJar : jars) {
                moduleNames.add(baseJar.getTypedArchiveName());
            }
        } else if(jar != null) {
            moduleNames.add(jar.getTypedArchiveName());
        }
    }

    /**
     * Used to build the contents from the package target of a build.xml
     * @param target ant "package" target
     */
    public void parseTarget(Target target) {
        Location location = target.getLocation();
        String ifProperty = target.getIf();
        if(ifProperty != null && !project.isDefined(ifProperty)) {
            return;
        }
        String unlessProperty = target.getUnless();
        if(unlessProperty != null && project.isDefined(unlessProperty)) {
            return;
        }
        // Parse the tasks for
        Task[] tasks = target.getTasks();
        Task dirname = null;
        Task basename = null;
        for (Task task : tasks) {
            switch (task.getTaskName()) {
                case "dirname":
                    dirname = task;
                    break;
                case "basename":
                    basename = task;
                    break;
                case "ts.clientjar":
                    parseTsClientjar(task, dirname, basename);
                    break;
                case "ts.ejbjar":
                    parseTsEjbjar(task, dirname, basename);
                    break;
                case "ts.war":
                    parseTsWar(task, dirname, basename);
                    break;
                case "ts.ear":
                    parseTsEar(task, dirname, basename);
                    break;
                case "ts.par":
                    parseTsPar(task, dirname, basename);
                    break;
                case "ts.rar":
                    parseTsRar(task, dirname, basename);
                    break;
                case "ts.vehicles":
                    parseTsVehicles(task, dirname, basename);
                    break;
                case "echo":
                    // Ignore
                    break;
                default:
                    unhandledTaks.add(new TaskInfo(task.getTaskName(), task.getLocation()));
                    break;
            }
        }
    }

    /**
     * Called by a {@link org.apache.tools.ant.BuildEvent} driven parser to add a task after it has finished
     * executing. This invokes the corresponding parseTs* method with a null dirname and basename task since
     * those will have already executed.
     *
     * @param task a ts.* task
     * @return true if the task type was known, false if it was added to unhandled tasks list
     */
    public BaseJar addTask(Task task) {
        BaseJar taskJar = switch (task.getTaskName()) {
            case "ts.clientjar" ->   parseTsClientjar(task, null, null);
            case "ts.ejbjar"-> parseTsEjbjar(task, null, null);
            case "ts.war" -> parseTsWar(task, null, null);
            case "ts.ear" -> parseTsEar(task, null, null);
            case "ts.par" -> parseTsPar(task, null, null);
            case "ts.rar" -> parseTsRar(task, null, null);
            case "ts.vehicles" -> parseTsVehicles(task, null, null);
            case "echo" -> null;
            default -> {
                unhandledTaks.add(new TaskInfo(task.getTaskName(), task.getLocation()));
                yield null;
            }
        };
        return taskJar;
    }

    /**
     * Parse a ts.clientjar task into a pojo form
     * @param task a ts.clientjar task
     * @param dirname - optional dirname task that needs to run to set properties
     * @param basename - optional basename task that needs to run to set properties
     */
    public BaseJar parseTsClientjar(Task task, Task dirname, Task basename) {
        if(clientJarDef != null) {
            // There are multiple app clients in the package
            clientJars = new ArrayList<>();
            clientJars.add(clientJarDef);
        }
        if(dirname != null) {
            dirname.maybeConfigure();
            dirname.execute();
        }
        if(basename != null) {
            basename.maybeConfigure();
            basename.execute();
        }

        clientJarDef = new ClientJar(project.getProject(), task.getRuntimeConfigurableWrapper());
        if(clientJars != null) {
            clientJars.add(clientJarDef);
        }
        return clientJarDef;
    }

    /**
     * Parse a ts.ejbjar task into a pojo form
     * @param task a ts.ejbjar task
     * @param dirname - optional dirname task that needs to run to set properties
     * @param basename - optional basename task that needs to run to set properties
     */
    public BaseJar parseTsEjbjar(Task task, Task dirname, Task basename) {
        if(ejbJarDef != null) {
            // There are multiple ts.ejbjar tasks
            ejbJars = new ArrayList<>();
            ejbJars.add(ejbJarDef);
        }
        if(dirname != null) {
            dirname.maybeConfigure();
            dirname.execute();
        }
        if(basename != null) {
            basename.maybeConfigure();
            basename.execute();
        }

        ejbJarDef = new EjbJar(project.getProject(), task.getRuntimeConfigurableWrapper());
        if(ejbJars != null) {
            ejbJars.add(ejbJarDef);
        }
        return ejbJarDef;
    }

    /**
     * Parse a ts.war task into a pojo form
     * @param task a ts.war task
     * @param dirname - optional dirname task that needs to run to set properties
     * @param basename - optional basename task that needs to run to set properties
     */
    public BaseJar parseTsWar(Task task, Task dirname, Task basename) {
        if(warDef != null) {
            // Multiple wars
            wars = new ArrayList<>();
            wars.add(warDef);
        }
        if(dirname != null) {
            dirname.maybeConfigure();
            dirname.execute();
        }
        if(basename != null) {
            basename.maybeConfigure();
            basename.execute();
        }

        warDef = new War(project.getProject(), task.getRuntimeConfigurableWrapper());
        if(wars != null) {
            wars.add(warDef);
        }
        return warDef;
    }

    /**
     * Parse a ts.ear task into a pojo form
     * @param task a ts.ear task
     * @param dirname - optional dirname task that needs to run to set properties
     * @param basename - optional basename task that needs to run to set properties
     */
    public BaseJar parseTsEar(Task task, Task dirname, Task basename) {
        if(earDef != null) {
            // Multiple ears
            ears = new ArrayList<>();
            ears.add(earDef);
        }
        if(dirname != null) {
            dirname.maybeConfigure();
            dirname.execute();
        }
        if(basename != null) {
            basename.maybeConfigure();
            basename.execute();
        }

        earDef = new Ear(project.getProject(), task.getRuntimeConfigurableWrapper());
        if(ears != null) {
            ears.add(earDef);
        }
        return earDef;
    }

    /**
     * Parse a ts.par task into a pojo form
     * @param task a ts.par task
     * @param dirname - optional dirname task that needs to run to set properties
     * @param basename - optional basename task that needs to run to set properties
     */
    public BaseJar parseTsPar(Task task, Task dirname, Task basename) {
        if(parDef != null) {
            // Multiple pars
            pars = new ArrayList<>();
            pars.add(parDef);
        }
        if(dirname != null) {
            dirname.maybeConfigure();
            dirname.execute();
        }
        if(basename != null) {
            basename.maybeConfigure();
            basename.execute();
        }

        parDef = new Par(project.getProject(), task.getRuntimeConfigurableWrapper());
        if(pars != null) {
            pars.add(parDef);
        }
        return parDef;
    }

    /**
     * Parse a ts.rar task into a pojo form
     * @param task a ts.rar task
     * @param dirname - optional dirname task that needs to run to set properties
     * @param basename - optional basename task that needs to run to set properties
     */

    public BaseJar parseTsRar(Task task, Task dirname, Task basename) {
        if(rarDef != null) {
            throw new RuntimeException("multiple RARs is not supported");
        }
        if(dirname != null) {
            dirname.maybeConfigure();
            dirname.execute();
        }
        if(basename != null) {
            basename.maybeConfigure();
            basename.execute();
        }

        rarDef = new Rar(project.getProject(), task.getRuntimeConfigurableWrapper());
        return rarDef;
    }

    /**
     * Parse a ts.vehicles task into a pojo form
     * @param task a ts.vehicles task
     * @param dirname - optional dirname task that needs to run to set properties
     * @param basename - optional basename task that needs to run to set properties
     */
    public BaseJar parseTsVehicles(Task task, Task dirname, Task basename) {
        if(vehiclesDef != null) {
            throw new RuntimeException("vehiclesDef is already set");
        }

        Location pkgLocation = task.getLocation();
        Hashtable<String, Object> rcAttrs = task.getRuntimeConfigurableWrapper().getAttributeMap();
        AttributeMap attributeMap = new AttributeMap(project.getProject(), rcAttrs);
        String tsVehicleName = attributeMap.getAttribute("name");
        String vehiclePkgDir = project.getProperty("vehicle.pkg.dir");

        vehiclesDef = new Vehicles(tsVehicleName, vehiclePkgDir, attributeMap, task.getRuntimeConfigurableWrapper(), pkgLocation);
        return null;
    }

    public String toSummary() {
        StringBuilder tmp = new StringBuilder();
        tmp.append(String.format("Package(%s), loc=%s", project.getProject().getName(), pkgTarget.getLocation()));
        String depends = Utils.toList(pkgTarget.getDependencies()).toString();
        tmp.append(" [").append(depends).append("]; ");
        tmp.append(String.format("{hasClient=%s, hasEjb=%s, hasWar=%s, hasEar=%s, hasPar=%s, hasRar=%s, hasVehicles=%s}; ",
                hasClientJarDef(), hasEjbJarDef(), hasWarDef(), hasEarDef(), hasParDef(), hasRarDef(), hasVehiclesDef()));
        tmp.append("unhandledTaks=").append(unhandledTaks);
        return tmp.toString();
    }
}
