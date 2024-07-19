package tck.conversion.ant.st4;

import com.sun.ts.tests.ejb.ee.bb.session.lrapitest.A;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import tck.jakarta.platform.ant.Rar;
import tck.jakarta.platform.ant.TSFileSet;
import tck.jakarta.platform.ant.War;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests of stringtemplate4 features
 * https://github.com/antlr/stringtemplate4/blob/master/doc/cheatsheet.md
 */
public class StringTemplateTests {
    STGroup testGroup = new STGroupFile("StringTemplate4Tests.stg");
    @Test
    public void testIf() {
        //System.out.println(ejbJarGroup.show());
        ST template = testGroup.getInstanceOf("testIf");
        template.add("attr1", "attr1Value");
        template.add("attr2", "attr2Value");
        String out = template.render().trim();
        Assertions.assertEquals("Saw attr1Value", out);

        template.remove("attr1");
        out = template.render().trim();
        Assertions.assertEquals("Saw attr2Value", out);

        template.remove("attr2");
        out = template.render().trim();
        Assertions.assertEquals("Saw default", out);

    }

    @Test
    public void testAltIf() {
        // (!a||b)&&!(c||d) == broken else works
        ST template = testGroup.getInstanceOf("testAltIf");
        template.add("a", Boolean.TRUE);
        template.add("b", Boolean.FALSE);
        template.add("c", Boolean.FALSE);
        template.add("d", Boolean.FALSE);
        String out = template.render().trim();
        Assertions.assertEquals("works", out);
        template.remove("a");
        out = template.render().trim();
        Assertions.assertEquals("broken", out);

        // Just a
        template = testGroup.getInstanceOf("testAltIf");
        template.add("a", "value");
        out = template.render().trim();
        Assertions.assertEquals("works", out);
        // Just b
        template = testGroup.getInstanceOf("testAltIf");
        template.add("b", "value");
        out = template.render().trim();
        Assertions.assertEquals("broken", out);
        // Just c
        template = testGroup.getInstanceOf("testAltIf");
        template.add("c", "value");
        out = template.render().trim();
        Assertions.assertEquals("works", out);
        // Just d
        template = testGroup.getInstanceOf("testAltIf");
        template.add("d", "value");
        out = template.render().trim();
        Assertions.assertEquals("works", out);
    }

    @Test
    public void testMethod() {
        ST template = testGroup.getInstanceOf("method");
        template.add("name", "multx2");
        template.add("body", "x*=2;");
        template.add("return", "return x;");
        template.add("arg", "x");
        String out = template.render().trim();
        System.out.println(out);
        String expected = """
int multx2(int x) {
  x*=2;
  return x;
}
""";
        Assertions.assertEquals(expected.trim(), out);
    }

    @Test
    public void testMult2x() {
        ST template = testGroup.getInstanceOf("mult2x");
        template.add("arg", "x");
        String out = template.render().trim();
        System.out.println(out);
        String expected = """
int mult2x(int x) {
  x*=2;
  return x;
}
""";
        Assertions.assertEquals(expected.trim(), out);
    }

    @Test
    public void testAddModules() {
        ST template = testGroup.getInstanceOf("addModules");
        template.add("ear", "earArchive");
        template.add("modules", new String[]{"ejbJar1", "clientJar", "war1"});
        String out = template.render().trim();
        System.out.println(out);
        String expected = """
earArchive.addAsModule(ejbJar1);
    earArchive.addAsModule(clientJar);
    earArchive.addAsModule(war1);
""";
        Assertions.assertEquals(expected.trim(), out);

        // Try using a List for modules
        template.remove("modules");
        List modules = Arrays.asList(new String[]{"ejbJar1", "clientJar", "war1"});
        template.add("modules", modules);
        out = template.render().trim();
        System.out.println(out);
        Assertions.assertEquals(expected.trim(), out);
    }
    @Test
    public void testAddModulesFromPkg() {
        record Pkg(String[] modules){};
        STGroup.trackCreationEvents = true;
        testGroup.registerModelAdaptor(Pkg.class, new RecordAdaptor<Pkg>());
        STGroup.verbose = true;
        Interpreter.trace = true;
        ST template = testGroup.getInstanceOf("addModulesFromPkg");
        template.add("ear", "earArchive");
        template.add("pkg", new Pkg(new String[]{"ejbJar1", "clientJar", "war1"}));
        String out = template.render().trim();
        System.out.println(out);
        String expected = """
earArchive.addAsModule(ejbJar1);
    earArchive.addAsModule(clientJar);
    earArchive.addAsModule(war1);
""";
        Assertions.assertEquals(expected.trim(), out);
    }

    @Test
    public void testDeploymentMethod() {
        War warDef = new War();
        warDef.setArchiveName("bytesMsgTopic");
        warDef.setArchiveSuffix("_web.war");
        warDef.setDescriptor("servlet_vehicle_web.xml");
        warDef.setDescriptorDir("/jms/core/bytesMsgTopic");
        warDef.setInternalDescriptorName("web.xml");
        ArrayList<String> includes = new ArrayList<>();
        includes.add("com/sun/ts/tests/jms/core/bytesMsgTopic/BytesMsgTopicTests.class");
        includes.add("com/sun/ts/tests/jms/common/JmsTool.class");
        TSFileSet classes = new TSFileSet("classes", "WEB-INF/classes", includes);
        warDef.addFileSet(classes);

        System.out.printf("%s\n", warDef.getRelativeDescriptorPath());
        System.out.printf("%s\n", warDef.getInternalDescriptorName());

        DeploymentRecord deployment = new DeploymentRecord("bytesMsgTopic", "javatest", "servlet");
        deployment.setWar(warDef);

        STGroup.verbose = true;
        Interpreter.trace = true;
        ST template = testGroup.getInstanceOf("genMethodVehicle");
        template.add("deployment", deployment);
        template.add("testClass", "ClientServletTest");
        String out = template.render().trim();
        System.out.println(out);

    }

    @Test
    public void testRar() {
        Rar rarDef = new Rar();
        rarDef.setArchiveName("ejb32_mdb_modernconnector");
        rarDef.setArchiveSuffix("_ra.rar");
        rarDef.setDescriptor("ra.xml");
        rarDef.setDescriptorDir("com/sun/ts/tests/ejb32/mdb/modernconnector/connector");
        rarDef.setInternalDescriptorName("ra.xml");
        ArrayList<String> includes = new ArrayList<>();
        includes.add("com/sun/ts/tests/ejb32/mdb/modernconnector/connector/EventMonitor.class");
        includes.add("com/sun/ts/tests/ejb32/mdb/modernconnector/connector/EventMonitorConfig.class");
        includes.add("com/sun/ts/tests/ejb32/mdb/modernconnector/connector/NoUseListener.class");
        includes.add("com/sun/ts/tests/ejb32/mdb/modernconnector/connector/EventMonitorAdapter.class");
        includes.add("com/sun/ts/tests/ejb32/mdb/modernconnector/connector/EventMonitorAdapter$ActivatedEndpoint.class");
        TSFileSet classes = new TSFileSet("classes", "", includes);
        rarDef.addFileSet(classes);

        System.out.printf("%s\n", rarDef.getRelativeDescriptorPath());
        System.out.printf("%s\n", rarDef.getInternalDescriptorName());

        DeploymentRecord deployment = new DeploymentRecord("ejb32_mdb_modernconnector", "javatest", "none");
        deployment.setRar(rarDef);

        STGroup.verbose = true;
        Interpreter.trace = true;
        ST template = testGroup.getInstanceOf("genMethodNonVehicle");
        template.add("deployment", deployment);
        template.add("testClass", "ClientTest");
        String out = template.render().trim();
        System.out.println(out);

    }
}
