package tck.jakarta.platform.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import java.time.Duration;

/**
 * Converts methods with the @testName javadoc tag to methods with the Junit5 @Test annotation
 */
public class ConvertJavaTestNameRecipe extends Recipe {

    public ConvertJavaTestNameRecipe() {
        //doNext(new JavaTestToArquillianShrinkwrap());
    }
    @Override
    public String getDisplayName() {
        return "JavaTest to Junit5 @Test";
    }

    @Override
    public String getDescription() {
        return "Converts JavaTest @testName to @Test.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ConvertJavaTestNameVisitor<>();
    }
}
