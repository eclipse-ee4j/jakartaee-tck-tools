package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

public class War extends BaseJar {

    public War(Project project, RuntimeConfigurable taskRC) {
        super(project, taskRC);
    }

    @Override
    public String getType() {
        return "war";
    }

    /**
     *
     * @return
     */
    public String getWebContent() {
        StringBuilder sb = new StringBuilder();
        for(TSFileSet fs : fileSets) {
            if(fs.prefix == null || fs.prefix.isEmpty()) {
                String dir = fs.dir + '/';
                for(String f : fs.includes) {
                    if(!f.endsWith(".class")) {
                        f = f.replace(dir, "");
                        String clazz = f.replace('/', '.').replace('$', '.');
                        sb.append(clazz);
                        sb.append(",\n");
                    }
                }

            }
        }
        return sb.toString();
    }
}
