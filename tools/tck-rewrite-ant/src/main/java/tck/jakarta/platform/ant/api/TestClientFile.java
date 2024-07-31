package tck.jakarta.platform.ant.api;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A representation of a generated Arquillian/Junit5 test java file
 */
public class TestClientFile {
    private String name;
    private String pkg;
    private String content;

    /**
     * Create a new TestClientFile
     * @param name - the basename of the file minus the .java extension, e.g., ClientTest
     * @param pkg - the package name, e.g., com.sun.ts.tests.ejb30.misc.sameejbclass
     * @param content - the content of the ClientTest.java file
     */
    public TestClientFile(String name, String pkg, String content) {
        this.name = name;
        this.pkg = pkg;
        this.content = content;
    }

    /**
     * Base name of the file minus the .java extension, e.g., ClientTest
     * @return Base name of the file
     */
    public String getName() {
        return name;
    }

    /**
     * the test class package name, e.g., com.sun.ts.tests.ejb30.misc.sameejbclass
     * @return test class package name
     */
    public String getPackage() {
        return pkg;
    }

    /**
     * Return the relative path of the package in the src tree, e.g., com/sun/ts/tests/ejb30/misc/sameejbclass
     * @return Return the relative path of the package
     */
    public Path getPackagePath() {
        return Paths.get(pkg.replace(".", "/"));
    }

    /**
     * Return the relative path of the package from the root of the project, e.g.,
     * ejb30/src/main/java/com/sun/ts/tests/ejb30/misc/sameejbclass
     * This is useful for writing out the file to the correct location given the test repository root.
     * @return relative path of the package from the root
     */
    public Path getPackagePathFromRoot() {
        Path pkgPath = getPackagePath();
        String module = pkgPath.getName(4).toString();
        return Paths.get(module+"/src/main/java").resolve(pkgPath);
    }

    public String getContent() {
        return content;
    }

    /**
     * Returns the full information about the TestClientFile, including the .java file content
     * @return full string view of the TestClientFile
     */
    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append(String.format("TestClientFile[%s, %s]\n", name, pkg));
        tmp.append("code:\n");
        tmp.append(content);
        return tmp.toString();
    }
}
