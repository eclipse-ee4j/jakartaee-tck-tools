package tck.jakarta.platform.ant;

import com.sun.ts.tests.ejb.ee.bb.session.lrapitest.A;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * Represents the common base of the CTS artifact tasks
 */
public abstract class BaseJar {
    String descriptor;
    String manifest;
    String archiveName;
    String archiveSuffix;
    String archiveFile;
    String descriptordir;
    String internaldescriptorname;
    String internalpermissionsdescriptorname = "META-INF/permissions.xml";
    boolean includedefaultfiles;
    boolean update;
    List<String> excludedFiles;
    List<FileSet> fileSets = new ArrayList<>();
    Project project;

    public BaseJar(Project project, RuntimeConfigurable taskRC) {
        this.project = project;
        Hashtable<String,Object> attrs = taskRC.getAttributeMap();
        AttributeMap attrsMap = new AttributeMap(project, attrs);
        setArchiveName(attrsMap.getAttribute("archivename"));
        setArchiveSuffix(attrsMap.getAttribute("archivesuffix"));
        setArchiveFile(attrsMap.getAttribute("archivefile"));
        this.manifest = attrsMap.getAttribute("manifest");
        setDescriptor(attrsMap.getAttribute("descriptor"));
        setDescriptorDir(attrsMap.getAttribute("descriptordir"));
        this.includedefaultfiles = Boolean.parseBoolean(attrsMap.getAttribute("includedefaultfiles"));

        Object excludes = attrsMap.getAttribute("excludedfiles");
        if(excludes != null) {
            String[] excludeFiles = excludes.toString().split(", ");
            setExcludedFiles(Arrays.asList(excludeFiles));
        }
        Enumeration<RuntimeConfigurable> children = taskRC.getChildren();
        while(children.hasMoreElements()) {
            RuntimeConfigurable rc = children.nextElement();
            AttributeMap attrsMaps = new AttributeMap(project, rc.getAttributeMap());
            if(rc.getElementTag().equals("fileset")) {
                addFileSet(new FileSet(attrsMaps));
            }
        }
    }

    public abstract String getType();

    public String getDescriptor() {
        return descriptor;
    }

    public String getManifest() {
        return manifest;
    }

    public void setManifest(String manifest) {
        this.manifest = manifest;
    }

    public String getDescriptorDir() {
        return descriptordir;
    }

    public String getArchiveSuffix() {
        return archiveSuffix;
    }

    public void setArchiveSuffix(String archiveSuffix) {
        this.archiveSuffix = archiveSuffix;
    }

    public String getArchiveFile() {
        return archiveFile;
    }

    public void setArchiveFile(String archiveFile) {
        this.archiveFile = archiveFile;
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public void setDescriptorDir(String descriptordir) {
        this.descriptordir = descriptordir;
    }

    public boolean isIncludedefaultfiles() {
        return includedefaultfiles;
    }

    public void setIncludedefaultfiles(boolean includedefaultfiles) {
        this.includedefaultfiles = includedefaultfiles;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public void setArchiveName(String archiveName) {
        this.archiveName = archiveName;
    }

    public List<String> getExcludedFiles() {
        return excludedFiles;
    }

    public void setExcludedFiles(List<String> excludedFiles) {
        this.excludedFiles = excludedFiles;
    }

    public List<FileSet> getFileSets() {
        return fileSets;
    }
    public void addFileSet(FileSet fs) {
        fileSets.add(fs);
    }

    public String getRelativeDescriptorPath() {
        return null;
    }
    /**
     * Combine all fileset contents that are *.class files into a comma separted string of dot package name
     * class file references, one per line. This can be passed to a {@link org.jboss.shrinkwrap.api.spec.JavaArchive#addClasses(Class[])}
     * method.
     * @return string of dot package class files, one per line
     */
    public String getClassFilesString() {
        StringBuilder sb = new StringBuilder();
        for(FileSet fs : fileSets) {
            for(String f : fs.includes) {
                if(f.endsWith(".class")) {
                    String clazz = f.replace('/', '.');
                    sb.append(clazz);
                    sb.append(",\n");
                }
            }
        }
        return sb.toString();
    }
    @Override
    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append("%s{descriptor=%s, descriptorDir=%s, archiveName=%s, excludedFiles=%s}".formatted(getType(), descriptor, descriptordir, archiveName, excludedFiles));
        for(FileSet fs : fileSets) {
            tmp.append('\n');
            tmp.append(fs);
        }
        return tmp.toString();
    }
}
