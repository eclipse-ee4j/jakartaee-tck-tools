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



class PairComp extends PairCommon
{

    boolean visible (XClassMember m)
    {
        return XModifier.isPublic(m.modifiers);
    }

}



public class CheckComp extends CheckCommon implements ClassProcessor
{
    boolean source = true;

    //  API to compare :
    API baseapi,
        testapi;

    // "Members" property of the corresponing API :        
    String basemembers,
           testmembers;

    ClassFilter cfilter;
    Set undefs;    

    // Classes to compare :
    XClass baseclass,  
           testclass;

           
    CheckComp (String mode)
    {
        source = mode.equals("src");
    }
    
    
    boolean compare (API base, API test, ClassFilter cf)
    {
    //  Initialization
    
        baseapi = base;
        testapi = test;
        cfilter = cf;
        undefs = new HashSet();
        super.open();

        // Filtered, Inherited, All, null
        basemembers = (String)baseapi.getProp("Members", "");
        testmembers = (String)testapi.getProp("Members", "");
        
    //  Main loop
    
        showProps("Base:", baseapi.getProps());            
        showProps("Test:", testapi.getProps());            
        out.println();
        
        String s = (cf == null) ? "*all*" : "'"+cf.toString()+"'";
        out.println("- Two-way " + (source ? "source" : "binary") + " check of " + s + " -");
        
        base.rewind();
        
        XClass bclass, tclass;
        String classname;
        while ((bclass = base.getXClass()) != null) {
            classname = bclass.getFullName();
            if (cfilter.inPath(classname)) 
                if ((tclass = test.getXClass(classname)) != null)
                    compareClasses(bclass, tclass);
                else
                    deleted(bclass);
                    
            bclass = null;
            tclass = null;
            classname = null;
        }
        
    //  Check for extra classes
        
        ClassIterator ci = test.getClassIterator();
        if (ci != null)  {
            ci.iterate(this, cf);
        }
        else
            out.println("WARNIMG: extra classes are not checked");
            
    //  Check for undefined classes
            
        if (undefs.size() != 0) {
            out.println();
            out.println("WARNIMG: the following superclasses were not found:");
            Utils.printSorted(out, undefs);
            out.println();
            //errs++; ???
        }
        
    //  Cleanup
    
        super.close();
        cfilter = null;
        testapi = null;
        baseapi = null;
        
        if (errs == 0)
            out.println("\n- Passed, No differences found -");
        else
            out.println("\n- Failed, Differences found : " + errs + " -");
            
        return errs == 0;
    }
    
    
    public void process (String fqn, ClassData cd)
    {
        if (fqn.indexOf('$') == -1 && baseapi.getXClass(fqn) == null) {
            XClass xclass = testapi.getXClass(fqn);
            if (xclass == null)
                System.out.println("? Class not found: " + fqn);
            else if (XModifier.access(xclass.modifiers) >= 2) // public+protected
                added(xclass);
        }
    }


    //  Compares outer or inner classes.
    //
    void compare (XClass base, XClass test)
    {
        if (!XModifier.isPublic(base.modifiers)  && !XModifier.isPublic(test.modifiers))
            return;
    
        pushHead((!base.isInterface() ? "class '" : "interface '") + base.getFullName()+"'");
        
        baseclass = base;
        testclass = test;
        
    //  Collect inherited members

        XClass basetmp = base,
               testtmp = test;

        if (!basemembers.equals("Inherited")) {
            basetmp = getInherited(baseapi, base);
            if (base.ctors != null)
                basetmp.ctors = (XNodes)base.ctors.clone();
        }

        if (!testmembers.equals("Inherited")) {
            testtmp = getInherited(testapi, test);
            if (test.ctors != null)
                testtmp.ctors = (XNodes)test.ctors.clone();
        }

        //	Compare modifiers
            
        testMods("", 
                 base.modifiers, 
                 test.modifiers, 
                 XModifier.xinterface | XModifier.xabstract | XModifier.xstatic | XModifier.xfinal);

        //	Compare superclass

        final String bs = base.extend == null ? "" : base.extend, 
                     ts = test.extend == null ? "" : test.extend;

        if (!bs.equals(ts))
            report("superclass changed:",
                   "from '" + bs + "' to '" + ts + "'");

        //	Compare interfaces
			
        if (!compareTypes(basetmp.implement, testtmp.implement))
            report("superinterfaces changed:",
                   "from '" + Utils.list(basetmp.implement) +
                   "' to '" + Utils.list(testtmp.implement) + "'");

        //	Compare class members
        
        compareMembers(basetmp, testtmp, new PairComp());
        
        testtmp = null;
        basetmp = null;
        
        testclass = null;
        baseclass = null;

        popHead();
    }
    
    
    void deleted (XClass base)
    {
        if (XModifier.isPublic(base.modifiers)) {
            String s = stackEmpty() ? base.getFullName() : base.name;
            report((!base.isInterface() ? "class" : "interface") + " deleted:", 
                   "'" + s + "'");
        }
    }
	

    void added (XClass test)
    {
        if (XModifier.isPublic(test.modifiers)) {
            String s = stackEmpty() ? test.getFullName() : test.name;
            report((!test.isInterface() ? "class" : "interface") + " added:", 
                   "'" + s + "'");
        }
    }
    

//  Constructors
	

    void compare (XClassCtor base, XClassCtor test)
    {
        testMods ("constructor '" + base + "' ", 
                  base.modifiers, 
                  test.modifiers, 
                  0); 

        if (source)
            if (!compareTypes(base.xthrows, test.xthrows))
	    		report("throws changed:",
                       "constructor '" + base +
                            "' from '" + Utils.list(base.xthrows) +
                              "' to '" + Utils.list(test.xthrows) + "'");
    }


    void deleted (XClassCtor base)
    {
        report("constructor deleted:", "'" + base + "'");
    }
	
	
    void added (XClassCtor test)
    {
        report("constructor added:", "'" + test + "'");
    }


//  Methods
	
	
    void compare (XClassMethod base, XClassMethod test)
    {
        testMods ("method '" + name(base) + "' ", 
                  base.modifiers, 
                  test.modifiers, 
                  XModifier.xabstract | XModifier.xstatic | XModifier.xfinal); 

        if (!base.type.equals(test.type))
			report("type changed:",
                   "method '" + name(base) + 
                   "' from '" + base.type + 
                     "' to '" + test.type + "'");

        if (source)
            if (!compareTypes(base.xthrows, test.xthrows))
	    		report("throws changed:",
                       "method '" + name(base) +
                       "' from '" + Utils.list(base.xthrows) +
                         "' to '" + Utils.list(test.xthrows) + "'");
    }
	
	
    void deleted (XClassMethod base)
    {
        report("method deleted:", "'" + name(base) + "'");
    }
	
	
    void added (XClassMethod test)
    {
        report("method added:", "'" + name(test) + "'");
    }


//  Fields


    void compare (XClassField base, XClassField test)
    {
        final int checked = (source) 
                          ? XModifier.xstatic | XModifier.xfinal | XModifier.xvolatile
                          : XModifier.xstatic | XModifier.xfinal;
        testMods ("field '" + name(base) + "' ", 
                  base.modifiers, 
                  test.modifiers, 
                  checked); 

        if (!base.type.equals(test.type))
            report("type changed:",
                   "field '" + name(base) +
                  "' from '" + base.type +
                    "' to '" + test.type + "'");
    }


    void deleted (XClassField base)
    {
        report("field deleted:", "'" + name(base) + "'");
    }
	
	
    void added (XClassField test)
    {
        report("field added:",   "'" + name(test) + "'");
    }


//  Inner classes
	

    void xcompare (XClass base, XClass test)
    {
        if (base.home == baseclass && test.home == testclass) {
            XClass bclass = baseclass,
                   tclass = testclass;
               
            compare(base, test); // use method for outer classes
        
            testclass = tclass;
            baseclass = bclass;
        }
        
        else  {
            testMods ("inner class '"+ name(base) + "' ", 
                      base.modifiers, 
                      test.modifiers, 
       XModifier.xinterface | XModifier.xabstract | XModifier.xstatic | XModifier.xfinal);
        
            if (base.home == baseclass)
                xdeleted(base);
                
            if (test.home == testclass)
                xadded(test);
        }
    }

    
    void xdeleted (XClass base)
    {
        report("inner class deleted:", "'" + name(base) + "'");
    }
	

    void xadded (XClass test)
    {
        report("inner class added:", "'" + name(test) + "'");
    }
    
    
//  Utility  


    //  Collects all declared plus inherited members for the specified class 
    //  in the temporary XClass structure and returns it.
    //
    XClass getInherited (API api, XClass xclass)
    {
        XClass tmp = new XClass();  // create temporary structure to hold inherited members
        
    //  start with declared members
    
        tmp.methods =  (xclass.methods == null) ? 
                        new XNodes() : (XNodes)xclass.methods.clone();
                        
        tmp.fields  =  (xclass.fields  == null) ? 
                        new XNodes() : (XNodes)xclass.fields.clone();
                        
        tmp.inners  =  (xclass.inners  == null) ? 
                        new XNodes() : (XNodes)xclass.inners.clone();
                        
        tmp.implement = (xclass.implement == null) ? 
                        new String[0] : (String[])xclass.implement.clone();
        
        XClass xsuper;
        
    //  inherit from the superclass first
        
        if (xclass.extend != null && (xsuper = findSuper(api, xclass.extend)) != null)
            mixMembers(xclass, tmp, getInherited(api, xsuper));
            
    //  inherit from the superinterfaces second
            
        if (xclass.implement != null)
            for (int i = 0; i < xclass.implement.length; i++)
                if ((xsuper = findSuper(api, xclass.implement[i])) != null)
                    mixMembers(xclass, tmp, getInherited(api, xsuper));
            
        return tmp;        
    }
    
    
    static void mixMembers (XClass xclass, XClass tmp, XClass xsuper)
    {
        for (int i = 0; i < xsuper.implement.length; i++)
            mixInterface(tmp, xsuper.implement[i]);
    
        for (Iterator it = xsuper.methods.iterator(); it.hasNext();) 
            mixMember(xclass, tmp.methods, (XClassMember)it.next());
            
        for (Iterator it = xsuper.fields.iterator(); it.hasNext();) 
            mixMember(xclass, tmp.fields, (XClassMember)it.next());
            
        for (Iterator it = xsuper.inners.iterator(); it.hasNext();) 
            mixMember(xclass, tmp.inners, (XClassMember)it.next());
    }
    
    
    //  Adds the specified interface name ('fqn') to the list of 'tmp' superinterfaces
    //  if it is not here already.
    //
    static void mixInterface (XClass tmp, String fqn)
    {
        for (int i = 0; i < tmp.implement.length; i++)
            if (tmp.implement[i].equals(fqn))
                return;
                
        String[] xxx = new String[tmp.implement.length + 1];
        for (int i = 0; i < tmp.implement.length; i++)
            xxx[i] = tmp.implement[i];
            
        xxx[tmp.implement.length] = fqn;
        tmp.implement = xxx;
    }
    
    
    static void mixMember (XClass xclass, XNodes members, XClassMember x)
    {
        if (!xclass.isAccessible(x))
            return;
      
    //  Declared members should be first in the 'members'
      
        for (Iterator it = members.iterator(); it.hasNext();) {
            XClassMember y = (XClassMember)it.next();
            
        //  Is this member already inherited ?
            if (y == x)
                return;

            if (!y.getSignature().equals(x.getSignature())) 
                continue;
                
            if (y.home == xclass)
                return;
        }
        
        members.add(x);
    }
    
    
    //  This is a central point of the 'limited check' algorithm.
    //  If the required class is from the 'base' api (i.e. from the sigfile) and
    //  its superclass (superinterface) is not belong to the tested package,
    //  then superclass should be looked for in the 'test' api.
    //
    XClass findSuper (API api, String fqn)
    {
        if (fqn == null) 
            return null;
    
        //XClass xsuper = api.getXClass(fqn);
        //if (xsuper == null && api == baseapi && !cfilter.inPath(fqn)) 
        //    xsuper = testapi.getXClass(fqn);

        XClass xsuper;
        if (api == baseapi && !cfilter.inPath(fqn)) 
            xsuper = testapi.getXClass(fqn);
        else
            xsuper = api.getXClass(fqn);

        if (xsuper == null) 
            undefs.add(fqn);
            
        return xsuper;
    }
    
    
//  Utility  


    String name (XClassMember x) 
    {
        if (x.home == baseclass || x.home == testclass)
            return x.getSignature();
        else
            return x.home.getFullName() + "." + x.getSignature();
    }

}

