package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
    List<String> anonymousClasses = new ArrayList<>();
    Project project;
    // An override to descriptor that can be set by vehicles
    String vehicleDescriptor;

    public BaseJar() {}

    public BaseJar(Project project, RuntimeConfigurable taskRC) {
        this.project = project;
        if(taskRC == null) {
            return;
        }

        Hashtable<String,Object> attrs = taskRC.getAttributeMap();
        AttributeMap attrsMap = new AttributeMap(project, attrs);
        setArchiveName(attrsMap.getAttribute("archivename"));
        setArchiveSuffix(attrsMap.getAttribute("archivesuffix"));
        if(this.archiveSuffix == null) {
            this.archiveSuffix = "_"+getType();
        }
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

    public void updateFromComponentAttrs(AttributeMap attrs) {
        for (String key : attrs.getAttributes().keySet()) {
            String value = attrs.getAttribute(key);
            switch (key) {
                case "archivename":
                    setArchiveName(value);
                    break;
                case "archivesuffix":
                    setArchiveSuffix(value);
                    break;
                case "descriptor":
                    setDescriptor(value);
                    break;
                case "manifest":
                    setManifest(value);
                    break;
                case "descriptordir":
                    // If this is not under the src dir, ignore it
                    if(value.contains("src/com/sun")) {
                        setDescriptorDir(value);
                    }
                    break;
                case "internaldescriptorname":
                    setInternalDescriptorName(value);
                    break;
                case "mainclass":
                    MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
                    MethodType mt = MethodType.methodType(void.class, String.class);
                    try {
                        MethodHandle setMainClass = publicLookup.findVirtual(getClass(), "setMainClass", mt);
                        setMainClass.invokeExact(this, value);
                    } catch (Throwable e) {
                        // Ignore
                    }
                    break;
            }
        }
    }

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

    public String getInternalDescriptorName() {
        return internaldescriptorname;
    }

    public void setInternalDescriptorName(String internaldescriptorname) {
        this.internaldescriptorname = internaldescriptorname;
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
        if(fs != null) {
            fileSets.add(fs);
        }
    }
    public void addFileSet(Collection<TSFileSet> fs) {
        fileSets.addAll(fs);
    }

    /**
     * Override the parsed definition of the filesets with the exact resources that went into
     * the jar based on the jar task information
     * @param taskInfo - the resources are those found in the last jar task build event
     */
    public void addJarResources(TsTaskInfo taskInfo) {
        fileSets.clear();
        // TODO, verify that all archives have the same archiveName
        List<TsArchiveInfo> allArchives = taskInfo.getArchives().values().stream().flatMap(Collection::stream).toList();
        for(TsArchiveInfo archive : allArchives) {
            fileSets.addAll(archive.getResources());
        }
    }
    public void addJarResources(TsPackageInfo pkgInfo) {
    }

    public void setVehicleDescriptor(String resPath) {
        this.vehicleDescriptor = resPath;
        if(this.vehicleDescriptor.startsWith("/")) {
            this.vehicleDescriptor = this.vehicleDescriptor.substring(1);
        }
    }

    /**
     * The path of the descriptor based on descriptorDir with everything before the com/sun/ts/... path removed
     * @return possibly null path to jar descriptor
     */
    public String getRelativeDescriptorPath() {
        // If there is a vehicleDescriptor just use that
        if(vehicleDescriptor != null) {
            return vehicleDescriptor;
        }

        String relativePath = getDescriptorDir();
        if(relativePath != null) {
            // Start is the com/sun/... path
            int start = relativePath.indexOf("com/");
            if(start != -1) {
                relativePath = relativePath.substring(start);
            }
            relativePath += "/" + getDescriptor();
            if(relativePath.startsWith("/")) {
                relativePath = relativePath.substring(1);
            }
        }
        return relativePath;
    }
    public String getRelativeDescriptorPathNoXml() {
        String relativePath = getRelativeDescriptorPath();
        if(relativePath != null && relativePath.endsWith(".xml")) {
            relativePath = relativePath.substring(0, relativePath.length()-4);
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
        anonymousClasses.clear();
        return Utils.getClassFilesString(fileSets, anonymousClasses);
    }
    public boolean getHasAnonymousClasses() {
        return !anonymousClasses.isEmpty();
    }
    public List<String> getAnonymousClasses() {
        return anonymousClasses;
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
        tmp.append("%s{descriptor=%s, descriptorDir=%s, archiveName=%s, archivesuffix=%s, fullArchiveName=%s, excludedFiles=%s}".formatted(getType(),
                descriptor, descriptordir, archiveName, archiveSuffix, getFullArchiveName(), excludedFiles));
        for(TSFileSet fs : fileSets) {
            tmp.append('\n');
            tmp.append(fs);
        }
        return tmp.toString();
    }

}
