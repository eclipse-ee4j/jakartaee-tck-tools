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

package com.sun.tdk.sigtest.api;

import java.util.*;


public class XClass extends XClassMember 
{

    public boolean  defined = false;
    public String   packname;
    public String   extend;
    public String[] implement;
    public XNodes/*XClassCtor*/   ctors;
    public XNodes/*XClassMethod*/ methods;
    public XNodes/*XClassField*/  fields;
    public XNodes/*XClassClass*/  inners;


    public void link (XClass x) 
    {
	    home = x;
        if (home.inners == null)
            home.inners = new XNodes();
	    home.inners.add(this);
    }


    public void unLink ()
    {
        home.inners.remove(this);
        home = null;
    }
    
    
    public XClassMember findSame (XClass x)
    {
        return x.inners == null ? null : (XClassMember)x.inners.find(name);
    }
    
    
    public String getFullName () 
    {
	    if (home == null) 
            return packname + "." + name;
	    else
            return home.getFullName() + "$" + name;
    }
    
    
    public String getPackName ()
    {
        XClass x;
        
        for (x = this; x.home != null;)
            x = x.home;
            
        return x.packname;
    }


    public String getInnerName ()
    {
        StringBuffer sb = new StringBuffer();
        
        sb.append(name);
        for (XClass x = home; x != null; x = x.home)
            sb.insert(0,'$').insert(0, x.name);
            
        return sb.toString();
    }
    
    
    public XClass findInner (String fqn)
    {
        if (getFullName().equals(fqn))
            return this;
        else {
            XClass x;
            if (inners != null)
                for (Iterator it = inners.iterator(); it.hasNext();) 
                    if ((x = ((XClass)it.next()).findInner(fqn)) != null)
                        return x;
        }
        
        return null;
    }


    public String getFullDescription ()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(XModifier.toString(modifiers & ~XModifier.xinterface));
        sb.append(' ').append(getFullName());

        if ((modifiers & XModifier.xinterface) != 0) {
            if (implement != null && implement.length != 0) 
                sb.append(" extends ").append(Utils.list(implement));
        }
        else {
            if (extend != null && !extend.equals("java.lang.Object")) 
                sb.append(" extends ").append(extend);

            if (implement != null && implement.length != 0) 
                sb.append(" implements ").append(Utils.list(implement));
        }

        return sb.toString();
    }
    
    
    //  Setup all default modifiers in class
    //
    public void setDefaults ()
    {
        if ((modifiers & XModifier.xinterface) != 0)
        {
            // Every interface is impicitly abstract
            modifiers |= XModifier.xabstract;

            // Every method declaration in the body of an interface is implicitly public & abstract
            if (methods != null)
                for (int i = 0; i < methods.size(); i++)
                    ((XClassMethod)methods.get(i)).modifiers |= 
                        (XModifier.xpublic | XModifier.xabstract);

            // Every field declaration in the body of an interface is implicitly public, 
            // static and final
            if (fields != null)
                for (int i = 0; i < fields.size(); i++)
                    ((XClassField)fields.get(i)).modifiers |= 
                        (XModifier.xpublic | XModifier.xstatic | XModifier.xfinal);
        }
    }
    
    
    public boolean isAccessible (XClassMember x)
    {
        if (x == null)
            return false;
    
        // private members never inherited
        if (XModifier.access(x.modifiers) == 0) 
            return false;
            
        // package access members from other package not inherited
        if (XModifier.access(x.modifiers) == 1 
         && !getPackName().equals(x.getPackName())) 
            return false;

        return true;
    }
    
    
    public boolean isInterface ()
    {    
        return (modifiers & XModifier.xinterface) != 0;
    }
    
}
