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

public class DefaultMappingsTest {
    static Path tsHome = Paths.get(System.getProperty("ts.home"));

    @EnabledIfSystemProperty(named = "ts.home", matches = ".*")
    @Test
    public void testClassNMappings() {
        DefaultEEMapping mappings = (DefaultEEMapping) DefaultEEMapping.getInstance();
        Class<?> baseTestClass = ee.jakarta.tck.persistence.core.entityManager.Client2.class;
        String ee10Name = mappings.addTestClassMapping(baseTestClass, tsHome);
        Assertions.assertEquals("com.sun.ts.tests.jpa.core.entityManager.Client.class", ee10Name);

        // TsFileSet(String dir, String prefix, List<String> includes) {
        Path testDir = tsHome.resolve("classes");
        TsFileSet fileSet = new TsFileSet(testDir.toString(), "", List.of("com/sun/ts/tests/jpa/core/entityManager/Client.class"));
        String classes = Utils.getClassFilesString(mappings, List.of(fileSet), new ArrayList<>());
        Assertions.assertEquals(baseTestClass.getName()+".class", classes);
    }
}
