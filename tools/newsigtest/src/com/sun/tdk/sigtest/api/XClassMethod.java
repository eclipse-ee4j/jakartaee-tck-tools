/*
 * Copyright (c) 2003, 2018 Oracle and/or its affiliates. All Rights Reserved.
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

package com.sun.tdk.sigtest.api;


public class XClassMethod extends XClassFunc 
{

    public String type;
    
    
    public void link (XClass x) 
    {
	    home = x;
        if (home.methods == null)
            home.methods = new XNodes();
	    home.methods.add(this);
    }
    
    
    public void unLink ()
    {
        home.methods.remove(this);
        home = null;
    }
    
    
    public XClassMember findSame (XClass x)
    {
        return x.methods == null ? null : (XClassMember)x.methods.findSignature(getSignature());
    }


    public String getFullDescription ()
    {
        StringBuffer sb = new StringBuffer();

        sb.append(XModifier.toString(modifiers));
        sb.append(' ');
        sb.append(type);
        sb.append(' ');
        sb.append(home.getFullName());
        sb.append('.');
        sb.append(getSignature());
        if (xthrows != null && xthrows.length != 0)
            sb.append(" throws ").append(Utils.list(xthrows));

        return sb.toString();
    }
    
}


