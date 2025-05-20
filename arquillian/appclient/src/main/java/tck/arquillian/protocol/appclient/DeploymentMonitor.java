package tck.arquillian.protocol.appclient;

import java.util.logging.Logger;

/**
 * Track the status of a test deployment to know if it needs a client sutb jar
 */
public class DeploymentMonitor {
    Logger log = Logger.getLogger(DeploymentMonitor.class.getName());

    public static enum Status {
        DEPLOYED,
        NEEDS_STUBS,
        HAS_STUBS,
        FAILED,
        UNDEPLOYED,
        DISPOSED;
    }
    private String name;
    private Status status = Status.DEPLOYED;

    public DeploymentMonitor(String name) {
        this.name = name;
        log.info(name+", deployed");
        this.status = Status.NEEDS_STUBS;
    }
    public boolean needsStubs() {
        return status == Status.NEEDS_STUBS;
    }
    public void haveStubJar(String name) {
        log.info(name+", haveStubJar: " + name);
        status = Status.HAS_STUBS;
    }
    public void undeploy() {
        log.info(name+", undeployed");
        status = Status.UNDEPLOYED;
    }
    public void dispose() {
        log.info(name+", disposed");
        status = Status.DISPOSED;
    }

    @Override
    public String toString() {
        return "DeploymentMonitor{" +
                "name='" + name + '\'' +
                ", status=" + status +
                '}';
    }
}
