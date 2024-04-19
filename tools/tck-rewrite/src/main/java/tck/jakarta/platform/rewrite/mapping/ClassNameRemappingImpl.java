package tck.jakarta.platform.rewrite.mapping;

import jakartatck.jar2shrinkwrap.ClassNameRemapping;

/**
 * ClassNameRemappingImpl will map EE 10 TCK test class names to their equivalent EE 11 names
 *
 * @author Scott Marlow
 */
public class ClassNameRemappingImpl implements ClassNameRemapping {

    private String classBeingUpdated;
    private String getClassBeingUpdatedNoClass;
    static final String CLASS = ".class";

    public ClassNameRemappingImpl(String classBeingUpdated) {
        if(!classBeingUpdated.endsWith(CLASS)) {
            classBeingUpdated = classBeingUpdated + CLASS;
        }
        this.classBeingUpdated = classBeingUpdated;
        this.getClassBeingUpdatedNoClass = classBeingUpdated.substring(0, classBeingUpdated.length() - CLASS.length());
    }

    public String getName(String className) {

        className = className.replace("com.sun.ts.tests.jpa.common.schema30", "ee.jakarta.tck.persistence.common.schema30").
                replace("com.sun.ts.tests.jpa", "ee.jakarta.tck.persistence").
                replace("ee.jakarta.tck.persistence.core.query.language.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.criteriaapi.misc.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.criteriaapi.From.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.criteriaapi.metamodelquery.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.criteriaapi.CriteriaBuilder.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.criteriaapi.CriteriaQuery.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.annotations.orderby.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.entityManager2.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.entityManager.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.query.apitests.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.criteriaapi.Join.Client.class", classBeingUpdated).
                replace("ee.jakarta.tck.persistence.core.types.property.Client.class", classBeingUpdated)
                ;
        if (getClassBeingUpdatedNoClass.equals("ee.jakarta.tck.persistence.core.criteriaapi.CriteriaQuery.Client3")) {
            className = className.replace("ee.jakarta.tck.persistence.core.criteriaapi.CriteriaQuery.Client$ExpectedResult.class", getClassBeingUpdatedNoClass + "$ExpectedResult.class");
        } else if (getClassBeingUpdatedNoClass.startsWith("ee.jakarta.tck.persistence.core.criteriaapi.CriteriaQuery.Client")) {

        }



        return className;

    }

    @Override
    public boolean shouldBeIgnored(String className) {
        // do not put TCK vehicle classes in the (ShrinkWrap) test deployment archives
        if(className != null && className.startsWith("com.sun.ts.tests.common.vehicle")) {
            return true;
        }
        return false;
    }

    @Override
    public String getTargetClassName() {
        return classBeingUpdated;
    }
}
