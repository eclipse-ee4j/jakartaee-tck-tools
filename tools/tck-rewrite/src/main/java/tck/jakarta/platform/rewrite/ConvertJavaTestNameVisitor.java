package tck.jakarta.platform.rewrite;

import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.TextComment;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Find methods marked with a @testName javadoc comment and add a Junit5 @Test annotation
 * @param <ExecutionContext>
 */
public class ConvertJavaTestNameVisitor<ExecutionContext> extends JavaIsoVisitor<ExecutionContext> {
    private static final Logger log = Logger.getLogger(ConvertJavaTestNameRecipe.class.getName());
    private final AnnotationMatcher TEST_ANN_MATCH = new AnnotationMatcher("@org.junit.jupiter.api.Test");

    private final JavaTemplate testAnnotationTemplate =
            JavaTemplate.builder( "@Test")/*[Rewrite8 migration] contextSensitive() could be unnecessary, please follow the migration guide*/.contextSensitive()
                    .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                    .imports("org.junit.jupiter.api.Test")
                    .build();

    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ec) {
        String methodName = method.getSimpleName();
        if(method.getAllAnnotations().stream().anyMatch(TEST_ANN_MATCH::matches)) {
            log.fine("Visting(%s) skipped due to @Test".formatted(methodName));
            return super.visitMethodDeclaration(method, ec);
        }

        method = super.visitMethodDeclaration(method, ec);
        List<Comment> comments = method.getComments();
        log.fine("Visting(%s), comments=%d".formatted(methodName, comments.size()));

        for(Comment c : comments) {
            if(c instanceof Javadoc.DocComment) {
                for(Javadoc jd : ((Javadoc.DocComment) c).getBody()) {
                    if(jd instanceof Javadoc.UnknownBlock) {
                        String name = ((Javadoc.UnknownBlock) jd).getName();
                        if(name.equals("testName:")) {
                            // Javadoc comment with a @testName tag
                            method = testAnnotationTemplate.apply(/*[Rewrite8 migration] getCursor() could be updateCursor() if the J instance is updated, or it should be updated to point to the correct cursor, please follow the migration guide*/getCursor(),
                                    method.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                            maybeAddImport("org.junit.jupiter.api.Test");
                            log.fine("Added @Test annotation to: "+method);
                        } else {
                            log.finer("Unknown block tag: "+name);
                        }
                    }
                }
            } else if(c instanceof TextComment) {
                String text = ((TextComment)c).getText();
                int testNameIndex = text.indexOf("testName:");
                if(testNameIndex >= 0) {
                    // Java comment with a @testName tag
                    String name = text.substring(testNameIndex+9).strip();
                    String[] parts = name.split("[\s\n\t]+");
                    method = testAnnotationTemplate.apply(/*[Rewrite8 migration] getCursor() could be updateCursor() if the J instance is updated, or it should be updated to point to the correct cursor, please follow the migration guide*/getCursor(),
                            method.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                    maybeAddImport("org.junit.jupiter.api.Test", null, false);
                    log.fine("Added @Test annotation to: "+method);
                }
            }
        }

        return method;
    }

}