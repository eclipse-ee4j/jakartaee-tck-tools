package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A wrapper that adds convenience methods to access various information from the ant project
 */
public class ProjectWrapper {
    private static final String ANT_FILE_TYPE = "ant.file.type.";
    private static final String ANT_FILE = "ant.file.";

    private Project project;

    public ProjectWrapper(Project project) {
        this.project = project;
    }

    public List<String> getImportNames() {
        List<String> importNames = new ArrayList<>();
        for(String key : project.getUserProperties().keySet()) {
            if(key.startsWith(ANT_FILE_TYPE)) {
                importNames.add(key.substring(ANT_FILE_TYPE.length()));
            }
        }
        return importNames;
    }
    public List<String> getImports() {
        List<String> importFiles = new ArrayList<>();
        for (String name : getImportNames()) {
            String file = project.getUserProperty(ANT_FILE+name);
            if(file != null) {
                importFiles.add(file);
            }
        }
        return importFiles;
    }
}
