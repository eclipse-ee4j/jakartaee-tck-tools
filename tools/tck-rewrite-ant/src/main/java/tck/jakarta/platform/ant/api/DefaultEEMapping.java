package tck.jakarta.platform.ant.api;

import java.nio.file.Path;

/**
 * Collection of default mappings from EE11 to EE10.
 */
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
    // Packages removed from EE11. Mostly ejb module classes that are not in the EE11 TCK
    private static final String[] EE11_PKG_EXCLUDES = {
            "com.sun.ts.tests.common.vehicle.ejb.EJBVehicleHome",
            "com.sun.ts.tests.common.dao",
            "com.sun.ts.tests.assembly.util.refbean",
            "com.sun.ts.tests.ejb.ee.bb.entity",
            "com.sun.ts.tests.ejb.ee.timer.helper.FlagStore",
            "com.sun.ts.tests.common.ejb.wrappers.CMP11Wrapper",
            "com.sun.ts.tests.common.ejb.wrappers.BMPWrapper",
            "com.sun.ts.tests.common.ejb.wrappers.CMP20Wrapper",
            "com.sun.ts.tests.common.ejb.calleebeans.CMP20CalleeLocal",
            "com.sun.ts.tests.common.ejb.calleebeans.CMP20Callee",
            "com.sun.ts.tests.common.ejb.calleebeans.CMP20CalleeEJB",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.mdbqaccesstest.A",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.mdbqaccesstest.CE",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.mdbqaccesstest.CLocal",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.webaccesstest.A",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.webaccesstest.CE",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.webaccesstest.CLocal",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.mdbtaccesstest.A",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.mdbtaccesstest.CE",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.mdbtaccesstest.CLocal",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.sbaccesstest.A",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.sbaccesstest.CE",
            "com.sun.ts.tests.ejb.ee.bb.localaccess.sbaccesstest.CLocal",

    };
    /**  A mapping from EE11 to EE10 test class names
     * [0] is the EE11 full class name, ee.jakarta.tck.persistence.core.entityManager.Client2
     * [1] is the EE10 full class name, com.sun.ts.tests.jpa.core.entityManager.Client
     */
    private String[] testClassMappings = {"", ""};

    /**
     * Get the singleton instance of the DefaultEEMapping.
     * @return the DefaultEEMapping instance
     */
    public static DefaultEEMapping getInstance() {
        return INSTANCE;
    }
    private DefaultEEMapping() {}

    /**
     * Pass in a class from the EE11 TCK and if the class has been split into multiple classes in the EE11
     * as indicated by a number at the end of the class name, then add a mapping from the EE11 class to the
     * EE10 class.
     * @param ee11Class - the EE11 class
     * @param tsHome - the path to the TCK home directory
     * @return the EE10 class name prefix if the mapping was added, otherwise null
     */
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
                ee10Name = ee10Pkg + "." + simpleName;
                testClassMappings[0] = ee11Name;
                testClassMappings[1] = ee10Name;
            }
        }
        return ee10Name;
    }
    public String getMappedTestClass() {
        String mappedTestClass = null;
        if(!testClassMappings[1].isEmpty()) {
            mappedTestClass = getEE11NameNoTestClassMapping(testClassMappings[1]);
        }
        return mappedTestClass;
    }

    /**
     * Given an ee11 package name, return the equivalent ee10 package name.
     * @param ee11Name - dot package name from the EE11 TCK repository
     * @return the EE10 platform TCK package that starts with com.sun.ts.tests.
     */
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

    /**
     * Given an ee10 class or package name, return the equivalent ee11 class or package name.
     * @param ee10Name - .class or dot package name from the EE10 TCK dist
     * @return the EE11 name
     */
    @Override
    public String getEE11Name(String ee10Name) {
        String ee11Name = ee10Name;
        // First check for a test class mapping
        if(!testClassMappings[1].isEmpty() && ee10Name.startsWith(testClassMappings[1])) {
            // Replace the EE10 name with the EE11 name
            return ee10Name.replace(testClassMappings[1], testClassMappings[0]);
        }
        ee11Name = getEE11NameNoTestClassMapping(ee10Name);
        return ee11Name;
    }
    private String getEE11NameNoTestClassMapping(String ee10Name) {
        String ee11Name = ee10Name;
        // Check for a package mapping
        for (int i = 1; i < EE11_PKG_PREFIXES.length; i += 2) {
            if (ee11Name.startsWith(EE11_PKG_PREFIXES[i])) {
                ee11Name = ee10Name.replace(EE11_PKG_PREFIXES[i], EE11_PKG_PREFIXES[i-1]);
                break;
            }
        }
        return ee11Name;

    }

    public boolean isExcluded(String ee10Class) {
        for (String exclude : EE11_PKG_EXCLUDES) {
            if (ee10Class.startsWith(exclude)) {
                return true;
            }
        }

        return false;
    }

}
