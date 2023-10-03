package tck.jakarta.platform.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;

import java.time.Duration;

/**
 * Main entry point for the JavaTest to Arquillian/Shrinkwrap/Junit5 based TCK tests.
 * This chains to the {@linkplain ConvertJavaTestNameRecipe} for the testName to @Test
 * conversions.
 */
public class JavaTestToArquillianShrinkwrap extends Recipe {

    /**
     * Chain this Recipe to the ConvertJavaTestNameRecipe
     */
    public JavaTestToArquillianShrinkwrap() {
        doNext(new ConvertJavaTestNameRecipe());
    }

    @Override
    public String getDisplayName() {
        return "JavaTest to Arquillian/Shrinkwrap/Junit5";
    }

    @Override
    public String getDescription() {
        return "Main entry point for the JavaTest to Arquillian/Shrinkwrap based TCK tests.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new AddArquillianDeployMethod<>();
    }
}
