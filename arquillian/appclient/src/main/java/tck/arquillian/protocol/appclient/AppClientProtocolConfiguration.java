package tck.arquillian.protocol.appclient;

import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.test.spi.client.protocol.ProtocolConfiguration;
import tck.arquillian.protocol.common.ProtocolCommonConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Configuration for the Application Client Protocol in the Jakarta EE TCK.
 * This class manages the configuration settings needed to launch and execute
 * Application Client tests within the TCK environment.
 *
 * <p>The configuration includes settings for:
 * <ul>
 *   <li>Application Client process execution environment</li>
 *   <li>Command line arguments and environment variables</li>
 *   <li>Working directories and file locations</li>
 *   <li>Timeout settings and execution modes</li>
 * </ul>
 *
 * @see ProtocolConfiguration
 * @see ProtocolCommonConfig
 */
public class AppClientProtocolConfiguration implements ProtocolConfiguration, ProtocolCommonConfig {
    /**
     * Flag indicating whether to run the client process
     */
    private boolean runClient = true;

    /**
     * Flag indicating if this Application Client is being used as a test vehicle runner.
     * When true, the client acts as a test runner for another vehicle type rather than
     * running as a standalone Application Client.
     */
    private boolean runAsVehicle = false;

    /**
     * Environment variables to be passed to the Application Client process.
     * Format: "env1=value1;env2=value2"
     * 
     * @see Runtime#exec(String[], String[])
     */
    private String clientEnvString;

    /**
     * Command line arguments for the Application Client process.
     * Arguments are separated by the {@link #cmdLineArgSeparator} character (default is semicolon).
     * These arguments are passed directly to the process execution.
     * 
     * @see Runtime#exec(String[], String[])
     */
    private String clientCmdLineString;

    /**
     * Separator character used to split the {@link #clientCmdLineString} into individual arguments.
     * Default value is semicolon (";").
     */
    private String cmdLineArgSeparator = ";";

    /**
     *
     */
    private String clientStubsCmdLine;
    private String clientStubsJarSuffix;

    /**
     * Working directory for the Application Client process.
     * This directory is used as the process working directory when executing the client.
     * 
     * @see Runtime#exec(String[], String[], File)
     */
    private String clientDir;

    /**
     * Directory where the Application Client EAR test artifact will be extracted.
     * Default value is "target/appclient".
     */
    private String clientEarDir = "target/appclient";

    /**
     * Flag to ensure that only one Application Client EAR test artifact is extracted at a time.
     * Default value is false.
     */
    private boolean isolateClientEars = false;

    /**
     * Maximum time in milliseconds to wait for the Application Client process to complete.
     * Default value is 60000ms (1 minute).
     */
    private long clientTimeout = 60000;

    /**
     * Base working directory for test execution.
     */
    private String workDir;

    /**
     * Path to the ts.jte configuration file for Jakarta EE 10+ TCK execution.
     */
    private String tsJteFile;

    /**
     * Path to the tssql.stmt file for Jakarta EE 10+ TCK database operations.
     */
    private String tsSqlStmtFile;

    /**
     * Flag to enable tracing for the Application Client process.
     */
    private boolean trace;

    /**
     * Flag to indicate whether the client EAR should be unpacked.
     * When true, the client EAR will be unpacked into the {@link #clientEarDir} directory.
     */
    private boolean unpackClientEar = false;
    private boolean anySetter;

    // Values set by the AppClientDeploymentPackager

    /**
     * Directory where the Application Client EAR library files are located.
     * Default value is "lib". This is not really an external configuration setting as it
     * will be set if the appclient ear contains an application.xml with a library-directory element.
     */
    private String earLibDir = "lib";
    /**
     * The name of the ear deployment from the ear archive name, or the application.xml/application-name
     */
    private String deploymentName;
    /**
     * The name of the jar in the application client ear as determined by the jar with the Main-Class element.
     */
    private AppClientArchiveName appClientArchiveName;
    /**
     * The name of the archive in the application client ear that contains the vehicle code which calls
     * from the appclient to the server side test code.
     */
    private String vehicleArchiveName;

    public boolean isAppClient() {
        return true;
    }

    public boolean isRunClient() {
        return runClient;
    }

    public void setRunClient(boolean runClient) {
        this.runClient = runClient;
        this.anySetter = true;
    }

    public boolean isRunAsVehicle() {
        return runAsVehicle;
    }

    public void setRunAsVehicle(boolean runAsVehicle) {
        this.runAsVehicle = runAsVehicle;
        this.anySetter = true;
    }

    public boolean isTrace() {
        return trace;
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
        this.anySetter = true;
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

    public String getClientEnvString() {
        return clientEnvString;
    }

    public void setClientEnvString(String clientEnvString) {
        this.clientEnvString = clientEnvString;
        this.anySetter = true;
    }

    public String getClientCmdLineString() {
        return clientCmdLineString;
    }

    /**
     * Set the command line to use for launching the appclient. The individual arguments are separated by the cmdLineArgSeparator
     * setting, which defaults to ';'. A long command line can be split across multiple lines in the arquillian.xml file because
     * the parsed command line array elements are trimmed of leading and trailing whitespace.
     * The command line should be filtered against the ts.jte file if it contains any property references. In addition
     * to ts.jte property references, the command line can contain ${clientEarDir} which will be replaced with the
     * #clientEarDir value. Any ${vehicleArchiveName} ref will be replaced with the vehicle archive name extracted by
     * {@link tck.arquillian.protocol.appclient.AppClientDeploymentPackager}.
     * Any ${clientAppArchive} ref will be replaced with the clientAppArchive extracted by the
     * {@link AppClientDeploymentPackager} processing of the target appclient ear.
     * Any ${clientEarLibClasspath} ref will be replaced with the classpath of the client ear lib directory if
     * {@link #unpackClientEar} is true and the clientEarDir/lib directory exists.
     * Any ${clientAppArchive} ref will be replaced with the clientAppArchive determined by the AppClientDeploymentPackager
     * Any ${clientAppArchiveName} ref will be replaced with the clientAppArchive minus any .jar suffix determined by
     * the AppClientDeploymentPackager
     * Any ${clientStubJar} ref will be replaced by the ${clientAppArchiveName}${clientStubsJarSuffix}.jar value from the
     * AppClientProtocolConfiguration#clientStubsJarSuffix.
     *
     * @param clientCmdLineString a cmdLineArgSeparator delimited string of command line arguments
     */
    public void setClientCmdLineString(String clientCmdLineString) {
        this.clientCmdLineString = clientCmdLineString;
        this.anySetter = true;
    }

    public String getCmdLineArgSeparator() {
        return cmdLineArgSeparator;
    }

    /**
     * Set the separator to use for splitting the clientCmdLineString
     * @param cmdLineArgSeparator
     */
    public void setCmdLineArgSeparator(String cmdLineArgSeparator) {
        this.cmdLineArgSeparator = cmdLineArgSeparator;
        this.anySetter = true;
    }

    public String getClientStubsCmdLine() {
        return clientStubsCmdLine;
    }

    /**
     * Any ${deploymentName} will be replaced with the {@link AppClientProtocolConfiguration#deploymentName} value
     * @param clientStubsCmdLine a cmdLineArgSeparator delimited string of command line arguments
     */
    public void setClientStubsCmdLine(String clientStubsCmdLine) {
        this.clientStubsCmdLine = clientStubsCmdLine;
    }

    public String getClientStubsJarSuffix() {
        return clientStubsJarSuffix;
    }

    public void setClientStubsJarSuffix(String clientStubsJarSuffix) {
        this.clientStubsJarSuffix = clientStubsJarSuffix;
    }

    public String getClientDir() {
        return clientDir;
    }

    public void setClientDir(String clientDir) {
        this.clientDir = clientDir;
        this.anySetter = true;
    }

    public String getClientEarDir() {
        return clientEarDir;
    }

    /**
     * Set the directory to extract the final appclient ear test artifact. The default is "target/appclient".
     * Any ${clientEarDir} ref in the {@link #clientCmdLineString} will be replaced with the clientEarDir
     * value.
     * @param clientEarDir
     */
    public void setClientEarDir(String clientEarDir) {
        this.clientEarDir = clientEarDir;
        this.anySetter = true;
    }

    public boolean isUnpackClientEar() {
        return unpackClientEar;
    }

    /**
     * Set to true to unpack the client ear into the clientEarDir. The default is false. This is useful if the
     * vendor appclient requires the ear to be exploded in order to access the appclient jar and bundled ear
     * lib jars.
     * @param unpackClientEar
     */
    public void setUnpackClientEar(boolean unpackClientEar) {
        this.unpackClientEar = unpackClientEar;
        this.anySetter = true;
    }

    public boolean isIsolateClientEars() {
        return isolateClientEars;
    }

    /**
     * Set to true to clear the clientEarDir before extracting a new appclient ear test artifact. The default is false.
     * This is useful if the vendor appclient loads all of the ears in clientEarDir by default.
     * @param isolateClientEars
     */
    public void setIsolateClientEars(boolean isolateClientEars) {
        this.isolateClientEars = isolateClientEars;
        this.anySetter = true;
    }

    public long getClientTimeout() {
        return clientTimeout;
    }

    /**
     * Set the timeout in milliseconds for waiting for the appclient process to exit. The default is 60000 (1 minute).
     * @param clientTimeout
     */
    public void setClientTimeout(long clientTimeout) {
        this.clientTimeout = clientTimeout;
        this.anySetter = true;
    }

    // Setters for the values set by the AppClientDeploymentPackager

    public String getEarLibDir() {
        return earLibDir;
    }
    public void setEarLibDir(String earLibDir) {
        this.earLibDir = earLibDir;
    }
    public String getDeploymentName() {
        return deploymentName;
    }

    /**
     * Set by the AppClientDeploymentPackager to the name of the deployment from the appclient ear application.xml
     * @param deploymentName - either the application-name from the application.xml or the name of the ear file
     */
    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public AppClientArchiveName getAppClientArchiveName() {
        return appClientArchiveName;
    }
    public void setAppClientArchiveName(AppClientArchiveName appClientArchiveName) {
        this.appClientArchiveName = appClientArchiveName;
    }

    public String getVehicleArchiveName() {
        return vehicleArchiveName;
    }
    public void setVehicleArchiveName(String vehicleArchiveName) {
        this.vehicleArchiveName = vehicleArchiveName;
    }

    /**
     * If a clientStubsCmdLine was set, then this method returns true. This indicates that the client stubs
     * will be needed to the appclient container.
     * @return true if the clientStubsCmdLine is not null and not blank
     */
    public boolean needsStubs() {
        return clientStubsCmdLine != null && !clientStubsCmdLine.isBlank();
    }

    /**
     * Validate if any setter was called indicating if there was a matching protocol config
     * @return true if any setter was called
     */
    @Override
    public boolean wasAnySetterCalled() {
        return anySetter;
    }

    /** Helper methods to turn the strings into the types used by Runtime#exec
     * @return a File object for the clientDir
     */
    public File clientDirAsFile() {
        File dir = null;
        if (clientDir != null) {
            dir = new File(clientDir);
        }
        return dir;
    }

    /**
     * If #unpackClientEar is true, and clientEarDir/${earLibDir} exists, then this method returns the contents
     * of the clientEarDir/${earLibDir} as a classpath string. It is possible that earLibDir is null to disable
     * the ear lib directory. If the earLibDir is null, then this method will return an empty string.
     * @return a classpath string for the client ear lib directory
     */
    public String clientEarLibClasspath() {
        StringBuilder cp = new StringBuilder();
        if (earLibDir != null) {
            File libDir = new File(clientEarDir, earLibDir);
            if (unpackClientEar && libDir.exists()) {
                File[] jars = libDir.listFiles();
                for (File jar : jars) {
                    if (!cp.isEmpty()) {
                        cp.append(File.pathSeparator);
                    }
                    cp.append(jar.getAbsolutePath());
                }
            }
        }
        return cp.toString();
    }

    /**
     * Parse the clientCmdLineString into an array of strings using the cmdLineArgSeparator. This calls String#split on
     * the clientCmdLineString using the cmdLineArgSeparator as the split expression and then trims each element of the
     * resulting array.
     * @return a command line array of strings for use with Runtime#exec.
     */
    public String[] clientCmdLineAsArray() {
        String[] cmdArray = clientCmdLineString.trim().split(cmdLineArgSeparator);
        // Now trim each element
        for (int i = 0; i < cmdArray.length; i++) {
            cmdArray[i] = cmdArray[i].trim();
        }
        return cmdArray;
    }
    /**
     * Parse the clientStubsCmdLine into an array of strings using the cmdLineArgSeparator. This calls String#split on
     * the clientStubsCmdLine using the cmdLineArgSeparator as the split expression and then trims each element of the
     * resulting array.
     * @return a command line array of strings for use with Runtime#exec.
     */
    public String[] clientStubsCmdLineAsArray() {
        String[] cmdArray = clientStubsCmdLine.trim().split(cmdLineArgSeparator);
        // Now trim each element
        for (int i = 0; i < cmdArray.length; i++) {
            cmdArray[i] = cmdArray[i].trim();
        }
        return cmdArray;
    }

    public String[] clientEnvAsArray() {
        String[] envp = null;
        if (clientEnvString != null) {
            ArrayList<String> tmp = new ArrayList<String>();
            // Split on the env1=value1 ; separator
            envp = clientEnvString.trim().split(";");
            // Now trim each element
            for (int i = 0; i < envp.length; i++) {
                envp[i] = envp[i].trim();
            }

        }
        return envp;
    }


}
