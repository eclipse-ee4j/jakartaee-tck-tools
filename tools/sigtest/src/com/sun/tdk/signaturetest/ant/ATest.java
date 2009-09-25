/*
 * $Id: SignatureTest.java 4549 2008-03-24 08:03:34Z me155718 $
 *
 * Copyright 1996-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tdk.signaturetest.ant;

import com.sun.tdk.signaturetest.Result;
import com.sun.tdk.signaturetest.SigTest;
import com.sun.tdk.signaturetest.SignatureTest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * <pre>
 * Ant wrapper for test command
 *
 * Required parameters:
 *   "package" attribute or nested "package" element is required.
 *     Corresponds to -package option
 *     Samples -
 *     &lt;setup package="javax.swing" ...
 *     or
 *     &lt;setup ...
 *       &lt;package name="javax.swing" /&gt;
 *       &lt;package name="java.lang" /&gt;
 *       ...
 *     &lt;/setup&gt;
 *   "classpath" attribute or nested "classpath" element is required.
 *     Corresponds to -classpath option
 *
 *   "filename" attribute is required.
 *     Corresponds to -filename option
 *
 * Optional parameters:
 *   "failonerror" - Stop the build process if the command exits with an error. Default is "false".
 *   "apiVersion" -  corresponds to -apiVersion. Set API version for signature file
 *   "backward" - corresponds to -Backward option. Performs backward compatibility checking.
 *      Default is "false".
 *   "binary" - corresponds to "-mode bin" option. Turns on binary mode. Default is "false".
 *   "errorAll" - corresponds to "-errorAll" option. Handles warnings as errors. Default is "false".
 *   "debug" - corresponds to "-debug" option, prints debug information. Default is "false".
 *   "formatHuman" - corresponds to "-formatHuman" option, processes human readable error output.
 *     Default is "false".
 *   "output" - corresponds to "-out filename" option, specifies report file name
 *   "negative" - inverts result (that is passed status treats as faild and vice versa, default is "false"
 *   "exclude" attribute or nested "exclude" element. Corresponds to -exclude option.
 *     package or class, which is not required to be tested
 *     Samples -
 *     &lt;setup package="javax.swing" exclude="javax.swing.text.ParagraphView" ...
 *     or
 *     &lt;setup ...
 *       &lt;exclude package="javax.swing.text" /&gt;
 *       &lt;exclude class="javax.swing.JTree$EmptySelectionModel" /&gt;
 *       ...
 *     &lt;/setup&gt;
 *
 * Task definition sample:
 * &lt;taskdef name="setup"
 *   classname="com.sun.tdk.signaturetest.ant.ASetup"
 *   classpath="sigtestdev.jar"/&gt;
 *
 * Task usage sample:
 * &lt;setup package="javax.swing" failonerror="true" apiVersion="swing"
 *   filename="javax_swing.sig"&gt;
 *   &lt;classpath&gt;
 *      &lt;pathelement location="/opt/java/jdk1.6.0_04/jre/lib/rt.jar"/&gt;
 *   &lt;/classpath&gt;
 *
 *   &lt;exclude class="javax.swing.tree.DefaultTreeSelectionModel"/&gt;
 *   &lt;exclude class="javax.swing.text.ParagraphView"/&gt;
 *   &lt;exclude class="javax.swing.tree.DefaultTreeSelectionModel"/&gt;
 *   &lt;exclude class="javax.swing.plaf.basic.BasicTextFieldUI$I18nFieldView"/&gt;
 *   &lt;exclude class="javax.swing.JEditorPane$PlainEditorKit$PlainParagraph"/&gt;
 *   &lt;exclude class="javax.swing.text.html.ParagraphView"/&gt;
 *   &lt;exclude class="javax.swing.plaf.basic.BasicTextAreaUI$PlainParagraph"/&gt;
 *   &lt;exclude class="javax.swing.JTree$EmptySelectionModel"/&gt;
 * &lt;/setup&gt;
 * </pre>
 *
 *
 * @author Mikhail Ershov
 */
public class ATest extends ABase {

    private boolean binary = false;
    private boolean backward = false;
    private boolean human = false;
    private String out;
    private boolean debug = false;
    private boolean errorAll = false;

    public void execute() throws BuildException {
        checkParams();
        SignatureTest s = testFactory();
        System.setProperty(Result.NO_EXIT, "true");
        s.run(createParams(), new PrintWriter(System.out, true), null);
        if (negative ? s.isPassed() : !s.isPassed()) {
            if (failOnError) {
                throw new BuildException(s.toString());
            } else {
                getProject().log(s.toString(), Project.MSG_ERR);
            }
        }
    }

    protected SignatureTest testFactory() {
        return new SignatureTest();
    }

    private String[] createParams() {
        ArrayList params = new ArrayList();
        createBaseParameters(params);
        params.add(SigTest.STATIC_OPTION);
        if (binary) {
            params.add(SignatureTest.MODE_OPTION);
            params.add(SignatureTest.BINARY_MODE);
        }
        if (backward)
            params.add(SigTest.BACKWARD_OPTION);
        else if (human)
            params.add(SigTest.FORMATHUMAN_OPTION);

        if (out != null && out.length() > 0) {
            params.add(SigTest.OUT_OPTION);
            params.add(out);
        }

        if (debug)
            params.add(SigTest.DEBUG_OPTION);
        if (errorAll)
            params.add(SigTest.ERRORALL_OPTION);

        return (String[]) params.toArray(new String[]{});
    }

    private void checkParams() throws BuildException {
        // classpath
        if (classpath == null || classpath.size() == 0)
            throw new BuildException("Classpath is not specified");

        // package
        if (pac.size() == 0)
            throw new BuildException("Package is not specified");

        // filename
        if (fileName == null || fileName.length() == 0)
            throw new BuildException("Filename is not specified");

    }


    public void setBinary(boolean b) {
        binary = b;
    }

    public void setBackward(boolean b) {
        backward = b;
    }

    public void setFormatHuman(boolean b) {
        human = b;
    }

    public void setOutput(String s) {
        out = s;
    }

    public void setDebug(boolean d) {
        debug = d;
    }

    public void setErrorAll(boolean d) {
        debug = d;
    }

}
