package tck.jakarta.rewrite.fx.codeview;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.TextComment;

import java.util.ArrayList;
import java.util.List;

/**
 * Find methods marked with a @testName javadoc comment and add a Junit5 @Test annotation
 * @param <ExecutionContext>
 */
public class JavaTestNameVisitor<ExecutionContext> extends JavaIsoVisitor<ExecutionContext> {
    private List<String> methoodNames = new ArrayList<>();

    public List<String> getMethodNames() {
        return methoodNames;
    }
    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ec) {
        String methodName = method.getSimpleName();

        method = super.visitMethodDeclaration(method, ec);
        List<Comment> comments = method.getComments();
        System.out.printf("Visting(%s), comments=%d\n".formatted(methodName, comments.size()));

        for(Comment c : comments) {
            if(c instanceof Javadoc.DocComment) {
                for(Javadoc jd : ((Javadoc.DocComment) c).getBody()) {
                    if(jd instanceof Javadoc.UnknownBlock) {
                        String name = ((Javadoc.UnknownBlock) jd).getName();
                        if(name.equals("testName:")) {
                            System.out.printf("javadoc testName: %s\n", methodName);
                            methoodNames.add(methodName);
                        } else {
                            System.out.println("Unknown block tag: "+name);
                        }
                    }
                }
            } else if(c instanceof TextComment) {
                String text = ((TextComment)c).getText();
                int testNameIndex = text.indexOf("testName:");
                if(testNameIndex >= 0) {
                    // Java comment with a @testName tag
                    System.out.printf("non-javadoc testName: %s\n", methodName);
                    methoodNames.add(methodName);
                }
            }
        }

        return method;
    }

}