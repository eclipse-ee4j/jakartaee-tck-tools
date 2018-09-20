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
 *  This is a base class for all API nodes (class and class member nodes).
 *  The only common property for all nodes is the name.
 *  All nodes can be compared (sorted) by their names.
 */
public abstract class XNode implements Cloneable, Comparable 
{

    /**
     *   The (simple) name of the node.
     */
    public String name;


    /**
     *   Returns the simple name of the node
     *   Overrides the <CODE>java.lang.Object.toString()</CODE> method.
     *
     *  @return simple name of the underlying node
     */
    public String toString ()
    {
		return name;
    }


    /**
     *   Returns the fully qualified name of the underlying node,
     *
     *  @return fully qualified name of the underlying node
     */
    public abstract String getFullName ();


    /**
     *   Returns string describing the underlying node. Format of this string
     *   depends on the node type.
     *
     *  @return string describing the underlying node
     */
    public abstract String getFullDescription ();


    /**
     *   Returns the signature of the node. For fields and inner classes it is
     *   simple name, for constructors and methods - simple name with argument
     *   types list.
     *
     *  @return signature of the underlying node
     */
    public String getSignature ()
    {
        return name;
    }


    /**
     *  Implements the <CODE>Cloneable</CODE> interface
     */
    public Object clone ()
    {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }


    /**
     *  Implements the <CODE>Comparable</CODE> interface
     */
	public int compareTo (Object o)
	{
		return name.compareTo(((XNode)o).name);
	}
}
