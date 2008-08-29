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
import java.util.List;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Ant task to execute setup, check and strict check with the API check class.
 * @author Michal Zlamal, Jaroslav Tulach
 */
public final class Sigtest extends Task {

    File fileName;
    Path classpath;
    String version;
    String packages;
    ActionType action;
    Boolean failOnError;
    File report;
    String failureProperty;

    public void setFileName(File f) {
        fileName = f;
    }

    public void setPackages(String s) {
        packages = s;
    }

    public void setAction(ActionType s) {
        action = s;
    }

    public void setClasspath(Path p) {
        if (classpath == null) {
            classpath = p;
        } else {
            classpath.append(p);
        }
    }

    public void setVersion(String v) {
        version = v;
    }

    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    public void setFailOnError(boolean b) {
        failOnError = b;
    }

    public void setFailureProperty(String p) {
        failureProperty = p;
    }

    public void setReport(File report) {
        this.report = report;
    }

    @Override
    public void execute() throws BuildException {
        if (fileName == null) {
            throw new BuildException("FileName has to filed", getLocation());
        }
        if (packages == null) {
            throw new BuildException("Packages has to filed", getLocation());
        }
        if (action == null) {
            throw new BuildException("Action has to filed", getLocation());
        }
        if (classpath == null) {
            throw new BuildException("Classpath has to filed", getLocation());
        }

        if (packages.equals("-")) {
            log("No public packages, skipping");
            return;
        }

        boolean generate = false;
        boolean strictcheck = false;
        boolean addBootCP = false;
        boolean onlySameVersion = false;
        List<String> arg = new ArrayList<String>();
        arg.add("-FileName");
        arg.add(fileName.getAbsolutePath());
        if (action.getValue().equals("generate")) {
            generate = true;
            addBootCP = true;
            arg.add("-static");
        } else if (action.getValue().equals("check")) {
            // no special arg for check
        } else if (action.getValue().equals("binarycheck")) {
            arg.add("-extensibleinterfaces");
        } else if (action.getValue().equals("strictcheck")) {
            addBootCP = true;
            strictcheck = true;
            arg.add("-static");
        } else if (action.getValue().equals("versioncheck")) {
            addBootCP = true;
            strictcheck = true;
            arg.add("-static");
            onlySameVersion = true;
        } else {
            throw new BuildException("Unknown action: " + action);
        }
        if (version != null) {
            arg.add("-Version");
            arg.add(version);
        }

        log("Packages: " + packages);
        StringTokenizer packagesTokenizer = new StringTokenizer(packages, ",");
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

        if (classpath != null) {
            StringBuffer sb = new StringBuffer();
            String pref = "";
            for (String e : classpath.list()) {
                sb.append(pref);
                sb.append(e);
                pref = File.pathSeparator;
            }
            if (addBootCP) {
                boolean rtJAR = false;
                File lib = new File(System.getProperty("java.home"), "lib");
                if (!lib.exists()) {
                    throw new BuildException("Missing " + lib + "/rt.jar");
                }
                for (File f : lib.listFiles()) {
                    if (f.getName().endsWith(".jar")) {
                        sb.append(File.pathSeparator).append(f);
                    }
                    if ("rt.jar".equals(f.getName())) {
                        rtJAR = true;
                    }
                }
                if (!rtJAR) {
                    log("Missing " + lib + "/rt.jar");
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
        } else if (strictcheck) {
            SignatureTest t = new SignatureTest();
            t.run(args, w, null);
            returnCode = t.isPassed() ? 0 : 1;
            
            if (onlySameVersion && !t.isPassed()) {
                // check the printed out versions
                final String prefix = "Base version: ";
                int index = output.toString().indexOf(prefix);
                if (index < 0) {
                    throw new BuildException("Missing " + prefix + " in:\n" + output.toString());
                }
                int end = output.toString().indexOf('\n', index);
                String base = output.toString().substring(index + prefix.length(), end);
                log("versioncheck.TestedVersion: " + version, Project.MSG_VERBOSE);
                log("versioncheck.BaseVersion: " + base, Project.MSG_VERBOSE);
                if (!version.equals(base)) {
                    log("versioncheck. clearing the return status.", Project.MSG_VERBOSE);
                    returnCode = 0;
                }
            }
        } else {
            returnCode = Main.run(args, w, w).getType();
        }
        
        String mail = getProject().getProperty("sigtest.mail");
        String out;
        if (mail != null) {
            out = "\nemail: " + mail + "\n" + output;
        } else {
            out = output.toString();
        }

        log(out);
        boolean fail;
        if (report != null) {
            writeReport(report, out, returnCode == 0 || Boolean.FALSE.equals(failOnError));
            fail = Boolean.TRUE.equals(failOnError);
        } else {
            fail = !Boolean.FALSE.equals(failOnError);
        }
        if (returnCode != 0) {
            if (failureProperty != null) {
                getProject().setProperty(failureProperty, "true");
            } else {
                if (fail) {
                    throw new BuildException("Signature tests return code is wrong (" + returnCode + "), check the messages above", getLocation());
                } 
            }
            log("Signature tests return code is wrong (" + returnCode + "), check the messages above");
        }
    }

    public static final class ActionType extends EnumeratedAttribute {

        public String[] getValues() {
            return new String[]{
                        "generate",
                        "check",
                        "strictcheck",
                        "versioncheck",
                        "binarycheck",
                    };
        }
    }

    //
    // Implementation
    //
    /**
     * Possibly write out a report.
     * @param reportFile an XML file to create with the report; if null, and there were some failures,
     *                   throw a {@link BuildException} instead
     */
    private void writeReport(File reportFile, String msg, boolean success) throws BuildException {
        assert reportFile != null;
        try {
            Document reportDoc = createDocument("testsuite");
            Element testsuite = reportDoc.getDocumentElement();
            int failures = 0;
            testsuite.setAttribute("errors", "0");
            testsuite.setAttribute("time", "0.0");
            testsuite.setAttribute("name", Sigtest.class.getName()); // http://www.nabble.com/difference-in-junit-publisher-and-ant-junitreport-tf4308604.html#a12265700
            Element testcase = reportDoc.createElement("testcase");
            testsuite.appendChild(testcase);
            String apiName = fileName.getName().replace(".sig", "").replace("-", ".");
            testcase.setAttribute("classname", apiName);
            testcase.setAttribute("name", action.getValue());
            testcase.setAttribute("time", "0.0");
            if (!success) {
                failures++;
                Element failure = reportDoc.createElement("failure");
                testcase.appendChild(failure);
                failure.setAttribute("type", "junit.framework.AssertionFailedError");
                failure.setAttribute("message", "Failed " + action.getValue() + " for " + apiName + " in version " + version);
            }
            testsuite.setAttribute("failures", Integer.toString(failures));
            testsuite.setAttribute("tests", Integer.toString(1));
            Element systemerr = reportDoc.createElement("system-err");
            systemerr.appendChild(reportDoc.createCDATASection(msg));
            testsuite.appendChild(systemerr);
            OutputStream os = new FileOutputStream(reportFile);
            try {
                DOMSource dom = new DOMSource(reportDoc);
                StreamResult res = new StreamResult(os);
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.transform(dom, res);
            } finally {
                os.close();
            }
            log(reportFile + ": " + failures + " failures in " + fileName);
        } catch (TransformerException ex) {
            throw new BuildException(ex);
        } catch (IOException x) {
            throw new BuildException("Could not write " + reportFile + ": " + x, x, getLocation());
        }
    }

    private static Document createDocument(String rootQName) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            return factory.newDocumentBuilder().getDOMImplementation().createDocument(null, rootQName, null);
        } catch (ParserConfigurationException ex) {
            throw (DOMException) new IOException("Cannot create parser").initCause(ex); // NOI18N
        }
    }
}
