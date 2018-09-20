/*
 * Copyright (c) 2006, 2018 Oracle and/or its affiliates. All rights reserved.
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

/*
 * $Id$
 */


package com.sun.ant.taskdefs.common;

import  org.apache.tools.ant.*;
import  org.apache.tools.ant.taskdefs.*;


public class PropertyIndex
        extends Task
{
    private String from;
    private String property;
    private int index = 1;
    private String delim = ",";

    public PropertyIndex()
    {
        super();
    }


    public void setFrom(String from)
    {
        this.from = from;
    }

    public void setName(String property)
    {
		this.property = property;

    }
	
    public void setIndex(String sIndex)
    {
        Integer i = new Integer(sIndex);
        this.index = i.intValue();
    }

    public void setDelimiter(String delim)
    {
        this.delim = delim;
    }

    protected void validate()
    {
        if (from == null || property == null )
            throw new BuildException("No inputProp specified.");
    }


    public void execute()
            throws BuildException
    {
        validate();

        String value = getProject().getProperty(from);
        if (value != null){
            if (index == 0){
                setPropertyValue(value);
            } else {
                String[] sArray = value.split(delim);
                if (sArray.length > 0){
                   if (index <= sArray.length){
                      if (sArray[index-1] != null){
                          setPropertyValue(sArray[index-1]);
                      }
                   }
                }
            }
        }
    }

    protected void setPropertyValue(String value)
	{
	   if (value != null)
	   {
	      if (getProject().getUserProperty(property) == null)
	           getProject().setProperty(property, value);
	      else
	           getProject().setUserProperty(property, value);
	   }
    }
}
