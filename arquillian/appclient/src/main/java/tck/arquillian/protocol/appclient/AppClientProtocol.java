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

import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

import java.util.logging.Logger;

/**
 * Implementation of the Arquillian Protocol interface for Application Client testing.
 * This protocol handles the deployment and execution of tests in a Jakarta EE
 * Application Client container environment.
 */
public class AppClientProtocol implements Protocol<AppClientProtocolConfiguration> {
    static final Logger log = Logger.getLogger(AppClientProtocol.class.getName());

    @Inject
    private Instance<Injector> injectorInstance;

    /**
     * {@inheritDoc}
     * @return The configuration class for the Application Client protocol
     */
    @Override
    public Class<AppClientProtocolConfiguration> getProtocolConfigurationClass() {
        return AppClientProtocolConfiguration.class;
    }

    /**
     * {@inheritDoc}
     * @return A protocol description identifying this as the "appclient" protocol
     */
    @Override
    public ProtocolDescription getDescription() {
        return new ProtocolDescription("appclient");
    }

    /**
     * {@inheritDoc}
     * Creates and returns a deployment packager configured for Application Client deployments.
     * The packager is injected with Arquillian dependencies before being returned.
     * 
     * @return An initialized AppClientDeploymentPackager instance
     */
    @Override
    public DeploymentPackager getPackager() {
        log.info("getPackager() called");
        AppClientDeploymentPackager packager = new AppClientDeploymentPackager();
        Injector injector = injectorInstance.get();
        injector.inject(packager);

        return packager;
    }

    /**
     * {@inheritDoc}
     * Creates and returns a method executor that can run tests in an Application Client container.
     * 
     * @param protocolConfiguration The protocol-specific configuration. Unfortunately, this is not the instance
     *                              that was passed to the packager. We rely on injection of the protocol configuration
     *                              from the packager to the executor via the Arquillian Injector.
     * @param metaData Metadata about the deployed application
     * @param callback Callback for handling command results
     * @return A ContainerMethodExecutor configured for Application Client test execution
     */
    @Override
    public ContainerMethodExecutor getExecutor(AppClientProtocolConfiguration protocolConfiguration, ProtocolMetaData metaData,
            CommandCallback callback) {

        Injector injector = injectorInstance.get();
        // Create the AppClientCmd and AppClientMethodExecutor instances and have arquillian inject the Deployment into the executor
        AppClientCmd clientCmd = new AppClientCmd();
        injector.inject(clientCmd);
        clientCmd.init();
        AppClientMethodExecutor executor = new AppClientMethodExecutor(clientCmd, clientCmd.getConfig());
        injector.inject(executor);
        log.info("getExecutor() called, config="+clientCmd.getConfig());
        return executor;
    }
}
