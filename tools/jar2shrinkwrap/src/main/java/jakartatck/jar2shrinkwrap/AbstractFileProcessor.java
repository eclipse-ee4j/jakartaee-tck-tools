package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    protected File baseDir;
    protected final ArrayList<String> subModules = new ArrayList<>();

    private Map<String, JarProcessor> libraryContent = new HashMap<>();


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

    protected void processLibrary(String jarName, File libFile, ZipInputStream zipInputStream) {
        if (!libFile.exists()) { // Typical usage for EAR is that module/library entries archives will already exist in test folder but if not, create them)
            try (FileOutputStream libFileOS = new FileOutputStream(libFile)) {
                byte[] libContent = zipInputStream.readAllBytes();
                libFileOS.write(libContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // load the library content
        JarVisit visit = new JarVisit(libFile);
        JarProcessor jar = visit.execute();
        libraryContent.put(jarName, jar);
        addLibrary(libFile.getName());

    }

    public JarProcessor getLibrary(String name) {
        return libraryContent.get(name);
    }

    protected String archiveName(String archiveName) {
        return archiveName.replace(".", "_");
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

    public File getBaseDir() {
        return baseDir;
    }

    @Override
    public ArrayList<String> getSubModules() {
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

    protected void addModule(String name) {
        subModules.add(name);
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
    @Override
    public void saveOutput(final File fileInputArchive) {
        String testclient = "Client";
        File output = new File(fileInputArchive.getParentFile(), testclient + ".java");
        System.out.println("generating " + output.getName() + " for input file " + fileInputArchive.getName());
        output.getParentFile().mkdirs();
        try (FileWriter fileWriter = new FileWriter(output)) {
            saveOutput(fileWriter, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
