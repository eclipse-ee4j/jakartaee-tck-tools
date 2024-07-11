package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * AbstractFileProcessor
 *
 * @author Scott Marlow
 */
public abstract class AbstractFileProcessor implements JarProcessor {
    private static final Logger log = Logger.getLogger(Jar2ShrinkWrap.class.getName());
    public static final String WEB_INF_CLASSES = "WEB-INF/classes/";
    public static final String CLASS = ".class";
    public static final String WEB_INF = "WEB-INF/";
    public static final String WEB_INF_LIB = "WEB-INF/lib/";
    public static final String META_INF = "META-INF/";
    /**
     * This is a list of jar names in a unique directory for a given package that
     * need to be loaded as JavaArchive files in the deployment method.
     */
    protected final ArrayList<String> libraries = new ArrayList<>();
    protected final ArrayList<String> metainf = new ArrayList<>();
    protected final ArrayList<String> webinf = new ArrayList<>();
    protected final Set<String> classes = new HashSet<>();
    protected final ArrayList<String> otherFiles = new ArrayList<>();
    protected File archiveFile;
    protected File baseDir;
    protected final ArrayList<String> subModules = new ArrayList<>();

    private Map<String, JarProcessor> libraryContent = new HashMap<>();

    private ClassNameRemapping classNameRemapping;


    @Override
    public void process(ZipInputStream zipInputStream, ZipEntry entry, ClassNameRemapping classNameRemapping) {
        if (this.classNameRemapping != classNameRemapping) {
            this.classNameRemapping = classNameRemapping;
        }

        if (entry.isDirectory()) {
            // ignore
        } else if (entry.toString().startsWith(META_INF)) {
            addMetainf(entry.getName());
        } else if (entry.getName().endsWith(".jar")) {

        } else if (entry.getName().endsWith(".war")) {

        } else if (entry.getName().endsWith(".class")) {
            addClass(entry.getName());
        } else {
            otherFiles(entry.getName());
        }
    }

    protected void processLibrary(String jarName, File libFile, ZipInputStream zipInputStream, ClassNameRemapping classNameRemapping) {
        if (!libFile.exists()) { // Typical usage for EAR is that module/library entries archives will already exist in test folder but if not, create them)
            try (FileOutputStream libFileOS = new FileOutputStream(libFile)) {
                byte[] libContent = zipInputStream.readAllBytes();
                libFileOS.write(libContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // load the library content
        JarVisit visit = new JarVisit(libFile, classNameRemapping);
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
        if (name.startsWith(META_INF))
            name = name.substring(META_INF.length());
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
    public Set<String> getClasses() {
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
        if (!libraries.contains(name)) {
            libraries.add(name);
        }
    }

    protected void addModule(String name) {
        if (!subModules.contains(name)) {
            subModules.add(name);
        } else {
            throw new RuntimeException("attempted to add the same module " + name + " in " + archiveFile.getName());
        }

    }


    protected void addClass(String name) {

        if (name.startsWith(WEB_INF_CLASSES))
            name = name.substring(WEB_INF_CLASSES.length());

        if (name.startsWith(META_INF)) { // handle resources like META-INF/persistence.xml
            name = name.substring(META_INF.length());
            webinf.add(name);
            return;
        }
        if (name.endsWith(CLASS))
            name = name.substring(0, name.length() - CLASS.length());
        name = name.replace('/', '.');
        if (classNameRemapping.shouldBeIgnored(name)) {
            return;
        }
        if (!name.endsWith(CLASS)) {
            name = name + CLASS; // add .class extension
        }
        name = classNameRemapping.getName(name);

        if(name.startsWith(classNameRemapping.getTargetClassNamePackage())) {
            String nameNoclass = name.substring(0, name.length() - CLASS.length());
            // reduce to just the class name
            nameNoclass = nameNoclass.substring(nameNoclass.lastIndexOf(".")+1,nameNoclass.length());
            if (nameNoclass.equals("Client")) {
                // replace typical Client.class test client with the actual Test client source file
                name = classNameRemapping.getTargetClassName();
            }
        }
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
        log.fine("generating " + output.getName() + " for input file " + fileInputArchive.getName());
        output.getParentFile().mkdirs();
        try (FileWriter fileWriter = new FileWriter(output)) {
            saveOutput(fileWriter, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveOutputWar(PrintWriter printWriter, boolean includeImports, String archiveName) {
        final String indent = " ";
        final String newLine = "\n";

        // WebArchive war = ShrinkWrap.create(WebArchive.class, name)
        printWriter.println(newLine + indent + "WebArchive %s = ShrinkWrap.create(WebArchive.class, \"%s\");".formatted(archiveName(archiveName), archiveName(archiveName)));
        for (String webinfFile : getWebinf()) {
            if (!ignoreFile(webinfFile)) {
                printWriter.println(indent.repeat(3) + "%s.addAsWebInfResource(\"%s\");".formatted(archiveName(archiveName), webinfFile));
            }
        }
        for (String otherFile : getOtherFiles()) {
            if (!ignoreFile(otherFile)) {
                printWriter.println(indent.repeat(3) + "%s.addAsWebResource(\"%s\");".formatted(archiveName(archiveName), otherFile));
            }
        }

        for (String warlibrary : getLibraries()) {
            JarProcessor warLibraryProcessor = getLibrary(warlibrary);
            printWriter.println(newLine + indent + "{");  // we can add multiple variations of the same archive so enclose it in a code block
            printWriter.println(newLine + indent + "JavaArchive %s = ShrinkWrap.create(JavaArchive.class, \"%s\");".formatted(archiveName(warlibrary), warlibrary));
            for (String className: warLibraryProcessor.getClasses()) {
                if (!ignoreFile(className)) {
                    printWriter.println(indent + "%s.addClass(%s);".formatted(archiveName(warlibrary), className));
                }
            }
            for (String otherFile: warLibraryProcessor.getOtherFiles()) {
                if (!ignoreFile(otherFile)) {
                    printWriter.println(indent.repeat(1) + "%s.addAsManifestResource(\"%s\");".formatted(archiveName(warlibrary), otherFile));
                }
            }
            for (String metainf : warLibraryProcessor.getMetainf()) {
                if (!ignoreFile(metainf)) {
                    printWriter.println(indent.repeat(1) + "%s.addAsManifestResource(\"%s\");".formatted(archiveName(warlibrary), metainf));
                }
            }
            printWriter.println(indent.repeat(1)+"%s.addAsLibrary(%s);".formatted(archiveName(archiveName),archiveName(warlibrary)));
            printWriter.println(newLine + indent + "}");  // we can add multiple variations of the same archive so enclose it in a code block
        }
        // add classes
        for (String className: getClasses()) {
            if (!ignoreFile(className)) {
                printWriter.println(indent + "%s.addClass(%s);".formatted(archiveName(archiveName), className));
            }
        }

    }

    protected boolean ignoreFile(String filename) {
        return filename.isEmpty() || filename.endsWith(".java") || filename.contains("com.sun.ts.lib.");
    }

}
