package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.RuntimeConfigurable;

import java.nio.file.Path;
import java.nio.file.Paths;
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
    public String getSunDescriptorSuffix() {
        return ".war.sun-web.xml";
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
        for(TsFileSet fs : fileSets) {
            String dir = fs.dir + '/';
            Path dirPath = Paths.get(dir);
            for(String f : fs.includes) {
                if(!f.endsWith(".class") && !f.endsWith("EJBLiteJSPTag.java.txt")) {
                    Path resPath = dirPath.resolve(f);
                    f = resPath.toString();
                    // Strip the path before com/sun/...
                    int index = f.indexOf("com/sun");
                    if(index != -1) {
                        String contentPath = f.substring(index);
                        String target = f.replace(dir, "");
                        if(fs.prefix != null) {
                            target = fs.prefix + "/" + target;
                        } else if(isDescriptor(f)) {
                            target = "WEB-INF/" + target;
                        }
                        Content content = new Content(contentPath, target);
                        if(!webContent.contains(content)) {
                            webContent.add(content);
                        }
                    }
                }
            }
        }
        // Sort by target
        webContent.sort((a, b) -> a.target.compareTo(b.target));
        return webContent;
    }
    private boolean isDescriptor(String f) {
        return f.endsWith(".xml");
    }
}
