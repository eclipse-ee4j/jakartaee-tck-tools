package tck.arquillian.protocol.common;

public interface ProtocolCommonConfig {
    /**
     * Check if the test is an appclient test
     * @return true if the test is an appclient test
     */
    default boolean isAppClient() {
        return false;
    };

    /**
     * A trace flag to enable tracing
     * @return true if trace debugging is enabled
     */
    public boolean isTrace();
    public void setTrace(boolean trace);

    /**
     * A working directory needed by some tests
     * @return a path to a working directory
     */
    public String getWorkDir();
    public void setWorkDir(String workDir);

    /**
     * The ts.jte file location for the tests
     * @return a path to the ts.jte file
     */
    public String getTsJteFile();
    public void setTsJteFile(String tsJteFile);

    /**
     * The ts.jte tssql.stmt file location for the tests
     * @return a path to the tssql.stmt file
     */
    public String getTsSqlStmtFile();
    public void setTsSqlStmtFile(String tsJteFile);

    /**
     * Validate if there was any setter called indicating if there was a matching protocol config
     * @return true if any setter was called
     */
    public boolean wasAnySetterCalled();
}
