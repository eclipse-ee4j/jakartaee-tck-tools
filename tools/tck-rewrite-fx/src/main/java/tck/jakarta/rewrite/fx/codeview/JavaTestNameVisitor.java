package tck.jakarta.rewrite.fx.codeview;

import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TextComment;
import tck.jakarta.platform.ant.api.TestMethodInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Find methods marked with a @testName javadoc comment
 * @param <ExecutionContext>
 */
public class JavaTestNameVisitor<ExecutionContext> extends JavaIsoVisitor<ExecutionContext> {
    private List<TestMethodInfo> methoodNames = new ArrayList<>();
    private List<TestMethodInfo> extMethoodNames = new ArrayList<>();

    String toString(List<NameTree> methodThrows) {
        if(methodThrows == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for(NameTree nt : methodThrows) {
            sb.append(nt.toString());
            sb.append(", ");
        }
        sb.setLength(sb.length()-2);
        return sb.toString();
    }

    public List<TestMethodInfo> getMethodNames() {
        return methoodNames;
    }
    public List<TestMethodInfo> getExtMethodNames() {
        return extMethoodNames;
    }
    @Override
    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ec) {
        String methodName = method.getSimpleName();
        List<NameTree> methodThrows = method.getThrows();
        String methodThrowsString = toString(methodThrows);

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
                            methoodNames.add(new TestMethodInfo(methodName, methodThrowsString));
                        } else {
                            System.out.println("Unknown block tag: "+name);
                        }
                    }
                }
            } else if(c instanceof TextComment) {
                String text = ((TextComment)c).getText();
                int testNameIndex = text.indexOf("testName:");
                if(testNameIndex >= 0) {
                    // Java comment with a @testName tag. This may not apply to method, so parse the name
                    String nameText = text.substring(testNameIndex+9).trim();
                    String[] parts = nameText.split("\\s+", 2);
                    String commentMethodName = parts[0];
                    System.out.printf("non-javadoc on method(%s), testName: %s\n", methodName, commentMethodName);
                    if(!commentMethodName.equals(methodName)) {
                        methoodNames.add(new TestMethodInfo(commentMethodName, ""));
                    } else {
                        methoodNames.add(new TestMethodInfo(methodName, methodThrowsString));
                    }
                }
            }
        }

        return method;
    }

    String jdocString(List<Javadoc> content) {
        if(content == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for(Javadoc jd : content) {
            sb.append(jd.toString());
        }
        return sb.toString();
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
        classDecl = super.visitClassDeclaration(classDecl, executionContext);

        String superclass = classDecl.getExtends().toString();
        System.out.printf("Visiting class %s extends %s\n", classDecl.getSimpleName(), superclass);
        return classDecl;
    }

    @Override
    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
        cu = super.visitCompilationUnit(cu, executionContext);
        return cu;
    }

    /**
     * This captures comments at the end of the class file
     * @param block
     * @param executionContext
     * @return
     */
    @Override
    public J.Block visitBlock(J.Block block, ExecutionContext executionContext) {
        block = super.visitBlock(block, executionContext);
        List<Comment> comments = block.getComments();
        if(comments.isEmpty()) {
            comments = block.getEnd().getComments();
        }

        block.getEnd().getComments();
        for(Comment c : comments) {
            if(c instanceof Javadoc.DocComment) {
                for(Javadoc jd : ((Javadoc.DocComment) c).getBody()) {
                    if(jd instanceof Javadoc.UnknownBlock) {
                        Javadoc.UnknownBlock jdu = (Javadoc.UnknownBlock) jd;
                        String name = jdu.getName();
                        if(name.equals("testName:")) {
                            String methodName = jdocString(jdu.getContent());
                            System.out.printf("javadoc testName: %s\n", methodName);
                            extMethoodNames.add(new TestMethodInfo(methodName, "Exception"));
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
                    String nameText = text.substring(testNameIndex+9).trim();
                    String[] parts = nameText.split("\\s+", 2);
                    String methodName = parts[0];
                    System.out.printf("javadoc testName: %s\n", methodName);
                    TestMethodInfo methodInfo = new TestMethodInfo(methodName, "Exception");
                    methodInfo.setFromSuperclass(true);
                    extMethoodNames.add(methodInfo);
                }
            }
        }
        return block;
    }
}