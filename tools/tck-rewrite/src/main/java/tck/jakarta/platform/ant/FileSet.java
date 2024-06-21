package tck.jakarta.platform.ant;

import org.apache.tools.ant.RuntimeConfigurable;

import java.util.Arrays;
import java.util.List;

/**
 * fileset and zipfileset
 * org.apache.tools.ant.types.FileSet
 * org.apache.tools.ant.types.ZipFileSet
 */
public class FileSet {
    String dir;
    String prefix;
    List<String> includes;

    FileSet(AttributeMap fileset) {
        this.dir = fileset.getAttribute("dir");
        this.prefix = fileset.getAttribute("prefix");
        String includes = fileset.getAttribute("includes");
        String[] asArray = includes.split(",");
        this.includes = Arrays.asList(asArray);
    }

    @Override
    public String toString() {
        return "FileSet{" +
                "dir='" + dir + '\'' +
                ", prefix='" + prefix + '\'' +
                ", includes=" + includes +
                '}';
    }
}
