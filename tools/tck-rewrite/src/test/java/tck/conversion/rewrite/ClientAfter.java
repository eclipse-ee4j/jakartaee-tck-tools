// package com.sun.ts.tests.assembly.altDD;
package tck.conversion.rewrite;

import java.util.Properties;

import com.sun.ts.lib.harness.Status;
import com.sun.ts.lib.harness.EETest;
import com.sun.ts.lib.util.TSNamingContext;
import com.sun.ts.lib.util.TestUtil;
import com.sun.ts.tests.assembly.altDD.PainterBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class ClientAfter extends EETest {

    @Deployment(testable = false)
    public static Archive<?> deployment() {

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "assembly_altDD.ear");
        // Add ear submodules

        JavaArchive assembly_altDD_client_jar = ShrinkWrap.create(JavaArchive.class, "assembly_altDD_client_jar");
        assembly_altDD_client_jar.addClass(com.sun.ts.tests.assembly.altDD.Client.class);
        assembly_altDD_client_jar.addClass(com.sun.ts.tests.assembly.altDD.PainterBean.class);
        ear.addAsModule(assembly_altDD_client_jar);

        JavaArchive assembly_altDD_ejb_jar = ShrinkWrap.create(JavaArchive.class, "assembly_altDD_ejb_jar");
        assembly_altDD_ejb_jar.addClass(com.sun.ts.tests.assembly.altDD.PainterBean.class);
        assembly_altDD_ejb_jar.addClass(com.sun.ts.tests.assembly.altDD.PainterBeanEJB.class);
        assembly_altDD_ejb_jar.addClass(com.sun.ts.tests.assembly.util.shared.ejbref.common.ReferencedBeanCode.class);
        assembly_altDD_ejb_jar.addClass(com.sun.ts.tests.common.ejb.wrappers.StatelessWrapper.class);
        ear.addAsModule(assembly_altDD_ejb_jar);
        return ear;
    }

    private static final String prefix = "java:comp/env/";

    private static final String entryLookup = prefix + "myCountry";

    private static final String beanLookup = prefix + "ejb/myPainter";

    /* Expected values for bean name */
    private static final String entryNameRef = "France";

    private static final String beanNameRef = "Gaughin";

    private Properties props = null;

    private TSNamingContext nctx = null;

    public static void main(String[] args) {
        com.sun.ts.tests.assembly.altDD.Client theTests = new com.sun.ts.tests.assembly.altDD.Client();
        Status s = theTests.run(args, System.out, System.err);
        s.exit();
    }

    /*
     * @class.setup_props: org.omg.CORBA.ORBClass; java.naming.factory.initial;
     */
    public void setup(String[] args, Properties props) throws Fault {

        try {
            this.props = props;

            logTrace("[Client] Getting Naming Context...");
            nctx = new TSNamingContext();
            logTrace("[Client] Setup completed!");
        } catch (Exception e) {
            logErr("[Client] Failed to obtain Naming Context:" + e);
            throw new Fault("[Client] Setup failed:" + e, e);
        }
    }

    /**
     * @testName: testAppClient
     *
     * @assertion_ids: JavaEE:SPEC:10260
     *
     * @test_Strategy: Package an application containing:
     *
     *                 - An application client jar file including its own DD: DD2.
     *                 DD2 declares one String environment entry, named
     *                 'myCountry' whose value is 'Spain'.
     *
     *                 - An alternate DD: DD4. DD4 is almost identical to DD2.
     *                 Nevertheless it changes the value for the 'myCountry'
     *                 environment entry: the new value is 'France'.
     *
     *                 - An application DD including the application client module
     *                 and using an alt-dd element to define DD4 as an alternate
     *                 DD for the application client.
     *
     *                 We check that:
     *
     *                 - We can deploy the application.
     *
     *                 - The application client can lookup the 'ejb/myCountry'
     *                 environment entry.
     *
     *                 - The runtime value is 'France', validating the use of DD4
     *                 at deployment time.
     */
    @Test
    public void testAppClient() throws Fault {
        String entryValue;
        boolean pass = false;

        try {
            logTrace("[Client] Looking up " + entryLookup);
            entryValue = (String) nctx.lookup(entryLookup);

            pass = entryValue.equals(entryNameRef);
            if (!pass) {
                logErr("[Client] Expected " + entryLookup + " name to be "
                        + entryNameRef + ", not " + entryValue);

                throw new Fault("Alternative DD test failed!");
            }
        } catch (Exception e) {
            logErr("[Client] Caught exception: " + e);
            throw new Fault("Alternative DD test failed!" + e, e);
        }
    }

    /**
     * @testName: testEJB
     *
     * @assertion_ids: JavaEE:SPEC:255
     *
     * @test_Strategy: Package an application containing:
     *
     *                 - An ejb-jar file including its own DD: DD1. This ejb-jar
     *                 contains 2 beans sharing the same Home and Remote
     *                 interfaces. According to DD1:
     *
     *                 . The two ejb-name's are Bean1 and Bean2.
     *
     *                 . Bean1 declares a String environment entry named myName
     *                 whose value is 'Dali '
     *
     *                 . Bean2 declares a String environment entry named myName
     *                 whose value is 'Picasso'
     *
     *                 - An application client jar file including its own DD: DD2.
     *                 DD2 declares one EJB reference using ejb-ref-name
     *                 'ejb/myPainter' and an ejb-link element targeting Bean1.
     *
     *                 - An alternate DD: DD3. DD3 is almost identical to DD1.
     *                 Nevertheless it changes the values for the myName
     *                 environment entries: Bean1 is 'Gaughin' and Bean2 is
     *                 'Matisse'.
     *
     *                 - An application DD including the EJB jar module and the
     *                 application jar module, but also using an alt-dd element to
     *                 define DD3 as an alternate DD for the ejb-jar.
     *
     *                 We check that:
     *
     *                 - We can deploy the application.
     *
     *                 - The application client can lookup 'ejb/myPainter' and
     *                 create a Bean instance.
     *
     *                 - The client can call a business method on that instance
     *                 that return the value of the myName environment entry in
     *                 the bean environment.
     *
     *                 - The returned value is 'Matisse', validating the use of
     *                 DD3 at deployment time.
     */
    @Test
    public void testEJB() throws Fault {
        PainterBean bean = null;
        String nameValue;
        boolean pass = false;

        try {
            logTrace("[Client] Looking up " + beanLookup);
            bean = (PainterBean) nctx.lookup(beanLookup, PainterBean.class);
            bean.createNamingContext();
            bean.initLogging(props);

            logTrace("[Client] Checking referenced EJB...");
            nameValue = bean.whoAreYou();

            pass = nameValue.equals(beanNameRef);
            if (!pass) {
                logErr("[Client] Expected " + beanLookup + " name to be " + beanNameRef
                        + ", not " + nameValue);

                throw new Fault("Alternative DD test failed!");
            }
        } catch (Exception e) {
            logErr("[Client] Caught exception: " + e);
            throw new Fault("Alternative DD test failed!" + e, e);
        }

    }

    public void cleanup() {
        logTrace("[Client] Cleanup.");
    }

}
