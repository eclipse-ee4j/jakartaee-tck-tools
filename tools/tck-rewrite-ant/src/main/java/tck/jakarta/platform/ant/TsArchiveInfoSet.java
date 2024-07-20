package tck.jakarta.platform.ant;

import java.util.List;

/**
 * A collection of TSArchiveInfo objects for the indicated package target. The archives are
 * ordered in order they were created.
 * @param targetName
 * @param archives
 */
public record TsArchiveInfoSet(String targetName, List<TsArchiveInfo> archives) {
}
