package tck.jakarta.platform.rewrite;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
// import tck.jakarta.platform.rewrite.createtestsource.CreateNewEETest;
// import tck.jakarta.platform.rewrite.mapping.        ClassNameRemappingImpl;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import tck.jakarta.platform.ant.api.TestClientFile;
import tck.jakarta.platform.ant.api.TestMethodInfo;
import tck.jakarta.platform.ant.api.TestPackageInfo;
import tck.jakarta.platform.rewrite.mapping.EE11_2_EE10;
import tck.jakarta.platform.ant.api.TestPackageInfoBuilder;
// import tck.jakarta.platform.ant.api.DeploymentMethodInfoBuilder;


/**
 * GenerateNewTestClassRecipe       is used to generate new EE test classes
 * instead of updating existing test classes.
 *
 * @author Scott Marlow
 */
public class GenerateNewTestClassRecipe extends Recipe implements Serializable {
    private static final Logger log = Logger.getLogger(GenerateNewTestClassRecipe.class.getName());
    private static ThreadLocal<List> threadLocalMethodInfoList = new ThreadLocal<>();
    private static File generateTestFile = null;

    static final long serialVersionUID = 427023419L;
    private static String fullyQualifiedClassName = GenerateNewTestClassRecipe.class.getCanonicalName();
    private static Path tsHome = Paths.get(System.getProperty("ts.home"));

    private static Path srcDir = Paths.get(System.getProperty("tcksourcepath"));

    static {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Preparing to process with recipe " + fullyQualifiedClassName +
                    " ts.home = " + tsHome +
                    " tcksourcepath = " + srcDir
            );
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
         *
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
                    Path newFile = Files.move(generateTestFile.toPath(), Path.of(c.getSourcePath().getParent().toString(), generateTestFile.getName()), REPLACE_EXISTING);
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

            // will populate methodNameList in visitMethodDeclaration method
            List<TestMethodInfo> methodNameList = new ArrayList<>(); // will contain set of methods in the current classDecl
            threadLocalMethodInfoList.set(methodNameList);
            classDecl = super.visitClassDeclaration(classDecl, executionContext);
            threadLocalMethodInfoList.set(null);

            if (methodNameList.size() == 0) {
                log.fine("ignore class (" + classDecl.getSimpleName() + ") with zero test methods");
                return classDecl;
            }

            String pkg = classDecl.getType().getPackageName();
            if (isNewlyAddedTest(pkg)) {
                log.fine("ignore newly added test package " + pkg);
                return classDecl;
            } else if (isComponentOnlyTest(pkg)) {
                log.fine("ignore component only test package" + pkg);
                return classDecl;
            }

            String ee10pkg = EE11_2_EE10.mapEE11toEE10(pkg);
            try {

                if (isLegacyTestPackage(ee10pkg)) {
                    String tckClassName = classDecl.getType().getFullyQualifiedName();
                    Class tckClass = Class.forName(tckClassName);
                    if (tckClass == null) {
                        throw new RuntimeException("Could not load TCK test class name " + tckClassName);
                    }
                    TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
                    // update the methods to use the correct Throws exception
                    methodNameList = correctThrowsException(tckClass, methodNameList);

                    TestPackageInfo pkgInfo = builder.buildTestPackgeInfoEx(tckClass, methodNameList);
                    log.info("About to generate test class(es) for " + classDecl.getType().getFullyQualifiedName() + ", EE 10 test package " + ee10pkg + " EE 11 test package " + pkg);
                    for (TestClientFile testClient : pkgInfo.getTestClientFiles()) {
                        // The test package dir under the test module src/main/java directory
                        Path testPkgDir = srcDir.resolve(testClient.getPackage().replace(".", "/"));
                        Files.createDirectories(testPkgDir);
                        // The test client .java file
                        Path testClientJavaFile = testPkgDir.resolve(testClient.getName() + ".java");
                        if (testClientJavaFile.toFile().exists()) {
                            throw new IllegalStateException(testClientJavaFile + " was already previously generated which means we aren't handling something correctly." );
                        }
                        // Write out the test client .java file content
                        Files.writeString(testClientJavaFile, testClient.getContent(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        log.info("Generated " + testClientJavaFile + " for " + classDecl.getType().getFullyQualifiedName());
                    }
                } else {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("AddArquillianDeployMethodRecipe: ignoring package " + ee10pkg);
                    }
                    return classDecl;
                }
            } catch (RuntimeException e) {
                log.info("due to " + e.getMessage() + " class " + classDecl.getType().getFullyQualifiedName() + " couldn't be processed.");
                e.printStackTrace();
                // just print exception call stack for now and skip test
                return classDecl;
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e.getMessage() + ": Check if .class is available for source: " + classDecl.getType().getFullyQualifiedName(), e);
            }
            return classDecl;
        }

        /**
         * @param tckTestClass
         * @param methodInfoList
         * @return list of new TestMethodInfo("lookupTimerService", "InterruptedException, java.util.concurrent.ExecutionException"),
         * new TestMethodInfo("writeLockTimeout", "")
         */
        private List<TestMethodInfo> correctThrowsException(Class tckTestClass, List<TestMethodInfo> methodInfoList) {
            if (tckTestClass == null) {
                throw new IllegalStateException("missing TCK class");
            }
            Object[] testMethodInfoArray = methodInfoList.toArray();
            for (int index = 0; index < testMethodInfoArray.length; index++) {
                boolean foundMatch = false;
                Class tckClass = tckTestClass;
                boolean fromSuperClass = false;
                do {
                    Method[] methods = tckClass.getMethods();
                    for (Method method : methods) {
                        String name = method.getName();
                        if (!((TestMethodInfo) testMethodInfoArray[index]).getMethodName().equals(name)) {
                            continue;
                        }
                        foundMatch = true;
                        Class<?>[] exceptionTypes = method.getExceptionTypes();
                        String throwsExceptions = "";
                        String separator = "";

                        for (Class<?> exceptionType : exceptionTypes) {
                            throwsExceptions += separator + exceptionType.getName();
                            separator = ", ";
                        }
                        TestMethodInfo testMethodInfo = new TestMethodInfo(name, throwsExceptions);
                        testMethodInfo.setFromSuperclass(fromSuperClass);
                        testMethodInfoArray[index] = testMethodInfo;
                    }
                    tckClass = tckClass.getSuperclass();
                    if (tckClass == null) {
                        break;
                    }
                    fromSuperClass = true;
                } while (!foundMatch && tckClass != null && !tckClass.getName().equals("Ljava.lang.Object"));
            }

            methodInfoList = new ArrayList<>();
            for (int index = 0; index < testMethodInfoArray.length; index++) {
                methodInfoList.add((TestMethodInfo) testMethodInfoArray[index]);
            }
            return methodInfoList;
        }

        private boolean isNewlyAddedTest(String packageName) {
            // return true if specified test was newly added to Jakarta EE 11 Platform TCK
            return packageName.startsWith("ee.jakarta.tck.persistence.core.types.datetime");
        }

        private boolean isComponentOnlyTest(String packageName) {
            return packageName.startsWith("ee.jakarta.tck.persistence.jpa22.se") ||
                   packageName.startsWith("ee.jakarta.tck.persistence.se");
        }
        private boolean isLegacyTestPackage(String packageName) {

            if (packageName.startsWith("ee.jakarta.tck")) {
                throw new RuntimeException("EE 11 package name passed that should of been converted to EE 10 before calling.  Package name = " + packageName);
            }
            return packageName.startsWith("com.sun.ts.tests");
        }

        private final String TESTNAME = "@testName:";

        @Override
        public Space visitSpace(Space space, Space.Location loc, ExecutionContext executionContext) {
            List<TestMethodInfo> methodNameList = threadLocalMethodInfoList.get();
            List<Comment> comments = space.getComments();
            if (comments != null) {
                for (Comment comment : comments) {
                    if (comment instanceof TextComment) {
                        TextComment textComment = (TextComment) comment;
                        String text = textComment.getText();
                        int index;

                        if ((index = text.indexOf(TESTNAME)) != -1) {
                            // add TestMethodInfo with just the MethodName which we will look up in the super class and update
                            String testName;
                            while (index != -1) {
                                index += TESTNAME.length() + 1;  // skip past marker
                                text = text.substring(index);
                                int spaceAfterIndex = text.indexOf(' ');
                                int linebreakAfterIndex = text.indexOf('\n');
                                if (linebreakAfterIndex == -1) { // no more lines after this one
                                    if (spaceAfterIndex == -1) {
                                        testName = text;
                                    } else {
                                        testName = text.substring(0, spaceAfterIndex);
                                    }
                                    index = -1;
                                } else {                        // have another line to process after this one
                                    if (spaceAfterIndex == -1) {
                                        testName = text.substring(0, linebreakAfterIndex);
                                    } else if (spaceAfterIndex < linebreakAfterIndex) {
                                        testName = text.substring(0, spaceAfterIndex);
                                    } else {
                                        testName = text.substring(0, linebreakAfterIndex);
                                    }
                                    index = linebreakAfterIndex + 1; // move to character after newline
                                }
                                if (index != -1 && index < text.length()) {
                                    text = text.substring(index);
                                    index = text.indexOf(TESTNAME);
                                } else {
                                    index = -1;
                                }
                                log.fine("testName: " + testName);
                                TestMethodInfo testMethodInfo = new TestMethodInfo(testName, "java.lang.Exception");
                                methodNameList.add(testMethodInfo);
                            }
                        }
                    }
                }
            }
            return super.visitSpace(space, loc, executionContext);
        }
    }
}
