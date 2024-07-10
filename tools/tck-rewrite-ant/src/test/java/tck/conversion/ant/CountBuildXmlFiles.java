package tck.conversion.ant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CountBuildXmlFiles {
    public static void main(String[] args) throws IOException {
        scanBuildFiles(Paths.get("/home/starksm/Dev/Jakarta/wildflytck/jakartaeetck"));
    }
    /**
     * Walks the TCK src tree to collect and then parse the ant build.xml files
     *
     * @param sourceRoot
     * @throws IOException
     */
    static void scanBuildFiles(Path sourceRoot) throws IOException {
        BuildXmlFilter filter = new BuildXmlFilter();
        Files.walkFileTree(sourceRoot, filter);
        List<Path> buildXmls = filter.getBuildFiles();
        System.out.println(buildXmls.size());
        System.out.println(buildXmls);
    }
}
