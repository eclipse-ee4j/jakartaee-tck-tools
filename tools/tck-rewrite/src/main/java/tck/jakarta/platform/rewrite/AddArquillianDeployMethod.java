package tck.jakarta.platform.rewrite;

import jakartatck.jar2shrinkwrap.Jar2ShrinkWrap;
import jakartatck.jar2shrinkwrap.JarProcessor;
import org.openrewrite.Cursor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import tck.jakarta.platform.rewrite.mapping.ClassNameRemappingImpl;
import tck.jakarta.platform.rewrite.mapping.EE11_2_EE10;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * If a class is a non-abstract extension of com.sun.ts.lib.harness.EETest and it does not
 * already have an Arquillian @Deployment method, add one based on the Jar2ShrinkWrap
 * test artifact for the package.
 *
 * @param <ExecutionContext>
 */
public class AddArquillianDeployMethod<ExecutionContext> extends JavaIsoVisitor<ExecutionContext> {
    private static final Logger log = Logger.getLogger(AddArquillianDeployMethod.class.getName());
    private final AnnotationMatcher TEST_ANN_MATCH = new AnnotationMatcher("@org.jboss.arquillian.container.test.api.Deployment");

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {

        System.out.println(TreeVisitingPrinter.printTree(getCursor()));


        // Check if the class already has a method marked with @Deployment
        boolean deploymentMethodExists = classDecl.getBody().getStatements().stream()
                .filter(statement -> statement instanceof J.MethodDeclaration)
                .map(J.MethodDeclaration.class::cast)
                .anyMatch(methodDeclaration -> methodDeclaration.getAllAnnotations().stream().anyMatch(TEST_ANN_MATCH::matches));
        // If the class already has a `@Deployment *()` method, don't make any changes to it.
        if (deploymentMethodExists) {
            System.out.println("@Deployment annotated method exists, return existing class def");
            return classDecl;
        }

        // Get a set of the parent class types
        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
        Set<String> parentTypes = new HashSet<>();
        JavaType.FullyQualified currentFq = cd.getType();

        while (currentFq != null) {
            parentTypes.add(currentFq.getFullyQualifiedName());
            for (JavaType.FullyQualified i : currentFq.getInterfaces()) {
                parentTypes.add(i.getFullyQualifiedName());
            }
            currentFq = currentFq.getSupertype();
            if (currentFq != null && parentTypes.contains(currentFq.getFullyQualifiedName())) {
                break;
            }
        }

        boolean isEETest = classDecl.getSimpleName().contains("Client"); // this will match too much but still try

        List<J.Modifier> modifiers = classDecl.getModifiers();
        boolean isAbstract = modifiers.stream().anyMatch(modifier -> modifier.getType() == J.Modifier.Type.Abstract);
        System.out.printf("%s isEETest=%s, isAbstract=%s\n".formatted(cd.getType().getClassName(), isEETest, isAbstract));

        // If this is a concrete subclass of EETest, add an arq deployment method
        if(!isAbstract && isEETest) {
            String pkg = cd.getType().getPackageName();
            String ee10pkg = EE11_2_EE10.mapEE11toEE10(pkg);
            ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
                /*
                String clInfo = ClassLoaderUtils.showClassLoaderHierarchy(this, "visitClassDeclaration");
                System.out.println(clInfo);
                 */
                JarProcessor war = Jar2ShrinkWrap.fromPackage(ee10pkg, new ClassNameRemappingImpl(classDecl.getType().getFullyQualifiedName()));
                StringWriter methodCodeWriter = new StringWriter();
                war.saveOutput(methodCodeWriter, false);
                String methodCode = methodCodeWriter.toString();
                if (methodCode.length() == 0) {
                    System.out.printf("No Jar2ShrinkWrap artifact, no code generated for package: " + pkg + "(" + ee10pkg+ ")");
                    return cd;
                }
                System.out.printf("Applying template to method code: "+methodCode);

                JavaTemplate deploymentTemplate =
                        JavaTemplate.builder( methodCode)
                                .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                                .imports("org.jboss.arquillian.container.test.api.Deployment",
                                        "org.jboss.shrinkwrap.api.Archive",
                                        "org.jboss.shrinkwrap.api.ShrinkWrap",
                                        "org.jboss.shrinkwrap.api.spec.EnterpriseArchive",
                                        "org.jboss.shrinkwrap.api.spec.JavaArchive"
                                )
                                .build();
                System.out.printf("built JavaTemplate");
                String dotClassRef = classDecl.getType().getClassName()+".class";
                cd = classDecl.withBody( deploymentTemplate.apply(new Cursor(getCursor(), classDecl.getBody()),
                        classDecl.getBody().getCoordinates().firstStatement()));
                maybeAddImport("org.jboss.arquillian.container.test.api.Deployment");
                maybeAddImport("org.jboss.shrinkwrap.api.Archive");
                maybeAddImport("org.jboss.shrinkwrap.api.ShrinkWrap");
                maybeAddImport("org.jboss.shrinkwrap.api.spec.EnterpriseArchive");
                maybeAddImport("org.jboss.shrinkwrap.api.spec.JavaArchive");
                maybeAddImport("org.jboss.shrinkwrap.api.spec.WebArchive");
                System.out.printf("Added @Deployment method to class: "+classDecl.getType().getFullyQualifiedName());
            } catch (RuntimeException e) {
                StringWriter trace = new StringWriter();
                e.printStackTrace(new PrintWriter(trace));
                System.out.printf("No code generated for package: %s, due to exception: %s".formatted(pkg, e));
                System.out.printf(trace.toString());
                return cd;
            }
            finally {
                Thread.currentThread().setContextClassLoader(prevCL);
            }

        }
        return cd;
    }

}
