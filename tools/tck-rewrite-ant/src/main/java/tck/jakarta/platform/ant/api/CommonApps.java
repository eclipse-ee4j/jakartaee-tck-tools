package tck.jakarta.platform.ant.api;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import tck.jakarta.platform.vehicles.VehicleType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * A repository of the common apps that are used by the TCK tests. If a test package has a common
 * deployment archive, then it should be defined in the CommonApps.stg file.
 */
public class CommonApps {
    private static final Logger log = Logger.getLogger(CommonApps.class.getName());
    private final HashMap<Path, CommonAppInfo> commonDeployments = new HashMap<>();
    private static CommonApps INSTANCE;

    private final Path tsHome;

    public static class CommonAppInfo {
        String templateName;
        ST methodTemplate;
        ST importsTemplate;

        public CommonAppInfo(String templateName, ST methodTemplate, ST importsTemplate) {
            this.templateName = templateName;
            this.methodTemplate = methodTemplate;
            this.importsTemplate = importsTemplate;
        }

        DeploymentMethodInfo getDeploymentMethod(String simpleClassName) {
            if(methodTemplate.getAttribute("testClient") != null) {
                methodTemplate.remove("testClient");
            }
            methodTemplate.add("testClient", simpleClassName);
            String deploymentMethod = methodTemplate.render();
            List<String> imports = List.of(importsTemplate.render().split("\n"));
            DeploymentMethodInfo methodInfo = new DeploymentMethodInfo(VehicleType.none, imports, deploymentMethod);
            methodInfo.setName(templateName);
            return methodInfo;
        }
        public String toString() {
            return "CommonAppInfo{" + templateName + '}';
        }
    }
    public synchronized static CommonApps getInstance(Path tsHome) throws IOException{
        if (INSTANCE == null) {
            URL resURL = CommonApps.class.getResource("/commonarchives.properties");
            try (InputStreamReader reader = new InputStreamReader(resURL.openStream())) {
                Properties commonArchives = new Properties();
                commonArchives.load(reader);
                parseCommonArchives(tsHome, commonArchives);
            }
        }
        return INSTANCE;
    }

    public static Map<Path, CommonAppInfo> getCommonAppInfos(Path tsHome) throws IOException {
        return Collections.unmodifiableMap(getInstance(tsHome).commonDeployments);
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
            String appPath = commonArchives.getProperty(key);
            // There can be multiple common apps for a package root, separated by space
            String[] apps = appPath.split("\\s");
            //
            String templateName = mapToTemplateName(apps);
            String pkgRoot = key.substring("commonarchives.".length());
            Path pkgRootPath = Path.of(pkgRoot);
            CommonAppInfo appInfo = createAppInfo(templateName, commonAppsGroup);
            commonApps.addCommonDeployment(pkgRootPath, appInfo);
        }
        INSTANCE = commonApps;
    }

    /**
     * The template name is either the app path file name if there is a single app, or the
     * concatenation of the app path file names if there are multiple apps.
     * @param apps - relative paths to the common app files src dir
     * @return the template name
     */
    private static String mapToTemplateName(String[] apps) {
        String templateName = null;
        if(apps.length == 1) {
            Path appPath = Path.of(apps[0]);
            templateName = appPath.getFileName().toString();
        } else {
            StringBuilder sb = new StringBuilder();
            Path firstApp = Path.of(apps[0]);
            sb.append(firstApp.getName(firstApp.getNameCount()-2).toString());
            sb.append('_');
            for (String app : apps) {
                Path appPath = Path.of(app);
                sb.append(appPath.getFileName().toString());
                sb.append('_');
            }
            sb.setLength(sb.length() - 1);
            templateName = sb.toString();
        }
        return templateName;
    }

    private static CommonAppInfo createAppInfo(String templateName, STGroup commonAppsGroup) {
        ST methodTemplate = commonAppsGroup.getInstanceOf("/get_"+templateName);
        ST importsTemplate = commonAppsGroup.getInstanceOf("/imports_"+templateName);
        CommonAppInfo appInfo = null;
        if(methodTemplate != null) {
            appInfo = new CommonAppInfo(templateName, methodTemplate, importsTemplate);
        } else {
            log.fine("No deployment method found for "+templateName);
        }
        return appInfo;
    }


    private CommonApps(Path tsHome) {
        this.tsHome = tsHome;
    }
    void addCommonDeployment(Path pkgRoot, CommonAppInfo appInfo) {
        commonDeployments.put(pkgRoot, appInfo);
    }

    /**
     * Get the common deployment method for a given package root.
     * @param pkgRoot
     * @return the deployment method info, possibly null if no common deployment is defined.
     */
    public DeploymentMethodInfo getCommonDeployment(Path pkgRoot, String simpleClassName) {
        DeploymentMethodInfo methodInfo = null;
        if(pkgRoot.isAbsolute()) {
            Path src = tsHome.resolve("src");
            pkgRoot = src.relativize(pkgRoot);
        }
        do {
            if (commonDeployments.containsKey(pkgRoot)) {
                CommonAppInfo appInfo = commonDeployments.get(pkgRoot);
                methodInfo = appInfo.getDeploymentMethod(simpleClassName);
                break;
            }
            pkgRoot = pkgRoot.getParent();
        } while (pkgRoot != null && !pkgRoot.getFileName().endsWith("com"));
        return methodInfo;
    }
}

