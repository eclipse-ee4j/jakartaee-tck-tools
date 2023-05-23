package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.zip.ZipEntry;

/**
 * WarFileProcessor
 *
 * @author Scott Marlow
 */
public class WarFileProcessor implements JarProcessor {

    public static final String WEB_INF_CLASSES = "WEB-INF/classes/";
    public static final String CLASS = ".class";
    public static final String WEB_INF = "WEB-INF/";
    public static final String WEB_INF_LIB = "WEB-INF/lib/";
    public static final String META_INF = "META-INF";

    private final ArrayList<String> libraries = new ArrayList<>();
    private final ArrayList<String> metainf = new ArrayList<>();
    private final ArrayList<String> webinf = new ArrayList<>();
    private final ArrayList<String> classes = new ArrayList<>();
    private final ArrayList<String> otherFiles = new ArrayList<>();


    public WarFileProcessor(File archiveFile) {
    }

    @Override
    public void process(ZipEntry entry) {

        if (entry.isDirectory()) {
            // ignore
        } else if (entry.toString().startsWith("META-INF/")) {
            addMetainf(entry.getName());
        } else if (entry.toString().startsWith("WEB-INF/classes/")) {
            addClass(entry.getName());
        } else if (entry.toString().startsWith("WEB-INF/lib/")) {
            addLibrary(entry.getName());
        } else if (entry.toString().startsWith("WEB-INF/")) {
            addWebinf(entry.getName());
        } else {
            otherFiles(entry.getName());
        }
    }

    @Override
    public void saveOutput(final File fileInputArchive) {
        String testclient = "Client";
        File output = new File(fileInputArchive.getParentFile(), testclient + ".java");
        System.out.println("generating " + output.getName() + " for input file " + fileInputArchive.getName());
        output.getParentFile().mkdirs();
        try (FileWriter fileWriter = new FileWriter(output)) {
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("import org.jboss.arquillian.container.test.api.Deployment;");
            printWriter.println("import org.jboss.shrinkwrap.api.ShrinkWrap;");
            printWriter.println("import org.jboss.shrinkwrap.api.spec.WebArchive;\n");

            printWriter.println("@Deployment(testable = false)");
            printWriter.println("public static WebArchive getTestArchive() throws Exception {");
            printWriter.print("return ShrinkWrap.create(WebArchive.class, ");
            printWriter.println("\"" + testclient + ".war\")");
            for (String name : libraries) {
                printWriter.print(".addAsLibrary(");
                printWriter.print(name);
                printWriter.println(")");
            }
            for (String name : classes) {
                printWriter.print(".addClass(");
                printWriter.print(name);
                printWriter.println(")");
            }
            for (String name : webinf) {
                printWriter.print(".addAsWebInfResource(");
                printWriter.print(name);
                printWriter.println(")");
            }
            for (String name : metainf) {
                printWriter.print(".addAsManifestResource(");
                printWriter.print(name);
                printWriter.println(")");
            }
            printWriter.println("}");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public ArrayList<String> getLibraries() {
        return libraries;
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

    private void otherFiles(String name) {
        otherFiles.add(name);
    }

    private void addWebinf(String name) {
        if (name.startsWith(META_INF))
            name = name.substring(META_INF.length());
        webinf.add(name);
    }

    private void addLibrary(String name) {
        if (name.startsWith(WEB_INF_LIB))
            name = name.substring(WEB_INF_LIB.length());
        libraries.add(name);
    }

    private void addClass(String name) {
        if (name.startsWith(WEB_INF_CLASSES))
            name = name.substring(WEB_INF_CLASSES.length());
        if (name.endsWith(CLASS))
            name = name.substring(0, name.length() - CLASS.length());
        name = name.replace('/', '.');
        classes.add(name);
    }

    private void addMetainf(String name) {
        if (name.startsWith(WEB_INF))
            name = name.substring(WEB_INF.length());
        metainf.add(name);
    }
}
