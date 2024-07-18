package tck.jakarta.platform.rewrite.createtestsource;

import java.io.PrintWriter;

/**
 * CreateTestSourceFile
 *
 * @author Scott Marlow
 */
public class CreateTestSourceFile {
    private int indentation = 0;
    PrintWriter printWriter;

    public CreateTestSourceFile(PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    public CreateTestSourceFile copyright() {
        // TODO
        return this;
    }

    public CreateTestSourceFile emptyLine() {
        printWriter.println("");
        return this;
    }

    public CreateTestSourceFile addImport(String classname) {
        printWriter.printf("\nimport %s;", classname);
        return this;
    }

    public CreateTestSourceFile addPackage(String testPackageName) {
        printWriter.printf("package %s;\n", testPackageName);
        return this;
    }

    public CreateTestSourceFile startBlock() {
        // indent();
        printWriter.print("{\n");
        indentation++;
        return this;
    }

    public CreateTestSourceFile startBlockSameLine() {
        printWriter.print("{");
        indentation++;
        return this;

    }

    public CreateTestSourceFile endBlock() {
        indentation--;
        indent();
        printWriter.print("}\n");
        return this;
    }

    public CreateTestSourceFile indent() {
        for(int loop = 0; loop < indentation; loop++) {
            printWriter.print("    ");
        }
        return this;
    }

    public CreateTestSourceFile publicClass(String generateEETestClassName, String testClientClassName) {
            printWriter.printf("\n");
        indent();
        printWriter.printf("public class %s extends %s ", generateEETestClassName, testClientClassName);
        return startBlock();
    }

    public CreateTestSourceFile methodAnnotation(String annotationName) {
        printWriter.printf("\n%s", annotationName );
        return this;
    }

    public CreateTestSourceFile addMethod(String methodName) {
        printWriter.printf("\n%s", methodName );
        return startBlock();
    }


    public CreateTestSourceFile addMethod(String methodAnnotation, String method, String body) {
        indent();
        printWriter.printf("%s\n", methodAnnotation);
        indent();
        printWriter.printf("%s ", method);
        startBlockSameLine();
        newline();
        indent();
        printWriter.printf("%s", body);
        return this;
    }

    private CreateTestSourceFile newline() {
        printWriter.printf("\n");
        return this;
    }

    public CreateTestSourceFile addStatement(String statement) {
        indent();
        printWriter.print(statement);
        return this;
    }

    public CreateTestSourceFile addTestMethod(String methodName) {
        printWriter.printf("\npublic void %s() throws Exception ", methodName );
        return this;
    }
}
