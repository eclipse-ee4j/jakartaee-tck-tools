package tck.jakarta.platform.ant.api;

/**
 * Maps EE11 names to EE10 names.
 */
public interface EE11toEE10Mapping {
    /**
     * Map a package name from EE11 to EE10 to determine the location of the build.xml file.
     * The returned name is relative to the EE10 platform TCK com.sun.ts.tests package under
     * the ts.home/src directory.
     *
     * @param ee11Name - dot package name from the EE11 TCK repository
     * @return the EE10 platform TCK package that starts with com.sun.ts.tests.
     * If the name is not mapped, return the input name.
     */
    String getEE10TestPackageName(String ee11Name);
    /**
     * Map a class or package name from EE10 to EE11.
     * @param ee10Name - .class or dot package name from the EE10 TCK dist
     * @return the EE10 name relative to the EE10 platform TCK com.sun.ts.tests package.
     * If the name is not mapped, return the input name.
     */
    String getEE11Name(String ee10Name);
}
