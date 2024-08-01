package tck.conversion.rewrite;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import org.openrewrite.xml.ChangeTagContentVisitor;
import tck.jakarta.platform.rewrite.AddArquillianDeployMethod;
import tck.jakarta.platform.rewrite.ConvertJavaTestNameRecipe;
import tck.jakarta.platform.rewrite.ConvertJavaTestNameVisitor;
import tck.jakarta.platform.rewrite.JavaTestToArquillianShrinkwrap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.openrewrite.java.Assertions.java;

public class AddArqDeploymentTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        Path testClasses = Paths.get("/Users/starksm/Dev/Jakarta/tck-rewrite-tools/target", "test-classes");

        Recipe[] toRun = {new JavaTestToArquillianShrinkwrap(), new ConvertJavaTestNameRecipe()};
        CompositeRecipe testRecipes = new CompositeRecipe(Arrays.asList(toRun));

        spec.recipe(testRecipes)
                .parser(JavaParser.fromJavaVersion()
                        .classpath(JavaParser.runtimeClasspath())
                );
    }

    @Disabled
    @Test
    void addDeploymentMethod() {
        rewriteRun(
                java(
                        """
                                    package com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics;
                                    
                                    import com.sun.ts.lib.harness.EETest;
                                    
                                    public class Client extends EETest {
                                  
                                        /**
                                        @testName: someTestMethod
                                        */
                                        public void someTestMethod() {
                                        }
                                    }
                                """,
                        """
                                    package com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics;
                                    
                                    import com.sun.ts.lib.harness.EETest;
                                    import org.jboss.arquillian.container.test.api.Deployment;
                                    import org.jboss.shrinkwrap.api.Archive;
                                    import org.jboss.shrinkwrap.api.ShrinkWrap;
                                    import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
                                    import org.jboss.shrinkwrap.api.spec.JavaArchive;
                                    import org.junit.jupiter.api.Test;
                                    
                                    public class Client extends EETest {
                                    
                                        @Deployment(testable = false)
                                        public static Archive<?> deployment() {
                                    
                                            final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ejb_bb_ssl_argsemantics.ear");
                                            // Add ear submodules
                                    
                                            JavaArchive ejb_bb_ssl_argsemantics_client_jar = ShrinkWrap.create(JavaArchive.class, "ejb_bb_ssl_argsemantics_client_jar");
                                            ejb_bb_ssl_argsemantics_client_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBean.class);
                                            ejb_bb_ssl_argsemantics_client_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBeanHome.class);
                                            ejb_bb_ssl_argsemantics_client_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.Client.class);
                                            ear.addAsModule(ejb_bb_ssl_argsemantics_client_jar);
                                    
                                            JavaArchive ejb_bb_ssl_argsemantics_ejb_jar = ShrinkWrap.create(JavaArchive.class, "ejb_bb_ssl_argsemantics_ejb_jar");
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBean.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBeanEJB.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBeanHome.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.CMP20Callee.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.CMP20CalleeEJB.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.CMP20CalleeHome.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.CMP20CalleeLocal.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.CMP20CalleeLocalHome.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.SimpleArgument.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.StatefulCallee.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.StatefulCalleeEJB.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.StatefulCalleeHome.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.StatefulCalleeLocal.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.StatefulCalleeLocalHome.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.wrappers.CMP20Wrapper.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.wrappers.StatefulWrapper.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.wrappers.StatelessWrapper.class);
                                            ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.testlogic.ejb.bb.argsemantics.TestLogic.class);
                                            ear.addAsModule(ejb_bb_ssl_argsemantics_ejb_jar);
                                            return ear;                 
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

