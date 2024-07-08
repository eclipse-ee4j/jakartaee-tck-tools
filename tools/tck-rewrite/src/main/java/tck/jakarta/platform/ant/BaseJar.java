package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.types.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * Represents the common base of the ts.* artifact tasks
 */
public abstract class BaseJar {
    // Deployment descriptor added to the archive if the archive requires a deployment descriptor
    String descriptor;
    // Manifest to be used within the archive, optional
    String manifest;
    // Archive name minus the file suffix
    String archiveName;
    // Suffix for the archive name
    String archiveSuffix;
    // Full path to archive to be created, default="${dist.dir}/${pkg.dir}/@{archivename}@{archivesuffix}"
    String archiveFile;
    // Directory containing the deployment descriptor, default = ${src.dir}/${pkg.dir}
    String descriptordir;
    // Descriptor path and name within the archive
    String internaldescriptorname;
    // Permissions descriptor path and name within the archive
    String internalpermissionsdescriptorname = "META-INF/permissions.xml";
    boolean includedefaultfiles;
    // Update an archive if it exists
    boolean update;
    // A comma separated list of file expressions to exclude from the set of default included files  This list of file expressions is relative to the TS_HOME/classes directory.
    List<String> excludedFiles;
    //
    List<TSFileSet> fileSets = new ArrayList<>();
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
                addFileSet(new TSFileSet(attrsMaps));
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

    /**
     * @return the archiveName + archiveSuffix
     */
    public String getFullArchiveName() {
        return archiveName + archiveSuffix;
    }
    /**
     * @return the archiveName + '_' + getType()
     */
    public String getTypedArchiveName() {
        return archiveName + '_' + getType();
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

    public List<TSFileSet> getFileSets() {
        return fileSets;
    }
    public void addFileSet(TSFileSet fs) {
        fileSets.add(fs);
    }

    /**
     * Override the parsed definition of the filesets with the exact resources that went into
     * the jar based on the jar task information
     * @param taskInfo - the resources are those found in the last jar task build event
     */
    public void addJarResources(TsTaskInfo taskInfo) {
        this.archiveName = taskInfo.getArchiveName();
        fileSets.clear();
        fileSets.addAll(taskInfo.getResources());
    }

    /**
     * The path of the descriptor based on descriptorDir with everything before the com/sun/ts/... path removed
     * @return possibly null path to jar descriptor
     */
    public String getRelativeDescriptorPath() {
        String relativePath = getDescriptorDir();
        if(relativePath != null) {
            // Start is the com/sun/... path
            int start = relativePath.lastIndexOf("com");
            relativePath = relativePath.substring(start);
            relativePath += "/" + getDescriptor();
        }
        return relativePath;
    }
    /**
     * Combine all fileset contents that are *.class files into a comma separted string of dot package name
     * class file references, one per line. This can be passed to a {@link org.jboss.shrinkwrap.api.spec.JavaArchive#addClasses(Class[])}
     * method.
     * @return string of dot package class files, one per line
     */
    public String getClassFilesString() {
        StringBuilder sb = new StringBuilder();
        for(TSFileSet fs : fileSets) {
            String dir = fs.dir + '/';
            for(String f : fs.includes) {
                if(f.endsWith(".class")) {
                    f = f.replace(dir, "");
                    String clazz = f.replace('/', '.').replace('$', '.');
                    sb.append(clazz);
                    sb.append(",\n");
                }
            }
        }
        return sb.toString();
    }

    public String getArtifactName() {
        String artifactName = null;
        if(archiveName != null) {
            artifactName = archiveName + archiveSuffix;
        }
        return artifactName;
    }
    @Override
    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append("%s{descriptor=%s, descriptorDir=%s, archiveName=%s, excludedFiles=%s}".formatted(getType(), descriptor, descriptordir, archiveName, excludedFiles));
        for(TSFileSet fs : fileSets) {
            tmp.append('\n');
            tmp.append(fs);
        }
        return tmp.toString();
    }
}
