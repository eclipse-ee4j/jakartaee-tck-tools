import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import jakartatck.jar2shrinkwrap.Jar2ShrinkWrap;
import jakartatck.jar2shrinkwrap.JarProcessor;
import org.junit.jupiter.api.Test;

/**
 * Test that the com.sun.ts.tests.jpa.ee.pluggability.contracts.jta pkg
 * has the expected contents.
 */
public class Jar2ShrinkwrapEarTest {

    @Test
    public void canLocateTestDefinitions() {

        JarProcessor ear = Jar2ShrinkWrap.fromPackage("com.sun.ts.tests.jpa.ee.pluggability.contracts.jta");
        List<String> subModules = ear.getSubModules();
        System.out.printf("EAR Modules: %s\n", subModules);
        assertEquals(7, subModules.size()); // jpa_alternate_provider.jar + jpa_ee_pluggability_contracts_jta.jar
        // Write the java source to the console
        StringWriter src = new StringWriter();
        ear.saveOutput(src, false);
        System.out.printf("\nJavaSource:\n%s\n", src.toString());
    }

    @Test void locate_assembly_altDD() {
        JarProcessor ear = Jar2ShrinkWrap.fromPackage("com.sun.ts.tests.assembly.altDD");
        List<String> subModules = ear.getSubModules();
        System.out.printf("EAR Modules: %s\n", subModules);
        assertEquals(2, subModules.size());
        StringWriter src = new StringWriter();
        ear.saveOutput(src, true);
        String result = src.toString();
        assertTrue(result.contains("org.jboss.shrinkwrap.api.spec.EnterpriseArchive"));
        System.out.printf("\nJavaSource:\n%s\n", result);

    }
}
