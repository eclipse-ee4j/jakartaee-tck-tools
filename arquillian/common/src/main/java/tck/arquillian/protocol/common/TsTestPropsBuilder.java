package tck.arquillian.protocol.common;

import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.test.spi.TestMethodExecutor;

import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class TsTestPropsBuilder {
    static Logger log = Logger.getLogger(TsTestPropsBuilder.class.getName());

    // Property names passed from the ts.jte file to the tstest.jte file
    // Parsed from the test @class.setup_props: values + additional seen to be used by harness
    static String[] tsJtePropNames = {
            "Driver",
            "authpassword",
            "authuser",
            "db.supports.sequence",
            "db1",
            "generateSQL",
            "harness.log.port",
            "harness.log.traceflag",
            "java.naming.factory.initial",
            "javamail.protocol",
            "javamail.server",
            "javamail.username",
            "jdbc.db",
            "jms_timeout",
            "log.file.location",
            "logical.hostname.servlet",
            "org.omg.CORBA.ORBClass",
            "password",
            "platform.mode",
            "rapassword1",
            "rauser1",
            "securedWebServicePort",
            "sigTestClasspath",
            "ts_home",
            "user",
            "webServerHost",
            "webServerPort",
            "whitebox-anno_no_md",
            "whitebox-mdcomplete",
            "whitebox-mixedmode",
            "whitebox-multianno",
            "whitebox-notx",
            "whitebox-permissiondd",
            "whitebox-tx",
            "whitebox-xa",
            "ws_wait",
    };

    public static String vehicleArchiveName(Deployment deployment) {
        // Get deployment archive name and remove the .* suffix
        String vehicleArchiveName = deployment.getDescription().getArchive().getName();
        int dot = vehicleArchiveName.lastIndexOf('.');
        if(dot != -1) {
            vehicleArchiveName = vehicleArchiveName.substring(0, dot);
        }
        return vehicleArchiveName;
    }
    public static String[] runArgs(ProtocolCommonConfig config, Deployment deployment,
                                   TestMethodExecutor testMethodExecutor) throws IOException {
        Class<?> testSuperclass = testMethodExecutor.getMethod().getDeclaringClass().getSuperclass();
        TargetVehicle testVehicle = testMethodExecutor.getMethod().getAnnotation(TargetVehicle.class);
        log.info(String.format("Base class: %s, vehicle: %s", testSuperclass.getName(), testVehicle.value()));
        String testMethodName = testMethodExecutor.getMethod().getName();
        // Remove the _ testVehicle
        int index = testMethodName.lastIndexOf('_');
        String tsTestMethodName = testMethodName;
        if(index != -1) {
            tsTestMethodName = testMethodName.substring(0, index);
        }
        // Get deployment archive name and remove the .* suffix
        String vehicleArchiveName = vehicleArchiveName(deployment);

        // We need the JavaTest ts.jte file for now
        Path tsJte = Paths.get(config.getTsJteFile());
        // Create a test properties file
        Path testProps = Paths.get(config.getWorkDir(), "tstest.jte");

        // Seed the test properties file with select ts.jte file settings
        Properties tsJteProps = new Properties();
        tsJteProps.load(new FileReader(tsJte.toFile()));
        // The test specific properties file
        Properties props = new Properties();
        // A property set by the TSScript class
        props.setProperty("finder", "cts");
        // Vehicle
        props.setProperty("service_eetest.vehicles", testVehicle.value());
        props.setProperty("vehicle", testVehicle.value());
        props.setProperty("vehicle_archive_name", vehicleArchiveName);
        //
        props.setProperty("harness.log.delayseconds", "0");
        if(config.isTrace()) {
            // This overrides the ts.jte harness.log.traceflag value
            props.setProperty("harness.log.traceflag", "true");
        }
        // Copy over common ts.jte settings
        for (String propName : tsJtePropNames) {
            String propValue = tsJteProps.getProperty(propName);
            if(propValue != null) {
                if(propValue.startsWith("${") && propValue.endsWith("}")) {
                    String refName = propValue.substring(2, propValue.length() - 1);
                    propValue = tsJteProps.getProperty(refName);
                    log.info(String.format("Setting property %s -> %s to %s", propName, refName, propValue));
                    if(propValue == null) {
                        continue;
                    }
                }
                props.setProperty(propName, propValue);
            }
        }

        // The vehicle harness operates on the legacy CTS superclass of the Junit5 class.
        props.setProperty("test_classname", testSuperclass.getName());

        // Write out the test properties file, overwriting any existing file
        try(OutputStream out = Files.newOutputStream(testProps)) {
            props.store(out, "Properties for test: "+testMethodName);
            log.info(props.toString());
        }

        String[] args = {
                // test props are needed by EETest.run
                "-p", testProps.toFile().getAbsolutePath(),
                "classname", testMethodExecutor.getMethod().getDeclaringClass().getName(),
                "-t", tsTestMethodName,
                "-vehicle", testVehicle.value(),
        };
        return args;
    }
}
