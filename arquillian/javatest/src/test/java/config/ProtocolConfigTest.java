package config;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.config.descriptor.api.ProtocolDef;
import org.jboss.arquillian.container.test.impl.MapObject;
import org.jboss.arquillian.container.test.impl.domain.ProtocolDefinition;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tck.arquillian.protocol.javatest.JavaTestProtocol;
import tck.arquillian.protocol.javatest.JavaTestProtocolConfiguration;

import java.io.InputStream;

public class ProtocolConfigTest {
    @Test
    public void testMissingProtocolConfig() throws Exception {
        ArquillianDescriptor descriptor = loadConfiguration("bad-arquillian.xml");
        ContainerDef container = descriptor.getContainers().get(0);
        ProtocolDef protocolDef = null;
        JavaTestProtocol protocol = new JavaTestProtocol();
        for (ProtocolDef p : container.getProtocols()) {
            if (p.getType() != null && p.getType().equals(protocol.getDescription().getName())) {
                protocolDef = p;
                break;
            }
        }
        JavaTestProtocolConfiguration config;
        if (protocolDef == null) {
            config = (JavaTestProtocolConfiguration) new ProtocolDefinition(protocol).createProtocolConfiguration();
        } else {
            config = protocol.getProtocolConfigurationClass().newInstance();
            MapObject.populate(config, protocolDef.getProtocolProperties());
        }
        // This should have no
        Assertions.assertFalse(config.wasAnySetterCalled(), "No properties should have been set");
    }

    private ArquillianDescriptor loadConfiguration(String fileName) {
        final InputStream input = getClass().getResourceAsStream("/"+fileName);
        //First arquillian.xml is resolved
        final ArquillianDescriptor descriptor = Descriptors.importAs(ArquillianDescriptor.class)
                .fromStream(input);
        return descriptor;
    }
}
