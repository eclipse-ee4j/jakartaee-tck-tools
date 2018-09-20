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


/**
 *  Base class for all class member nodes - fields, methods, iner classes
 *  and constructors (which are not class members, but here will be
 *  considered as such).
 *  Each class member has enclosing class - its parent container (home).
 */
public abstract class XClassMember extends XNode 
{
    /**
     *  Bitmask of all modifiers
     *  @see XModifier
     */
    public int modifiers;   


    /**
     *  Back link to the parent container (enclosing class).
     *  Only outer classes don't have a parent (home == null).
     */
    public XClass home;
    
    
    /**
     *  Name of the class this member is inherited from.
     *  'null' for declared members.
     */
    public String inherited;
    
    
    /**
     *  Returns 'false' for declared and true for inherited members.
     */
    public boolean isInherited ()
    {
        return inherited != null;
    }


    /**
     *  Links this node to the class specified (which becames enclosing class  
     *  of the node).
     */
    public abstract void link (XClass x);
    
    
    /**
     *  Unlinks this node from the enclosing class.
     */
    public abstract void unLink ();
    
    
    /**
     *  Find member with the same signature as this member in the specified class.
     */
    public abstract XClassMember findSame (XClass x);
    
    
    /**
     *  This is default implementation of the required method.
     *  Overriden in XClass.
     */
    public String getFullName () 
    {
        return home.getFullName() + "." + name;
    }
    
    
    /**
     *  Returns name of the package this member belongs to.
     *  Overriden in XClass.
     */
    public String getPackName ()
    {
        return home.getPackName();
    }

}
