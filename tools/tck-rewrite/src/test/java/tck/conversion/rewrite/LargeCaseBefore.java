//package com.sun.ts.tests.servlet.api.jakarta_servlet_http.sessioncookieconfig;
package tck.conversion.rewrite;

import java.io.PrintWriter;

import com.sun.javatest.Status;
import com.sun.ts.tests.servlet.common.client.AbstractUrlClient;
import org.junit.jupiter.api.Disabled;

/**
 * An example of a larger conversion test. This is read in as a source file by the
 * JavaTestToArqTest#testLargeCase() method so that it can be updated easily
 * and syntax checked. The class name and package are replaced by the testLargeCase method.
 */
@Disabled
public class LargeCaseBefore extends AbstractUrlClient {

    /**
     * Entry point for different-VM execution. It should delegate to method
     * run(String[], PrintWriter, PrintWriter), and this method should not contain
     * any test configuration.
     */
    public static void main(String[] args) {
        com.sun.ts.tests.servlet.api.jakarta_servlet_http.sessioncookieconfig.URLClient theTests = new com.sun.ts.tests.servlet.api.jakarta_servlet_http.sessioncookieconfig.URLClient();
        Status s = theTests.run(args, new PrintWriter(System.out),
                new PrintWriter(System.err));
        s.exit();
    }

    /**
     * Entry point for same-VM execution. In different-VM execution, the main
     * method delegates to this method.
     */
    public Status run(String args[], PrintWriter out, PrintWriter err) {

        setContextRoot("/servlet_jsh_sessioncookieconfig_web");
        setServletName("TestServlet");

        return super.run(args, out, err);
    }

    /*
     * @class.setup_props: webServerHost; webServerPort; ts_home;
     */
    /* Run test */
    /*
     * @testName: constructortest1
     *
     * @assertion_ids: Servlet:JAVADOC:693; Servlet:JAVADOC:733;
     * Servlet:JAVADOC:734; Servlet:JAVADOC:735; Servlet:JAVADOC:736;
     * Servlet:JAVADOC:737; Servlet:JAVADOC:738; Servlet:JAVADOC:739;
     * Servlet:JAVADOC:740; Servlet:JAVADOC:741; Servlet:JAVADOC:742;
     * Servlet:JAVADOC:743; Servlet:JAVADOC:744; Servlet:JAVADOC:745;
     * Servlet:JAVADOC:746;
     *
     * @test_Strategy: Create a Servlet TestServlet, with a
     * ServletContextListener; In the Servlet, turn HttpSession on; In
     * ServletContextListener, create a SessionCookieConfig instance, Verify in
     * Client that the SessionCookieConfig instance is created, and all
     * SessionCookieConfig APIs work accordingly.
     */
    public void constructortest1() throws Exception {
        TEST_PROPS.setProperty(REQUEST,
                "GET /servlet_jsh_sessioncookieconfig_web/TestServlet?testname=constructortest1 HTTP/1.1");
        TEST_PROPS.setProperty(EXPECTED_HEADERS,
                "Set-Cookie:" + "TCK_Cookie_Name=" + "##Expires="
                        + "##Path=/servlet_jsh_sessioncookieconfig_web/TestServlet"
                        + "##Secure");
        TEST_PROPS.setProperty(UNEXPECTED_RESPONSE_MATCH, "Test FAILED");
        invoke();
    }

    /*
     * @testName: setNameTest
     *
     * @assertion_ids: Servlet:JAVADOC:744;
     *
     * @test_Strategy: Create a Servlet TestServlet, In the Servlet, turn
     * HttpSession on; Verify in servlet SessionCookieConfig.setName cannot be
     * called once is set.
     */
    public void setNameTest() throws Exception {
        TEST_PROPS.setProperty(APITEST, "setNameTest");
        invoke();
    }

    /*
     * @testName: setCommentTest
     *
     * @assertion_ids: Servlet:JAVADOC:740;
     *
     * @test_Strategy: Create a Servlet TestServlet, In the Servlet, turn
     * HttpSession on; Verify in servlet SessionCookieConfig.setComment cannot be
     * called once is set.
     */
    public void setCommentTest() throws Exception {
        TEST_PROPS.setProperty(APITEST, "setCommentTest");
        invoke();
    }

    /*
     * @testName: setPathTest
     *
     * @assertion_ids: Servlet:JAVADOC:745;
     *
     * @test_Strategy: Create a Servlet TestServlet, In the Servlet, turn
     * HttpSession on; Verify in servlet SessionCookieConfig.setPath cannot be
     * called once is set.
     */
    public void setPathTest() throws Exception {
        TEST_PROPS.setProperty(APITEST, "setPathTest");
        invoke();
    }

    /*
     * @testName: setDomainTest
     *
     * @assertion_ids: Servlet:JAVADOC:741;
     *
     * @test_Strategy: Create a Servlet TestServlet, In the Servlet, turn
     * HttpSession on; Verify in servlet SessionCookieConfig.setDomain cannot be
     * called once is set.
     */
    public void setDomainTest() throws Exception {
        TEST_PROPS.setProperty(APITEST, "setDomainTest");
        invoke();
    }

    /*
     * @testName: setMaxAgeTest
     *
     * @assertion_ids: Servlet:JAVADOC:743;
     *
     * @test_Strategy: Create a Servlet TestServlet, In the Servlet, turn
     * HttpSession on; Verify in servlet SessionCookieConfig.setMaxAge cannot be
     * called once is set.
     */
    public void setMaxAgeTest() throws Exception {
        TEST_PROPS.setProperty(APITEST, "setMaxAgeTest");
        invoke();
    }

    /*
     * @testName: setHttpOnlyTest
     *
     * @assertion_ids: Servlet:JAVADOC:742;
     *
     * @test_Strategy: Create a Servlet TestServlet, In the Servlet, turn
     * HttpSession on; Verify in servlet SessionCookieConfig.setHttpOnly cannot be
     * called once is set.
     */
    public void setHttpOnlyTest() throws Exception {
        TEST_PROPS.setProperty(APITEST, "setHttpOnlyTest");
        invoke();
    }

    /*
     * @testName: setSecureTest
     *
     * @assertion_ids: Servlet:JAVADOC:746;
     *
     * @test_Strategy: Create a Servlet TestServlet, In the Servlet, turn
     * HttpSession on; Verify in servlet SessionCookieConfig.setSecure cannot be
     * called once is set.
     */
    public void setSecureTest() throws Exception {
        TEST_PROPS.setProperty(APITEST, "setSecureTest");
        invoke();
    }

    /*
     * @testName: setAttributeTest
     *
     * @assertion_ids:
     *
     * @test_Strategy: Create a Servlet TestServlet, In the Servlet, turn
     * HttpSession on; Verify in servlet SessionCookieConfig.setAttribute cannot be
     * called once is set.
     */
    public void setAttributeTest() throws Exception {
        TEST_PROPS.setProperty(APITEST, "setAttributeTest");
        invoke();
    }
}
