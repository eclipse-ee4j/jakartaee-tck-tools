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


public class CheckChanges extends CheckCommon implements ClassProcessor
{
    //  API to compare :
    API baseapi,
        testapi;
        
    ClassFilter cfilter;

    // Outer classes being compared :
    
    
    boolean compare (API base, API test, ClassFilter cf)
    {
    //  Initialization
    
        baseapi = base;
        testapi = test;
        cfilter = cf;
        super.open();
        
    //  Main loop
        
        showProps("Base:", baseapi.getProps());            
        showProps("Test:", testapi.getProps());            
        out.println();
        
        String s = (cf == null) ? "*all*" : "'"+cf.toString()+"'";
        out.println("- Changes check of " + s + " -");
        
        base.rewind();
        
        XClass baseclass,  
               testclass;
        String classname;
        
        while ((baseclass = base.getXClass()) != null) {
            classname = baseclass.getFullName();
            if (cfilter.inPath(classname)) 
                if ((testclass = test.getXClass(classname)) != null)
                    compareClasses(baseclass, testclass);
                else
                    deleted(baseclass);
                    
            baseclass = null;
            testclass = null;
            classname = null;
        }
        
    //  Check for extra classes
        
        ClassIterator ci = test.getClassIterator();
        if (ci != null)  {
            ci.iterate(this, cf);
        }
        else
            out.println("WARNIMG: extra classes were not checked");
            
    //  Cleanup
        
        super.close();
        cfilter = null;
        testapi = null;
        baseapi = null;
        
        if (errs == 0)
            out.println("\n- No differences found -");
        else
            out.println("\n- Differences found : " + errs + " -");
            
        return errs == 0;
    }
    
    
    public void process (String fqn, ClassData cd)
    {
        if (fqn.indexOf('$') == -1 && baseapi.getXClass(fqn) == null) {
            XClass xclass = testapi.getXClass(fqn);
            if (xclass == null)
                System.out.println("? Class not found: " + fqn);
            else 
                added(xclass);
        }
    }


    //  Outer and inner classes 
    //
    void compare (XClass base, XClass test)
    {
        pushHead((!base.isInterface() ? "class '" : "interface '") + base.getFullName()+"'");

        //	Compare modifiers
            
        testMods("", base.modifiers, test.modifiers, 
                 XModifier.xinterface | XModifier.xabstract | XModifier.xstatic | XModifier.xfinal);

        //	Compare superclass

        final String bs = base.extend == null ? "" : base.extend, 
                     ts = test.extend == null ? "" : test.extend;

        if (!bs.equals(ts))
            report("superclass changed:",
                   "from '" + bs + "' to '" + ts + "'");

        //	Compare interfaces
			
        if (!compareTypes(base.implement, test.implement))
            report("superinterfaces changed:",
                   "from '" + Utils.list(base.implement)+
                   "' to '" + Utils.list(test.implement)+"'");

        //	Compare class members
        
        compareMembers(base, test, new PairCommon());

        popHead();
    }
    
    
    void deleted (XClass x)
    {
        String s = stackEmpty() ? x.getFullName() : x.name;
        report((!x.isInterface() ? "class" : "interface") + " deleted:", 
               "'" + s + "'");
    }
	

    void added (XClass x)
    {
        String s = stackEmpty() ? x.getFullName() : x.name;
        report((!x.isInterface() ? "class" : "interface") + " added:", 
               "'" + s + "'");
    }
    

//  Constructors
	

    void compare (XClassCtor base, XClassCtor test)
    {
        testMods ("constructor '" + base + "' ", base.modifiers, test.modifiers, 0); 

        if (!compareTypes(base.xthrows, test.xthrows))
			report("throws changed:",
                   "constructor '" + base +
                        "' from '" + Utils.list(base.xthrows) +
                          "' to '" + Utils.list(test.xthrows) + "'");
    }


    void deleted (XClassCtor x)
    {
        report("constructor deleted:", "'" + x + "'");
    }
	
	
    void added (XClassCtor x)
    {
        report("constructor added:", "'" + x + "'");
    }


//  Methods
	
	
    void compare (XClassMethod base, XClassMethod test)
    {
        testMods ("method '" + base + "' ", 
                  base.modifiers, 
                  test.modifiers, 
                  XModifier.xabstract | XModifier.xstatic | XModifier.xfinal); 

        if (!base.type.equals(test.type))
			report("type changed:",
                   "method '" + base +
                   "' from '" + base.type +
                     "' to '" + test.type + "'");

        if (!compareTypes(base.xthrows, test.xthrows))
			report("throws changed:",
                   "method '" + base +
                   "' from '" + Utils.list(base.xthrows) +
                     "' to '" + Utils.list(test.xthrows) + "'");
    }
	
	
    void deleted (XClassMethod x)
    {
        report("method deleted:", "'" + x + "'");
    }
	
	
    void added (XClassMethod x)
    {
        report("method added:", "'" + x + "'");
    }


//  Fields


    void compare (XClassField base, XClassField test)
    {
        testMods ("field '" + base + "' ", 
                  base.modifiers, 
                  test.modifiers, 
                  XModifier.xstatic | XModifier.xfinal | XModifier.xvolatile); 

        if (!base.type.equals(test.type))
            report("type changed:",
                   "field '" + base +
                  "' from '" + base.type +
                    "' to '" + test.type + "'");
    }


    void deleted (XClassField x)
    {
        report("field deleted:", "'" + x + "'");
    }
	
	
    void added (XClassField x)
    {
        report("field added:",   "'" + x + "'");
    }


//  Inner classes
	

    void xcompare (XClass base, XClass test)
    {
        compare(base, test); // use method for outer classes
    }


    void xdeleted (XClass x)
    {
        report("inner class deleted:", "'" + x + "'");
    }
	

    void xadded (XClass x)
    {
        report("inner class added:", "'" + x + "'");
    }

}

