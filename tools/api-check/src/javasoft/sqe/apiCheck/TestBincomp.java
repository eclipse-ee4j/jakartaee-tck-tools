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
 *      This module tests two-way binary compatibility of sets of classes (base and test), 
 *      represented by XProg structures.
 */


package javasoft.sqe.apiCheck;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;



class TestBincomp extends TestBase
{

//  Class

    void Compare (XClass base, XClass test)
    {
        PushHead((!base.IsInterface() ? "class" : "interface") + " \""+base.FullName()+"\"");

        //	Compare modifiers
            
        final 
        int checked = XModifier.xpublic   | XModifier.xprotected |
                      XModifier.xabstract | XModifier.xstatic    | XModifier.xfinal;

        TestMods("", base.modifier, test.modifier, checked);

        //	Compare superclass

        final
        String bs = base.extend == null ? "" : base.extend.FullName(), 
               ts = test.extend == null ? "" : test.extend.FullName();

        if (!bs.equals(ts))
            Report(CHANGE_OTHER,  
                   "superclass changed:",
                   "from \""+bs+"\" to \""+ts+"\"");

        //	Compare interfaces
			
        if (!CompareClasses(base.implement, test.implement))
            Report(CHANGE_OTHER,  
                   "superinterfaces changed:",
                   "from \"" +ClassesToString(base.implement)+
                   "\" to \""+ClassesToString(test.implement)+"\"");

        //	Compare class members
        
        CompareMembers(base, test);

        PopHead();
    }


    void Deleted (XClass x)
    {
        Report(tos != null && tos.IsBaseInherited(x) ? CHANGE_COMP : CHANGE_SUB, 
               (!x.IsInterface() ? "class" : "interface") + " deleted:", 
               "\""+x+"\"");
    }
	

    void Added (XClass x)
    {
        Report(tos != null && tos.IsTestInherited(x) ? CHANGE_COMP : CHANGE_ADD, 
               (!x.IsInterface() ? "class" : "interface") + " added:", 
               "\""+x+"\"");
    }
  

//  Constructor
	

    void Compare (XClassConstructor base, XClassConstructor test)
    {
        boolean changed = false;

        final 
        int checked = XModifier.xpublic | XModifier.xprotected;

        TestMods ("constructor \""+base+"\" ", base.modifier, test.modifier, checked); 

        if (!CompareTypes(base.xthrows, test.xthrows))
			Report(CHANGE_COMP,  
                   "throws changed:",
                   "constructor \""+base+"\" from \""+base.xthrows+"\" to \""+test.xthrows+"\"");
    }


    void Deleted (XClassConstructor x)
    {
        Report(CHANGE_SUB, "constructor deleted:", "\""+x+"\"");
    }
	
	
    void Added (XClassConstructor x)
    {
        Report(CHANGE_ADD, "constructor added:", "\""+x+"\"");
    }


//  Method
	
	
    void Compare (XClassMethod base, XClassMethod test)
    {

        final 
        int checked = XModifier.xpublic   | XModifier.xprotected | 
                      XModifier.xabstract | XModifier.xstatic    | XModifier.xfinal;

        TestMods ("method \""+base+"\" ", base.modifier, test.modifier, checked); 

        if (!base.type.equals(test.type))
			Report(CHANGE_OTHER,  
                   "type changed:",
                   "method \""+base+"\" from \""+base.type+"\" to \""+test.type+"\"");

        if (!CompareTypes(base.xthrows, test.xthrows))
			Report(CHANGE_COMP,  
                   "throws changed:",
                   "method \""+base+"\" from \""+base.xthrows+"\" to \""+test.xthrows+"\"");
    }
	
	
    void Deleted (XClassMethod x)
    {
        Report(tos.IsBaseInherited(x) ? CHANGE_COMP : CHANGE_SUB, 
               "method deleted:", 
               "\""+x+"\"");
    }
	
	
    void Added (XClassMethod x)
    {
        Report(tos.IsBaseInherited(x) ? CHANGE_COMP : CHANGE_ADD, 
               "method added:", 
               "\""+x+"\"");
    }


//  Field


    void Compare (XClassField base, XClassField test)
    {
        final 
        int checked = XModifier.xpublic | XModifier.xprotected | 
                      XModifier.xstatic | XModifier.xfinal;

        TestMods ("field \""+base+"\" ", base.modifier, test.modifier, checked); 

        if (!base.type.equals(test.type))
            Report(CHANGE_COMP,  
                   "type changed:",
                   "field \""+base+"\" from \""+base.type+"\" to \""+test.type+"\"");
    }


    void Deleted (XClassField x)
    {
        Report(tos.IsBaseInherited(x) ? CHANGE_COMP : CHANGE_SUB, 
               "field deleted:", 
               "\""+x+"\"");
    }
	
	
    void Added (XClassField x)
    {
        Report(tos.IsBaseInherited(x) ? CHANGE_COMP : CHANGE_ADD, 
               "field added:", 
               "\""+x+"\"");
    }


    void TestMods (String pref, int bm, int tm, int check)
    {
        final
        int vb = XModifier.Access(bm),
            vt = XModifier.Access(tm);

        if (vb != vt)
        {
            int acc = vb < 2 ? vt < 2 ? CHANGE_COMP : CHANGE_ADD
                             : vt < 2 ? CHANGE_SUB  : CHANGE_OTHER;

            Report(acc,  
                   "access changed:", 
                   pref + "from \"" + XModifier.toString(bm & accmods) + 
                         "\" to \"" + XModifier.toString(tm & accmods) + "\"");
        }

        final
        int cb = bm & ~accmods,
            ct = tm & ~accmods,
           def = vb < 2 && vt < 2 ? CHANGE_COMP : CHANGE_OTHER;

        TestBits(pref, cb & check,  ct & check,  def);
        TestBits(pref, cb & ~check, ct & ~check, CHANGE_COMP);
    }


    void Report (int t, String k, String s)
    {
        if (verbose || t != CHANGE_COMP)
            super.Report(t, k, s);
    }
}
