package tck.jakarta.platform.rewrite;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
// import tck.jakarta.platform.rewrite.createtestsource.CreateNewEETest;
// import tck.jakarta.platform.rewrite.mapping.        ClassNameRemappingImpl;
import tck.jakarta.platform.ant.api.TestClientFile;
import tck.jakarta.platform.ant.api.TestPackageInfo;
import tck.jakarta.platform.rewrite.mapping.EE11_2_EE10;
import tck.jakarta.platform.ant.api.TestPackageInfoBuilder;
// import tck.jakarta.platform.ant.api.DeploymentMethodInfoBuilder;
import tck.jakarta.platform.vehicles.VehicleType;

/**
 * GenerateNewTestClassRecipe       is used to generate new EE test classes
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
    private static String tcktestgroup = System.getProperty("tcktestgroup","jpa");
    private static Path tsHome = Paths.get(System.getProperty("ts.home"));

    static {
        if(log.isLoggable(Level.FINEST)) {
            log.finest("GenerateNewTestClassRecipe              : Preparing to process " + fullyQualifiedClassName);
        }
    }

    @Override
    public String getDisplayName() {
        return "Convert to Arquillian/Shrinkwrap/Junit5";
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
                log.fine("ignore non-test class " + classDecl.getSimpleName());
                return classDecl;
            }

            // will populate methodNameSet in visitMethodDeclaration method
            Set<String> methodNameSet = new HashSet<>(); // will contain set of methods in the current classDecl
            threadLocalMethodNamesSet.set(methodNameSet);
            // TODO: return the value returned by super.visitClassDeclaration
            // classDecl = super.visitClassDeclaration(classDecl, executionContext);
            classDecl = super.visitClassDeclaration(classDecl, executionContext);
            isTest = methodNameSet.stream().anyMatch(str -> str.contains("test"));
            threadLocalMethodNamesSet.set(null);


            // return if this is not a test client class
            if (!isTest) {
                log.fine("ignore class with zero test methods " + classDecl.getSimpleName());
                return classDecl;
            }


            String pkg = classDecl.getType().getPackageName();
            String ee10pkg = EE11_2_EE10.mapEE11toEE10(pkg);
            if (!ee10pkg.contains(tcktestgroup)) {
                log.fine("ignore class " + classDecl.getSimpleName() + " that is not from tcktestgroup test group " + tcktestgroup);
                return classDecl;
            }
            try {

                if (isLegacyTestPackage(ee10pkg)) {
                    // Generate the deployment() method
                    // jarProcessor = Jar2ShrinkWrap.fromPackage(ee10pkg, new ClassNameRemappingImpl(classDecl.getType().getFullyQualifiedName()));
                    //DeploymentMethodInfoBuilder builder = new DeploymentMethodInfoBuilder(tsHome);
                    //DeploymentMethodInfo deployMethod = builder.forTestClassAndVehicle(classDecl.getClass(), VehicleType.appclient);
                    String tckClassName = classDecl.getType().getFullyQualifiedName();
                    Class tckClass = Class.forName(tckClassName);
                    TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
                    List<String> testMethods = methodNameSet.stream().toList();
                    TestPackageInfo pkgInfo = builder.buildTestPackgeInfo(tckClass, testMethods);
                    System.out.println(pkgInfo);
                    System.out.println("deployMethod for " + classDecl.getClass().getName() + " ee10pkg " + ee10pkg + " builder" + builder);

                    System.out.println("TestClasses:");
                        // The test module src/main/java directory
                        Path srcDir = Paths.get("/tmp");
                            for (TestClientFile testClient : pkgInfo.getTestClientFiles()) {
                            // The test package dir under the test module src/main/java directory
                            Path testPkgDir = srcDir.resolve(testClient.getPackage().replace(".", "/"));
                            Files.createDirectories(testPkgDir);
                            // The test client .java file
                            Path tetClientJavaFile = testPkgDir.resolve(testClient.getName() + ".java");
                            // Write out the test client .java file content
                            Files.writeString(tetClientJavaFile, testClient.getContent(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        }

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
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            // Set<String> vehicleNames = testVehicles(ee10pkg);

            // app client container vehicles are named: appclient, appmanaged, appmanagedNoTx, ejb, stateful3, stateless3, wsappclient, wsejb
            // see https://github.com/jakartaee/platform-tck/tree/tckrefactor/common/src/main/java/com/sun/ts/tests/common/vehicle for each test vehicle

            // Generate deployment method

            // try {
            //    generateTestFile = CreateNewEETest.generateJavaSourceFileContent(jarProcessor, methodNameSet, pkg, classDecl.getType().getClassName());

            //} catch (IOException e) {
            //    throw new RuntimeException(e);
            //}

            if (generateTestFile == null || !generateTestFile.exists()) {
                // we shouldn't hit this case but still check for it
                throw new RuntimeException("generateJavaSourceFileContent output doesn't exist for" + classDecl.getType().getFullyQualifiedName());
            }
            return classDecl;
        }

        private boolean isLegacyTestPackage(String packageName) {

            if (packageName.startsWith("ee.jakarta.tck")) {
                throw new RuntimeException("EE 11 package name passed that should of been converted to EE 10 before calling.  Package name = " + packageName);
            }
            return packageName.startsWith("com.sun.ts.tests");
        }



        // TODO: also return super class test methods
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
