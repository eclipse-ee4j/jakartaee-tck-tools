package tck.jakarta.platform.ant.api;

import com.sun.ts.tests.ejb30.assembly.initorder.warejb.Client;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * A repository of the common apps that are used by the TCK tests. If a test package has a common
 * deployment archive, then it should be defined here, and
 */
public class CommonApps {
    private static Logger log = Logger.getLogger(CommonApps.class.getName());
    private static CommonApps INSTANCE;

    private HashMap<Path, DeploymentMethodInfo> commonDeployments = new HashMap<>();
    private Path tsHome;

    public static CommonApps getInstance(Path tsHome) throws IOException{
        if (INSTANCE == null) {
            Path commonArchivesPath = tsHome.resolve("src/com/sun/ts/lib/harness/commonarchives.properties");
            FileReader reader = new FileReader(commonArchivesPath.toFile());
            Properties commonArchives = new Properties();
            commonArchives.load(reader);
            parseCommonArchives(tsHome, commonArchives);
        }
        return INSTANCE;
    }

    /**
     *
     * @param tsHome - EE10 tck root
     * @param commonArchives - commonarchives.properties
     */
    private static void parseCommonArchives(Path tsHome, Properties commonArchives) {
        CommonApps commonApps = new CommonApps(tsHome);
        STGroup commonAppsGroup = new STGroupFile("CommonApps.stg");

        /* The key is of the form  commonarchives.pkg_root=common_app_path, e.g.:
        commonarchives.com/sun/ts/tests/webservices12/specialcases/clients/j2w/doclit/defaultserviceref
         */
        for (String key : commonArchives.stringPropertyNames()) {
            // Skip the webservices tests that are to be archived
            if(key.contains("webservice")) {
                continue;
            }
            String appPath = commonArchives.getProperty(key);
            String pkgRoot = key.substring("commonarchives.".length());
            Path pkgRootPath = Path.of(pkgRoot);
            Path appBuildXml = tsHome.resolve("src/"+appPath+"/build.xml");
            DeploymentMethodInfo methodInfo = parseBuildXml(tsHome, appBuildXml, commonAppsGroup);
            commonApps.addCommonDeployment(pkgRootPath, methodInfo);
        }
        INSTANCE = commonApps;
    }

    private static DeploymentMethodInfo parseBuildXml(Path tsHome, Path appBuildXml, STGroup commonAppsGroup) {
        if(!Files.exists(appBuildXml)) {
            log.fine("No build.xml found for "+appBuildXml);
            return null;
        }
        Project project = new Project();
        project.init();
        project.setProperty("ts.home", tsHome.toAbsolutePath().toString());
        ProjectHelper.configureProject(project, appBuildXml.toFile());
        String deploymentName = project.getProperty("app.name");
        ST methodTemplate = commonAppsGroup.getInstanceOf("/get_"+deploymentName);
        ST importsTemplate = commonAppsGroup.getInstanceOf("/imports_"+deploymentName);
        DeploymentMethodInfo methodInfo = null;
        if(methodTemplate != null) {
            String deploymentMethod = methodTemplate.render();
            List<String> imports = List.of(importsTemplate.render().split("\n"));
            methodInfo = new DeploymentMethodInfo(VehicleType.none, imports, deploymentMethod);
            methodInfo.setName(deploymentName);
        } else {
            log.fine("No deployment method found for "+appBuildXml);
        }
        return methodInfo;
    }


    private CommonApps(Path tsHome) {
        this.tsHome = tsHome;
    }
    void addCommonDeployment(Path pkgRoot, DeploymentMethodInfo deploymentMethodInfo) {
        commonDeployments.put(pkgRoot, deploymentMethodInfo);
    }

    /**
     * Get the common deployment method for a given package root.
     * @param pkgRoot
     * @return the deployment method info, possibly null if no common deployment is defined.
     */
    public DeploymentMethodInfo getCommonDeployment(Path pkgRoot) {
        DeploymentMethodInfo methodInfo = null;
        if(pkgRoot.isAbsolute()) {
            Path src = tsHome.resolve("src");
            pkgRoot = src.relativize(pkgRoot);
        }
        do {
            if (commonDeployments.containsKey(pkgRoot)) {
                methodInfo = commonDeployments.get(pkgRoot);
                break;
            }
            pkgRoot = pkgRoot.getParent();
        } while (pkgRoot != null && !pkgRoot.getFileName().endsWith("com"));
        return methodInfo;
    }
}

