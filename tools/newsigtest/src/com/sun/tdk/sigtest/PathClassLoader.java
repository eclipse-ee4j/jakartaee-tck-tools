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

import java.io.*;
import java.util.*;
import java.util.zip.*;



class PathClassLoader extends ClassLoader
{
	boolean delegate = false;
    ArrayList/*File*/  dirs = new ArrayList();
	HashSet/*String*/ seen = new HashSet();
    Hashtable/*String,Class*/ classes = new Hashtable();


    PathClassLoader ()
    {
        super();
    }
    

    PathClassLoader (String cp)
    {
        super();
        addPath(cp);
    }
    
    
    void setDelegate (boolean m)
    {
        delegate = m;
    }
    

    void addPath (String cp) {
	
		StringTokenizer st = new StringTokenizer(cp, 
                                                 File.pathSeparator,
                                                 false); 
		
		while (st.hasMoreElements()) {
		
			String s = (String)st.nextElement();
			
			if (seen.contains(s))
				continue;
			seen.add(s);
				
            File f = new File(s);

   	        if (f.isDirectory()) {
                dirs.add(f);
				//System.err.println("dir " + s);
           	}
            else if (f.isFile()) {
   	            ZipFile z;
       	        try {
           	        z = new ZipFile(f);
               	    dirs.add(z);
					//System.err.println("zip " + s);
                }
   	            catch (IOException x) {
					System.err.println("Not a zip file: " + s);
       	        }
			}
			else {
				System.err.println("Ignored: " + s);
			}
		}
    }

   
    void close () {
	
        for (int i = 0; i < dirs.size(); i++) {
            Object o = dirs.get(i);
            if (o instanceof ZipFile) {
                try {
                    ((ZipFile)o).close();
                }
                catch (IOException x) {
                }
            }
            else {
            }
        }
        dirs.clear();

        classes.clear();
    }


    protected 
    Class loadClass (String name, boolean resolve) throws ClassNotFoundException 
    {
        //System.err.println("PathLoader.loadClass:"+name+" "+resolve);			

        Class c = null;
        
        if (delegate) {
            try {
                c = super.loadClass(name, resolve);
            }
            catch (ClassNotFoundException e) {
                if ((c = (Class)classes.get(name)) == null)
                    c = locateClass(name);

                if (resolve)
                    resolveClass(c);
            }
        }
        else {
            if ((c = (Class)classes.get(name)) == null)
                c = locateClass(name);

            if (resolve)
                resolveClass(c);
        }
	
        return c;
    }


    private synchronized 
    Class locateClass(String name)	throws ClassNotFoundException 
    {
        //System.err.println("PathLoader.locateClass:"+name);			
        if (name.startsWith("java."))
            return findSystemClass(name);   

        Class c = null;
        String cname = name.replace('.', File.separatorChar) + ".class", //
               dname = name.replace('.', '/') + ".class";                // for zip-file
	    
        InputStream in = null;
        int bcount = 0;

        for (int i = 0; i < dirs.size(); i++)
        {
            Object o = dirs.get(i); 
            if (o instanceof File)                
            {
                try
                {
                    File f = new File((File)o, cname);
                    bcount = (int)f.length();
                    in = new FileInputStream(f);
                    break;
                }
                catch (FileNotFoundException x)
                {
                    // nothing to do
                }
            } 
            else if (o instanceof ZipFile)
            {
                try
                {
                    ZipFile  zf = (ZipFile)o;
                    ZipEntry ze = zf.getEntry(dname);
                    if (ze != null)
                    {
                        in = zf.getInputStream(ze);
                        bcount = (int)ze.getSize();
                        break;
                    }
                }
                catch (IOException x)
                {
                    x.printStackTrace();
                }
            }
        }

        if (in == null)
            return findSystemClass(name); // will throw ClassNotFoundException

        byte data[] = new byte[bcount];

        try 
        {
            for (int total = 0; total < data.length;) 
                total += in.read(data, total, data.length - total);
        }
        catch (IOException x)
        {
            x.printStackTrace();
            data = null;
        }

        try 
        {
            in.close();
            in = null;
        }
        catch (IOException x)
        {
            x.printStackTrace();
            in = null;
        }

        if (data == null)
            throw new ClassNotFoundException(name);

        c = defineClass(name, data, 0, data.length);
        classes.put(name, c);           
        //System.out.println("PathClassLoader put:"+name+" "+c.hashCode());			

        return c;
    }
}
