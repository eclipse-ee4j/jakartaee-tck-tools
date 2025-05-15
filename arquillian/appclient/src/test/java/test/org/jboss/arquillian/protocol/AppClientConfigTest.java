package test.org.jboss.arquillian.protocol;

import org.jboss.arquillian.config.impl.extension.ConfigurationRegistrar;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.test.impl.MapObject;
import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tck.arquillian.protocol.appclient.AppClientDeploymentPackager;
import tck.arquillian.protocol.appclient.AppClientProtocolConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;

public class AppClientConfigTest {
    @Test
    public void testFile() throws Exception {
        File dir = new File(".");
        System.out.println(dir.exists());
        System.out.println(dir.isDirectory());
        Process ls = Runtime.getRuntime().exec("/bin/ls", null, dir);
        int exit = ls.waitFor();
        System.out.println(exit);
    }
    @Test
    public void testConfig1() throws Exception {
        System.setProperty(ConfigurationRegistrar.ARQUILLIAN_XML_PROPERTY, "appclient1-arquillian.xml");
        ConfigurationRegistrar registrar = new ConfigurationRegistrar();
        ArquillianDescriptor descriptor = registrar.loadConfiguration();
        
        Assertions.assertNotNull(descriptor);
        Assertions.assertNotNull(descriptor.defaultProtocol("appclient"));
        String type = descriptor.defaultProtocol("appclient").getType();
        Assertions.assertEquals("appclient", type);

        Map<String, String> props = descriptor.defaultProtocol("appclient").getProperties();

        AppClientProtocolConfiguration config = new AppClientProtocolConfiguration();
        MapObject.populate(config, props);

        // Raw strings
        Assertions.assertEquals("-p;/home/jakartaeetck/bin/xml/../../tmp/tstest.jte", config.getClientCmdLineString());
        String expectedEnv = "JAVA_OPTS=-Djboss.modules.system.pkgs=com.sun.ts.lib,com.sun.javatest;CLASSPATH=${project.build.directory}/appclient/javatest.jar:${project.build.directory}/appclient/libutil.jar:${project.build.directory}/appclient/libcommon.jar";
        Assertions.assertEquals(expectedEnv, config.getClientEnvString());
        Assertions.assertTrue(config.isRunClient());
        Assertions.assertEquals(".", config.getClientDir());

        // Parsed strings
        String[] args = config.clientCmdLineAsArray();
        Assertions.assertEquals(2, args.length);
        Assertions.assertEquals("-p", args[0]);
        Assertions.assertEquals("/home/jakartaeetck/bin/xml/../../tmp/tstest.jte", args[1]);

        String[] envp = config.clientEnvAsArray();
        Assertions.assertEquals(2, envp.length);
        Assertions.assertTrue(envp[0].startsWith("JAVA_OPTS="));
        Assertions.assertEquals("-Djboss.modules.system.pkgs=com.sun.ts.lib,com.sun.javatest", envp[0].substring(10));
        Assertions.assertTrue(envp[1].startsWith("CLASSPATH="));
        String expectedCP = "${project.build.directory}/appclient/javatest.jar:${project.build.directory}/appclient/libutil.jar:${project.build.directory}/appclient/libcommon.jar";
        Assertions.assertEquals(expectedCP, envp[1].substring(10));
        File expectedDir = new File(".");
        Assertions.assertEquals(expectedDir, config.clientDirAsFile());
    }

    @Test
    public void testClientEarNoLibDir() throws IOException{

        EnterpriseArchive ear = createEarNoLibDir();
        // Config
        AppClientProtocolConfiguration config = new AppClientProtocolConfiguration();
        config.setClientEarDir("/tmp/appclient");
        config.setUnpackClientEar(true);

        // Remove the lib dir
        File libDir = new File("/tmp/appclient/lib");
        if (libDir.exists()) {
            deleteDirectory(libDir.toPath());
        }

        // TestDeployment
        DeploymentDescription dd = new DeploymentDescription(ear.getName(), ear);
        TestDeployment td = new TestDeployment(dd, ear, Collections.emptyList());
        td.setProtocolConfiguration(config);
        AppClientDeploymentPackager packager = new AppClientDeploymentPackager();
        packager.generateDeployment(td, Collections.emptyList());

        Assertions.assertFalse(libDir.exists());
        File jarFile = new File(libDir, "lib.jar");
        Assertions.assertFalse(jarFile.exists());
        File resFile = new File(libDir, "second-level-dir/second-level-dir.txt");
        Assertions.assertFalse(resFile.exists());

        String clientEarLibClasspath = config.clientEarLibClasspath();
        String[] expectedClasspath = {"/tmp/appclient/private-lib/arquillian-protocol-lib.jar"};
        boolean found = true;
        for (String path : expectedClasspath) {
            found &= clientEarLibClasspath.contains(path);
        }
        Assertions.assertTrue(found, "Classpath should contain all expected paths");
    }

    @Test
    public void testClientEarDefaultLibDir() {

        EnterpriseArchive ear = createEarDefaultLibDir();
        // Config
        AppClientProtocolConfiguration config = new AppClientProtocolConfiguration();
        config.setClientEarDir("/tmp/appclient");
        config.setUnpackClientEar(true);

        // TestDeployment
        DeploymentDescription dd = new DeploymentDescription(ear.getName(), ear);
        TestDeployment td = new TestDeployment(dd, ear, Collections.emptyList());
        td.setProtocolConfiguration(config);
        AppClientDeploymentPackager packager = new AppClientDeploymentPackager();
        packager.generateDeployment(td, Collections.emptyList());

        File libDir = new File("/tmp/appclient/lib");
        Assertions.assertTrue(libDir.exists());
        File jarFile = new File(libDir, "lib.jar");
        Assertions.assertTrue(jarFile.exists());
        File resFile = new File(libDir, "second-level-dir/second-level-dir.txt");
        Assertions.assertTrue(resFile.exists());

        String clientEarLibClasspath = config.clientEarLibClasspath();
        String[] expectedClasspath = {"/tmp/appclient/lib/lib.jar", "/tmp/appclient/lib/arquillian-protocol-lib.jar"};
        boolean found = true;
        for (String path : expectedClasspath) {
            found &= clientEarLibClasspath.contains(path);
        }
        Assertions.assertTrue(found, "Classpath should contain all expected paths");
    }

    @Test
    public void testClientEarCustomLibDirUnpackedEar() {

        EnterpriseArchive ear = createEarCustomLiDir();
        // Config
        AppClientProtocolConfiguration config = new AppClientProtocolConfiguration();
        config.setClientEarDir("/tmp/appclient");
        config.setUnpackClientEar(true);

        // TestDeployment
        DeploymentDescription dd = new DeploymentDescription(ear.getName(), ear);
        TestDeployment td = new TestDeployment(dd, ear, Collections.emptyList());
        td.setProtocolConfiguration(config);
        AppClientDeploymentPackager packager = new AppClientDeploymentPackager();
        packager.generateDeployment(td, Collections.emptyList());

        File libDir = new File("/tmp/appclient/lib");
        Assertions.assertTrue(libDir.exists());
        File jarFile = new File(libDir, "lib.jar");
        Assertions.assertTrue(jarFile.exists());
        File resFile = new File(libDir, "second-level-dir/second-level-dir.txt");
        Assertions.assertTrue(resFile.exists());

        String clientEarLibClasspath = config.clientEarLibClasspath();
        System.out.println(clientEarLibClasspath);
        String[] expectedClasspath = {"/tmp/appclient/1/2/3/custom.jar", "/tmp/appclient/1/2/3/arquillian-protocol-lib.jar"};
        boolean found = true;
        for (String path : expectedClasspath) {
            boolean pathFound = clientEarLibClasspath.contains(path);
            found &= pathFound;
            if (!pathFound) {
                System.out.println("Classpath does not contain: " + path);
            }
        }
        Assertions.assertTrue(found, "Classpath should contain all expected paths");
        Assertions.assertFalse(clientEarLibClasspath.contains("lib/lib.jar"), "Classpath should NOT contain lib/lib.jar");
    }

    public static void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private EnterpriseArchive createEarNoLibDir() {
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        libJar.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n"), "MANIFEST.MF");
        libJar.addAsResource(new StringAsset("foo.txt"), "foo.txt");
        JavaArchive clientJar = ShrinkWrap.create(JavaArchive.class, "client.jar");
        clientJar.addAsManifestResource(new StringAsset("Main-Class: tck.test.Client\n"), "MANIFEST.MF");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ear_no_lib.ear");
        ear.add(new StringAsset("second-level-dir.txt"), "/lib/second-level-dir/second-level-dir.txt");

        ear.addAsModule(clientJar);
        ear.addAsLibrary(libJar);
        URL earURL = getClass().getResource("/application-nolib.xml");
        ear.addAsManifestResource(earURL, "application.xml");

        return ear;
    }

    private EnterpriseArchive createEarDefaultLibDir() {
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        libJar.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n"), "MANIFEST.MF");
        libJar.addAsResource(new StringAsset("foo.txt"), "foo.txt");
        JavaArchive clientJar = ShrinkWrap.create(JavaArchive.class, "client.jar");
        clientJar.addAsManifestResource(new StringAsset("Main-Class: tck.test.Client\n"), "MANIFEST.MF");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ear_default_lib.ear");
        ear.add(new StringAsset("second-level-dir.txt"), "/lib/second-level-dir/second-level-dir.txt");

        ear.addAsModule(clientJar);
        ear.addAsLibrary(libJar);
        return ear;
    }

    private EnterpriseArchive createEarCustomLiDir() {
        // Standard lib location that should be ignored
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "lib.jar");
        libJar.addAsManifestResource(new StringAsset("Manifest-Version: 1.0\n"), "MANIFEST.MF");
        libJar.addAsResource(new StringAsset("foo.txt"), "foo.txt");

        JavaArchive customLib = ShrinkWrap.create(JavaArchive.class, "custom.jar");
        customLib.setManifest(new StringAsset("""
                        Manifest-Version: 1.0
                        Class-Path: second-level-dir/  4/second-level-jar.jar
                        """));
        customLib.addAsResource(new StringAsset("custom.txt"), "foo.txt");

        JavaArchive clientJar = ShrinkWrap.create(JavaArchive.class, "client.jar");
        clientJar.addAsManifestResource(new StringAsset("Main-Class: tck.test.Client\n"), "MANIFEST.MF");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ear_custom_lib.ear");
        ear.add(new StringAsset("second-level-dir.txt"), "/1/2/3/second-level-dir/second-level-dir.txt");

        ear.addAsModule(clientJar);
        // This should be ignored
        ear.addAsLibrary(libJar);
        // The custom.jar is under the /1/2/3 path in the ear
        ear.add(new ArchiveAsset(customLib, ZipExporter.class), "/1/2/3/" + customLib.getName());
        URL earURL = getClass().getResource("/application.xml");
        ear.addAsManifestResource(earURL, "application.xml");
        return ear;
    }
}
