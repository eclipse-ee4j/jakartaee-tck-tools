package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;
import tck.jakarta.platform.ant.api.EE11toEE10Mapping;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
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
    // Base name of the archive
    String baseName;
    boolean includedefaultfiles;
    // Update an archive if it exists
    boolean update;
    // A comma separated list of file expressions to exclude from the set of default included files  This list of file expressions is relative to the TS_HOME/classes directory.
    List<String> excludedFiles;
    //
    List<TsFileSet> fileSets = new ArrayList<>();
    private List<TsFileSet> copyFSSets = new ArrayList<>();

    List<String> anonymousClasses = new ArrayList<>();
    Project project;
    // An override to descriptor that can be set by vehicles
    String vehicleDescriptor;
    String extraClientClass = null;
    // A mapping from EE11 to EE10 names
    EE11toEE10Mapping mapping;

    public BaseJar() {}

    public BaseJar(Project project, RuntimeConfigurable taskRC) {
        this.project = project;
        if(taskRC == null) {
            return;
        }

        Hashtable<String,Object> attrs = taskRC.getAttributeMap();
        AttributeMap attrsMap = new AttributeMap(project, attrs);
        this.archiveName = attrs.get("archivename").toString();
        int index = archiveName.indexOf("${vehicle.name}");
        if(index > 0) {
            this.baseName = archiveName.substring(0, index);
        } else {
            this.baseName = attrsMap.getAttribute("archivename");
        }
        // Now use the attrsMap to resolve any property refs
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
                addFileSet(new TsFileSet(attrsMaps));
            }
        }
    }

    public EE11toEE10Mapping getMapping() {
        return mapping;
    }

    public void setMapping(EE11toEE10Mapping mapping) {
        this.mapping = mapping;
    }

    public String getExtraClientClass() {
        return extraClientClass;
    }
    public void setExtraClientClass(String extraClientClass) {
        this.extraClientClass = extraClientClass;
    }

    public abstract String getType();
    public abstract String getSunDescriptorSuffix();

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
    public String getDescriptorNoXml() {
        String noXml = descriptor;
        if(descriptor != null && descriptor.endsWith(".xml")) {
            noXml = descriptor.substring(0, descriptor.length()-4);
        }
        return noXml;
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
        String varName = archiveName + '_' + getType();
        varName = Utils.getSafeVarName(varName);
        return varName;
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

    public List<TsFileSet> getFileSets() {
        return fileSets;
    }
    public void addFileSet(TsFileSet fs) {
        if(fs != null) {
            fileSets.add(fs);
        }
    }
    public void addFileSet(Collection<TsFileSet> fs) {
        fileSets.addAll(fs);
    }
    public void addCopyFileSet(TsFileSet fs) {
        copyFSSets.add(fs);
    }

    /**
     * Used to scan the filesets to see if the indicated archive file is present as an include
     * @param archiveName - the name of the archive file to search for
     * @return true if the archive file is found in the filesets
     */
    public boolean hasFile(String archiveName) {
        for (TsFileSet fs : fileSets) {
            for (String include : fs.getIncludes()) {
                if(include.endsWith(archiveName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the list of descriptor paths found in the filesets
     * @return list of descriptor paths
     */
    public List<String> getFoundDescriptors() {
        HashSet<String> descriptorSet = new HashSet<>();
        ArrayList<String> descriptorList = new ArrayList<>();
        ArrayList<String> commonDescriptors = new ArrayList<>();
        for (TsFileSet fs : copyFSSets) {
            Path dir = Path.of(fs.getDir());
            String appDescriptor = baseName + getDescriptor();
            Path descriptorPath = dir.resolve(appDescriptor);
            if(descriptorPath.toFile().exists() && !descriptorSet.contains(descriptorPath.toString())) {
                descriptorSet.add(descriptorPath.toString());
                descriptorList.add(0, descriptorPath.toString());
            }
            appDescriptor += getSunDescriptorSuffix();
            descriptorPath = dir.resolve(appDescriptor);
            if(descriptorPath.toFile().exists() && !descriptorSet.contains(descriptorPath.toString())) {
                descriptorSet.add(descriptorPath.toString());
                descriptorList.add(0, descriptorPath.toString());
            }
            descriptorPath = dir.resolve(getDescriptor());
            if(descriptorPath.toFile().exists() && !descriptorSet.contains(descriptorPath.toString())) {
                descriptorSet.add(descriptorPath.toString());
                if(fs.isCommonDir()) {
                    commonDescriptors.add(descriptorPath.toString());
                } else {
                    descriptorList.add(descriptorPath.toString());
                }
            }
        }
        for (TsFileSet fs : fileSets) {
            Path dir = Path.of(fs.getDir());
            // Skip the tmp dir
            if(fs.isTmpDir() || getDescriptor() == null || getDescriptor().isEmpty()) {
                continue;
            }
            String appDescriptor = getDescriptor();
            Path descriptorPath = dir.resolve(appDescriptor);
            if(descriptorPath.toFile().exists() && !descriptorSet.contains(descriptorPath.toString())) {
                descriptorSet.add(descriptorPath.toString());
                // Insert app specific descriptors at the front
                descriptorList.add(0, descriptorPath.toString());
            }
            appDescriptor = baseName + getDescriptor();
            descriptorPath = dir.resolve(appDescriptor);
            if(descriptorPath.toFile().exists() && !descriptorSet.contains(descriptorPath.toString())) {
                descriptorSet.add(descriptorPath.toString());
                // Insert app specific descriptors at the front
                descriptorList.add(0, descriptorPath.toString());
            }
            appDescriptor = getDescriptorNoXml() + getSunDescriptorSuffix();
            descriptorPath = dir.resolve(appDescriptor);
            if(descriptorPath.toFile().exists() && !descriptorSet.contains(descriptorPath.toString())) {
                descriptorSet.add(descriptorPath.toString());
                descriptorList.add(descriptorPath.toString());
            }
            appDescriptor = baseName + getSunDescriptorSuffix();
            descriptorPath = dir.resolve(appDescriptor);
            if(descriptorPath.toFile().exists() && !descriptorSet.contains(descriptorPath.toString())) {
                descriptorSet.add(descriptorPath.toString());
                descriptorList.add(descriptorPath.toString());
            }
            descriptorPath = dir.resolve(getDescriptor());
            if(descriptorPath.toFile().exists() && !descriptorSet.contains(descriptorPath.toString())) {
                descriptorSet.add(descriptorPath.toString());
                if(fs.isCommonDir()) {
                    commonDescriptors.add(descriptorPath.toString());
                } else {
                    descriptorList.add(descriptorPath.toString());
                }
            }
        }
        // Add the common descriptors to the end
        descriptorList.addAll(commonDescriptors);
        // Now remove the path before com/sun/ts/... from the descriptor path
        for(int i = 0; i < descriptorList.size(); i++) {
            String descriptor = descriptorList.get(i);
            int start = descriptor.indexOf("/com/");
            if(start != -1) {
                descriptor = descriptor.substring(start);
                descriptorList.set(i, descriptor);
            }
        }
        return descriptorList;
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

        if(getDescriptor() == null || getDescriptor().isEmpty()) {
            return null;
        }

        String relativePath = getDescriptorDir();
        if(relativePath != null) {
            // Start is the com/sun/... path
            int start = relativePath.indexOf("/com/");
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
     * Combine all fileset contents that are *.class files into a comma separated string of dot package name
     * class file references, one per line. This can be passed to a {@link org.jboss.shrinkwrap.api.spec.JavaArchive#addClasses(Class[])}
     * method.
     * @return string of dot package class files, one per line
     */
    public String getClassFilesString() {
        anonymousClasses.clear();
        String classes = Utils.getClassFilesString(mapping, fileSets, anonymousClasses);
        if(extraClientClass != null) {
            classes += ",\n" + extraClientClass;
        }
        return classes;
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
        for(TsFileSet fs : fileSets) {
            tmp.append('\n');
            tmp.append(fs);
        }
        return tmp.toString();
    }

}
