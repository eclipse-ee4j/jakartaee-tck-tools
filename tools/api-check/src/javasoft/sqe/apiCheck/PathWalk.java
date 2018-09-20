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
 *      This module traverses the given classpath and find all class files.
 */


package javasoft.sqe.apiCheck;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;



interface ReadClasses 
{
	boolean ReadPath (API api, String classpath, PackageSet packs);

}



interface ClassProcessor
{
    void ProcessClass (String pack, String name, Object src, Object sub);
}



class PackageSet
{
	VectorString indepth = new VectorString(),
		         exdepth = new VectorString(), 
		         inplain = new VectorString(), 
		         explain = new VectorString();


    PackageSet ()
    {
    }


    PackageSet (String inpacks, String expacks)
    {
        String sep = File.pathSeparator;

        if (inpacks != null)
        {
            char op = '+';
            StringTokenizer st = new StringTokenizer(inpacks, sep+" +-", true);
            while (st.hasMoreElements())
            {
                String s = (String)st.nextElement();
                if (s.equals(sep) || s.equals(" ") || s.equals("+"))
                    op = '+';
                else if (s.equals("-"))
                    op = '-';
                else
                {
                    if (op == '+') {
			//Add(s);
			Add(s, true);
		    } else {
                        //Sub(s);
			Sub(s, true);
		    }
                }
            }
        }

        if (expacks != null)
        {
            StringTokenizer st = new StringTokenizer(expacks, sep+" +", false);
            while (st.hasMoreElements())
            {
                String s = (String)st.nextElement();
                //Sub(s);
		Sub(s, true);
            }
        }
    }


    boolean IsEmpty ()
    {
        return indepth.isEmpty() && inplain.isEmpty();
    }
	
    /*
     * Ryan added 8/6/02 to see if the inplain vector does not
     * have its sub-packages checked.
     */
    void Add (String s) {
	Add (s, false);
    }


    void Add (String s, boolean depth)
    {
	if (!s.endsWith("."))
	    s += '.';
	
	if (depth)
	    indepth.addElement(s);
	else
	    inplain.addElement(s);
    }
    
	
    /*
     * Ryan added 8/6/02 to see if the explain vector does not
     * have its sub-packages excluded.
     */
    void Sub (String s) {
	Sub (s, false);
    }


    void Sub (String s, boolean depth)
    {
        if (!s.endsWith("."))
            s += '.';
	
	if (depth)
	    exdepth.addElement(s);
	else
	    explain.addElement(s);
    }
	
	
	boolean Under (String s, VectorString plain, VectorString depth)
	{		
		String x;
		
        if (!s.endsWith("."))
            s += '.';

		for (x = plain.First(); x != null; x = plain.Next())
			if (x.equals(s))
				return true;
		
		for (x = depth.First(); x != null; x = depth.Next())
			if (s.startsWith(x))
				return true;
		
		return false;
	}


	boolean AboveUnder (String s, VectorString plain, VectorString depth)
	{		
        if (s.length() == 0)
            return true;
		
        if (!s.endsWith("."))
            s += '.';

		String x;

		for (x = plain.First(); x != null; x = plain.Next())
			if (x.startsWith(s))
				return true;
		
		for (x = depth.First(); x != null; x = depth.Next())
			if (x.startsWith(s) || s.startsWith(x))
				return true;

		return false;
	}


    boolean OnPath (String s)
    {
        if (Under(s, explain, exdepth))
            return false;

        return inplain.isEmpty() && indepth.isEmpty() ? true : AboveUnder(s, inplain, indepth);
    }


    boolean InPath (String s)
    {
        if (Under(s, explain, exdepth))
            return false;

        return inplain.isEmpty() && indepth.isEmpty() ? true : Under(s, inplain, indepth);
}


    public 
    String toString ()
    {
        String s = "";

        String x;
        for (x = indepth.First(); x != null; x = indepth.Next())
        {
            //s += "+<" + x + ">";
            if (s.length() != 0)
                s += '+';
            s += x.substring(0, x.length()-1);
        }

        for (x = exdepth.First(); x != null; x = exdepth.Next())
        {
            //s += "-<" + x + ">";
            s += "-" + x.substring(0, x.length()-1);
        }

        return s;
    }
}



class PathWalk	
{

	static final int maxdepth = 64;

    PackageSet packages;
    ClassProcessor cproc;
							   
	Vector /*String*/ visited = new Vector();


    PathWalk (PackageSet ps, ClassProcessor cp)
    {
        packages = ps;
        cproc = cp;
    }


    void Close ()
    {
        packages = null;
        cproc = null;
    }
	
	
	boolean Walk (String path)
	{
		if (path.length() == 0) 
            return false;
		
        File f = new File(path);
		if (!f.exists()) 
        {
            //System.err.println("file(directory) not exist \""+path+"\"");
            return true;
        }
			
        if (f.isDirectory())
        {
            WalkDir(0, "", path, "");
            return true;
        }
        else
        {
            ZipFile z;
            try
            {
                z = new ZipFile(path);
            }
            catch (IOException x)
            {
                System.err.println("not a zip file \"" + path + "\"");
                return false;
            }

            for (Enumeration e = z.entries(); e.hasMoreElements();)
                WalkZip(z, (ZipEntry)e.nextElement());

            try
            {
                z.close();
            }
            catch (IOException x)
            {
                // just ignore
            }
            return true;
        }
	}
	
	
	private
	void WalkDir (int depth, String pack, String path, String name)
	{
        //System.err.println("WalkDir depth="+depth+" pack=<"+pack+"> path=<"+path+
        //                   "> name=<"+name+">");

        String s = path+name;
				
        File f = new File(s);
        if (!f.exists()) return;
			
        if (f.isDirectory())
		{
            pack += name;	
            if (pack.length() != 0)
            {
                pack += '.';
                if (!packages.OnPath(pack))
                    return;
            }
				
            path = s;
            if (!path.endsWith(File.separator)) 
                path += File.separatorChar;
				
            if (depth == maxdepth)
			{
                System.err.println("out of stack: directory depth "+depth);
                return;
            }
									
            if (visited.contains(s)) return;
            visited.addElement(s);

            String[] ss = f.list();
            if (ss != null)
                for (int i = 0; i < ss.length; i++)
                    WalkDir(depth+1, pack, path, ss[i]);
        }
        else
		{
            WalkFile(pack, name, f, null);
        }
	}
	
	
	private
	void WalkZip (ZipFile z, ZipEntry ze)
	{
        if (ze.isDirectory())
        {
        }
        else
        {
        //  zip-file uses '/' as separator (not File.separator)

            String name = ze.getName();
            int i = name.lastIndexOf('/');
            if (i > 0)
                WalkFile(name.substring(0, i+1).replace('/', '.'), name.substring(i+1), z, ze);
            else
                WalkFile("", name, z, ze);
		}
	}
	
	
	void WalkFile (String pack, String name, Object src, Object sub)
	{
        //System.err.println("WalkFile pack:<"+pack+"> name:<"+name+">");

        if (pack != null && pack.length() != 0)
        {
            if (!pack.endsWith("."))
              pack += '.';
        }

        if (name.endsWith(".class") && packages.InPath(pack))
        {
            //System.err.println("WalkFile pack:<"+pack+"> name:<"+name+">");
            cproc.ProcessClass(pack, name.substring(0, name.length()-6), src, sub);
        }
	}
}

