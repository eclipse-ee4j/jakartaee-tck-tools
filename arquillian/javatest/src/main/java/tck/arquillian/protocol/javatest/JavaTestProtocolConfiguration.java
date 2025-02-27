package tck.arquillian.protocol.javatest;

import org.jboss.arquillian.container.test.spi.client.protocol.ProtocolConfiguration;
import tck.arquillian.protocol.common.ProtocolCommonConfig;

public class JavaTestProtocolConfiguration implements ProtocolConfiguration, ProtocolCommonConfig {
    // test working directory
    private String workDir;
    // EE10 type of ts.jte file location
    private String tsJteFile;
    // EE10 type of tssql.stmt file location
    private String tsSqlStmtFile;
    // harness.log.traceflag
    private boolean trace;
    // Should the VehicleClient main be run in a separate JVM
    private boolean fork;
    //
    private boolean anySetter;

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
        this.anySetter = true;
    }

    public String getTsJteFile() {
        return tsJteFile;
    }

    public void setTsJteFile(String tsJteFile) {
        this.tsJteFile = tsJteFile;
        this.anySetter = true;
    }

    @Override
    public String getTsSqlStmtFile() {
        return tsSqlStmtFile;
    }
    @Override
    public void setTsSqlStmtFile(String tsSqlStmtFile) {
        this.tsSqlStmtFile = tsSqlStmtFile;
        this.anySetter = true;
    }

    public boolean isTrace() {
        return trace;
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
        this.anySetter = true;
    }

    public boolean isFork() {
        return fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
        this.anySetter = true;
    }

    public boolean wasAnySetterCalled() {
        return anySetter;
    }
}
