package tck.conversion.ant;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link java.nio.file.SimpleFileVisitor} to look for build.xml files while skipping
 * any target dirs
 */
public class BuildXmlFilter extends SimpleFileVisitor<Path> {
    private ArrayList<Path> buildXmls = new ArrayList<>();

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        // Ignore any target directories
        if (dir.getFileName().endsWith("target")) {
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    /**
     * Attempt to filter to build.xml files that are in leaf directories, i.e., those with just a build.xml
     * file and no subdirectories.
     *
     * @param file
     *          a reference to the file
     * @param attrs
     *          the file's basic attributes
     *
     * @return
     * @throws IOException
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.getFileName().endsWith("build.xml")) {
            // Does the directory containing the build.xml have no child directories?
            long subdirs = Files.walk(file.getParent(), 1).filter(Files::isDirectory).count();
            if(subdirs == 1) {
                buildXmls.add(file);
            }
        }
        return FileVisitResult.CONTINUE;
    }

    public List<Path> getBuildFiles() {
        return buildXmls;
    }
}
