package tck.jakarta.platform.ant.api;

/**
 * Used to describe the test method in a test class that should be overriden as JUit 5 tests.
 */
public class TestMethodInfo {
    private String methodName;
    // fully qualified class name of the throws type
    private String throwsException;

    public TestMethodInfo(String methodName, String throwsException) {
        this.methodName = methodName;
        this.throwsException = throwsException;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getThrowsException() {
        return throwsException;
    }

    public String toString() {
        return String.format("%s(...) throws %s", methodName, throwsException);
    }
}
