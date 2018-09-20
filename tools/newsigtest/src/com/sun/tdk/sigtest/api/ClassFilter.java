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

import java.io.*;
import java.util.*;


public class ClassFilter
{
    static final String sep  = File.pathSeparator;
    static final String spec = "^";
    
    
    static class PathElem
    {
        boolean add,
                depth;
        String  path;
        
        
        PathElem (String s, boolean a, boolean d)
        {
            add   = a;
            depth = d;
            
            path  = s;
            if (!s.endsWith("."))
                path += '.';
        }
    }

    
    List pathParsed;

        
    public ClassFilter ()
    {
        clear();
    }


    public void clear () 
    {
    	pathParsed = new ArrayList();
    }


    public boolean parse (String args) 
    {
	    char op = '+';
        StringTokenizer st = new StringTokenizer(args, sep + " +-", true);
        while (st.hasMoreElements()) {
	        String s = (String)st.nextElement();
            if (s.equals(sep) || s.equals(" ") || s.equals("+"))
        	    op = '+';
            else if (s.equals("-"))
                op = '-';
    	    else {
                boolean depth = true;
                if (s.startsWith(spec)) {
                    s = s.substring(1);
                    depth = false;
                }
                pathParsed.add(new PathElem(s, op == '+', depth));
            }
	    }

    	return true;
    }


    public boolean isEmpty () 
    {
        return pathParsed.isEmpty();
    }
	
	
    public void add (String s, boolean depth) 
    {
        pathParsed.add(new PathElem(s, true, depth));
    }
	
	
    public void sub (String s, boolean depth) 
    {
        pathParsed.add(new PathElem(s, false, depth));
    }


    public boolean onPath (String s)
    {
        if (pathParsed.size() == 0)
            return true;
    
    	if (s == null)
	        s = "";
            
        if (!s.endsWith("."))
            s += '.';

        if (s.equals("."))
            return true;            
            
        int r = 0;
            
        for (Iterator it = pathParsed.iterator(); it.hasNext();) {
            PathElem pe = (PathElem)it.next();
                
            boolean hit = (pe.depth) 
                          ? pe.path.startsWith(s) || s.startsWith(pe.path) 
                          : pe.path.startsWith(s);
                
            if (hit)
                if (pe.add)
                    r++;
                else
                    r--;
        }
        
        return r > 0;
    }
        
        
    public boolean inPath (String s)
    {
        if (pathParsed.size() == 0)
            return true;
    
    	if (s == null)
	        s = "";
            
        if (!s.endsWith("."))
            s += '.';
            
        int r = 0;
            
        for (Iterator it = pathParsed.iterator(); it.hasNext();) {
            PathElem pe = (PathElem)it.next();
            
            boolean hit = (pe.depth) 
                            ? s.startsWith(pe.path) 
                            : s.startsWith(pe.path) && 
                              s.indexOf('.', pe.path.length()) == s.length() - 1;
                   
            if (hit)
                if (pe.add)
                    r++;
                else
                    r--;
        }
        
        return r > 0;
    }
        

    public String toString () 
    {
        StringBuffer sb = new StringBuffer();
		
        for (Iterator it = pathParsed.iterator(); it.hasNext();) {
            PathElem pe = (PathElem)it.next();
            
            if (!pe.add || sb.length() != 0)
                sb.append(pe.add ? '+' : '-');
            
            if (!pe.depth)
                sb.append(spec);    
                
            // remove the trailing '.'    
            sb.append(pe.path.substring(0, pe.path.length()-1));
        }

        return sb.toString();
    }
}


