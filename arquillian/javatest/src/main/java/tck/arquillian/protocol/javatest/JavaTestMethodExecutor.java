package tck.arquillian.protocol.javatest;

import com.sun.javatest.Status;
import com.sun.ts.lib.harness.EETest;
import com.sun.ts.tests.common.vehicle.VehicleClient;
import org.jboss.arquillian.container.spi.client.deployment.Deployment;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;
import tck.arquillian.protocol.common.TsTestPropsBuilder;

import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 */
public class JavaTestMethodExecutor implements ContainerMethodExecutor {
    static Logger log = Logger.getLogger(JavaTestMethodExecutor.class.getName());
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
    private JavaTestProtocolConfiguration config;
    @Inject
    @DeploymentScoped
    private Instance<Deployment> deploymentInstance;

    public JavaTestMethodExecutor(JavaTestProtocolConfiguration config) {
        this.config = config;
    }

    @Override
    public TestResult invoke(TestMethodExecutor testMethodExecutor) {
        log.fine("Executing test method: " + testMethodExecutor.getMethod().getName());
        long start = System.currentTimeMillis();
        // Get deployment archive name and remove the .* suffix
        Deployment deployment = deploymentInstance.get();


        String[] args;
        try {
            args = TsTestPropsBuilder.runArgs(config, deployment, testMethodExecutor);
        } catch (IOException e) {
            TestResult result = TestResult.failed(e);
            result.addDescription("Failed to write test properties");
            return result;
        }

        /*
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
        String vehicleArchiveName = deploymentInstance.get().getDescription().getArchive().getName();
        int dot = vehicleArchiveName.lastIndexOf('.');
        if(dot != -1) {
            vehicleArchiveName = vehicleArchiveName.substring(0, dot);
        }

        // We need the JavaTest ts.jte file for now
        Path tsJte = Paths.get(config.getTsJteFile());
        // Create a test properties file
        Path testProps = Paths.get(config.getWorkDir(), "tstest.jte");

        try {
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
        } catch (IOException e) {
            TestResult result = TestResult.failed(e);
            result.addDescription("Failed to write test properties to " + testProps);
            return result;
        }
        String[] args = {
          // test props are needed by EETest.run
          "-p", testProps.toFile().getAbsolutePath(),
          "classname", testMethodExecutor.getMethod().getDeclaringClass().getName(),
          "-t", tsTestMethodName,
          "-vehicle", testVehicle.value(),
        };
        */

        // We are running in the same JVM, JavaTest CTS runs in a separate JVM
        VehicleClient client = new VehicleClient();
        Status s = client.run(args, System.out, System.err);

        TestResult result = switch (s.getType()) {
            case Status.PASSED -> TestResult.passed(s.getReason());
            case Status.FAILED -> TestResult.failed(new Exception(s.getReason()));
            case Status.ERROR -> TestResult.failed(new EETest.Fault(s.getReason()));
            case Status.NOT_RUN -> TestResult.skipped(s.getReason());
            default -> TestResult.failed(new IllegalStateException("Unkown status type: " + s.getType()));
        };
        result.setStart(start);
        result.setEnd(System.currentTimeMillis());
        return result;
    }
}
