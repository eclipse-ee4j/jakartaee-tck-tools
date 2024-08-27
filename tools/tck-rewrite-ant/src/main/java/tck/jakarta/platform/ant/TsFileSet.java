package tck.jakarta.platform.ant;

import tck.jakarta.platform.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.List;

/**
 * fileset and zipfileset
 * org.apache.tools.ant.types.FileSet
 * org.apache.tools.ant.types.ZipFileSet
 */
public class TsFileSet {
    String dir;
    String prefix;
    List<String> includes;
    VehicleType vehicleType = VehicleType.none;

    /**
     * Build up the fileset from the attributes on the fileset element
     * @param fileset - a fileset configuration map
     */
    TsFileSet(AttributeMap fileset) {
        String dir = fileset.getAttribute("dir");
        if(dir == null) {
            dir = fileset.getAttribute("basedir");
        }
        this.dir = dir;
        this.prefix = fileset.getAttribute("prefix");
        String includes = fileset.getAttribute("includes");
        this.includes = new ArrayList<>();
        if(includes != null) {
            String[] asArray = includes.split(",");
            // Filter out wildcards
            for (String include : asArray) {
                if(include.contains("*")) {
                    continue;
                }
                this.includes.add(include);
            }
        }
    }
    public TsFileSet(String dir, String prefix, List<String> includes) {
        this.dir = dir;
        this.prefix = prefix;
        this.includes = includes;
    }

    public String getDir() {
        return dir;
    }
    public String getPrefix() {
        return prefix;
    }
    public List<String> getIncludes() {
        return includes;
    }
    public boolean isTmpDir() {
        return dir.endsWith("tmp");
    }
    public boolean isCommonDir() {
        return dir.contains("common");
    }
    public boolean isDistDir() {
        return dir.contains("dist");
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }
    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
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
