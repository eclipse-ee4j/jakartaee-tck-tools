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

package com.sun.tdk.sigtest;

import java.util.*;



public class Arguments extends ArrayList 
{

	public Arguments (String[] args) 
    {
		for (int i = 0; i < args.length; i++)
			add(args[i]);
	}
	
	
	Iterator iter = null;
	String modv = null;
	String argv = null;


	public boolean isArg (String arg, boolean value) 
    {
		return isArg(arg, value, false);
	}
	
	
	public boolean isArg (String arg, boolean value, boolean modf) 
    {
		modv = null;
		argv = null;
	
		for (begin(); hasNext();) {
			String x = next();
			
			int i = -1;
			if (modf && (i = x.indexOf(':')) != -1) 
				x = x.substring(0, i);
			
			if (x.equalsIgnoreCase(arg)) {
				remove();
				
				if (i != -1)
					modv = x.substring(i+1);
				
				boolean r = true;
				
				if (value) {
					if (hasNext()) {
						argv = next();
						remove();
					}
					else {
						System.err.println("missing value for " + arg);
						r = false;
					}
				}
					
				end();
				return r;
			}
		}
			
		return false;		
	}
	

	public String getModString () 
    {
		return modv;
	}
	
	
	public String getArgString () 
    {
		return argv;
	}
	
	
	public String getArgStringExpanded () 
    {
        return expand(argv);
	}
	
	
	public void begin () 
    {
		iter = iterator();
	}
	
	
	public void end () 
    {
		iter = null;
	}
	
	
	public boolean hasNext () 
    {
		if (iter != null && iter.hasNext())
			return true;
		else {
			iter = null;
			return false;
		}
	}
	
	
	public String next () 
    {
		return (String)iter.next();
	}
	
	
	public void remove () 
    {
		iter.remove();
	}
    
    public static String expand (String s)
    {
        int i = s.indexOf('@');
        if (i == -1)
            return s;
    
        StringBuffer sb = new StringBuffer(s);

        int k = i+1;        
        while (k < sb.length() && " ,;:/@".indexOf(sb.charAt(k)) == -1)
            k++;
        
        String key = sb.substring(i+1, k);
        try {
            sb.replace(i, k, System.getProperty(key));
            return expand(sb.toString());
        }
        catch (Exception e) {
            System.err.println("System property unknown: " + key);
            return s;
        }
    }
}
