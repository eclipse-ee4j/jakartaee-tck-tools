package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

import java.util.ArrayList;
import java.util.List;

public class War extends BaseJar {
    List<Lib> libs = new ArrayList<>();

    public War() {
        setArchiveSuffix("war");
    }
    public War(Project project, RuntimeConfigurable taskRC) {
        super(project, taskRC);
    }

    @Override
    public String getType() {
        return "web";
    }

    public record Content(String resPath, String target) {}

    public void addJarResources(TsPackageInfo pkgInfo) {
        libs.addAll(Utils.getJarLibs(this.mapping, pkgInfo));
    }

    /**
     *
     * @return
     */
    public List<Content> getWebContent() {
        List<Content> webContent = new ArrayList<>();
        for(TSFileSet fs : fileSets) {
            if(fs.prefix == null || fs.prefix.isEmpty()) {
                String dir = fs.dir + '/';
                for(String f : fs.includes) {
                    if(!f.endsWith(".class")) {
                        // Strip the path before com/sun/...
                        int index = f.indexOf("com/sun");
                        if(index != -1) {
                            String resPath = f.substring(index);
                            String target = f.replace(dir, "");
                            webContent.add(new Content(resPath, target));
                        }
                    }
                }

            }
        }
        return webContent;
    }
}
