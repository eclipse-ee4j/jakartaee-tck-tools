package tck.arquillian.protocol.appclient;

import org.jboss.arquillian.container.spi.event.DeploymentEvent;
import org.jboss.arquillian.container.spi.event.UnDeployDeployment;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

import java.lang.annotation.Annotation;
import java.util.logging.Logger;

/**
 * Observer the deployment events to track when the current test deployment is deployed to know
 * if the protocol needs to request the appclient stub jar.
 */
public class DeploymentMonitorProvider implements ResourceProvider {
    Logger log = Logger.getLogger(DeploymentMonitor.class.getName());

    @Inject
    @ApplicationScoped
    InstanceProducer<DeploymentMonitor> monitor;
    private DeploymentMonitor activeDeploymentMonitor;

    // Deployment events
    public void onDeployment(@Observes DeploymentEvent event) {
        log.info("Deployment event observed: %s, currentMonitor: %s".formatted(event.getDeployment(), activeDeploymentMonitor));
        if(activeDeploymentMonitor != null) {
            activeDeploymentMonitor.dispose();
        }
        activeDeploymentMonitor = new DeploymentMonitor(event.getDeployment().getDescription().getName());
        monitor.set(activeDeploymentMonitor);
    }
    public void onUndeployment(@Observes UnDeployDeployment event) {
        log.info("Undeployment event observed: %s, currentMonitor: %s".formatted(event.getDeployment(), activeDeploymentMonitor));
        if(activeDeploymentMonitor != null) {
            activeDeploymentMonitor.undeploy();
        }
    }

    // ResourceProvider
    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        return monitor.get();
    }

    @Override
    public boolean canProvide(Class<?> type) {
        return type.isAssignableFrom(DeploymentMonitor.class);
    }

}
