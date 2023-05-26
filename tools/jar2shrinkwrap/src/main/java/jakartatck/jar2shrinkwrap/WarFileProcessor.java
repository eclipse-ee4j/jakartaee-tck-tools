package jakartatck.jar2shrinkwrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    /**
     * This is a list of jar names in a unique directory for a given package that
     * need to be loaded as JavaArchive files in the deployment method.
     */
    private final ArrayList<String> libraries = new ArrayList<>();
    private File libDir;
    private final ArrayList<String> metainf = new ArrayList<>();
    private final ArrayList<String> webinf = new ArrayList<>();
    private final ArrayList<String> classes = new ArrayList<>();
    private final ArrayList<String> otherFiles = new ArrayList<>();


    public WarFileProcessor(File archiveFile) {
        libDir = new File(archiveFile.getAbsolutePath()+".lib");
        if(!libDir.exists()) {
            libDir.mkdirs();
        }
    }

    @Override
    public void process(ZipInputStream zipInputStream, ZipEntry entry) {

        if (entry.isDirectory()) {
            // ignore
        } else if (entry.toString().startsWith("META-INF/")) {
            addMetainf(entry.getName());
        } else if (entry.toString().startsWith("WEB-INF/classes/")) {
            addClass(entry.getName());
        } else if (entry.toString().startsWith("WEB-INF/lib/")) {
            String jarName = entry.getName().substring("WEB-INF/lib/".length());
            File libFile = new File(libDir, jarName);
            try(FileOutputStream libFileOS = new FileOutputStream(libFile)) {
                byte[] libContent = zipInputStream.readAllBytes();
                libFileOS.write(libContent);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            addLibrary(entry.getName());
        } else if (entry.toString().startsWith("WEB-INF/")) {
            addWebinf(entry.getName().substring("WEB-INF/".length()));
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
            saveOutput(fileWriter, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveOutput(Writer writer, boolean includeImports) {
        String testclient = "Client";
        String indent = "\t";
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            if(includeImports) {
                printWriter.println("import org.jboss.arquillian.container.test.api.Deployment;");
                printWriter.println("import org.jboss.shrinkwrap.api.ShrinkWrap;");
                printWriter.println("import org.jboss.shrinkwrap.api.spec.WebArchive;\n");
            }

            printWriter.println(indent+"@Deployment(testable = false)");
            printWriter.println(indent+"public static WebArchive getTestArchive() throws Exception {");
            // The libary jars
            printWriter.println(indent.repeat(2)+"List<File> libraryFiles = new ArrayList<>();");
            printWriter.println(indent.repeat(2)+"for (String jarName : war.getLibraries()) {");
            printWriter.println(indent.repeat(3)+"File jarFile = new File(war.getLibDir(), jarName);");
            printWriter.println(indent.repeat(3)+"libraryFiles.add(jarFile);");
            printWriter.println(indent.repeat(2)+"}");
            printWriter.println(indent.repeat(2)+"List<JavaArchive> warJars = libraryFiles.stream()\n"+
                    indent.repeat(2)+".map(file -> ShrinkWrap.createFromZipFile(JavaArchive.class, file))\n"+
                    indent.repeat(2)+".collect(Collectors.toList());");

            // Start war creation
            printWriter.print(indent.repeat(2)+"return ShrinkWrap.create(WebArchive.class, ");
            printWriter.println("\"" + testclient + ".war\")");

            printWriter.println(indent.repeat(3)+".addAsLibraries(warJars)");

            for (String name : classes) {
                printWriter.print(indent.repeat(3)+".addClass(");
                printWriter.print(name);
                printWriter.println(".class)");
            }
            for (String name : webinf) {
                printWriter.print(indent.repeat(3)+".addAsWebInfResource(\"");
                printWriter.print(name);
                printWriter.println("\")");
            }
            // I don't think this is valid in general as a custom manifest would not be added to a deployment
            /*
            for (String name : metainf) {
                printWriter.print(".addAsManifestResource(");
                printWriter.print(name);
                printWriter.println(")");
            }
            */
            printWriter.println("}");
        }
    }

    @Override
    public ArrayList<String> getLibraries() {
        return libraries;
    }

    public File getLibDir() {
        return libDir;
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
