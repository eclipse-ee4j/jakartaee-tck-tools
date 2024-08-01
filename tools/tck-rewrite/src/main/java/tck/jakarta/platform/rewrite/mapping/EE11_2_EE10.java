package tck.jakarta.platform.rewrite.mapping;

import tck.jakarta.platform.ant.api.EE11toEE10Mapping;

/**
 * EE11 to EE 10 mappings
 *
 * @author Scott Marlow
 */
public class EE11_2_EE10 implements EE11toEE10Mapping {

    @Override
    public String getEE10TestPackageName(String ee11Name) {
        return ee11Name.replace("ee.jakarta.tck.persistence.core.persistenceUnitUtil","com.sun.ts.tests.jpa.core.persistenceUtilUtil").
                        replace("ee.jakarta.tck.persistence.entitytest.persist.oneXmanyFetchEager","com.sun.ts.tests.jpa.core.entitytest.persist.oneXmanyFetchEager").
                        replace("ee.jakarta.tck.persistence.core", "com.sun.ts.tests.jpa.core").
                        replace("ee.jakarta.tck.persistence.entitytest", "com.sun.ts.tests.jpa.core.entitytest").
                        replace("ee.jakarta.tck.persistence.jpa22", "com.sun.ts.tests.jpa.jpa22").
                        // TODO: correct or remove replace("ee.jakarta.tck.persistence.se.cache.inherit", "com.sun.ts.tests.jpa.common.schema30").
                        replace("ee.jakarta.tck.persistence.common.schema30","com.sun.ts.tests.jpa.common.schema30").
                        replace("ee.jakarta.tck.persistence.jpa.ee.packaging.jar", "com.sun.ts.tests.jpa.ee.packaging.jar").
                        replace("ee.jakarta.tck.persistence", "com.sun.ts.tests.jpa").  // invoke use this catch all for persistence last
                        replace("ee.jakarta.tck.pages","com.sun.ts.tests.jsp");

    }

    @Override
    public String getEE11Name(String ee10Name) {
        return ee10Name.replace("com.sun.ts.tests.jpa.core","ee.jakarta.tck.persistence.core").
                        replace("com.sun.ts.tests.jpa.core.entitytest","ee.jakarta.tck.persistence.entitytest").
                        replace("com.sun.ts.tests.jpa.jpa22","ee.jakarta.tck.persistence.jpa22").
                        // TODO: correct or remove replace("ee.jakarta.tck.persistence.se.cache.inherit", "com.sun.ts.tests.jpa.common.schema30").
                        replace("com.sun.ts.tests.jpa.common.schema30","ee.jakarta.tck.persistence.common.schema30").
                        replace( "com.sun.ts.tests.jpa.ee.packaging.jar","ee.jakarta.tck.persistence.jpa.ee.packaging.jar").
                        replace( "com.sun.ts.tests.jpa.core.entitytest.persist.oneXmanyFetchEager", "ee.jakarta.tck.persistence.entitytest.persist.oneXmanyFetchEager").
                        replace("com.sun.ts.tests.jpa.core.persistenceUtilUtil","ee.jakarta.tck.persistence.core.persistenceUnitUtil").
                        replace("com.sun.ts.tests.jpa","ee.jakarta.tck.persistence").  // invoke use this catch all for persistence last
                        replace("com.sun.ts.tests.jsp","ee.jakarta.tck.pages");

    }
}
