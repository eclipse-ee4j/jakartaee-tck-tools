package tck.arquillian.protocol.appclient;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

public class AppClientDeploymentPackager implements DeploymentPackager {
    static Logger log = Logger.getLogger(AppClientDeploymentPackager.class.getName());

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> processors) {
        Archive<?> archive = testDeployment.getApplicationArchive();

        Collection<Archive<?>> auxiliaryArchives = testDeployment.getAuxiliaryArchives();
        EnterpriseArchive ear = (EnterpriseArchive) archive;
        ear.addAsLibraries(auxiliaryArchives.toArray(new Archive<?>[0]));
        File protocolJar = new File("target/protocol/protocol.jar");
        ear.addAsLibraries(protocolJar);

        String mainClass = extractAppMainClient(ear);
        log.info("mainClass: " + mainClass);

        // Write out the ear with the test dependencies for use by the appclient launcher
        AppClientProtocolConfiguration config = (AppClientProtocolConfiguration) testDeployment.getProtocolConfiguration();
        String extractDir = config.getClientEarDir();
        if(extractDir == null) {
            extractDir = "target/appclient";
        }
        File appclient = new File(extractDir);
        File archiveOnDisk = new File(appclient, ear.getName());
        final ZipExporter exporter = ear.as(ZipExporter.class);
        exporter.exportTo(archiveOnDisk, true);
        log.info("Exported test ear to: " + archiveOnDisk.getAbsolutePath());

        return ear;
    }

    private static String extractAppMainClient(EnterpriseArchive ear) {
        String mainClass = null;
        Map<ArchivePath, Node> contents = ear.getContent();
        for (Node node : contents.values()) {
            Asset asset = node.getAsset();
            if (asset instanceof ArchiveAsset) {
                ArchiveAsset jar = (ArchiveAsset) asset;
                Node mfNode = jar.getArchive().get("META-INF/MANIFEST.MF");
                if (mfNode == null)
                    continue;

                StringAsset manifest = (StringAsset) mfNode.getAsset();
                String source = manifest.getSource();
                String[] lines = source.split("\n");
                for (String line : lines) {
                    if (line.startsWith("Main-Class:")) {
                        mainClass = line.substring(11).trim();
                        break;
                    }
                }
            }
        }
        return mainClass;
    }
}
