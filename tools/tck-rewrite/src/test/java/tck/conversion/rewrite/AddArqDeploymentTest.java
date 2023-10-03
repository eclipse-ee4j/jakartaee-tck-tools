package tck.conversion.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import tck.jakarta.platform.rewrite.AddArquillianDeployMethod;
import tck.jakarta.platform.rewrite.ConvertJavaTestNameRecipe;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.openrewrite.java.Assertions.java;

/**
 * The test recipe which combines the AddArquillianDeployMethod and
 * ConvertJavaTestNameRecipe recipes.
 */
class LocalRecipe extends Recipe {
    LocalRecipe() {
        doNext(new ConvertJavaTestNameRecipe());
    }
    @Override
    public String getDisplayName() {
        return "Add missing @Deployment";
    }

    @Override
    public String getDescription() {
        return getDisplayName() + ".";
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new AddArquillianDeployMethod<>();
    }

}

public class AddArqDeploymentTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        Path testClasses = Paths.get("/Users/starksm/Dev/Jakarta/tck-rewrite-tools/target", "test-classes");

        spec
                .recipe(new LocalRecipe())
                .parser(JavaParser.fromJavaVersion()
                        .classpath(JavaParser.runtimeClasspath())
                );
    }

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
                                            initilizer_jar.addClass(com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TCKServletContainerInitializer.class);
                                            initilizer_jar.addAsManifestResource("META-INF/MANIFEST.MF");
                                            initilizer_jar.addAsManifestResource("META-INF/services/jakarta.servlet.ServletContainerInitializer");
                                            servlet_sci_setsessiontrackingmode_web_war.addAsLibrary(initilizer_jar);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TCKServletContainerInitializer.class);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TestListener.class);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.api.jakarta_servlet.scinitializer.setsessiontrackingmodes.TestServlet.class);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.common.servlets.GenericTCKServlet.class);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.common.util.Data.class);
                                            servlet_sci_setsessiontrackingmode_web_war.addClass(com.sun.ts.tests.servlet.common.util.ServletTestUtil.class);                  
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
}

