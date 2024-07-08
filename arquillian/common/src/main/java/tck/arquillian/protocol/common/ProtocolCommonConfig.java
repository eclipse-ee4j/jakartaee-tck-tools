package tck.arquillian.protocol.common;

public interface ProtocolCommonConfig {
    public boolean isTrace();
    public void setTrace(boolean trace);

    public String getWorkDir();
    public void setWorkDir(String workDir);

    public String getTsJteFile();
    public void setTsJteFile(String tsJteFile);
}
