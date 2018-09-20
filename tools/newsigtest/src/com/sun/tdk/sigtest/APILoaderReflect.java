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
import java.lang.reflect.*;
import java.util.*;


public class APILoaderReflect extends API 
{
    boolean valuemode  = false;
    String classpath   = null;
    PathClassLoader loader = null;
	
    static String stypes = "-boolean-byte-short-int-long-float-double-char-java.lang.String-";
    
    
    APILoaderReflect (String cp, boolean vm, boolean bootloader) 
    {
    	if (cp != null) {
            if (!bootloader) {
	            loader = new PathClassLoader();
                loader.delegate = true;
	            loader.addPath(cp);
            }
            classpath = cp;
    	}
        
        valuemode = vm;
    }


    //  Implementation of API interface

    public void close ()
    {
    	if (loader != null) {
	        loader.close();
    	    loader = null;
		}
        classpath = null;
    }    
	
    //  Implementation of API interface
    
    public ClassIterator getClassIterator ()
    {
        if (classpath == null)
            return null;
        else
            return new ClassIteratorPath(classpath);
    }
  
    
    //  Implementation of API interface
    //  Sequental-access method not supported

    public XClass getXClass () 
    {		
        throw new UnsupportedOperationException();
    }

    
    //  Implementation of API interface
    //  Sequental-access method not supported

    public void rewind ()
    {		
        throw new UnsupportedOperationException();
    }
    
    
    //  Implementation of API interface
    //  Random-access (by fully qulified name) method
    
    public XClass getXClass (String fqn) 
    {		
        XClass xclass = null;
        
        int idx = fqn.indexOf('$');
        String topfqn = (idx == -1 ) ? fqn : fqn.substring(0, idx);
        
	    Class c = null;
    	try {
            c = Class.forName(topfqn, 
		                      false, 
        		              loader == null ? this.getClass().getClassLoader() 
							                 : loader);
        }
        catch (ClassNotFoundException x) {
            System.err.println("Class not found: " + topfqn);
        }
        catch (Throwable x) {
            System.err.println("Failed to load class: " + topfqn);
            System.err.println(x.getMessage());
        }
		
/***        
        try {
	        if (c != null) {
            
                //for (Class cc; (cc = c.getDeclaringClass()) != null;) 
                //    c = cc;
                    
                for (;;) {
                    Class cc = null;
                    try {
                        cc = c.getDeclaringClass();
                    }
                    catch (Throwable e) {
                    }
                    
                    if (cc == null)
                        break;
                        
                    c = cc;
                }
                    
                XClass x = make(c);
                x.packname = Utils.getPackClassName(c.getName(), x.name);
                
                xclass = x.findInner(fqn);
                if (xclass == null) {
                    System.err.println("Problem with inner class: " + fqn);
                    //debug.Show.print("x", x);
                }
            }
        }
***/        
        try {
            if (c != null) {
                XClass x = make(c);
                x.packname = Utils.getPackClassName(c.getName(), x.name);
                
                if (idx == -1)
                    xclass = x;
                else {
                    xclass = x.findInner(fqn);
                    if (xclass == null) 
                        System.err.println("Problem with inner class: " + fqn);
                }
                    
            }
        }
        
        catch (Throwable x) {
            x.printStackTrace();
        }     
        
        //System.out.println("REFLECT " + fqn + (xclass == null ? " -MISSING" : ""));        
        return xclass;
    }

	
    
    XClass make (Class c) 
    {
		XClass xclass = newXClass();
		xclass.defined = true;
		
		xclass.name = Utils.getSimpleClassName(c.getName());
        xclass.modifiers = getModifiers(c.getModifiers(), 
                                        xclass.home == null ? XModifier.flagclass 
                                                            : XModifier.flaginner);

	//  If superclass and/or superinterfaces aren't available, wee proceed.
        try {
            Class xsuper = c.getSuperclass();
            if (xsuper != null)
				xclass.extend = xsuper.getName();
        }
        catch (Throwable x) {
            System.err.println("Failed to get superclass for "+xclass.getFullName());
            System.err.println(x.getMessage());
        }

        try {
            Class[] interfs = c.getInterfaces();
		    xclass.implement = getTypes(interfs);
        }
        catch (Throwable x) {
            System.err.println("Failed to get superinterfaces for "+xclass.getFullName());
            System.err.println(x.getMessage());
        }
		
	// make constructors
	
        Constructor[] ctors = null;
        try {
            ctors = c.getDeclaredConstructors();
        }
        catch (Throwable x) {
            System.err.println("Failed to get constructors for " + c.getName());
            System.err.println(x.getMessage());
        }

        if (ctors != null)
    		for (int i = 0; i < ctors.length; i++) {
	    		XClassCtor xctor = newXClassCtor();
		    	xctor.name = xclass.name;
                xctor.modifiers = getModifiers(ctors[i].getModifiers(), 
                                              XModifier.flagmethod);
                xctor.args      = getTypes(ctors[i].getParameterTypes());
                xctor.xthrows   = getTypes(ctors[i].getExceptionTypes());
                xctor.link(xclass);
            }

	// make methods
	
        Method[] methods = null;
        try {
            methods = c.getDeclaredMethods();
        }
        catch (Throwable x) {
            System.err.println("Failed to get methods for " + c.getName());
            System.err.println(x.getMessage());
        }

        if (methods != null)
		    for (int i = 0; i < methods.length; i++) 
		        if (methods[i].getDeclaringClass() == c) {
				    Method m = methods[i]; // shorthand
				    XClassMethod xmethod = newXClassMethod();
				    xmethod.name = m.getName();
                    xmethod.modifiers = getModifiers(m.getModifiers(), XModifier.flagmethod);
                    xmethod.type      = getType(m.getReturnType());
                    xmethod.args      = getTypes(m.getParameterTypes());
                    xmethod.xthrows   = getTypes(m.getExceptionTypes());
                    xmethod.link(xclass);
                }

	// make fields
	
        Field[] fields = null;
        try {
            fields = c.getDeclaredFields();
        }
        catch (Throwable x) {
            System.err.println("Failed to get fields for " + c.getName());
            System.err.println(x.getMessage());
        }

        if (fields != null)
		    for (int i = 0; i < fields.length; i++) 
                if (fields[i].getDeclaringClass() == c) {
                    Field f = fields[i]; // shorthand
    	            XClassField xfield = newXClassField();
    	            xfield.name = f.getName();
                    xfield.modifiers = getModifiers(f.getModifiers(), XModifier.flagfield);
                    xfield.type      = getType(f.getType());
                    xfield.link(xclass);

                    if (valuemode
                     && (xfield.modifiers & XModifier.xstatic) != 0 
                     && (xfield.modifiers & XModifier.xfinal)  != 0
                     && stypes.indexOf("-"+xfield.type+"-") != -1) {
                            try {
                                f.setAccessible(true);
                                xfield.value = f.get(null);
                            }
                            catch (IllegalAccessException x) {
                                xfield.value = null;
                                //System.err.println(x);
                                //System.err.println("exception field "+xfield.getFullName());
                            }                       
                        }
                    }

	// make inner classes

        Class[] classes = null;
        try {
            classes = c.getDeclaredClasses();
        }
        catch (Throwable x) {
            System.err.println("Failed to get inner classes for " + c.getName());
            System.err.println(x.getMessage());
        }

        if (classes != null)
     		for (int i = 0; i < classes.length; i++) {
                XClass x = make(classes[i]);
                x.link(xclass);
            }
        
        xclass.setDefaults();
        return xclass;
    }


    static int getModifiers (int mod, int allowed) 
    {
        int x = 0;

        StringTokenizer st = new StringTokenizer(Modifier.toString(mod), " ", false);
        while (st.hasMoreElements()) {
             String s = (String)st.nextElement();
             int m = XModifier.convert(s);
             if (m == 0)
                System.err.println("invalid modifier: \"" + s + "\"");
             else
                 x |= m;
        }

        //if ((x & ~allowed) != 0) {
        //    System.err.println("not allowed modifier(s): \""+
        //                       XModifier.toString(x & ~allowed)+"\"");
        //    x &= allowed;
        //}

        return x;
    }


    static String[] getTypes (Class[] classes) 
    {
		String[] names = null;
		
		if (classes != null) {
            names = new String[classes.length];
		    for (int i = 0; i < classes.length; i++)
    	        names[i] = getType(classes[i]);
		}

       	return names;
    }
	
	
	
    static String getType (Class c) 
    {
		return getType(c.getName());
    }
	
	
	
    static String getType (String s) 
    {
		if (!s.startsWith("["))
            return s;
	
		StringBuffer sb = new StringBuffer();
		
		int dims = 0;
		while (s.charAt(dims) == '[') {
			dims++;
			sb.append("[]");
		}
			
		if (s.charAt(dims) == 'L')
			sb.insert(0, s.substring(dims+1, s.length()-1));
		else
			sb.insert(0, Utils.getPrimitive(s.charAt(dims)));

		return sb.toString();	
    }
	
}			   



