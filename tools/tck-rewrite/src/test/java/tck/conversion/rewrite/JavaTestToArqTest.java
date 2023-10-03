package tck.conversion.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import tck.jakarta.platform.rewrite.JavaTestToArquillianShrinkwrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.openrewrite.java.Assertions.java;

class JavaTestToArqTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
                .recipe(new JavaTestToArquillianShrinkwrap())
                .parser(JavaParser.fromJavaVersion()
                        .classpath(JavaParser.runtimeClasspath())
                )
        ;
    }

    /**
     * Test of a war deployment from the com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes
     * pkg.
     */
    @Test
    void addDeploymentMethod() {
        rewriteRun(
                java(
                        """
                                    package com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes;
                                    
                                    import com.sun.ts.tests.servlet.common.client.AbstractUrlClient;
                                    
                                    public class SomeTestClass extends AbstractUrlClient {
                                  
                                        /**
                                        @testName: someTestMethod
                                        */
                                        public void someTestMethod() {
                                        }
                                    }
                                """,
                        """
                                    package com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes;
                                    
                                    import com.sun.ts.tests.servlet.common.client.AbstractUrlClient;
                                    import org.jboss.arquillian.container.test.api.Deployment;
                                    import org.jboss.shrinkwrap.api.ShrinkWrap;
                                    import org.jboss.shrinkwrap.api.spec.JavaArchive;
                                    import org.jboss.shrinkwrap.api.spec.WebArchive;
                                    import org.junit.jupiter.api.Test;
                                    
                                    public class SomeTestClass extends AbstractUrlClient {
                                    
                                        @Deployment(testable = false)
                                        public static WebArchive getTestArchive() throws Exception {

                                            WebArchive servlet_sci_setsessiontrackingmode_web_war = ShrinkWrap.create(WebArchive.class, "servlet_sci_setsessiontrackingmode_web_war");
                                            servlet_sci_setsessiontrackingmode_web_war.addAsWebInfResource("web.xml");
                                    
                                            JavaArchive initilizer_jar = ShrinkWrap.create(JavaArchive.class, "initilizer.jar");
                                            initilizer_jar.addClass("com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TCKServletContainerInitializer");
                                            initilizer_jar.addAsManifestResource("META-INF/MANIFEST.MF");
                                            initilizer_jar.addAsManifestResource("META-INF/services/jakarta.servlet.ServletContainerInitializer");
                                            servlet_sci_setsessiontrackingmode_web_war.addAsLibrary(initilizer_jar);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TCKServletContainerInitializer.class);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TestListener.class);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TestServlet.class);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.common.servlets.GenericTCKServlet.class);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.common.util.Data.class);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.common.util.ServletTestUtil.class);
                                            return servlet_sci_setsessiontrackingmode_web_war;
                                        }
                                  
                                        /**
                                        @testName: someTestMethod
                                        */
                                        @Test
                                        public void someTestMethod() {
                                        }
                                    }
                                """
                )
        );
    }

    @Test
    public void testAppClientEar() throws IOException {
        String className = "Client";
        String pkg = "com.sun.ts.tests.assembly.altDD";
        runTestFromSource(className, pkg);
    }

    /**
     * A test from the com.sun.ts.tests.servlet.api.jakarta_servlet_http.sessioncookieconfig pkg that has several
     * methods. The before and after source are read in from the LargeCaseBefore.java/LargeCaseAfter.java files
     *
     * @throws IOException
     */

    @Test
    public void testLargeCase() throws IOException {
        String className = "LargeCase";
        String pkg = "com.sun.ts.tests.servlet.api.jakarta_servlet_http.sessioncookieconfig";
        runTestFromSource(className, pkg);
    }

    private void runTestFromSource(String className, String pkg) throws IOException {
        // Assumes this is being run within the project, not as a bundled test artifact
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path beforePath = projectRoot.resolve("src/test/java/tck/conversion/rewrite/"+className+"Before.java");
        String before = Files.readString(beforePath);
        before = before.replace(className+"Before", className)
                .replace("tck.conversion.rewrite", pkg);
        Path afterPath = projectRoot.resolve("src/test/java/tck/conversion/rewrite/"+className+"After.java");
        String after = Files.readString(afterPath);
        after = after.replace(className+"After", className)
                .replace("tck.conversion.rewrite", pkg);

        rewriteRun(java(before, after));
    }
}

