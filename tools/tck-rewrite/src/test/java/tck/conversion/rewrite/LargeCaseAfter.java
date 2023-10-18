//package com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics;
package tck.conversion.rewrite;

import java.util.Properties;

import com.sun.javatest.Status;
import com.sun.ts.lib.harness.EETest;
import com.sun.ts.lib.util.TSNamingContext;
import com.sun.ts.lib.util.TestUtil;

import com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBean;
import com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBeanHome;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * An example of a larger conversion test. This is read in as a source file by the
 * JavaTestToArqTest#testLargeCase() method so that it can be updated easily
 * and syntax checked. The class name and package are replaced by the testLargeCase method.
 */
@Disabled
public class LargeCaseAfter extends EETest {

    @Deployment(testable = false)
    public static Archive<?> deployment() {

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "ejb_bb_ssl_argsemantics.ear");
        // Add ear submodules

        JavaArchive ejb_bb_ssl_argsemantics_client_jar = ShrinkWrap.create(JavaArchive.class, "ejb_bb_ssl_argsemantics_client_jar");
        ejb_bb_ssl_argsemantics_client_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBean.class);
        ejb_bb_ssl_argsemantics_client_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBeanHome.class);
        ejb_bb_ssl_argsemantics_client_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.Client.class);
        ear.addAsModule(ejb_bb_ssl_argsemantics_client_jar);

        JavaArchive ejb_bb_ssl_argsemantics_ejb_jar = ShrinkWrap.create(JavaArchive.class, "ejb_bb_ssl_argsemantics_ejb_jar");
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBean.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBeanEJB.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.CallerBeanHome.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.CMP20Callee.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.CMP20CalleeEJB.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.CMP20CalleeHome.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.CMP20CalleeLocal.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.CMP20CalleeLocalHome.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.SimpleArgument.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.StatefulCallee.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.StatefulCalleeEJB.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.StatefulCalleeHome.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.StatefulCalleeLocal.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.calleebeans.StatefulCalleeLocalHome.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.wrappers.CMP20Wrapper.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.wrappers.StatefulWrapper.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.ejb.wrappers.StatelessWrapper.class);
        ejb_bb_ssl_argsemantics_ejb_jar.addClass(com.sun.ts.tests.common.testlogic.ejb.bb.argsemantics.TestLogic.class);
        ear.addAsModule(ejb_bb_ssl_argsemantics_ejb_jar);
        return ear;
    }

    private static final String testName = "EntityBeanTest";

    private static final String beanLookup = "java:comp/env/ejb/Caller";

    private CallerBean bean = null;

    private CallerBeanHome beanHome = null;

    private Properties props = null;

    private TSNamingContext nctx = null;

    public static void main(String[] args) {
        com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.Client theTests = new com.sun.ts.tests.ejb.ee.bb.session.stateless.argsemantics.Client();
        Status s = theTests.run(args, System.out, System.err);
        s.exit();
    }

    /**
     * @class.setup_props: org.omg.CORBA.ORBClass; java.naming.factory.initial;
     *                     generateSQL;
     */
    public void setup(String[] args, Properties props) throws Fault {
        try {
            logMsg("[Client] setup()");
            this.props = props;

            logTrace("[Client] Getting Naming Context...");
            nctx = new TSNamingContext();

            logTrace("[Client] Looking up " + beanLookup);
            beanHome = (CallerBeanHome) nctx.lookup(beanLookup, CallerBeanHome.class);
            logTrace("[Client] Create EJB instance...");
            bean = (CallerBean) beanHome.create();
        } catch (Exception e) {
            throw new Fault("Setup failed:", e);
        }
    }

    /**
     * @testName: testStatefulRemote
     *
     * @assertion_ids: EJB:SPEC:906
     *
     * @test_Strategy:
     *
     *                 This is applicable to : - a Session Stateful Callee bean
     *                 defining a remote client view only (No local view). - a
     *                 Stateless Caller bean, Calling this Callee Bean home or
     *                 remote interface.
     *
     *                 We package in the same ejb-jar: - a Session Stateful Callee
     *                 bean defining a remote client view only (No local view). -
     *                 a Stateless Caller bean
     *
     *                 Remote Home arg semantics verification:
     *
     *                 - We set a non-remote object 'arg' (of type SimpleArgument)
     *                 to an initial value. This SimpleArgument class is just a
     *                 data structure holding an int.
     *
     *                 - The Caller bean calls the Callee Home create(...) method,
     *                 passing this 'arg' object as an argument.
     *
     *                 - The Callee create(..) method modifies the value of the
     *                 argument (should be a copy of 'arg')
     *
     *                 - When we return from the create method, the Caller check
     *                 that the argument value is still set to the initial value.
     *
     *                 Remote interface arg semantics verif/loication: Same
     *                 strategy but the Caller call a business method on the
     *                 Callee remote interface.
     */
    @Test
    public void testStatefulRemote() throws Fault {
        boolean pass;

        try {
            pass = bean.testStatefulRemote(props);
        } catch (Exception e) {
            throw new Fault("testStatefulRemote failed", e);
        } finally {
            if (null != bean) {
                try {
                    bean.cleanUpBean();
                    bean.remove();
                } catch (Exception e) {
                    TestUtil.logErr("[Client] Ignoring Exception on " + "bean remove", e);
                }
            }
        }

        if (!pass) {
            throw new Fault("testStatefulRemote failed");
        }

    }

    /**
     * @testName: testStatefulLocal
     *
     * @assertion_ids: EJB:SPEC:907.2; EJB:SPEC:1
     *
     * @test_Strategy: This is applicable to : - a Session Stateful Callee bean
     *                 defining a local client view only (No remote view).
     *
     *                 - a Stateless Caller bean, Calling this Callee Bean local
     *                 home or local interface.
     *
     *                 We package in the same ejb-jar: - a Session Stateful Callee
     *                 bean defining a local client view only (No remote view). -
     *                 a Stateless Caller bean
     *
     *                 Local Home arg semantics verification:
     *
     *                 - We set a non-remote object 'arg' (of type SimpleArgument)
     *                 to an initial value. This SimpleArgument class is just a
     *                 data structure holding an int.
     *
     *                 - The Caller bean calls the Callee local home create(...)
     *                 method, passing this 'arg' object as an argument.
     *
     *                 - The Callee create(..) method modifies the value of the
     *                 argument (should be a reference to original 'arg')
     *
     *                 - When we return from the create method, the Caller check
     *                 that the argument value is not set to the initial value,
     *                 and reflect the changes made by the Callee.
     *
     *                 Local interface arg semantics verification:
     *
     *                 Same strategy but the Caller call a business method on the
     *                 Callee local interface.
     */
    @Test
    public void testStatefulLocal() throws Fault {
        boolean pass;

        try {
            pass = bean.testStatefulLocal(props);
        } catch (Exception e) {
            throw new Fault("testStatefulLocal failed", e);
        } finally {
            if (null != bean) {
                try {
                    bean.cleanUpBean();
                    bean.remove();
                } catch (Exception e) {
                    TestUtil.logErr("[Client] Ignoring Exception on " + "bean remove", e);
                }
            }
        }

        if (!pass) {
            throw new Fault("testStatefulLocal failed");
        }

    }

    /**
     * @testName: testStatefulBoth
     *
     * @assertion_ids: EJB:SPEC:906; EJB:SPEC:907; EJB:SPEC:907.2
     *
     *
     * @test_Strategy: This is applicable to :
     *
     *                 - a Session Stateful Callee bean defining a remote AND a
     *                 local client view.
     *
     *                 - a Stateless Caller bean, Calling this Callee Bean home,
     *                 local home, remote, or local interface.
     *
     *                 The test strategy is a cumulated version of the two
     *                 previous tests ('testStatefulRemote' and
     *                 'testStatefulLocal') on the Callee bean defining a local
     *                 and a remote client view.
     */
    @Test
    public void testStatefulBoth() throws Fault {
        boolean pass;

        try {
            pass = bean.testStatefulBoth(props);
        } catch (Exception e) {
            throw new Fault("testStatefulBoth failed", e);
        } finally {
            if (null != bean) {
                try {
                    bean.cleanUpBean();
                    bean.remove();
                } catch (Exception e) {
                    TestUtil.logErr("[Client] Ignoring Exception on " + "bean remove", e);
                }
            }
        }

        if (!pass) {
            throw new Fault("testStatefulBoth failed");
        }

    }

    /**
     * @testName: testCMP20Remote
     *
     * @assertion_ids: EJB:SPEC:906
     *
     * @test_Strategy:
     *
     *                 This is applicable to : - a CMP 2.0 Callee bean defining a
     *                 remote client view only (No local view). - a Stateless
     *                 Caller bean, Calling this CMP 2.0 Bean home or remote
     *                 interface.
     *
     *                 We package in the same ejb-jar: - a CMP 2.0 Callee bean
     *                 defining a remote client view only (No local view). - a
     *                 Stateless Caller bean
     *
     *                 Remote Home arg semantics verification:
     *
     *                 - We set a non-remote object 'arg' (of type SimpleArgument)
     *                 to an initial value. This SimpleArgument class is just a
     *                 data structure holding an int.
     *
     *                 - The Caller bean calls the Callee Home create(...) method,
     *                 passing this 'arg' object as an argument.
     *
     *                 - The Callee create(..) method modifies the value of the
     *                 argument (should be a copy of 'arg')
     *
     *                 - When we return from the create method, the Caller check
     *                 that the argument value is still set to the initial value.
     *
     *                 Remote interface arg semantics verification: Same strategy
     *                 but the Caller call a business method on the Callee remote
     *                 interface.
     */
    @Test
    public void testCMP20Remote() throws Fault {
        boolean pass;

        try {
            pass = bean.testCMP20Remote(props);
        } catch (Exception e) {
            throw new Fault("testCMP20Remote failed", e);
        } finally {
            if (null != bean) {
                try {
                    bean.remove();
                } catch (Exception e) {
                    TestUtil.logErr("[Client] Ignoring Exception on " + "bean remove", e);
                }
            }
        }

        if (!pass) {
            throw new Fault("testCMP20Remote failed");
        }

    }

    /**
     * @testName: testCMP20Local
     *
     * @assertion_ids: EJB:SPEC:1; EJB:SPEC:907.2
     *
     * @test_Strategy:
     *
     *                 This is applicable to : - a CMP 2.0 Callee bean defining a
     *                 local client view only (No remote view).
     *
     *                 - a Stateless Caller bean, Calling this CMP 2.0 Bean local
     *                 home or local interface.
     *
     *                 We package in the same ejb-jar:
     *
     *                 - a CMP 2.0 Callee bean defining a local client view only
     *                 (No remote view).
     *
     *                 - a Stateless Caller bean
     *
     *                 Local Home arg semantics verification:
     *
     *                 - We set a non-remote object 'arg' (of type SimpleArgument)
     *                 to an initial value. This SimpleArgument class is just a
     *                 data structure holding an int.
     *
     *                 - The Caller bean calls the Callee local home create(...)
     *                 method, passing this 'arg' object as an argument.
     *
     *                 - The Callee create(..) method modifies the value of the
     *                 argument (should be a reference to original 'arg')
     *
     *                 - When we return from the create method, the Caller check
     *                 that the argument value is not set to the initial value,
     *                 and reflect the changes made by the Callee.
     *
     *                 Local interface arg semantics verification:
     *
     *                 Same strategy but the Caller call a business method on the
     *                 Callee local interface.
     */
    @Test
    public void testCMP20Local() throws Fault {
        boolean pass;

        try {
            pass = bean.testCMP20Local(props);
        } catch (Exception e) {
            throw new Fault("testCMP20Local failed", e);
        } finally {
            if (null != bean) {
                try {
                    bean.remove();
                } catch (Exception e) {
                    TestUtil.logErr("[Client] Ignoring Exception on " + "bean remove", e);
                }
            }
        }

        if (!pass) {
            throw new Fault("testCMP20Local failed");
        }

    }

    /**
     * @testName: testCMP20Both
     *
     * @assertion_ids: EJB:SPEC:907; EJB:SPEC:907.1
     *
     * @test_Strategy:
     *
     *                 This is applicable to :
     *
     *                 - a CMP 2.0 Callee bean defining a remote AND a local
     *                 client view.
     *
     *                 - a Stateless Caller bean, Calling the CMP 2.0 Bean home,
     *                 local home, remote, or local interface.
     *
     *                 The test strategy is a cumulated version of the two
     *                 previous tests ('testCMP20Remote' and 'testCMP20Local') on
     *                 the Callee bean defining a local and a remote client view.
     */
    @Test
    public void testCMP20Both() throws Fault {
        boolean pass;

        try {
            pass = bean.testCMP20Both(props);
        } catch (Exception e) {
            throw new Fault("testCMP20Both failed", e);
        } finally {
            if (null != bean) {
                try {
                    bean.remove();
                } catch (Exception e) {
                    TestUtil.logErr("[Client] Ignoring Exception on " + "bean remove", e);
                }
            }
        }

        if (!pass) {
            throw new Fault("testCMP20Both failed");
        }

    }

    public void cleanup() throws Fault {
        logMsg("[Client] cleanup()");
    }
}
