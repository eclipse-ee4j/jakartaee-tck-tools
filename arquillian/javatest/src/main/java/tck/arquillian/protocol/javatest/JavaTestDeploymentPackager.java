package tck.arquillian.protocol.javatest;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Logger;

public class JavaTestDeploymentPackager implements DeploymentPackager {
    static Logger log = Logger.getLogger(JavaTestDeploymentPackager.class.getName());

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> processors) {
        Archive<?> archive = testDeployment.getApplicationArchive();

        // Include the protocol.jar in the deployment
        Collection<Archive<?>> auxiliaryArchives = testDeployment.getAuxiliaryArchives();
        File protocolJar = resolveProtocolJar();
        if(protocolJar == null) {
            throw new RuntimeException("Failed to resolve protocol.jar. You either need a jakarta.tck.arquillian:arquillian-protocol-lib"+
                    " dependency in the runner pom.xml or a downloaded target/protocol/protocol.jar file");
        }

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

    /**
     * Resolve the protocol.jar from the runner pom.xml dependencies
     * @return The protocol.jar file if found, null otherwise
     */
    private File resolveProtocolJar() {
        File protocolJar = null;
        String[] activeMavenProfiles = {"staging"};
        String libGAV = "jakarta.tck.arquillian:arquillian-protocol-lib";
        String version = versionInfo();
        if(version != null && !version.isEmpty()) {
            libGAV += ":" + version;
        }
        MavenResolvedArtifact protocolLib = null;
        try {
            MavenResolvedArtifact[] resolvedArtifacts = Maven.resolver().loadPomFromFile("pom.xml", activeMavenProfiles)
                    .resolve(libGAV)
                    .withTransitivity()
                    .asResolvedArtifact();
            for (MavenResolvedArtifact resolvedArtifact : resolvedArtifacts) {
                MavenCoordinate gav = resolvedArtifact.getCoordinate();
                if (gav.getGroupId().equals("jakarta.tck.arquillian") && gav.getArtifactId().equals("arquillian-protocol-lib")) {
                    protocolLib = resolvedArtifact;
                    break;
                }
            }
        } catch (Exception e) {
            log.warning("Failed to resolve jakarta.tck.arquillian:arquillian-protocol-lib: " + e.getMessage());
        }

        if(protocolLib != null) {
            protocolJar = protocolLib.asFile();
        } else {
            log.warning("Failed to resolve jakarta.tck.arquillian:arquillian-protocol-lib, check the runner pom.xml dependencies");
            // Fallback to the local unpacked protocol.jar
            protocolJar = new File("target/protocol/protocol.jar");
            if(!protocolJar.exists()) {
                log.warning("Failed to find downloaded jakarta.tck.arquillian:arquillian-protocol-lib in target/protocol/protocol.jar");
                protocolJar = null;
            }
        }
        return protocolJar;
    }
    private String versionInfo() {
        URL versionURL = JavaTestDeploymentPackager.class.getResource("/javatest.version");
        String versionInfo = "";
        try {
            assert versionURL != null;
            try(InputStream is = versionURL.openStream()) {
                if(is != null) {
                    byte[] info = is.readAllBytes();
                    versionInfo = new String(info);
                }
            }
        } catch (Exception e) {
            log.warning("Failed to read javatest.version: " + e.getMessage());
        }
        return versionInfo;
    }
}
