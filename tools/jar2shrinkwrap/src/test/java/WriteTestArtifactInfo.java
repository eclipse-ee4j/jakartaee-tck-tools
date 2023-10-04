import jakartatck.jar2shrinkwrap.Jar2ShrinkWrap;
import jakartatck.jar2shrinkwrap.JarProcessor;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A utility class to write war-info.txt files in the tck source tree of the pre-update servlet
 * branch as input into the code-pilot machine learning data.
 */
public class WriteTestArtifactInfo {
    static int infoCount = 0;
    static Path srcRepo;

    /**
     * Writes a war-info.txt file for the given pkg and war file
     * @param pkg test package name
     * @param war war from the legacy tck dist
     */
    static void writeInfo(String pkg, JarProcessor war) {
        List<File> libraryFiles = new ArrayList<>();
        for (String jarName : war.getLibraries()) {
            File jarFile = new File(war.getBaseDir(), jarName);
            libraryFiles.add(jarFile);
        }
        List<JavaArchive> warJars = libraryFiles.stream()
                .map(file -> ShrinkWrap.createFromZipFile(JavaArchive.class, file))
                .toList();

        StringWriter out = new StringWriter();
        out.append(war.getName());
        out.append('\n');
        String indent = "  ";
        for (String c : war.getClasses()) {
            String cls = "%sWEB-INF/classes/%s\n".formatted(indent.repeat(1), c);
            out.append(cls);
        }
        for (JavaArchive jar : warJars) {
            String jname = "%sWEB-INF/lib/%s\n".formatted(indent.repeat(1), jar.getName());
            out.append(jname);
            Map<ArchivePath, Node> content = jar.getContent();
            for (ArchivePath path : content.keySet()) {
                Asset asset = content.get(path).getAsset();
                if(asset != null) {
                    String ainfo = "%s%s\n".formatted(indent.repeat(2), path.get());
                    out.append(ainfo);
                }
            }
        }
        Path warPath = srcRepo.resolve(pkg.replace('.', '/'));
        Path warInfoPath = warPath.resolve("war-info.txt");
        try(BufferedWriter infoWriter = Files.newBufferedWriter(warInfoPath)) {
            infoWriter.write(out.toString());
            infoCount ++;
            System.out.printf("Wrote(%d) %s\n", infoCount, warInfoPath.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Specify the root source directory of the servlet tck platform tests as the only argument
     * @param args tck repo src path, /Users/starksm/Dev/Jakarta/platform-tck-refactor/servlet/src/main/java
     * @throws IOException on failure
     */
    public static void main(String[] args) throws IOException {
        if(args.length == 0) {
            throw new IllegalArgumentException("Need to specify src directory of tests to write war-info.txt into");
        }
        srcRepo = Paths.get(args[0]);
        File tckDir = Jar2ShrinkWrap.maybeDownloadTck();
        Path tckRoot = tckDir.toPath().resolve("jakartaeetck/dist");
        Set<String> pkgNames = Jar2ShrinkWrap.getTestPkgNames("com/sun/ts/tests/servlet");

        for (String pkg : pkgNames) {
            try {
                JarProcessor war = Jar2ShrinkWrap.fromPackage(pkg);
                writeInfo(pkg, war);
            } catch (IllegalStateException ise) {
                System.out.printf("WARN: pkg %s has multiple artifacts: %s\n", pkg, ise.getMessage());
            }
        }
    }
}
