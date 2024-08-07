package tck.jakarta.platform.ant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Common base for the TsPackageInfo and TsTaskInfo classes that supports a map of archives
 */
public class TsBaseInfo {
    private HashMap<String, List<TsArchiveInfo>> archives = new HashMap<>();
    private List<TsFileSet> copyFSSets = new ArrayList<>();

    /**
     * Lookup a archive by its full archive name
     * @param fullArchiveName the full archive name including the .xar suffix
     * @return the Lib object
     */
    public List<TsArchiveInfo> getArchive(String fullArchiveName) {
        return archives.get(fullArchiveName);
    }

    public HashMap<String, List<TsArchiveInfo>> getArchives() {
        return archives;
    }

    public void addArchive(TsArchiveInfo archiveInfo) {
        List<TsArchiveInfo> archiveInfos = archives.computeIfAbsent(archiveInfo.getFullArchiveName(), k -> new ArrayList<>());
        archiveInfos.add(archiveInfo);
    }

    public void addCopyFS(TsFileSet copyFS) {
        copyFSSets.add(copyFS);
    }
    public List<TsFileSet> getCopyFSSets() {
        return copyFSSets;
    }
}
