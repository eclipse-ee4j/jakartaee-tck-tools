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
 *      This module builds class description by reflection (java.lang.reflection).      
 *
 */


package javasoft.sqe.apiCheck;

import java.io.File;
import java.lang.reflect.Modifier;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;



class ReadReflect implements ReadClasses
{
	XProg prog;
	PathLoader loader;
    boolean valuemode;
    static String stypes = "-boolean-byte-short-int-long-float-double-char-java.lang.String-";

	

    ReadReflect (boolean v)
    {       
        valuemode = v;
    }


    public
	boolean ReadPath (API api, String classpath, PackageSet packs)
	{
        long t0 = Main.GetTimer();

		api.xprog = prog = new XProg();

        api.props.Add("date", new Date().toString());

        if (classpath == null || classpath.length() == 0)
        {
            classpath = System.getProperty("java.class.path");
            loader = new PathLoader();
            loader.OpenPath(classpath);
	    System.err.println("&&&& No classpath specified using system property java.class.path");
        }
        else if (classpath.equals("$") || classpath.equals("@"))
        {
            classpath = System.getProperty("sun.boot.class.path");
            loader = null;
            api.props.Add("version", System.getProperty("java.version")); 
	    System.err.println("&&&& No classpath specified using system property sun.boot.class.path");
        }
        else
        {
        /*
            if (!packs.IsEmpty() && packs.OnPath("java"))
            {
                System.err.println("java.* classes can be loaded only by system class loader"+
                                   " (use $)");
                return false;
            }
        */

            loader = new PathLoader();
            loader.OpenPath(classpath);
	    System.err.println("&&&& Classpath specified by user");
        }

	System.err.println("&&&& Searching for package members in: \"" + classpath + "\"");

        api.props.Add("classpath", classpath);
        //prog.props.Add("reflect", null);

        if (!packs.IsEmpty())
            api.props.Add("package",  packs.toString());

        PathWalk path = new PathWalk(packs, new Processor());
		
		StringTokenizer st = new StringTokenizer(classpath, 
                                                 File.pathSeparator,
                                                 false); 
        
        int errs = 0;
        
        while (st.hasMoreElements())
            if (!path.Walk((String)st.nextElement()))
                errs++;
		
		path.Close();	
        loader = null;
        prog = null;

        Main.PrintTimer("ReadPath "+classpath, t0);
        return errs == 0;
	}


    class Processor implements ClassProcessor
    {
        Class  c;
        XClass xclass;


		public void ProcessClass (String pack, String name, Object src, Object sub)
		{		
            String s = (pack == null) ? name : pack+name;

			try
			{
			    //c = Class.forName(s, false, loader);
                   /*
                    * Instead of calling forName passing in the specified classloader, loader
                    * is an instance of PathLoader, we are going to simply call Class.forName.
                    * The thought here is that the classes under test should exist in the environment
                    * since the sig tests are running in the 4 J2EE containers.  So forName should
                    * succeed.  This increases the power of the sig tests by verifying that the 
                    * expected classes are actually accessible by code running in the containers.
                    * API Check previously used the VM classpath, or the boot classpath or a specified
                    * path to load the classes from.  Sice we don't know how classes are made available to
                    * the containers it seems like forName is the only way to load the classes in
                    * an implementation independent manner (Should be no different than saying
                    * new TestClass()).
                    */
		  //System.err.println("%%%%%%%%%%%%%%% Loading Class \"" + s + "\"");
                    c = Class.forName(s);
		  //System.err.println("%%%%%%%%%%%%%%% Class Loaded \"" + s + "\"");
            }
            catch (Throwable x)
            {
                System.err.println("Failed to load class "+s);
                System.err.println(x.getMessage());
		x.printStackTrace(System.out);
		x.printStackTrace(System.err);
            }

            try
            {
                if (c != null)
                //  class was succefully loaded, create XClass structure
                    ProcessClass();
                else
                //  class was not loaded by Class.forName, create an empty XClass structure
                    prog.DefineClass(s);
            }
            catch (Throwable x)
            {
                x.printStackTrace();
            }           

            c = null;
            xclass = null;
		}

			
		void ProcessClass ()
		{
            xclass = prog.DefineClass(c.getName());
            if (xclass.defined)
            {
                System.err.println("Duplicate class \"" + xclass.FullName() + "\" - ignored");
                return;
            }

            xclass.defined  = true;
            xclass.modifier = GetModifiers(c.getModifiers(), 
                                           xclass.home == null ? XModifier.flagclass 
                                                               : XModifier.flaginner);

            Class xsuper;
            try
            {
                xsuper = c.getSuperclass();
            }
            catch (Throwable x)
            {
                System.err.println("Failed to get superclass for "+xclass.FullName());
                System.err.println(x.getMessage());
                return;
            }
            if (xsuper != null)
                xclass.extend = prog.DefineClass(xsuper.getName());

            Class[] interfs;
            try
            {
                interfs = c.getInterfaces();
            }
            catch (Throwable x)
            {
                System.err.println("Failed to get interfaces for "+xclass.FullName());
                System.err.println(x.getMessage());
                return;
            }
            if (interfs != null)
                for (int i = 0; i < interfs.length; i++)
                    xclass.implement.Add(prog.DefineClass(interfs[i].getName()));

            ProcessClassConstructors();
            ProcessClassMethods();
            ProcessClassFields();			
            ProcessClassInners();			
		}


		void ProcessClassConstructors ()
		{
            Constructor[] constructors;
            try
            {
                constructors = c.getDeclaredConstructors();
            }
            catch (Throwable x)
            {
                System.err.println("Failed to get constructors for "+xclass.FullName());
                System.err.println(x.getMessage());
                return;
            }

			for (int i = 0; i < constructors.length; i++)
			{
				XClassConstructor xconstructor = new XClassConstructor();

                xconstructor.name = constructors[i].getName();
            //  get simple name of the constructor
                int k = xconstructor.name.lastIndexOf('.'),
                    l = xconstructor.name.lastIndexOf('$');
                if (l > k) k = l;
                if (k > 0) xconstructor.name = xconstructor.name.substring(k+1);

                xconstructor.modifier = GetModifiers(constructors[i].getModifiers(), 
                                                     XModifier.flagmethod);
                xconstructor.args     = GetArgs(constructors[i].getParameterTypes());
                xconstructor.xthrows  = GetArgs(constructors[i].getExceptionTypes());
                xconstructor.Link(xclass);
            }
		}


		void ProcessClassMethods ()
		{
            Method[] methods;
            try
            {
                methods = c.getDeclaredMethods();
            }
            catch (Throwable x)
            {
                System.err.println("Failed to get methods for "+xclass.FullName());
                System.err.println(x.getMessage());
                return;
            }

			for (int i = 0; i < methods.length; i++)
			{
				XClassMethod xmethod = new XClassMethod();
                if (methods[i].getDeclaringClass() == c)
                {
                    xmethod.name = methods[i].getName();
                    xmethod.modifier = GetModifiers(methods[i].getModifiers(), XModifier.flagmethod);
                    xmethod.type     = prog.DefineType(methods[i].getReturnType().getName());
                    xmethod.args     = GetArgs(methods[i].getParameterTypes());
                    xmethod.xthrows  = GetArgs(methods[i].getExceptionTypes());
                    xmethod.Link(xclass);
                }
            }
		}


		void ProcessClassFields ()
		{
            Field[] fields;
            try
            {
                fields = c.getDeclaredFields();
            }
            catch (Throwable x)
            {
                System.err.println("Failed to get fields for "+xclass.FullName());
                System.err.println(x.getMessage());
                return;
            }

			for (int i = 0; i < fields.length; i++)
			{
                XClassField xfield = new XClassField();
                if (fields[i].getDeclaringClass() == c)
                {
                    Field f = fields[i];
                    xfield.name = f.getName();
                    xfield.modifier = GetModifiers(f.getModifiers(), XModifier.flagfield);
                    xfield.type = prog.DefineType(f.getType().getName());
                    xfield.Link(xclass);
                    
                    if (valuemode
                     && XModifier.IsAnd(xfield.modifier, XModifier.xstatic | XModifier.xfinal)
                     && stypes.indexOf("-"+xfield.type+"-") != -1)
                    {
                        try
                        {
                            f.setAccessible(true);
                            xfield.value = f.get(null);
                        }
                        catch (IllegalAccessException x)
                        {
                            xfield.value = null;
                            //System.err.println(x);
                            //System.err.println("exception field "+xfield.FullName());
                        }                       
                    }
                }
			}
		}


		void ProcessClassInners ()
		{
            Class[] classes;
            try
            {
                classes = c.getDeclaredClasses();
            }
            catch (Throwable x)
            {
                System.err.println("Failed to get inner classes for "+xclass.FullName());
                System.err.println(x.getMessage());
                return;
            }

			for (int i = 0; i < classes.length; i++)
			{
                XClass xinner = prog.DefineClass(classes[i].getName());
			}
		}


        int GetModifiers (int mod, int allowed)
        {
            int x = 0;

            StringTokenizer st = new StringTokenizer(Modifier.toString(mod), " ", false);
            while (st.hasMoreElements())
                 x |= XModifier.ConvertNew((String)st.nextElement());

            if ((x & ~allowed) != 0)
                System.err.println("not allowed modifier(s): \""+XModifier.toString(x & ~allowed)+"\"");

            return x & allowed;
        }


        XTypes GetArgs (Class[] pars)
        {
            XTypes tt = new XTypes(pars.length);

            for (int i = 0; i < pars.length; i++)
                tt.Add(prog.DefineType(pars[i].getName()));

            return tt;
        }
	}
}



