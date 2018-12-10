/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.eclipse.ee4j.jakartaeetck.tools.jtreportparser;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.text.StringEscapeUtils;

/**
 *
 * @author anajosep
 */
public class TestCase {
    
    private final String testName;
    
    private final String className;
    
    private final long duration;
    
    private final TestStatus status;
    
    private final String sysoutFile;
    
    private String errorMsg;
    
    public TestCase (String testName, String className, long duration, TestStatus status, String sysoutFile) {
        this.testName = testName;
        this.className = className;
        this.duration = duration;
        this.status = status;
        this.sysoutFile=sysoutFile;
    }
    
    public TestStatus getStatus() {
        return status;
    }
    public void setErrorMsg(String msg) {
        String value = msg;
        value = value.replaceAll("\"", "&#34;");
        value = value.replaceAll("&", "&#38;");
        value = value.replaceAll("'", "&#39;");
        value = value.replaceAll("<", "&lt;");
        value = value.replaceAll(">", "&gt;");
        this.errorMsg = value;
    }
    
    public String getErrorMsg(){
        return this.errorMsg;
    }
    
    public String getName() {
        return "" + this.className + "#" + this.testName;
    }
    
    public String toXML() {
        StringBuilder sb = new StringBuilder("  <testcase ");
        sb.append(" name=\"").append(testName).append("\"");
        sb.append(" classname=\"").append(className).append("\"");
        sb.append(" time=\"").append(duration).append("\"");
        sb.append(" status=\"").append(status.getStatus()).append("\"");
        sb.append(">");
        sb.append(System.getProperty("line.separator"));
        if (status == TestStatus.ERROR)
            sb.append("<error type=\"Error\" message=\"").append(errorMsg).append("\">").append(errorMsg).append("</error>");
        if (status == TestStatus.FAILED)
            sb.append("<failure type=\"AssertionFailure\" message=\"").append(errorMsg).append("\">").append(errorMsg).append("</failure>");
        if (status == TestStatus.EXCLUDED)
            sb.append("<skipped />");
        sb.append("<system-out>");
        sb.append(System.getProperty("line.separator"));
        sb.append(sysoutFile);
        sb.append(System.getProperty("line.separator"));
        sb.append("</system-out>");
        sb.append(System.getProperty("line.separator"));
        sb.append("</testcase>");
        return sb.toString();
    }
    
    public void toXML(PrintWriter writer) {
        writer.println("  <testcase ");
        writer.println(" name=\"" + testName + "\"");
        writer.println(" classname=\"" + className + "\"");
        writer.println(" time=\"" + duration + "\"");
        writer.println(" status=\"" + status.getStatus() + "\"");
        writer.println(">");
        if (status == TestStatus.ERROR)
            writer.println("<error type=\"Error\" message=\"" + errorMsg + "\">" + errorMsg + "</error>");
        if (status == TestStatus.FAILED)
            writer.println("<failure type=\"AssertionFailure\" message=\"" + errorMsg + "\">" + errorMsg + "</failure>");
        if (status == TestStatus.EXCLUDED)
            writer.println("<skipped />");
        writer.println("<system-out>");      
        File outputFile = new File(sysoutFile);
        if (outputFile.exists() && outputFile.isFile() && outputFile.canRead()) {
            try (Stream<String> stream = Files.lines(Paths.get(sysoutFile))) {
                stream.forEach(x -> writer.println(StringEscapeUtils.escapeXml11(x)));
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
        writer.println("</system-out>");
        writer.println("</testcase>");
        writer.flush();
    }
        
    @Override
    public String toString() {
        return "TestCase{" + "testName=" + testName + ", className=" + 
                className + ", status=" + status + '}';
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.testName);
        hash = 67 * hash + Objects.hashCode(this.className);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TestCase other = (TestCase) obj;
        if (!Objects.equals(this.testName, other.testName)) {
            return false;
        }
        return Objects.equals(this.className, other.className);
    }
}
