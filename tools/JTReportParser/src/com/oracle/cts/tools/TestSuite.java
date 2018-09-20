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

package com.oracle.cts.tools;

import java.time.LocalDateTime;

/**
 *
 * @author anajosep
 */
public class TestSuite {
    
    private final String name;
    
    private final String id;
    
    private final String hostname;
    
    private String timestamp;
    
    //Store all counts
    private int passedTestsCount;
    
    private int failedTestsCount;
    
    private int errorTestsCount;
    
    private int excludedTestsCount;
   
    //Store all durations
    private long passedTestsDuration;
    
    private long failedTestsDuration;
    
    private long errorTestsDuration;
  
    private LocalDateTime startDateTime;
    
    private LocalDateTime endDateTime;
    
    
    public TestSuite (String id, String name, String hostname) {
        this.id = id;
        this.name = name;
        this.hostname = hostname;
    }

    public String getTimestamp() {
        if (this.endDateTime != null)
            return this.endDateTime.toString().replaceAll("\\.\\d{3}$", "");
        else 
            return LocalDateTime.now().toString().replaceAll("\\.\\d{3}$", "");
    }
       
    public int getTotalTestsCount() {
        return passedTestsCount + failedTestsCount + errorTestsCount + excludedTestsCount;
    }

        public int getPassedTestsCount() {
        return passedTestsCount;
    }

    public void setPassedTestsCount(int passedTestsCount) {
        this.passedTestsCount = passedTestsCount;
    }
    
    public int getFailedTestsCount() {
        return failedTestsCount;
    }

    public void setFailedTestsCount(int failedTestsCount) {
        this.failedTestsCount = failedTestsCount;
    }

    public int getErrorTestsCount() {
        return errorTestsCount;
    }

    public void setErrorTestsCount(int errorTestsCount) {
        this.errorTestsCount = errorTestsCount;
    }

    public int getExcludedTestsCount() {
        return excludedTestsCount;
    }

    public void setExcludedTestsCount(int disabledTestsCount) {
        this.excludedTestsCount += disabledTestsCount;
    }

    public long getTotalDuration() {
        return passedTestsDuration + failedTestsDuration + errorTestsDuration;
    }

    public void setPassedTestsDuration(long passedTestsDuration) {
        this.passedTestsDuration = passedTestsDuration;
    }

    public void setFailedTestsDuration(long failedTestsDuration) {
        this.failedTestsDuration = failedTestsDuration;
    }

    public void setErrorTestsDuration(long errorTestsDuration) {
        this.errorTestsDuration = errorTestsDuration;
    }
    
    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        if (this.startDateTime == null)
            this.startDateTime = startDateTime;
        else if (startDateTime.isBefore(this.startDateTime))
            this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        if (this.endDateTime == null)
            this.endDateTime = endDateTime;
        else if (endDateTime.isAfter(this.endDateTime))
            this.endDateTime = endDateTime;
    }

    public String  getXMLStartElement() {
        StringBuilder sb = new StringBuilder("<testsuite ");
        sb.append(" id=\"" + this.id + "\"");
        sb.append(" name=\"" + this.name + "\"");
        sb.append(" hostname=\"" + this.hostname + "\"");
        sb.append(" tests=\"" + this.getTotalTestsCount()+"\"");
        sb.append(" failures=\"" + this.getFailedTestsCount()+"\"");
        sb.append(" errors=\"" + this.getErrorTestsCount()+"\"");
        sb.append(" disabled=\"" + this.getExcludedTestsCount()+"\"");
        sb.append(" skipped=\"" + this.getExcludedTestsCount()+"\"");
        sb.append(" time=\"" + this.getTotalDuration()+"\"");
        sb.append(" timestamp=\"" + this.getTimestamp()+"\"");
        sb.append(">");
        return sb.toString();
    }
    
    public String getXMLEndElement() {
        return "</testsuite>";
    }
    
    public String asText() {
        StringBuilder sb = new StringBuilder(this.name);
        sb.append(":");
        sb.append(this.getTotalTestsCount());
        sb.append(",");
        sb.append(this.getPassedTestsCount());
        sb.append(",");
        sb.append(this.getFailedTestsCount());
        sb.append(",");
        sb.append(this.getExcludedTestsCount());
        return sb.toString();
    }
    
     public String asJson() {
        StringBuilder sb = new StringBuilder(this.name);
        sb.append("{");
        sb.append("\"suite:\"");
        sb.append("\"").append(this.name).append("\"");
        sb.append("\"total:\"");
        sb.append(this.getTotalTestsCount());
        sb.append(",");
        sb.append("\"passed:\"");
        sb.append(this.getPassedTestsCount());
        sb.append(",");
        sb.append("\"failed:\"");
        sb.append(this.getFailedTestsCount());
        sb.append(",");
        sb.append("\"excluded:\"");
        sb.append(this.getExcludedTestsCount());
        return sb.toString();
    }
}
