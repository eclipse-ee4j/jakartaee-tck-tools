package org.jboss.arquillian.protocol.appclient;

import org.jboss.arquillian.container.test.spi.client.protocol.Protocol;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class AppClientProtocolExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(Protocol.class, AppClientProtocol.class);
    }
}
