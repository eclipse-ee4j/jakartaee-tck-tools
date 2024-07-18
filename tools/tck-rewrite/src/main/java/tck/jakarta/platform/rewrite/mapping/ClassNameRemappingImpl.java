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

    private String targetClassPackage;

    static final String CLASS = ".class";

    public ClassNameRemappingImpl(String classBeingUpdated) {
        if(!classBeingUpdated.endsWith(CLASS)) {
            classBeingUpdated = classBeingUpdated + CLASS;
        }
        this.classBeingUpdated = classBeingUpdated;
        this.getClassBeingUpdatedNoClass = classBeingUpdated.substring(0, classBeingUpdated.length() - CLASS.length());
        this.targetClassPackage = getClassBeingUpdatedNoClass.substring(0,getClassBeingUpdatedNoClass.lastIndexOf("."));
    }

    /**
     *
     * @param className is an EE 10 TCK test class name
     * @return is an EE 11 TCK test class name
     */
    public String getName(String className) {

        className = className.replace("com.sun.ts.tests.jpa.common.schema30", "ee.jakarta.tck.persistence.common.schema30").
                replace("com.sun.ts.tests.jpa.core.entitytest.persist.oneXmanyFetchEager","ee.jakarta.tck.persistence.entitytest.persist.oneXmanyFetchEager").
                replace("com.sun.ts.tests.jpa", "ee.jakarta.tck.persistence");

        // TODO: also handle ee.jakarta.tck.pages
        return className;

    }

    @Override
    public boolean shouldBeIgnored(String className) {
        if(className.contains("$")) { // ignore Client$ExpectedResult
            return true;
        }

        // do not put TCK vehicle classes in the (ShrinkWrap) test deployment archives
        //if(className != null && className.startsWith("com.sun.ts.tests.common.vehicle")) {
        //    return true;
        // }
        return false;
    }

    @Override
    public String getTargetClassName() {
        return classBeingUpdated;
    }

    public String getTargetClassPackage() {
        return targetClassPackage;
    }

}
