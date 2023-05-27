package jakartatck.jar2shrinkwrap;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;

/**
 * A FileVisitor that identifies test pkg names based on the presence of test
 * artifact files in an unpacked legacy TCK bundle structure.
 */
public class TestPkgVisitor implements FileVisitor<Path> {
    private Path tckRoot;
    private Path filterPath;

    private HashSet<String> testPkgs = new HashSet<>();

    /**
     * Create a visitor of an unpacked legacy TCK bundle.
     * @param tckRoot The root of the unpacked bundle, usually the dist directory
     * @param basePkgPath - a base pkg to filter against, e.g., com/sun/ts/tests/servlet. Only pkgs
     *                under this path will be examined.
     */
    TestPkgVisitor(Path tckRoot, String basePkgPath) {
        this.tckRoot = tckRoot;
        this.filterPath = tckRoot.resolve(basePkgPath);
    }

    /**
     * The packages seen during the last visit that had artifacts in the tck bundle
     * @return packages seen during the last visit
     */
    public HashSet<String> getTestPkgs() {
        return testPkgs;
    }
    public void clearTestPkgs() {
        testPkgs.clear();
    }

    /**
     * Filter out directories that are outside the base pkg path provided in the ctor.
     * @param dir
     *          a reference to the directory
     * @param attrs
     *          the directory's basic attributes
     *
     * @return CONTINUE if dir is a subpath of filterPath, SKIP_SUBTREE otherwise
     */
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        FileVisitResult result = FileVisitResult.CONTINUE;
        if (dir.getNameCount() >= filterPath.getNameCount()) {
            if (!dir.startsWith(filterPath)) {
                result = FileVisitResult.SKIP_SUBTREE;
                //System.out.println("Skipping: " + dir.subpath(tckRoot.getNameCount(), dir.getNameCount()));
            }
        }
        return result;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String fileName = file.getFileName().toString();
        // TODO for other test deployment extensions...
        if (fileName.endsWith(".war")) {
            Path warSubpath = file.subpath(tckRoot.getNameCount(), file.getNameCount());
            Path warPkg = warSubpath.subpath(0, warSubpath.getNameCount() - 1);
            String warPkgName = warPkg.toString().replace('/', '.');
            testPkgs.add(warPkgName);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return FileVisitResult.CONTINUE;
    }
}
