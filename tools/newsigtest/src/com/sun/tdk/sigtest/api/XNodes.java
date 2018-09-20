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

import java.util.*;


/**
 *  Container for API nodes.
 *  Each instance of the container contains a nodes of the same type only.
 */
public class XNodes extends ArrayList 
{

    public boolean add (Object x) 
    {
    	if (x == null)
            throw new Error();

	    if (!isEmpty() && x.getClass() != get(0).getClass())
	        throw new Error();
            
    	return super.add(x);
    }


    public XNode find (String n) 
    {
    	for (Iterator i = iterator(); i.hasNext();) {
	        XNode x = (XNode)i.next();
	        if (x.name.equals(n))
        		return x;
	    }

    	return null;
    }
    
    
    public XNode findSignature (String n) 
    {
    	for (Iterator i = iterator(); i.hasNext();) {
	        XNode x = (XNode)i.next();
    	    if (x.getSignature().equals(n))
	        	return x;
    	}

    	return null;
    }
    
}
