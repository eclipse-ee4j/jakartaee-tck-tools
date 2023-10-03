package tck.conversion.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import tck.jakarta.platform.rewrite.ConvertJavaTestNameRecipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.openrewrite.java.Assertions.java;

class ConvertTestNameTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
                .recipe(new ConvertJavaTestNameRecipe())
                .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"));
    }

    @Test
    void addTestAnnotation() {
        rewriteRun(
                java(
                        """
                                    package test.somepkg;
                                    
                                    public class SomeTestClass {
                                  
                                        /**
                                        @testName: someTestMethod
                                        */
                                        public void someTestMethod() {
                                        }
                                    }
                                """,
                        """
                                    package test.somepkg;
                                    
                                    import org.junit.jupiter.api.Test;
                                    
                                    public class SomeTestClass {
                                  
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
    void textComment() {
        rewriteRun(
                java(
                        """
                                    package test.somepkg;
                                    
                                    public class SomeTestClass {
                                  
                                        /**
                                        @testName: someTestMethod
                                        */
                                        public void someTestMethod() {
                                        }
                                                       
                                        /*
                                        Another example where the test method does not use javadoc comments
                                        */                   
                                        /*
                                        @testName: anotherTestMethod
                                        */
                                        public void anotherTestMethod() {
                                        }
                                    }
                                """,
                        """
                                    package test.somepkg;
                                    
                                    import org.junit.jupiter.api.Test;
                                    
                                    public class SomeTestClass {
                                  
                                        /**
                                        @testName: someTestMethod
                                        */
                                        @Test
                                        public void someTestMethod() {
                                        }
 
                                        /*
                                        Another example where the test method does not use javadoc comments
                                        */                   
                                        /*
                                        @testName: anotherTestMethod
                                        */
                                        @Test
                                        public void anotherTestMethod() {
                                        }
                                    }
                                """
                )
        );
    }
}

