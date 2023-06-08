package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;



/**
 * AbstractFileProcessor
 *
 * @author Scott Marlow
 */
public abstract class AbstractFileProcessor implements JarProcessor {
    public static final String WEB_INF_CLASSES = "WEB-INF/classes/";
    public static final String CLASS = ".class";
    public static final String WEB_INF = "WEB-INF/";
    public static final String WEB_INF_LIB = "WEB-INF/lib/";
    public static final String META_INF = "META-INF";
    /**
     * This is a list of jar names in a unique directory for a given package that
     * need to be loaded as JavaArchive files in the deployment method.
     */
    protected final ArrayList<String> libraries = new ArrayList<>();
    protected final ArrayList<String> metainf = new ArrayList<>();
    protected final ArrayList<String> webinf = new ArrayList<>();
    protected final ArrayList<String> classes = new ArrayList<>();
    protected final ArrayList<String> otherFiles = new ArrayList<>();
    protected File archiveFile;
    protected File libDir;
    private final ArrayList<JarProcessor> subModules = new ArrayList<>();

    @Override
    public void process(ZipInputStream zipInputStream, ZipEntry entry) {

        if (entry.isDirectory()) {
            // ignore
        } else if (entry.toString().startsWith("META-INF/")) {
            addMetainf(entry.getName());
        } else if (entry.getName().endsWith(".jar")) {

        } else if (entry.getName().endsWith(".war")) {

        } else if (entry.getName().endsWith(".class")) {
            addClass(entry.getName());
        } else {
            otherFiles(entry.getName());
        }
    }

    protected void addMetainf(String name) {
        if (name.startsWith(WEB_INF))
            name = name.substring(WEB_INF.length());
        metainf.add(name);
    }

    protected void otherFiles(String name) {
        otherFiles.add(name);
    }

    @Override
    public ArrayList<String> getLibraries() {
        return libraries;
    }

    public File getLibDir() {
        return libDir;
    }

    @Override
    public ArrayList<JarProcessor> getSubModules() {
        return subModules;
    }

    @Override
    public ArrayList<String> getMetainf() {
        return metainf;
    }

    @Override
    public ArrayList<String> getWebinf() {
        return webinf;
    }

    @Override
    public ArrayList<String> getClasses() {
        return classes;
    }

    @Override
    public ArrayList<String> getOtherFiles() {
        return otherFiles;
    }

    protected void addWebinf(String name) {
        if (name.startsWith(META_INF))
            name = name.substring(META_INF.length());
        webinf.add(name);
    }

    protected void addLibrary(String name) {
        if (name.startsWith(WEB_INF_LIB))
            name = name.substring(WEB_INF_LIB.length());
        libraries.add(name);
    }

    protected void addModule(JarProcessor subModuleProcessor) {
        subModules.add(subModuleProcessor);
    }

    protected void addLibrary(JarProcessor subModuleProcessor) {
        subModules.add(subModuleProcessor);
    }

    protected void addClass(String name) {
        if (name.startsWith(WEB_INF_CLASSES))
            name = name.substring(WEB_INF_CLASSES.length());
        if (name.endsWith(CLASS))
            name = name.substring(0, name.length() - CLASS.length());
        name = name.replace('/', '.');
        classes.add(name);
    }

    @Override
    public String getName() {
        return archiveFile.getName();
    }

    @Override
    public Path getArchivePath() {
        return archiveFile.toPath();
    }

}
