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
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.Javadoc;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import tck.jakarta.platform.ant.api.DefaultEEMapping;
import tck.jakarta.platform.ant.api.TestClientFile;
import tck.jakarta.platform.ant.api.TestMethodInfo;
import tck.jakarta.platform.ant.api.TestPackageInfo;

import tck.jakarta.platform.ant.api.TestPackageInfoBuilder;


/**
 * GenerateNewTestClassRecipe       is used to generate new EE test classes
 * instead of updating existing test classes.
 *
 * @author Scott Marlow
 */
public class GenerateNewTestClassRecipe extends Recipe implements Serializable {
    private static final Logger log = Logger.getLogger(GenerateNewTestClassRecipe.class.getName());
    private static final ThreadLocal<List> threadLocalMethodInfoList = new ThreadLocal<>();
    private static File generateTestFile = null;

    static final long serialVersionUID = 427023419L;
    private static final String fullyQualifiedClassName = GenerateNewTestClassRecipe.class.getCanonicalName();
    // EE10 tck home
    private static final Path tsHome = Paths.get(System.getProperty("ts.home"));
    // EE11 tck module src root, usually src/main/java
    private static final Path srcDir = Paths.get(System.getProperty("tcksourcepath"));
    // Optional property to restrict to a specific test package
    private static final String tckpackage = System.getProperty("tckpackage");
    private static boolean overwriteExistingTests = Boolean.valueOf(System.getProperty("overwriteExistingTests", "false"));

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
        return Objects.equals(fullyQualifiedClassName, fullyQualifiedClassName);
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
            // return if the test is not in the specified tckpackage
            if (tckpackage != null && !classDecl.getType().getPackageName().equals(tckpackage)) {
                return classDecl;
            }

            List<TestMethodInfo> methodNameList = new ArrayList<>(); // will contain set of methods in the current classDecl
            threadLocalMethodInfoList.set(methodNameList);
            classDecl = super.visitClassDeclaration(classDecl, executionContext);
            threadLocalMethodInfoList.set(null);

            if (methodNameList.size() == 0) {
                log.fine("TODO: investigate why there are no tests methods for class " + classDecl.getSimpleName());
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

            String ee10pkg = DefaultEEMapping.getInstance().getEE10TestPackageName(pkg);
            try {

                if (isLegacyTestPackage(ee10pkg)) {
                    String tckClassName = classDecl.getType().getFullyQualifiedName();
                    Class tckClass = Class.forName(tckClassName);
                    if (tckClass == null) {
                        throw new RuntimeException("TODO: Could not load TCK test class name " + tckClassName);
                    }
                    TestPackageInfoBuilder builder = new TestPackageInfoBuilder(tsHome);
                    // update the methods to use the correct Throws exception
                    methodNameList = correctThrowsException(tckClass, methodNameList);

                    TestPackageInfo pkgInfo = builder.buildTestPackgeInfoEx(tckClass, methodNameList, DefaultEEMapping.getInstance());
                    log.info("About to generate test class(es) for " + classDecl.getType().getFullyQualifiedName() + ", EE 10 test package " + ee10pkg + " EE 11 test package " + pkg);
                    for (TestClientFile testClient : pkgInfo.getTestClientFiles()) {
                        // The test package dir under the test module src/main/java directory
                        Path testPkgDir = srcDir.resolve(testClient.getPackage().replace(".", "/"));
                        Files.createDirectories(testPkgDir);
                        // The test client .java file
                        Path testClientJavaFile = testPkgDir.resolve(testClient.getName() + ".java");
                        if (!overwriteExistingTests && testClientJavaFile.toFile().exists()) {
                            log.warning("TODO: " + testClientJavaFile + " was already previously generated which means we aren't handling something correctly.");
                            Thread.dumpStack();
                            continue;
                        }
                        // Write out the test client .java file content
                        Files.writeString(testClientJavaFile, testClient.getContent(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                        log.info("Generated " + testClientJavaFile + " for " + classDecl.getType().getFullyQualifiedName());
                    }
                } else {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("AddArquillianDeployMethodRecipe: ignoring new test package " + ee10pkg);
                    }
                    return classDecl;
                }
            } catch (RuntimeException e) {
                log.info("TODO: due to " + e.getMessage() + " class " + classDecl.getType().getFullyQualifiedName() + " couldn't be processed.");
                e.printStackTrace();
                // just print exception call stack for now and skip test
                return classDecl;
            } catch (IOException e) {
                log.info("TODO: due to " + e.getMessage() + " class " + classDecl.getType().getFullyQualifiedName() + " couldn't be processed.");
                e.printStackTrace();
                // just print exception call stack for now and skip test
                return classDecl;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("TODO: " + e.getMessage() + ": Check if .class is available for source: " + classDecl.getType().getFullyQualifiedName(), e);
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
            methodInfoList = removeDuplicateMethodNames(methodInfoList);
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

        private List<TestMethodInfo> removeDuplicateMethodNames(List<TestMethodInfo> methodInfoList) {
            Set<String> methodList = new HashSet<>();
            int[] duplicates = new int[methodInfoList.size()];
            int lastDuplicateAdded = 0;
            int index = 0;
            for (TestMethodInfo testMethodInfo : methodInfoList) {
                if (!methodList.add(testMethodInfo.getMethodName())) {
                    duplicates[lastDuplicateAdded] = index;
                    lastDuplicateAdded++;
                }
                index++;
            }

            // remove duplicates from methodInfoList
            while (lastDuplicateAdded > 0) {
                Object removedTestMethodInfo = methodInfoList.remove(duplicates[lastDuplicateAdded - 1]);
                log.info("removed duplicate TestMethodInfo " + ((TestMethodInfo) (removedTestMethodInfo)).getMethodName());
                lastDuplicateAdded--;
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

        private String jdocString(List<Javadoc> content) {
            if (content == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (Javadoc jd : content) {
                if (jd instanceof Javadoc.Text text) {
                    sb.append(text.getText().trim());
                }
            }
            return sb.toString();
        }

        @Override
        public Space visitSpace(Space space, Space.Location loc, ExecutionContext executionContext) {
            space = super.visitSpace(space, loc, executionContext);
            List<TestMethodInfo> methodNameList = threadLocalMethodInfoList.get();
            List<Comment> comments = space.getComments();
            if (comments != null) {
                for (Comment c : comments) {
                    if (c instanceof TextComment) {
                        String text = ((TextComment) c).getText();
                        int testNameIndex = text.indexOf("testName:");
                        if (testNameIndex >= 0) {
                            // Java comment with a @testName tag. This may not apply to method, so parse the name
                            String nameText = text.substring(testNameIndex + 9).trim();
                            String[] parts = nameText.split("\\s+", 2);
                            if (parts[0].equals("*")) {
                                parts = parts[1].split("\\s+", 2);
                            }
                            String commentMethodName = parts[0];
                            log.info("testName: " + commentMethodName);
                            TestMethodInfo testMethodInfo = new TestMethodInfo(commentMethodName, "java.lang.Exception");
                            methodNameList.add(testMethodInfo);
                        }
                    } else if (c instanceof Javadoc.DocComment docComment) {
                        for (Javadoc javadoc : docComment.getBody()) {
                            if (javadoc instanceof Javadoc.UnknownBlock jdu) {
                                String name = jdu.getName();
                                if (name.equals("testName:")) {
                                    String methodName = jdocString(jdu.getContent());
                                    TestMethodInfo testMethodInfo = new TestMethodInfo(methodName, "java.lang.Exception");
                                    methodNameList.add(testMethodInfo);
                                    log.info("testName: " + methodName);
                                } else {
                                    log.finest("TODO: skipped handling UnknownBlock Javadoc.DocComment " + javadoc);
                                }
                            } else {
                                log.finest("TODO: skipped handling Javadoc.DocComment " + javadoc);
                            }
                        }
                    }
                }
            }
            return space;
        }
    }
}
