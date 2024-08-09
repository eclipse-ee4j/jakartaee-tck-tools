package tck.conversion.ant.api;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import tck.jakarta.platform.ant.TsFileSet;
import tck.jakarta.platform.ant.Utils;
import tck.jakarta.platform.ant.api.DefaultEEMapping;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@EnabledIfSystemProperty(named = "ts.home", matches = ".*")
public class DefaultMappingsTest {
    static Path tsHome = Paths.get(System.getProperty("ts.home"));

    @Test
    public void testClassNMappings() {
        DefaultEEMapping mappings = DefaultEEMapping.getInstance();
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.entityManager.Client2.class;
        String ee10Name = mappings.addTestClassMapping(baseTestClass, tsHome);
        Assertions.assertEquals("com.sun.ts.tests.jpa.core.entityManager.Client", ee10Name);

        // TsFileSet(String dir, String prefix, List<String> includes) {
        Path testDir = tsHome.resolve("classes");
        TsFileSet fileSet = new TsFileSet(testDir.toString(), "", List.of("com/sun/ts/tests/jpa/core/entityManager/Client.class"));
        String classes = Utils.getClassFilesString(mappings, List.of(fileSet), new ArrayList<>());
        Assertions.assertEquals(baseTestClass.getName()+".class", classes);
    }

    @Test
    public void testJpaMappings() {
        DefaultEEMapping mappings = DefaultEEMapping.getInstance();
        Class<?> baseTestClass = ee.jakarta.tck.persistence.ee.propagation.cm.extended.Client.class;
        String ee10Name = mappings.addTestClassMapping(baseTestClass, tsHome);
        Assertions.assertNull(ee10Name);

        // TsFileSet(String dir, String prefix, List<String> includes) {
        Path testDir = tsHome.resolve("classes");
        TsFileSet fileSet = new TsFileSet(testDir.toString(), "", List.of("com/sun/ts/tests/jpa/ee/propagation/cm/extended/Client.class"));
        String classes = Utils.getClassFilesString(mappings, List.of(fileSet), new ArrayList<>());
        Assertions.assertEquals(baseTestClass.getName()+".class", classes);
    }

    /**
     * https://github.com/eclipse-ee4j/jakartaee-tck-tools/issues/97
     */
    @Test
    public void testClassNNestedMappings() {
        DefaultEEMapping mappings = DefaultEEMapping.getInstance();
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.criteriaapi.CriteriaQuery.Client1.class;
        String ee10Name = mappings.addTestClassMapping(baseTestClass, tsHome);
        Assertions.assertEquals("com.sun.ts.tests.jpa.core.criteriaapi.CriteriaQuery.Client", ee10Name);

        // TsFileSet(String dir, String prefix, List<String> includes) {
        Path testDir = tsHome.resolve("classes");
        ArrayList<String> includes = new ArrayList<>();
        includes.add("com/sun/ts/tests/jpa/core/criteriaapi/CriteriaQuery/Client.class");
        includes.add("com/sun/ts/tests/jpa/core/criteriaapi/CriteriaQuery/Client$ExpectedResult.class");
        TsFileSet fileSet = new TsFileSet(testDir.toString(), "", includes);
        String classes = Utils.getClassFilesString(mappings, List.of(fileSet), new ArrayList<>());
        System.out.println(classes);
        Assertions.assertTrue(classes.contains(baseTestClass.getName()+".class"));
        Assertions.assertTrue(classes.contains(baseTestClass.getName()+".ExpectedResult.class"));
    }
}
