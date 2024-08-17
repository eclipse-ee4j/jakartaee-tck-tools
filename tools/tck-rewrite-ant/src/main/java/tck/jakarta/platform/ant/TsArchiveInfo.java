package tck.jakarta.platform.ant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A temporary class to hold information about the currently active archive that is being built
 */
public class TsArchiveInfo {
    private String fullArchiveName;
    private List<TsFileSet> fileSets = new ArrayList<>();
    private int order;

    TsArchiveInfo(String fullArchiveName, int order) {
        this.fullArchiveName = fullArchiveName;
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
    public String getFullArchiveName() {
        return fullArchiveName;
    }
    public String getArchiveName() {
        int lastDot = fullArchiveName.lastIndexOf('.');
        if (lastDot == -1) {
            return fullArchiveName;
        }
        return fullArchiveName.substring(0, lastDot);
    }
    public List<TsFileSet> getResources() {
        return fileSets;
    }
    public void addResource(TsFileSet fs) {
        fileSets.add(fs);
    }
    public void addResources(Collection<TsFileSet> fs) {
        fileSets.addAll(fs);
    }

    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append("TsArchiveInfo{fullArchiveName=%s}".formatted(fullArchiveName));
        tmp.append("resources:\n");
        for(TsFileSet fs : fileSets) {
            tmp.append('\n');
            tmp.append(fs);
        }
        return tmp.toString();
    }
}
