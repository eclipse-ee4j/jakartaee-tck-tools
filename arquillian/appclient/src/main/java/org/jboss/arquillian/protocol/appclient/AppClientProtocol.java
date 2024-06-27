package org.jboss.arquillian.protocol.appclient;

import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.test.spi.ContainerMethodExecutor;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentPackager;
import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.container.test.spi.command.CommandCallback;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

public class AppClientProtocol implements Protocol<AppClientProtocolConfiguration> {
    @Override
    public Class<AppClientProtocolConfiguration> getProtocolConfigurationClass() {
        return AppClientProtocolConfiguration.class;
    }

    @Override
    public ProtocolDescription getDescription() {
        return new ProtocolDescription("appclient");
    }

    @Override
    public DeploymentPackager getPackager() {
        return new AppClientDeploymentPackager();
    }

    @Override
    public ContainerMethodExecutor getExecutor(AppClientProtocolConfiguration protocolConfiguration, ProtocolMetaData metaData,
            CommandCallback callback) {

        AppClientCmd clientCmd = new AppClientCmd(protocolConfiguration);
        return new AppClientMethodExecutor(clientCmd, protocolConfiguration);
    }
}
