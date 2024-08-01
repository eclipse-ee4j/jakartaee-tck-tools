package tck.conversion.rewrite;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import tck.jakarta.platform.rewrite.ConvertJavaTestNameRecipe;
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
        Recipe[] toRun = {new JavaTestToArquillianShrinkwrap(), new ConvertJavaTestNameRecipe()};
        CompositeRecipe testRecipes = new CompositeRecipe(Arrays.asList(toRun));

        spec
                .recipe(testRecipes)
                .parser(JavaParser.fromJavaVersion()
                        .classpath(JavaParser.runtimeClasspath())
                )
        ;
    }

    @Disabled
    @Test
    public void testAppClientEar() throws IOException {
        String className = "Client";
        String pkg = "com.sun.ts.tests.assembly.altDD";
        runTestFromSource(className, pkg);
    }

    /**
     * A test from the com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics pkg that has several
     * methods. The before and after source are read in from the LargeCaseBefore.java/LargeCaseAfter.java files
     *
     * @throws IOException
     */

    @Disabled
    @Test
    public void testLargeCase() throws IOException {
        String className = "LargeCase";
        String pkg = "com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics";
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

