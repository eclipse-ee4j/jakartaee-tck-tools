/*
 * Copyright 2024 Red Hat, Inc., and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 */
package tck.arquillian.protocol.appclient;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import tck.arquillian.protocol.common.ProtocolJarResolver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A {@link DeploymentPackager} implementation for packaging an AppClient deployment.
 * - jakarta.tck.arquillian:arquillian-protocol-lib to EAR lib
 * - Extract the EAR to the clientEarDir for use by the appclient launcher
 * - Unzip the EAR content to the clientEarDir if unpackClientEar is true
 * - Determines the main class of the AppClient jar and sets the AppClientArchiveName
 */
public class AppClientDeploymentPackager implements DeploymentPackager {
    static Logger log = Logger.getLogger(AppClientDeploymentPackager.class.getName());

    @Inject
    @ApplicationScoped
    private InstanceProducer<AppClientArchiveName> appClientArchiveName;

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> processors) {
        Archive<?> archive = testDeployment.getApplicationArchive();
        String deploymentName = testDeployment.getDeploymentName();
        log.info("Generating deployment for: " + deploymentName);

        Collection<Archive<?>> auxiliaryArchives = testDeployment.getAuxiliaryArchives();
        EnterpriseArchive ear = (EnterpriseArchive) archive;
        ear.addAsLibraries(auxiliaryArchives.toArray(new Archive<?>[0]));
        // Include the protocol.jar in the deployment
        File protocolJar = ProtocolJarResolver.resolveProtocolJar();
        if(protocolJar == null) {
            String msg = "Failed to resolve protocol.jar. You either need a jakarta.tck.arquillian:arquillian-protocol-lib"+
                    " dependency in the runner pom.xml or a downloaded target/protocol/protocol.jar file.\n" +
                    " The runner pom needs to be pom.xml or the path needs to be set by the system property tck.arquillian.protocol.runnerPom"
                    ;
            throw new RuntimeException(msg);
        }
        ear.addAsLibrary(protocolJar, "arquillian-protocol-lib.jar");

        // If this is one of the
        if(deploymentName.contains("appmanagedNoTx_vehicle")) {
            WebArchive war = ShrinkWrap.create(WebArchive.class, "appmanagedNoTx_vehicle_web.war");
            war.addClass(com.sun.ts.tests.common.vehicle.servlet.ServletVehicle.class);
            war.addClass(com.sun.ts.tests.common.vehicle.appmanagedNoTx.AppManagedNoTxServletVehicle.class);
            war.addClass(com.sun.ts.tests.common.vehicle.web.AltWebVehicleRunner.class);
            ear.addAsModule(war);
            System.setProperty("vehicle_archive_name_override", "appmanagedNoTx_vehicle_web");
            log.info("Added appmanagedNoTx_vehicle.war to: " + deploymentName);
        }
        else if(deploymentName.contains("appmanaged_vehicle")) {
            WebArchive war = ShrinkWrap.create(WebArchive.class, "appmanaged_vehicle_web.war");
            war.addClass(com.sun.ts.tests.common.vehicle.servlet.ServletVehicle.class);
            war.addClass(com.sun.ts.tests.common.vehicle.appmanaged.AppManagedServletVehicle.class);
            war.addClass(com.sun.ts.tests.common.vehicle.web.AltWebVehicleRunner.class);
            ear.addAsModule(war);
            System.setProperty("vehicle_archive_name_override", "appmanaged_vehicle_web");
            log.info("Added appmanaged_vehicle_web.war to: " + deploymentName);
        }

        AppClientProtocolConfiguration config = (AppClientProtocolConfiguration) testDeployment.getProtocolConfiguration();
        String mainClass = extractAppMainClient(ear);
        log.info("mainClass: " + mainClass);

        // Write out the ear with the test dependencies for use by the appclient launcher
        String extractDir = config.getClientEarDir();
        if(extractDir == null) {
            extractDir = "target/appclient";
        }
        File appclient = new File(extractDir);
        if(!appclient.exists()) {
            if(appclient.mkdirs()) {
                log.info("Created appclient directory: " + appclient.getAbsolutePath());
            } else {
                throw new RuntimeException("Failed to create appclient directory: " + appclient.getAbsolutePath());
            }
        }
        File archiveOnDisk = new File(appclient, ear.getName());
        final ZipExporter exporter = ear.as(ZipExporter.class);
        exporter.exportTo(archiveOnDisk, true);
        log.info("Exported test ear to: " + archiveOnDisk.getAbsolutePath());

        if(config.isUnpackClientEar()) {
            for (ArchivePath path : ear.getContent().keySet()) {
                Node node = ear.get(path);
                if (node.getAsset() instanceof ArchiveAsset) {
                    ArchiveAsset asset = (ArchiveAsset) node.getAsset();
                    File archiveFile = new File(appclient, path.get());
                    if(!archiveFile.getParentFile().exists()) {
                        archiveFile.getParentFile().mkdirs();
                    }
                    final ZipExporter zipExporter = asset.getArchive().as(ZipExporter.class);
                    zipExporter.exportTo(archiveFile, true);
                    log.info("Exported test ear content to: " + archiveFile.getAbsolutePath());
                } else if(node.getAsset() instanceof FileAsset) {
                    FileAsset asset = (FileAsset) node.getAsset();
                    File file = new File(appclient, path.get());
                    if(!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    try {
                        Files.copy(asset.openStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        log.info("Exported test ear content to: " + file.getAbsolutePath());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to export test ear content to: " + file.getAbsolutePath(), e);
                    }
                }
            }
        }
        return ear;
    }

    private String extractAppMainClient(EnterpriseArchive ear) {
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
                        appClientArchiveName.set(new AppClientArchiveName(jar.getArchive().getName()));
                        break;
                    }
                }
            }
        }
        return mainClass;
    }
}
