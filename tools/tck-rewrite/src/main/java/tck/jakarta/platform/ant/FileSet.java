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

    FileSet(RuntimeConfigurable fileset) {
        Object dir = fileset.getAttributeMap().get("dir");
        if(dir != null) {
            this.dir = dir.toString();
        }
        Object prefix = fileset.getAttributeMap().get("prefix");
        if(prefix != null) {
            this.prefix = prefix.toString();
        }
        String includes = fileset.getAttributeMap().get("includes").toString();
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
