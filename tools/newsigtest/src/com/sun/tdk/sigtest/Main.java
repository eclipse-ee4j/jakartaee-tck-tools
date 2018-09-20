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
 
/*
 * The Sigtest Tool
 */
 
 
package com.sun.tdk.sigtest;

//import com.sun.tdk.sigtest.api.*;
//import java.io.*;
//import java.util.*;

 
class Main
{

    static void stop (String msg)
    {
        if (msg != null)
            System.err.println(msg);
        
        throw new Error("Terminated.");
    }


    static String getVersion ()
    {
        Package pkg = Package.getPackage("com.sun.tdk.sigtest");           
        if (pkg != null)
            return pkg.getImplementationVersion();
        else
            return "*version unknown*";
    }

}
 
