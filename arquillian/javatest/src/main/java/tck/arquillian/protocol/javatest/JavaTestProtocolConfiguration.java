package tck.arquillian.protocol.javatest;

import org.jboss.arquillian.container.test.spi.client.protocol.ProtocolConfiguration;
import tck.arquillian.protocol.common.ProtocolCommonConfig;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class JavaTestProtocolConfiguration implements ProtocolConfiguration, ProtocolCommonConfig {
    static JavaTestProtocolConfiguration instance;

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

    public static Properties getTsJteProps() throws IOException {
        Properties tsJteProps = new Properties();
        Path tsJteFile = Paths.get(instance.getTsJteFile());
        tsJteProps.load(new FileReader(tsJteFile.toFile()));
        return tsJteProps;
    }

    public JavaTestProtocolConfiguration() {
        instance = this;
    }

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

    public String getVehicleArchiveName() {
        return "none";
    }
    public boolean wasAnySetterCalled() {
        return anySetter;
    }
}
