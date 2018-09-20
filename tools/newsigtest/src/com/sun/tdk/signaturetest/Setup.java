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

 
package com.sun.tdk.signaturetest;

import java.util.*;


public class Setup
{

    public static void main (String[] pars) 
    {
    	com.sun.tdk.sigtest.Arguments args = new com.sun.tdk.sigtest.Arguments(pars);
        
        List tmp = new ArrayList();
        boolean reflect = true;

        if (args.isArg("-help", false)) {
        //  Ignore
        }
    
        if (args.isArg("-package", true)) {
            tmp.add("-package");
            tmp.add(args.getArgString());
        }
        
        if (args.isArg("-exclude", true)) {
            tmp.add("-expackage");
            tmp.add(args.getArgString());
        }
        
        if (args.isArg("-static", false)) {
            tmp.add("-access");
            tmp.add("static");
            reflect = false;
        }
    
        if (args.isArg("-reflect", false)) {
            tmp.add("-access");
            tmp.add("reflect");
        }
        
        if (reflect)
            tmp.add("-BootLoader");

        if (args.isArg("-CheckValue", false)) {
            tmp.add("-ConstValues");
        }
        
        if (args.isArg("-classpath", true)) {
            tmp.add("-test");
            tmp.add(args.getArgString());
        }
        
        if (args.isArg("-Version", true)) {
            tmp.add("-TestVersion");
            tmp.add(args.getArgString());
        }
        
        if (args.isArg("-testURL", true)) {
        }
        
        if (args.isArg("-FileName", true)) {
            tmp.add("-out");
            tmp.add(args.getArgString());
        }
            
        if (args.isArg("-FileFormat", true)) {
        //  Ignore
        }
        
        if (args.isArg("-ClosedFile", false)) {
        //  Ignore
        }
        
        if (args.isArg("-verbose", false)) {
        //  Ignore
        }
   
        if (!args.isEmpty()) {
            System.err.println("Extra arguments: " + args.toString());
            System.exit(1);
        }

        String[] wrk = (String[])tmp.toArray(new String[tmp.size()]);
        System.exit(com.sun.tdk.sigtest.Setup.run(wrk) == 0 ? 95 : 98);
        
    }
    
}

 
