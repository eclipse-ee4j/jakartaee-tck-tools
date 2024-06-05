package tck.conversion.ant;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Don't remember what this was for?
 */
public class FixBuildTreeImports {
    public static void main(String[] args) throws IOException {
        System.setProperty("ts.home", "/tmp/legacytck/LegacyTCKFolderName/jakartaeetck");
        //
        System.setProperty("deliverabledir", "tck");

        File deliveryDir = new File("/tmp/legacytck/LegacyTCKFolderName/jakartaeetck/install/tck");
        deliveryDir.mkdirs();
        Path binDir = Paths.get("/tmp/legacytck/LegacyTCKFolderName/jakartaeetck/bin");
        Path installBinDir = Paths.get("/tmp/legacytck/LegacyTCKFolderName/jakartaeetck/install/tck/bin");
        if(!Files.isSymbolicLink(installBinDir)) {
            Files.createSymbolicLink(installBinDir, binDir);
        }
        scanBuildFiles(Paths.get("/home/starksm/Dev/Jakarta/t-platform-tck"));
    }
    static void scanBuildFiles(Path sourceRoot) throws IOException {
        BuildXmlImportFixer filter = new BuildXmlImportFixer(sourceRoot);
        Files.walkFileTree(sourceRoot, filter);
    }
    static class BuildXmlImportFixer extends SimpleFileVisitor<Path> {
        Pattern importRE = Pattern.compile("\\s+<import file=\"([^\"]+)\"");
        Path sourceRoot;
        BuildXmlImportFixer(Path sourceRoot) {
            this.sourceRoot = sourceRoot;
        }
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            // Ignore any target directories
            if(dir.getFileName().endsWith("target")) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if(file.getFileName().endsWith("build.xml")) {
                // Validate any import
                fixImports(file);
            }
            return FileVisitResult.CONTINUE;
        }
        private void fixImports(Path buildXml) throws IOException {
            List<String> lines = Files.readAllLines(buildXml);
            StringWriter tmp = new StringWriter();
            boolean updateFile = false;
            for (String line : lines) {
                if(line.contains("<import file=")) {
                    Matcher matcher = importRE.matcher(line);
                    if(matcher.find()) {
                        String path = matcher.group(1);
                        Path target = buildXml.getParent().resolve(path);
                        boolean resolved = false;
                        if(!Files.isReadable(target)) {
                            target = target.normalize();
                            System.out.printf("%s import of %s resolves to: %s, but it does not exist\n", buildXml, path, target);
                            while(target.startsWith(sourceRoot)) {
                                path = "../" + path;
                                target = buildXml.getParent().resolve(path).normalize();
                                if(Files.isReadable(target)) {
                                    resolved = true;
                                    updateFile = true;
                                    line = String.format("    <import file=\"%s\" />", path);
                                    System.out.printf("Correct import is: "+path);
                                    System.out.println(target.toFile().getCanonicalFile());
                                }
                            }
                            if(!resolved) {
                                // Ignore imports marked as optional
                                if(!line.contains("optional")) {
                                    System.out.printf("Failed to resolve import in: %s\n", buildXml);
                                    line = String.format("    <import file=\"%s\" optional=\"true\"/>", path);
                                }
                            }
                        }
                    }
                }
                tmp.append(line);
                tmp.append('\n');
            }
            //
            if(updateFile) {
                Files.writeString(buildXml, tmp.toString(), StandardOpenOption.WRITE);
            }
        }
    }

}
