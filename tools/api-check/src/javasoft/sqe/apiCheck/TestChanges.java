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
 *      This module compares to sets of classes (base and test), represented by
 *      XProg structures.
 */


package javasoft.sqe.apiCheck;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;



class TestChanges extends TestBase
{

//  Class


    void Compare (XClass base, XClass test)
    {
        PushHead((!base.IsInterface() ? "class" : "interface") + " \""+base.FullName()+"\"");

        boolean changed = false;

	//	Compare modifiers

        final
        int mb = base.modifier & ~XModifier.xstrictfp,
            mt = test.modifier & ~XModifier.xstrictfp;

		if (mb != mt)
        {
            changed = true;
            TestBits("", mb, mt, 0);
        }

	//	Compare superclass

        String s1 = base.extend == null ? "" : base.extend.FullName(), 
               s2 = test.extend == null ? "" : test.extend.FullName();

        if (!s1.equals(s2))
        {
            changed = true;
            Report("superclass changed:",
                   "from \""+s1+"\" to \""+s2+"\"");
        }

	//	Compare interfaces
			
        if (!CompareClasses(base.implement, test.implement))
        {
            changed = true;
			Report("superinterfaces changed:",
                   "from \"" +ClassesToString(base.implement)+
                   "\" to \""+ClassesToString(test.implement)+"\"");
        }

        if (changed)
            upd.Change(base, test);

	//	Compare class members

        CompareMembers(base, test);
		
        PopHead();
    }


    void Deleted (XClass x)
    {
        Report((!x.IsInterface() ? "class" : "interface") + " deleted:", "\""+x+"\"");
        upd.Delete(x);
    }
	

    void Added (XClass x)
    {
        Report((!x.IsInterface() ? "class" : "interface") + " added:", "\""+x+"\"");
        upd.Add(x);
    }


//  Constructor
	

    void Compare (XClassConstructor base, XClassConstructor test)
    {
        boolean changed = false;

		if (base.modifier != test.modifier)
        {
            changed = true;
            TestBits ("constructor \""+base+"\" ", base.modifier, test.modifier, 0); 
        }

	// normalize the two throws clauses so neither one contains any runtime exceptions.
	// then compare them, they must be equal else the test will fail.
	removeRuntimeExceptions(base.xthrows);
	removeRuntimeExceptions(test.xthrows);
        if (!CompareTypes(base.xthrows, test.xthrows))
        {
            changed = true;
			Report(0,  
                   "throws changed:",
                   "constructor \""+base+"\" from \""+base.xthrows+"\" to \""+test.xthrows+"\"");
        }

        if (changed)
            upd.Change(base,test);
    }


    void Deleted (XClassConstructor x)
    {
        Report("constructor deleted:", "\""+x+"\"");
        upd.Delete(x);
    }
	
	
    void Added (XClassConstructor x)
    {
        Report("constructor added:", "\""+x+"\"");
        upd.Add(x);
    }


//  Method
	
	
    void Compare (XClassMethod base, XClassMethod test)
    {
        boolean changed = false;

	int mask = XModifier.xstrictfp | XModifier.xsynchronized | XModifier.xnative;
	
        final
	    int mb = base.modifier & ~mask,
                mt = test.modifier & ~mask;

		if (mb != mt)
        {
            changed = true;
            TestBits("method \""+base+"\" ", mb, mt, 0);
        }

        if (!base.type.equals(test.type))
        {
            changed = true;
			Report("type changed:",
                   "method \""+base+"\" from \""+base.type+"\" to \""+test.type+"\"");
        }

	// normalize the two throws clauses so neither one contains any runtime exceptions.
	// then compare them, they must be equal else the test will fail.
	removeRuntimeExceptions(base.xthrows);
	removeRuntimeExceptions(test.xthrows);
        if (!CompareTypes(base.xthrows, test.xthrows))
        {
            changed = true;
			Report("throws changed:",
                   "method \""+base+"\" from \""+base.xthrows+"\" to \""+test.xthrows+"\"");
        }

        if (changed)
            upd.Change(base, test);
    }

    private void removeRuntimeExceptions(XTypes exs) {
	int numExs = (exs == null) ? 0 : exs.size();
	if (numExs == 0) return;
        Vector temp = (Vector)exs.clone();
	for (int i = 0; i < numExs; i++) {
	    XType ex = (XType)temp.get(i);
	    //System.err.println("$$$$$$ Checking exception \"" + ex + "\"");
	    if (exIsRuntimeException(ex)) {
		//System.err.println("$$$$$$ Found runtime exception \"" + ex + "\" removing");
		exs.remove(ex);
	    }
	}
    }	
	
    private Class runtimeExClass = java.lang.RuntimeException.class;

    private boolean exIsRuntimeException(XType ex) {
	boolean result = false;
	try {
	    Class clazz = Class.forName(ex.toString());
	    result = runtimeExClass.isAssignableFrom(clazz);
	} catch (Exception e) {
	    System.err.println("$$$$$ Could not create class \"" + ex.toString() + "\"");
	}
	return result;
    }

    void Deleted (XClassMethod x)
    {
        Report("method deleted:", "\""+x+"\"");
        upd.Delete(x);
    }
	
	
    void Added (XClassMethod x)
    {
        Report("method added:", "\""+x+"\"");
        upd.Add(x);
    }


//  Field


    void Compare (XClassField base, XClassField test)
    {
        boolean changed = false;
	int mask = XModifier.xvolatile | XModifier.xtransient;
	
        final
	    int mb = base.modifier & ~mask,
                mt = test.modifier & ~mask;

		if (mb != mt)
        {
            changed = true;
            TestBits("field \""+base+"\" ", mb, mt, 0);
        }

        if (!base.type.equals(test.type))
        {
            changed = true;
            Report("type changed:",
                   "field \""+base+"\" from \""+base.type+"\" to \""+test.type+"\"");
        }

        if (values)
            if (base.value != null || test.value != null)
            {
                if (base.value == null)
                    Report("value added:",
                           "field \""+base+"\"");

                else if (test.value == null)
                    Report("value deleted:",
                           "field \""+base+"\"");

                else if (!base.value.equals(test.value))
                    Report("value changed:",
                           "field \""+base+"\" from "+WriteJh.Value(base.value)+
                                               " to "+WriteJh.Value(test.value));
            }

        if (changed)
            upd.Change(base, test);
    }


    void Deleted (XClassField x)
    {
        Report("field deleted:", "\""+x+"\"");
        upd.Delete(x);
    }
	
	
    void Added (XClassField x)
    {
        Report("field added:", "\""+x+"\"");
        upd.Add(x);
    }


    void Report (String k, String s)
    {
        super.Report(0, k, s);
    }
}



