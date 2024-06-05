package tck.jakarta.platform.ant;

import org.apache.tools.ant.RuntimeConfigurable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

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

    public BaseJar(RuntimeConfigurable taskRC) {
        Hashtable<String,Object> attrs = taskRC.getAttributeMap();
        Object archive = attrs.get("archivename");
        if(archive != null) {
            setArchiveName(archive.toString());
        }
        Object archivesuffix = attrs.get("archivesuffix");
        if(archivesuffix != null) {
            setArchiveSuffix(archivesuffix.toString());
        }
        Object archivefile = attrs.get("archivefile");
        if(archivefile != null) {
            setArchiveFile(archivefile.toString());
        }

        Object manifest = attrs.get("manifest");
        if(manifest != null) {
            this.manifest = manifest.toString();
        }
        Object descriptor = attrs.get("descriptor");
        if(descriptor != null) {
            setDescriptor(descriptor.toString());
        }
        Object descriptordir = attrs.get("descriptordir");
        if(descriptordir != null) {
            setDescriptorDir(descriptordir.toString());
        }
        Object flag = attrs.get("includedefaultfiles");
        if(flag != null) {
            this.includedefaultfiles = Boolean.parseBoolean(flag.toString());
        }
        Object excludes = attrs.get("excludedfiles");
        if(excludes != null) {
            String[] excludeFiles = excludes.toString().split(", ");
            setExcludedFiles(Arrays.asList(excludeFiles));
        }
        Enumeration<RuntimeConfigurable> children = taskRC.getChildren();
        RuntimeConfigurable fileset = null;
        while(children.hasMoreElements()) {
            RuntimeConfigurable rc = children.nextElement();
            if(rc.getElementTag().equals("fileset")) {
                addFileSet(new FileSet(rc));
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
    @Override
    public String toString() {
        return "%s{descriptor=%s, archiveName=%s, excludedFiles=%s}".formatted(getType(), descriptor, archiveName, excludedFiles);
    }
}
