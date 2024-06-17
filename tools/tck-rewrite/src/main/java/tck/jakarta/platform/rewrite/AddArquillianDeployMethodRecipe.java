package tck.jakarta.platform.rewrite;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakartatck.jar2shrinkwrap.Jar2ShrinkWrap;
import jakartatck.jar2shrinkwrap.JarProcessor;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import tck.jakarta.platform.rewrite.mapping.ClassNameRemappingImpl;
import tck.jakarta.platform.rewrite.mapping.EE11_2_EE10;
import tck.jakarta.platform.rewrite.shrinkwrap.TestGenerator;

/**
 * AddArquillianDeployMethodRecipe based on AddArquillianDeployMethod + referenced doc from https://github.com/moderneinc/rewrite-recipe-starter?tab=readme-ov-file#getting-started
 *
 * @author Scott Marlow
 */
public class AddArquillianDeployMethodRecipe extends Recipe implements Serializable {
    private static final Logger log = Logger.getLogger(AddArquillianDeployMethodRecipe.class.getName());
    private static ThreadLocal<Set> threadLocalMethodNamesSet = new ThreadLocal<>();

    static final long serialVersionUID = 427023419L;
    private static String fullyQualifiedClassName = AddArquillianDeployMethodRecipe.class.getCanonicalName();

    static {
        if(log.isLoggable(Level.FINEST)) {
            log.finest("AddArquillianDeployMethodRecipe: Preparing to process " + fullyQualifiedClassName);
        }
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
    public String toString() {
        return fullyQualifiedClassName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        AddArquillianDeployMethodRecipe that = (AddArquillianDeployMethodRecipe) o;
        return Objects.equals(fullyQualifiedClassName, that.fullyQualifiedClassName);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // getVisitor() should always return a new instance of the visitor to avoid any state leaking between cycles
        return new testClassVisitor();
    }

    public class testClassVisitor extends JavaIsoVisitor<ExecutionContext> {


        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            List<J.Modifier> modifiers = classDecl.getModifiers();
            boolean isAbstract = modifiers.stream().anyMatch(modifier -> modifier.getType() == J.Modifier.Type.Abstract);

            if (isAbstract) {
                // Ignore refactoring abstract classes
                return classDecl;
            }

            boolean isEETest = classDecl.getSimpleName().contains("Client"); // this will match too much but still try
            if (!isEETest) {
                return classDecl;
            }

            // will populate methodNameSet in visitMethodDeclaration method
            Set<String> methodNameSet = new HashSet<>(); // will contain set of methods in the current classDecl
            threadLocalMethodNamesSet.set(methodNameSet);
            super.visitClassDeclaration(classDecl, executionContext);
            isEETest = methodNameSet.stream().anyMatch(str -> str.contains("test"));
            threadLocalMethodNamesSet.set(null);
            methodNameSet = null;

            // return if this is not an EE test
            if (!isEETest) {
                return classDecl;
            }

            // Check if the class already has a method that handles EE "deployment".
            boolean deploymentMethodExists = classDecl.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodDeclaration)
                    .map(J.MethodDeclaration.class::cast)
                    .anyMatch(methodDeclaration -> methodDeclaration.getName().getSimpleName().equals("getEarTestArchive") ||
                            methodDeclaration.getName().getSimpleName().equals("getWarTestArchive") ||
                            methodDeclaration.getName().getSimpleName().equals("getJarTestArchive"));

            // If the class already has an `ee deployment()` method, don't make any changes to it.
            if (deploymentMethodExists) {
                return classDecl;
            }


            String pkg = classDecl.getType().getPackageName();
            String ee10pkg = EE11_2_EE10.mapEE11toEE10(pkg);
            JarProcessor jarProcessor = null;
            try {

                if (Jar2ShrinkWrap.isLegacyTestPackage(ee10pkg)) {
                    // Generate the deployment() method
                    jarProcessor = Jar2ShrinkWrap.fromPackage(ee10pkg, new ClassNameRemappingImpl(classDecl.getType().getFullyQualifiedName()));
                } else {
                    if(log.isLoggable(Level.FINEST)) {
                        log.finest("AddArquillianDeployMethodRecipe: ignoring package " + ee10pkg);
                    }
                    return classDecl;
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                // just print exception call stack for now and skip test
                return classDecl;
            }

            Set<String> vehicleNames = testVehicles(ee10pkg);
            if(log.isLoggable(Level.FINEST)) {
                log.finest("vehicleNames for " + ee10pkg + " = " + vehicleNames);
            }
            // app client container vehicles are named: appclient, appmanaged, appmanagedNoTx, ejb, stateful3, stateless3, wsappclient, wsejb
            // see https://github.com/jakartaee/platform-tck/tree/tckrefactor/common/src/main/java/com/sun/ts/tests/common/vehicle for each test vehicle

            // Generate deployment method
            String methodCode = TestGenerator.saveOutput(jarProcessor);

            if (methodCode.length() == 0) {
                // we shouldn't hit this case but still check for it
                throw new RuntimeException("AddArquillianDeployMethodRecipe generated empty code block that is supposed to handle deployment of " + classDecl.getType().getFullyQualifiedName());
            }


            final JavaTemplate deploymentTemplate =
                    JavaTemplate.builder(methodCode).imports()
                            .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                            .imports(
                                    "org.jboss.arquillian.container.test.api.Deployment",
                                    "org.jboss.shrinkwrap.api.Archive",
                                    "org.jboss.shrinkwrap.api.ShrinkWrap",
                                    "org.jboss.shrinkwrap.api.spec.EnterpriseArchive",
                                    "org.jboss.shrinkwrap.api.spec.JavaArchive",
                                    "org.jboss.shrinkwrap.api.spec.EnterpriseArchive",
                                    "org.jboss.shrinkwrap.api.spec.WebArchive",
                                    "org.jboss.shrinkwrap.api.spec.JavaArchive"
                            )
                            .build();
            maybeAddImport("org.jboss.arquillian.container.test.api.Deployment");
            maybeAddImport("org.jboss.shrinkwrap.api.Archive");
            maybeAddImport("org.jboss.shrinkwrap.api.ShrinkWrap");
            maybeAddImport("org.jboss.shrinkwrap.api.spec.EnterpriseArchive");
            maybeAddImport("org.jboss.shrinkwrap.api.spec.WebArchive");
            maybeAddImport("org.jboss.shrinkwrap.api.spec.JavaArchive");

            // Interpolate the fullyQualifiedClassName into the template and use the resulting LST to update the class body
            try {

                //classDecl = classDecl.withBody(deploymentTemplate.apply(new Cursor(getCursor(), classDecl.getBody()),
                //        classDecl.getBody().getCoordinates().lastStatement(),
                //        fullyQualifiedClassName));
                classDecl = classDecl.withBody(deploymentTemplate.apply(new Cursor(getCursor(), classDecl.getBody()),
                        classDecl.getBody().getCoordinates().firstStatement()));
            } catch (RuntimeException e) {
                throw new RuntimeException("AddArquillianDeployMethodRecipe: error " + e.getMessage() + "occurred for (EE11) " + pkg + " (EE10) " + ee10pkg +
                                        " classDecl.getType() = " + classDecl.getType() + "methodCode " + methodCode, e);
            }
            return classDecl;
        }

        private Set<String> testVehicles(String ee10pkg) {
            File vehiclePropertiesFile = Jar2ShrinkWrap.getEETestVehiclesFile();
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(vehiclePropertiesFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String testPackage = ee10pkg.replace(".", "/");
            boolean foundTestVehicles = false;
            String vehicles = null;
            while (!foundTestVehicles) {
                vehicles = properties.getProperty(testPackage);
                foundTestVehicles = vehicles != null;
                if (!foundTestVehicles) {
                    // remove one package segment at a time until there are none left
                    if (testPackage.contains("/")) {
                        testPackage = testPackage.substring(0, testPackage.lastIndexOf("/"));
                    } else {
                        break;
                    }
                } else if (vehicles.contains(".java")) {
                    // TODO: deal with vehicle mappings that are specified at the test class + test method name level.  Like the following:
                    //  com/sun/ts/tests/jpa/core/entityManager/Client.java#mergeTest = appmanagedNoTx pmservlet stateless3
                    //  com/sun/ts/tests/jpa/core/entityManager/Client.java#setPropertyTest = stateless3 stateful3 appmanaged puservlet appmanagedNoTx
                    //  com/sun/ts/tests/jpa/core/enums/Client.java#setgetFlushModeEntityManagerTest = stateless3 stateful3 appmanaged puservlet appmanagedNoTx
                    //  com/sun/ts/tests/jpa/core/StoredProcedureQuery/Client.java#executeUpdateTransactionRequiredExceptionTest = appmanagedNoTx pmservlet puservlet stateless3
                    throw new IllegalStateException("named test" + ee10pkg);
                }
            }
            if (vehicles == null) {
                // TODO: instead of returning empty vehicle list, return an indicator that the default should be used which should at least cover web deployment archives which have no test vehicles included
                return new HashSet<String>();
            }
            return new HashSet<String>(Arrays.asList(vehicles.split(" ")));
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            Set<String> methodNameSet = threadLocalMethodNamesSet.get();
            if (methodNameSet != null) {
                methodNameSet.add(method.getSimpleName().toLowerCase());
            }
            return super.visitMethodDeclaration(method, executionContext);
        }
    }
}
