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
import java.util.*;

    
class ClassMemberFilterBin implements ClassMemberFilter
{
    static final int pub = 2;
    
    API api;
    
    
    ClassMemberFilterBin (API a)
    {
        api = a;
    }

    
    public boolean ok (XClass x)
    {
        return XModifier.access(x.modifiers) >= pub || hides(x);
    }
    
    
    public boolean ok (XClassCtor x)
    {
        return XModifier.access(x.modifiers) >= pub;
    }
    
    
    public boolean ok (XClassMethod x)
    {
        return XModifier.access(x.modifiers) >= pub;
    }
    
    
    public boolean ok (XClassField x)
    {
        return XModifier.access(x.modifiers) >= pub || hides(x);
    }
    
    
    boolean hides (XClassMember x)
    {
        if (x.home != null) {
            if (hides(x, x.home.extend)) {
                //System.out.println("HIDE " + x.getFullName() + 
                //                   " FROM class " + x.home.extend);        
                return true;
            }
                
            if (x.home.implement != null)
                for (int i = 0; i < x.home.implement.length; i++)
                    if (hides(x, x.home.implement[i])) {
                        //System.out.println("HIDE  " + x.getFullName() + 
                        //                   " FROM intf " + x.home.implement[i]);        
                        return true;
                    }
        }
        
        return false;
    }
    
    
    boolean hides (XClassMember x, String fqn)
    {
        if (fqn == null)
            return false;
            
        XClass xsuper = api.getXClass(fqn);
        if (xsuper == null) {
            //System.out.println("HIDE undef " + fqn);        
            return false;
        }
        
        XClassMember xx = x.findSame(xsuper);
        if (xx != null) 
            return XModifier.access(xx.modifiers) >= pub;
            
        return false;
    }
    
}
    
