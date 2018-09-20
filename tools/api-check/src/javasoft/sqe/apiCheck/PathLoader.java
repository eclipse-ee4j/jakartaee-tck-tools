/*
 * Copyright (c) 2000, 2018 Oracle and/or its affiliates. All rights reserved.
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
 *      This is ClassLoader for loading classes from a given classpath.
 */


package javasoft.sqe.apiCheck;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;



class PathLoader extends ClassLoader
{
	
    Vector /*File*/ dirs = new Vector();
    Hashtable /*String,Class*/ classes = new Hashtable();
    

    boolean OpenPath (String cp) 
    {
        int n = dirs.size();
		StringTokenizer st = new StringTokenizer(cp, 
                                                 File.pathSeparator,
                                                 false); 
		
        Vector tmp = new Vector();
		while (st.hasMoreElements())
		{
			String s = (String)st.nextElement();
            if (!tmp.contains(s))
            {
                tmp.addElement(s);

                File f = new File(s);

                if (f.isDirectory())
                {
                    dirs.addElement(f);
                }
                else if (f.isFile())
                {
                    ZipFile z;
                    try
                    {
                        z = new ZipFile(f);
                        dirs.addElement(z);
                    }
                    catch (IOException x)
                    {
                    }
                }
            }
		}

        return dirs.size() != n;
    }

   
    boolean OpenFile (File f) 
    {
        ZipFile z = null;
        try
        {
            z = new ZipFile(f);
            dirs.addElement(z);
        }
        catch (IOException x)
        {
        }

        return z != null;
    }

   
    void Close ()
    {
        for (int i = 0; i < dirs.size(); i++)
        {
            Object o = dirs.elementAt(i);
            if (o instanceof ZipFile)
            {
                try                   
                {
                    ((ZipFile)o).close();
                }
                catch (IOException x)
                {
                }
            }
            else
            {
            }
        }
        dirs.clear();

        classes.clear();
    }


    protected 
    Class loadClass (String name, boolean resolve) throws ClassNotFoundException 
    {
        //System.err.println("PathLoader.loadClass:"+name+" "+resolve);			

        Class c = (Class)classes.get(name); 
        
        if (c == null) 
            c = locateClass(name);
	
        if (resolve)
            resolveClass(c);
	
        return c;
    }


    private synchronized 
    Class locateClass(String name)	throws ClassNotFoundException 
    {
        if (name.startsWith("java."))
            return findSystemClass(name);   

        Class c = Class.forName(name);
	return c;

	/********
        Class c = null;
        String cname = name.replace('.', File.separatorChar) + ".class", //
               dname = name.replace('.', '/') + ".class";                // for zip-file
	    
        InputStream in = null;
        int bcount = 0;

        for (int i = 0; i < dirs.size(); i++)
        {
            Object o = dirs.elementAt(i); 
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
            return findSystemClass(name); // wil throw ClassNotFoundException

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
	****/
    }
}
