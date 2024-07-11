package tck.jakarta.platform.rewrite;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import tck.jakarta.platform.rewrite.createtestsource.CreateNewEETest;
import tck.jakarta.platform.rewrite.mapping.ClassNameRemappingImpl;
import tck.jakarta.platform.rewrite.mapping.EE11_2_EE10;

/**
 * GenerateNewTestClassRecipe is a fork of AddArquillianDeployMethodRecipe that is used to generate new EE test classes
 * instead of updating existing test classes.
 *
 * @author Scott Marlow
 */
public class GenerateNewTestClassRecipe extends Recipe implements Serializable {
    private static final Logger log = Logger.getLogger(GenerateNewTestClassRecipe.class.getName());
    private static ThreadLocal<Set> threadLocalMethodNamesSet = new ThreadLocal<>();
    private static File generateTestFile = null;

    static final long serialVersionUID = 427023419L;
    private static String fullyQualifiedClassName = GenerateNewTestClassRecipe.class.getCanonicalName();

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
        GenerateNewTestClassRecipe that = (GenerateNewTestClassRecipe) o;
        return Objects.equals(fullyQualifiedClassName, that.fullyQualifiedClassName);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // getVisitor() should always return a new instance of the visitor to avoid any state leaking between cycles
        return new testClassVisitor();
    }

    public class testClassVisitor extends JavaIsoVisitor<ExecutionContext> {
        /**
         * Called after visitClassDeclaration which we will use to determine the file path of the Test Client class
         * just processed last.
         * @param tree
         * @param executionContext
         * @return
         */
        @Override
        public @Nullable J postVisit(J tree, ExecutionContext executionContext) {

            if (tree instanceof JavaSourceFile && generateTestFile != null) {
                JavaSourceFile c = (JavaSourceFile) requireNonNull(tree);
                System.out.println("postVisit source file source path = " + c.getSourcePath().toFile().getAbsolutePath());// ((CompilationUnit)c).sourcePath.toFile().getAbsolutePath()
                // c.getSourcePath().toFile().getAbsolutePath() = jpa/spec-tests/src/main/java/ee/jakarta/tck/persistence/core/StoredProcedureQuery/Client.java
                // c.getSourcePath().getParent().toFile().getAbsolutePath() = /home/smarlow/tck/platformtck/jpa/spec-tests/src/main/java/ee/jakarta/tck/persistence/core/EntityGraph
                //
                try {
                    // move generated EE test client file to same package location as the test client currently is in
                    Path newFile = Files.move(generateTestFile.toPath(), Path.of(c.getSourcePath().getParent().toString(), generateTestFile.getName()),REPLACE_EXISTING);
                    System.out.println("new test file location is " + newFile + " which should be same location as " + c.getSourcePath().toFile());
                    generateTestFile = null;

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }
            return super.postVisit(tree, executionContext);

        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            List<J.Modifier> modifiers = classDecl.getModifiers();
            boolean isAbstract = modifiers.stream().anyMatch(modifier -> modifier.getType() == J.Modifier.Type.Abstract);

            if (isAbstract) {
                // Ignore refactoring abstract classes
                return classDecl;
            }

            boolean isTest = classDecl.getSimpleName().contains("Client"); // this will match too much but still try
            if (!isTest) {
                return classDecl;
            }

            // will populate methodNameSet in visitMethodDeclaration method
            Set<String> methodNameSet = new HashSet<>(); // will contain set of methods in the current classDecl
            threadLocalMethodNamesSet.set(methodNameSet);
            super.visitClassDeclaration(classDecl, executionContext);
            isTest = methodNameSet.stream().anyMatch(str -> str.contains("test"));
            threadLocalMethodNamesSet.set(null);


            // return if this is not a test client class
            if (!isTest) {
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
                log.info("due to " + e.getMessage() + " class" + classDecl.getType().getFullyQualifiedName() + " couldn't be processed.");
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

            try {
                generateTestFile = CreateNewEETest.generateJavaSourceFileContent(jarProcessor, methodNameSet, pkg, classDecl.getType().getClassName());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (generateTestFile == null || !generateTestFile.exists()) {
                // we shouldn't hit this case but still check for it
                throw new RuntimeException("generateJavaSourceFileContent output doesn't exist for" + classDecl.getType().getFullyQualifiedName());
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
