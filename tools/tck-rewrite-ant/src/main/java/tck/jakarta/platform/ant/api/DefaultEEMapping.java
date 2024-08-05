package tck.jakarta.platform.ant.api;

public class DefaultEEMapping implements EE11toEE10Mapping {
    // Mappings from EE11 to EE10 package prefixes
    private static final String[] EE11_PKG_PREFIXES = {
            "ee.jakarta.tck.persistence.common.schema30", "com.sun.ts.tests.jpa.common.schema30",
            "ee.jakarta.tck.persistence.core.persistenceUnitUtil","com.sun.ts.tests.jpa.core.persistenceUtilUtil",
            "ee.jakarta.tck.persistence.jpa.ee.packaging.jar", "com.sun.ts.tests.jpa.ee.packaging.jar",
            "ee.jakarta.tck.persistence.entitytest.persist.oneXmanyFetchEager","com.sun.ts.tests.jpa.core.entitytest.persist.oneXmanyFetchEager",
            "ee.jakarta.tck.persistence", "com.sun.ts.tests.jpa"
    };
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
        for (int i = 1; i < EE11_PKG_PREFIXES.length; i += 2) {
            if (ee11Name.startsWith(EE11_PKG_PREFIXES[i])) {
                ee11Name = ee10Name.replace(EE11_PKG_PREFIXES[i], EE11_PKG_PREFIXES[i-1]);
                break;
            }
        }
        return ee11Name;
    }
}
