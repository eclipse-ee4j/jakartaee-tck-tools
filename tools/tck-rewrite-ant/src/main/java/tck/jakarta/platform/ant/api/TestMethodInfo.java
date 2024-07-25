package tck.jakarta.platform.ant.api;

/**
 * Used to describe the test method in a test class that should be overriden as JUit 5 tests.
 */
public class TestMethodInfo {
    private String methodName;
    // fully qualified class name(s) of the throws type as it would appear on the method code
    private String throwsException;
    private boolean fromSuperclass;

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
    public void setThrowsException(String throwsException) {
        this.throwsException = throwsException;
    }
    public boolean getThrows() {
        return throwsException != null ? !throwsException.isBlank() : false;
    }

    public boolean isFromSuperclass() {
        return fromSuperclass;
    }

    public void setFromSuperclass(boolean fromSuperclass) {
        this.fromSuperclass = fromSuperclass;
    }

    public String toString() {
        return String.format("%s(...) throws %s; fromSuper=%s", methodName, throwsException, fromSuperclass);
    }
}
