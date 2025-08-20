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

import com.sun.ts.tests.common.vehicle.VehicleType;
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
import org.jboss.shrinkwrap.impl.base.path.BasicPath;
import tck.arquillian.protocol.common.ProtocolJarResolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    // Used if the ear wants no library-directory
    public static String INTERNAL_LIB_DIR = "private-lib";

    /**
     * This is the adjusted AppClientProtocolConfiguration that has test deployment specific values
     * for the appclient jar and possibly a non-standard earLibDir.
     */
    @Inject
    @ApplicationScoped
    private InstanceProducer<AppClientProtocolConfiguration> deploymentConfig;

    @Override
    public Archive<?> generateDeployment(TestDeployment testDeployment, Collection<ProtocolArchiveProcessor> processors) {
        Archive<?> archive = testDeployment.getApplicationArchive();
        String deploymentName = testDeployment.getDeploymentName();
        String xmlDeploymentName = deploymentName;
        String archiveName = archive.getName();
        if(!archiveName.equals(xmlDeploymentName)) {
            // The archive name does not match the @Deployment(name), so use the archive name as that is what a server will use
            xmlDeploymentName = archiveName.substring(0, archiveName.length()-4);
        }
        log.info("Generating deployment for: " + deploymentName);

        Collection<Archive<?>> auxiliaryArchives = testDeployment.getAuxiliaryArchives();
        EnterpriseArchive ear = (EnterpriseArchive) archive;
        // Look for an application.xml file in the test deployment
        Node appXmlNode = ear.getContent().get(new BasicPath("META-INF", "application.xml"));
        String earLibDir = "lib";
        if (appXmlNode != null) {
            /* This is a simple way to get the lib directory from the application.xml file. It will fail if the
                library-directory element text content is split across multiple lines. Proper way it to use an XML
                parser, but to not add a dependency on a simple XML parser, we just use a simple string.
            */
            Asset appXml = appXmlNode.getAsset();
            try(InputStream is = appXml.openStream()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                for(String line : reader.lines().toList()) {
                    if(line.contains("<library-directory>")) {
                        earLibDir = line.substring(line.indexOf("<lib") + 19, line.indexOf("</"));
                        // If library-directory, it means disable the default lib directory
                        if(earLibDir.isBlank()) {
                            earLibDir = INTERNAL_LIB_DIR;
                            log.info("EAR lib is disabled in application.xml");
                        } else {
                            log.info("Using EAR application/library-directory: "+earLibDir);
                        }
                    } else if(line.contains("<application-name>")) {
                        xmlDeploymentName = line.substring(line.indexOf("<app") + 18, line.indexOf("</"));
                        log.info("Using EAR application/application-name: " + xmlDeploymentName);
                    }
                }
            } catch (IOException e) {
                log.warning("Failed to open application.xml: " + e.getMessage());
            }
        }

        log.info("Using ear lib directory: "+earLibDir);

        for (Archive<?> auxiliaryArchive : auxiliaryArchives) {
            ear.add(new ArchiveAsset(auxiliaryArchive, ZipExporter.class), new BasicPath(earLibDir, auxiliaryArchive.getName()));
        }
        // Include the protocol.jar in the deployment
        File protocolJar = ProtocolJarResolver.resolveProtocolJar();
        if(protocolJar == null) {
            String msg = "Failed to resolve protocol.jar. You either need a jakarta.tck.arquillian:arquillian-protocol-lib"+
                    " dependency in the runner pom.xml or a downloaded target/protocol/protocol.jar file.\n" +
                    " The runner pom needs to be pom.xml or the path needs to be set by the system property tck.arquillian.protocol.runnerPom"
                    ;
            throw new RuntimeException(msg);
        }

        ear.add(new FileAsset(protocolJar), new BasicPath(earLibDir, "arquillian-protocol-lib.jar"));

        AppClientProtocolConfiguration config = (AppClientProtocolConfiguration) testDeployment.getProtocolConfiguration();
        config.setEarLibDir(earLibDir);
        config.setDeploymentName(xmlDeploymentName);
        String mainClass = determineAppMainJar(ear, config);
        log.info("mainClass: " + mainClass);
        /*
         If this is one of the JPA vehicles using a remote EJB, add the JPA servlet vehicle
         We try both the appclient archive name and the deployment name to determine the vehicle type as the
         JPA tests have inconsistent naming of the deployment name and the appclient archive name.
        */
        VehicleType vehicleType = getVehicleType(config.getAppClientArchiveName().name());
        if(vehicleType == VehicleType.none) {
            vehicleType = getVehicleType(deploymentName);
        }
        if(vehicleType != VehicleType.none) {
            addJPAServletVehicle(ear, vehicleType, config);
        } else {
            config.setVehicleArchiveName("none");
        }

        // Make this updated config available for injection into other Arquillian components
        if(deploymentConfig != null) {
            deploymentConfig.set(config);
        }

        // Write out the ear with the test dependencies for use by the appclient launcher
        String extractDir = config.getClientEarDir();
        if (extractDir == null) {
            extractDir = "target/appclient";
        }
        File appclient = new File(extractDir);

        if (!appclient.exists()) {
            if (appclient.mkdirs()) {
                log.info("Created appclient directory: " + appclient.getAbsolutePath());
            } else {
                throw new RuntimeException("Failed to create appclient directory: " + appclient.getAbsolutePath());
            }
        } else {
            // Directory exists, clear it if requested
            if (config.isIsolateClientEars()) {
                File[] contents = appclient.listFiles();
                if (contents != null) {
                    for (File f : contents) {
                        deleteRecursively(f);
                    }
                }
            }
        }

        File archiveOnDisk = new File(appclient, ear.getName());
        final ZipExporter exporter = ear.as(ZipExporter.class);
        exporter.exportTo(archiveOnDisk, true);
        log.info("Exported test ear to: " + archiveOnDisk.getAbsolutePath());

        // If unpackClientEar is true, extract the ear to the clientEarDir
        if(config.isUnpackClientEar()) {
            unpackClientEar(ear, appclient, earLibDir);
        }
        return ear;
    }

    /**
     * Delete a File (or directory) recursively. Needed because the contents of clientEarDir
     * may have been expanded upon request.
     */
    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            throw new RuntimeException("Failed to delete: " + file.getAbsolutePath());
        }
    }

    /**
     * Map the deployment name to a vehicle type for the JPA vehicles that need to have a
     * servlet vehicle added to the deployment.
     *
     * @param deploymentName - the deployment name
     * @return the vehicle type or VehicleType.none if not a JPA vehicle
     */
    private VehicleType getVehicleType(String deploymentName) {
        VehicleType vehicleType = VehicleType.none;
        if(deploymentName.contains("appmanagedNoTx_vehicle")) {
            vehicleType = VehicleType.appmanagedNoTx;
        } else if(deploymentName.contains("appmanaged_vehicle")) {
            vehicleType = VehicleType.appmanaged;
        } else if(deploymentName.contains("stateful3_vehicle")) {
            vehicleType = VehicleType.stateful3;
        } else if(deploymentName.contains("stateless3_vehicle")) {
            vehicleType = VehicleType.stateless3;
        }
        return vehicleType;
    }

    /**
     * Add the JPA servlet vehicle to the deployment
     * @param ear
     * @param vehicleType
     */
    private void addJPAServletVehicle(EnterpriseArchive ear, VehicleType vehicleType, AppClientProtocolConfiguration config) {
        log.info("Adding JPA servlet vehicle: " + vehicleType);
        String deploymentName = ear.getName();
        String webArchiveName = vehicleType.name() + "_vehicle_web";
        WebArchive war = ShrinkWrap.create(WebArchive.class, webArchiveName+".war");
        war.addClass(com.sun.ts.tests.common.vehicle.servlet.ServletVehicle.class);
        war.addClass(com.sun.ts.tests.common.vehicle.web.AltWebVehicleRunner.class);

        // Add the vehicle specific servlet
        switch (vehicleType) {
            case appmanagedNoTx:
                    war.addClass(com.sun.ts.tests.common.vehicle.appmanagedNoTx.AppManagedNoTxServletVehicle.class);
                break;
            case appmanaged:
                    war.addClass(com.sun.ts.tests.common.vehicle.appmanaged.AppManagedServletVehicle.class);
                    war.addAsWebInfResource(new StringAsset(""), "beans.xml");
                break;
            case stateful3:
                    war.addClass(com.sun.ts.tests.common.vehicle.stateful3.Stateful3ServletVehicle.class);
                    war.addAsWebInfResource(new StringAsset(""), "beans.xml");
                break;
            case stateless3:
                war.addClass(com.sun.ts.tests.common.vehicle.stateless3.Stateless3ServletVehicle.class);
                break;
        }
        ear.addAsModule(war);
        config.setVehicleArchiveName(webArchiveName);
        log.info(String.format("Added %s.war to: %s", webArchiveName, deploymentName));
    }

    /**
     * Unpack the ear to the clientEarDir
     * @param ear - the deployment ear
     * @param clientEarDir - the directory to unpack the ear to
     */
    private void unpackClientEar(EnterpriseArchive ear, File clientEarDir, String earLibDir) {
        for (ArchivePath path : ear.getContent().keySet()) {
            Node node = ear.get(path);
            if (node.getAsset() == null) {
                continue;
            }
            String archivePath = path.get();
            if(earLibDir.equals(INTERNAL_LIB_DIR) && archivePath.startsWith("/lib")) {
                // Skip the lib directory if the library-directory is set to private-lib
                continue;
            }
            if (node.getAsset() instanceof ArchiveAsset asset) {
                File archiveFile = new File(clientEarDir, path.get());
                if(!archiveFile.getParentFile().exists()) {
                    archiveFile.getParentFile().mkdirs();
                }
                final ZipExporter zipExporter = asset.getArchive().as(ZipExporter.class);
                zipExporter.exportTo(archiveFile, true);
                log.info("Exported test ear content to: " + archiveFile.getAbsolutePath());
            } else {
                File file = new File(clientEarDir, path.get());
                if(!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                try {
                    Asset asset = node.getAsset();
                    Files.copy(asset.openStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    log.info("Exported test ear content to: " + file.getAbsolutePath());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to export test ear content to: " + file.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * Determine the jar with the Main-Class manifest entry from the ear, and set the AppClientArchiveName
     * value on the config.
     *
     * @param ear    deployment ear
     * @param config the app client protocol configuration to update with the AppClientArchiveName
     * @return the FQN of the main class
     */
    private String determineAppMainJar(EnterpriseArchive ear, AppClientProtocolConfiguration config) {
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
                        config.setAppClientArchiveName(new AppClientArchiveName(jar.getArchive().getName()));
                        break;
                    }
                }
            }
        }
        return mainClass;
    }
}
