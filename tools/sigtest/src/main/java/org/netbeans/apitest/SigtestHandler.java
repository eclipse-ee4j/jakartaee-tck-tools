/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
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
package org.netbeans.apitest;

import com.sun.tdk.signaturetest.Setup;
import com.sun.tdk.signaturetest.SignatureTest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

abstract class SigtestHandler {
    static String[] ACTIONS = new String[] {
        "generate",
        "check",
        "strictcheck",
        "versioncheck",
        "binarycheck",
    };

    public final int execute() throws IOException {
        if (getPackages().equals("-")) {
            logInfo("No public packages, skipping");
            return 0;
        }
        boolean generate = false;
        boolean addBootCP = false;
        boolean onlySameVersion = false;
        List<String> arg = new ArrayList<String>();
        arg.add("-FileName");
        arg.add(getFileName().getAbsolutePath());
        if (getAction().equals("generate")) {
            generate = true;
            addBootCP = true;
            arg.add("-static");
            arg.add("-ErrorAll");
            arg.add("-KeepFile");
        } else if (getAction().equals("check") || getAction().equals("binarycheck")) {
            // no special arg for check
            arg.add("-static");
            arg.add("-b");
            arg.add("-Mode");
            arg.add("bin");
            addBootCP = true;
            if (getAction().equals("binarycheck")) {
                arg.add("-extensibleinterfaces");
            }
        } else if (getAction().equals("strictcheck")) {
            addBootCP = true;
            arg.add("-static");
        } else if (getAction().equals("versioncheck")) {
            addBootCP = true;
            arg.add("-static");
            onlySameVersion = true;
        } else {
            throw new IOException("Unknown action: " + getAction() + " available actions are " + Arrays.toString(ACTIONS));
        }
        if (getVersion() != null) {
            arg.add("-ApiVersion");
            arg.add(getVersion());
        }
        for (String ignorePath : getIgnoreJDKClassEntries()) {
            arg.add("-IgnoreJDKClass");
            arg.add(ignorePath);
        }

        logInfo("Packages: " + getPackages());
        StringTokenizer packagesTokenizer = new StringTokenizer(getPackages(), ",:;");
        while (packagesTokenizer.hasMoreTokens()) {
            String p = packagesTokenizer.nextToken().trim();
            String prefix = "-PackageWithoutSubpackages "; // NOI18N
            //Strip the ending ".*"
            int idx = p.lastIndexOf(".*");
            if (idx > 0) {
                p = p.substring(0, idx);
            } else {
                idx = p.lastIndexOf(".**");
                if (idx > 0) {
                    prefix = "-Package "; // NOI18N
                    p = p.substring(0, idx);
                }
            }
            arg.add(prefix.trim());
            arg.add(p);
        }
        if (getClasspath() != null) {
            StringBuffer sb = new StringBuffer();
            String pref = "";
            for (String e : getClasspath()) {
                sb.append(pref);
                sb.append(e);
                pref = File.pathSeparator;
            }
            if (addBootCP) {
                Integer release = getRelease();
                arg.add("-BootCP");
                if (release != null) {
                    arg.add("" + release);
                }
            }
            arg.add("-Classpath");
            arg.add(sb.toString());
        }
        int returnCode;
        String[] args = arg.toArray(new String[0]);
        StringWriter output = new StringWriter();
        PrintWriter w = new PrintWriter(output, true);
        if (generate) {
            Setup t = new Setup();
            t.run(args, w, null);
            returnCode = t.isPassed() ? 0 : 1;
        } else {
            SignatureTest t = new SignatureTest();
            t.run(args, w, null);
            returnCode = t.isPassed() ? 0 : 1;
            if (onlySameVersion && !t.isPassed()) {
                // check the printed out versions
                final String prefix = "Base version: ";
                int index = output.toString().indexOf(prefix);
                if (index < 0) {
                    throw new IOException("Missing " + prefix + " in:\n" + output.toString());
                }
                int end = output.toString().indexOf('\n', index);
                String base = output.toString().substring(index + prefix.length(), end);
                logInfo("versioncheck.TestedVersion: " + getVersion());
                logInfo("versioncheck.BaseVersion: " + base);
                if (!getVersion().equals(base)) {
                    logInfo("versioncheck. clearing the return status.");
                    returnCode = 0;
                }
            }
        }
        String out;
        if (getMail() != null) {
            out = "\nemail: " + getMail() + "\n" + output;
        } else {
            out = output.toString();
        }
        if (returnCode == 0) {
            logInfo(out);
        } else {
            logError(out);
        }
        boolean fail;
        if (getReport() != null) {
            writeReport(getReport(), out, returnCode == 0);
            fail = Boolean.TRUE.equals(isFailOnError());
        } else {
            fail = !Boolean.FALSE.equals(isFailOnError());
        }
        return fail ? returnCode : 0;
    }

    //
    // Implementation
    //
    /**
     * Possibly write out a report.
     * @param reportFile an XML file to create with the report; if null, and there were some failures,
     *                   throw a {@link BuildException} instead
     */
    protected void writeReport(File reportFile, String msg, boolean success) throws IOException {
        assert reportFile != null;
        try {
            String apiName = getFileName().getName().replace(".sig", "").replace("-", ".");
            StringBuilder name = new StringBuilder();
            name.append(apiName).append('.').append(getAction());
            if (getVersion() != null) {
                name.append("_Version_").append(getVersion().replace('.', '_'));
            }
            Document reportDoc = createDocument("testsuite");
            Element testsuite = reportDoc.getDocumentElement();
            int failures = 0;
            testsuite.setAttribute("errors", "0");
            testsuite.setAttribute("time", "0.0");
            testsuite.setAttribute("name", name.toString()); // http://www.nabble.com/difference-in-junit-publisher-and-ant-junitreport-tf4308604.html#a12265700
            Element testcase = reportDoc.createElement("testcase");
            testsuite.appendChild(testcase);
            testcase.setAttribute("classname", name.toString());
            testcase.setAttribute("name", getAction());
            testcase.setAttribute("time", "0.0");
            if (!success) {
                failures++;
                Element failure = reportDoc.createElement("failure");
                testcase.appendChild(failure);
                failure.setAttribute("type", "junit.framework.AssertionFailedError");
                failure.setAttribute("message", "Failed " + getAction() + " for " + apiName + " in version " + getVersion());
            }
            testsuite.setAttribute("failures", Integer.toString(failures));
            testsuite.setAttribute("tests", Integer.toString(1));
            Element systemerr = reportDoc.createElement("system-err");
            systemerr.appendChild(reportDoc.createCDATASection(msg));
            testsuite.appendChild(systemerr);
            reportFile.getParentFile().mkdirs();
            OutputStream os = new FileOutputStream(reportFile);
            try {
                DOMSource dom = new DOMSource(reportDoc);
                StreamResult res = new StreamResult(os);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.transform(dom, res);
            } finally {
                os.close();
            }
            logInfo(reportFile + ": " + failures + " failures in " + getFileName());
        } catch (TransformerException ex) {
            throw new IOException(ex);
        } catch (IOException x) {
            throw new IOException("Could not write " + reportFile + ": " + x, x);
        }
    }
    private static Document createDocument(String rootQName) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            return factory.newDocumentBuilder().getDOMImplementation().createDocument(null, rootQName, null);
        } catch (ParserConfigurationException ex) {
            throw (IOException) new IOException("Cannot create parser").initCause(ex); // NOI18N
        }
    }

    protected abstract Integer getRelease();
    protected abstract String getPackages();
    protected abstract File getFileName();
    protected abstract String getAction();
    protected abstract String getVersion();
    protected abstract String[] getClasspath();
    protected abstract File getReport();
    protected abstract String getMail();
    protected abstract Boolean isFailOnError();
    protected abstract String[] getIgnoreJDKClassEntries();
    protected abstract void logInfo(String message);
    protected abstract void logError(String message);
}
