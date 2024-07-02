package tck.jakarta.platform.rewrite.mapping;

/**
 * EE11 to EE 10 mappings
 *
 * @author Scott Marlow
 */
public class EE11_2_EE10 {
    public static String mapEE11toEE10(String classname) {
        //
        return classname.replace("ee.jakarta.tck.persistence.core", "com.sun.ts.tests.jpa.core").
                replace("ee.jakarta.tck.persistence.entitytest", "com.sun.ts.tests.jpa.core.entitytest").
                replace("ee.jakarta.tck.persistence.jpa22", "com.sun.ts.tests.jpa.jpa22").
                // TODO: correct or remove replace("ee.jakarta.tck.persistence.se.cache.inherit", "com.sun.ts.tests.jpa.common.schema30").
                replace("ee.jakarta.tck.persistence.common.schema30","com.sun.ts.tests.jpa.common.schema30").
                replace("ee.jakarta.tck.persistence", "com.sun.ts.tests.jpa").  // invoke use this catch all for persistence last
                replace("ee.jakarta.tck.pages","com.sun.ts.tests.jsp");
    }
}
