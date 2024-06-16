package org.jboss.arquillian.protocol.appclient;

import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.test.spi.TestMethodExecutor;
import org.jboss.arquillian.test.spi.TestResult;

import java.util.logging.Logger;

public class AppClientMethodExecutor implements ContainerMethodExecutor {
    static Logger log = Logger.getLogger(AppClientMethodExecutor.class.getName());
    private AppClientCmd appClient;
    private AppClientProtocolConfiguration config;

    static enum MainStatus {
        PASSED,
        FAILED,
        ERROR,
        NOT_RUN;

        static MainStatus parseStatus(String reason) {
            MainStatus status = FAILED;
            if (reason.contains("Passed.")) {
                status = PASSED;
            } else if (reason.contains("Error.")) {
                status = ERROR;
            } else if (reason.contains("Not run.")) {
                status = NOT_RUN;
            }
            return status;
        }
    }

    public AppClientMethodExecutor(AppClientCmd appClient, AppClientProtocolConfiguration config) {
        this.appClient = appClient;
        this.config = config;
    }

    @Override
    public TestResult invoke(TestMethodExecutor testMethodExecutor) {
        TestResult result = TestResult.passed();

        // Run the appclient for the test if required
        String testMethod = testMethodExecutor.getMethodName();
        if (config.isRunClient()) {
            log.info("Running appClient for: " + testMethod);
            try {
                appClient.run("-t", testMethod);
            } catch (Exception ex) {
                result = TestResult.failed(ex);
                return result;
            }
        } else {
            log.info("Not running appClient for: " + testMethod);
        }
        String[] lines = appClient.readAll(5000);

        log.info(String.format("AppClient(%s) readAll returned %d lines\n", testMethod, lines.length));
        boolean sawStatus = false;
        MainStatus status = MainStatus.NOT_RUN;
        String reason = "None";
        String description = "None";
        for (String line : lines) {
            System.out.println(line);
            if (line.contains("STATUS:")) {
                sawStatus = true;
                description = line;
                status = MainStatus.parseStatus(line);
                // Format of line is STATUS:StatusText.Reason
                // see com.sun.javatest.Status#exit()
                int reasonStart = line.indexOf('.');
                if (reasonStart > 0 && reasonStart < line.length() - 1) {
                    reason = line.substring(reasonStart + 1);
                }
            }
        }
        if (!sawStatus) {
            Throwable ex = new IllegalStateException("No STATUS: output seen from client");
            result = TestResult.failed(ex);
        } else {
            switch (status) {
                case PASSED:
                    result = TestResult.passed(reason);
                    break;
                case ERROR:
                case FAILED:
                    result = TestResult.failed(new Exception(reason));
                    break;
                case NOT_RUN:
                    result = TestResult.skipped(reason);
                    break;
            }
            result.addDescription(description);
        }

        return result;
    }
}
