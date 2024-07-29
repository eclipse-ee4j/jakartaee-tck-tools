package tck.arquillian.protocol.javatest;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;

public class JavaTestDeploymentPackager implements DeploymentPackager {
    static Logger log = Logger.getLogger(JavaTestDeploymentPackager.class.getName());

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> processors) {
        Archive<?> archive = testDeployment.getApplicationArchive();

        Collection<Archive<?>> auxiliaryArchives = testDeployment.getAuxiliaryArchives();
        File protocolJar = new File("target/protocol/protocol.jar");

        if(archive instanceof EnterpriseArchive) {
            EnterpriseArchive ear = (EnterpriseArchive) archive;
            ear.addAsLibraries(auxiliaryArchives.toArray(new Archive<?>[0]));
            ear.addAsLibraries(protocolJar);
        } else if(archive instanceof WebArchive) {
            WebArchive war = (WebArchive) archive;
            war.addAsLibraries(auxiliaryArchives.toArray(new Archive<?>[0]));
            war.addAsLibraries(protocolJar);
        }

        return archive;
    }
}
