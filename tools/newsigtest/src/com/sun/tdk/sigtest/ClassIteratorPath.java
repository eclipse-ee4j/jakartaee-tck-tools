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

import com.sun.tdk.sigtest.api.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;


class ClassIteratorPath implements ClassIterator 
{
    static final int maxDepth = 128;

    String classpath;
    ClassProcessor classproc;
    ClassFilter   classfilter;
    ArrayList/*String*/ pathVisited = new ArrayList();
	
	
    ClassIteratorPath () 
    {
    }
	
	
    ClassIteratorPath (String p) 
    {
    	addPath(p);
    }
		
		
    void addPath (String p) 
    {
	    if (classpath == null)
	        classpath = p;
    	else
	        classpath += File.pathSeparator + p;
    }
	
	
    public String getPath () 
    {
	    return classpath;
    }
	
	
    public void iterate (ClassProcessor cp, ClassFilter cf) 
    {
	    //System.err.println("classpath " + classpath);
    	classproc   = cp;
    	classfilter = cf;
		
	    StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator, false);
    	while (st.hasMoreElements()) 
	        walkPath((String)st.nextElement());
			
    	classfilter = null;
    	classproc   = null;
    }
	
	
    void walkPath (String path) 
    {
    	if (path.length() == 0)
	        return;	

    	File f = new File(path);
	    if (!f.exists())
	        return;
			
    	if (f.isDirectory()) {
	        walkDir(0, null, path);
    	}
		
    	else {
            if (pathVisited.contains(path)) 
		        return;
        	pathVisited.add(path);
			
	        ClassDataZip zip = null;
            try {
        		zip = new ClassDataZip(f);
	        }
    	    catch (IOException e) {
	        	System.err.println("Ignored (not a zip file): " + path);
		        return;
    	    }

            ZipEntry ze;
            for (Enumeration ents = zip.entries(); ents.hasMoreElements(); ) {
                ze = (ZipEntry)ents.nextElement();
        		if (!ze.isDirectory()) {
                    zip.set(ze);
		            walkFile(ze.getName().replace('/', '.'), zip);
                }
            }
					
        	zip.close();
    	}
    }
		
	
    //  This is recursive procedure	
    void walkDir (int depth, String pack, String path) 
    {
    	ClassDataFile f = new ClassDataFile(path);
	    if (!f.exists())
            return;
			
    	if (f.isDirectory()) {	
	        if (pathVisited.contains(path)) 
        		return;
	        pathVisited.add(path);
			
    	    if (classfilter != null && !classfilter.onPath(pack))
	        	return;
			
            if (depth == maxDepth) {
        		System.err.println("directory too depth " + path);
		        return;
    	    }
			
    	    if (pack == null || pack.length() == 0)
	        	pack = "";
    	    else
	        	pack += '.';
		
    	    if (!path.endsWith(File.separator))
	        	path += File.separator;
		
    	    String[] list = f.list();
            if (list != null)
           		for (int i = 0; i < list.length; i++)
	                walkDir(depth+1, pack + list[i], path + list[i]);
    	}
		
    	else {
	        walkFile(pack, f);
    	}
		
    }
	
	
    void walkFile (String name, ClassData cd) 
    {
    	if (name.endsWith(".class") && name.indexOf('$') == -1) {
            String n = name.substring(0, name.length()-6);
	        if (classfilter == null || classfilter.inPath(n))
		        classproc.process(n, cd);
    	}
    }
    
}



