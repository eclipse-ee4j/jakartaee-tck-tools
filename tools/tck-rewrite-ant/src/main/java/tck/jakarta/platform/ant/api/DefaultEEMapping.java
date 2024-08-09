package tck.jakarta.platform.ant.api;

import java.nio.file.Path;

public class DefaultEEMapping implements EE11toEE10Mapping {
    private static final DefaultEEMapping INSTANCE = new DefaultEEMapping();
    // Mappings from EE11 to EE10 package prefixes
    private static final String[] EE11_PKG_PREFIXES = {
            "ee.jakarta.tck.persistence.common.schema30", "com.sun.ts.tests.jpa.common.schema30",
            "ee.jakarta.tck.persistence.core.persistenceUnitUtil","com.sun.ts.tests.jpa.core.persistenceUtilUtil",
            "ee.jakarta.tck.persistence.jpa.ee.packaging.jar", "com.sun.ts.tests.jpa.ee.packaging.jar",
            "ee.jakarta.tck.persistence.entitytest.persist.oneXmanyFetchEager","com.sun.ts.tests.jpa.core.entitytest.persist.oneXmanyFetchEager",
            "ee.jakarta.tck.persistence", "com.sun.ts.tests.jpa"
    };
    //  A mapping from EE11 to EE10 class names
    private String[] testClassMappings = {"", ""};

    public static DefaultEEMapping getInstance() {
        return INSTANCE;
    }
    private DefaultEEMapping() {}

    @Override
    public String addTestClassMapping(Class<?> ee11Class, Path tsHome) {
        String ee11Name = ee11Class.getName();
        String ee10Name = null;
        char lastChar = ee11Name.charAt(ee11Name.length() - 1);
        if (Character.isDigit(lastChar)) {
            String simpleNameN = ee11Class.getSimpleName();
            String simpleName = simpleNameN.substring(0, simpleNameN.length() - 1);

            // See if this class exists in EE10
            String ee10Pkg = ee11Class.getPackageName();
            ee10Pkg = getEE10TestPackageName(ee10Pkg);
            String ee10Path = ee10Pkg.replace('.', '/');
            Path ee10TestPathN = tsHome.resolve("classes").resolve(ee10Path).resolve(simpleNameN + ".class");
            Path ee10TestPath = tsHome.resolve("classes").resolve(ee10Path).resolve(simpleName + ".class");
            if (!ee10TestPathN.toFile().exists() && ee10TestPath.toFile().exists()) {
                // Add the mapping
                ee10Name = ee10Pkg + "." + simpleName + ".class";
                testClassMappings[0] = ee11Name + ".class";
                testClassMappings[1] = ee10Name;
            }
        }
        return ee10Name;
    }

    @Override
    public String getEE10TestPackageName(String ee11Name) {
        String ee10Name = ee11Name;
        for (int i = 0; i < EE11_PKG_PREFIXES.length; i += 2) {
            if (ee11Name.startsWith(EE11_PKG_PREFIXES[i])) {
                ee10Name = ee11Name.replace(EE11_PKG_PREFIXES[i], EE11_PKG_PREFIXES[i + 1]);
                break;
            }
        }
        return ee10Name;
    }

    @Override
    public String getEE11Name(String ee10Name) {
        String ee11Name = ee10Name;
        if(ee10Name.equals(testClassMappings[1])) {
            return testClassMappings[0];
        }
        for (int i = 1; i < EE11_PKG_PREFIXES.length; i += 2) {
            if (ee11Name.startsWith(EE11_PKG_PREFIXES[i])) {
                ee11Name = ee10Name.replace(EE11_PKG_PREFIXES[i], EE11_PKG_PREFIXES[i-1]);
                break;
            }
        }
        return ee11Name;
    }
}
