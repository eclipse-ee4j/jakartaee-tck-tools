/*
 * Copyright (c) 2005, 2018 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.ts.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.FileHandler;

public class Etool{
    public static Properties props;
    private static final String PROPS_FILE_LOCATION = Util.ETOOLHOME + 
    Util.PATH_SEP + "bin" + Util.PATH_SEP + "etool.properties";
    private static final String USAGE = "usage: etool [command] [args] " +
    Util.NEWLINE + "valid commands are:" + Util.NEWLINE +
            "1. test_list [path to exclude list] [exclude list version]" +
            Util.NEWLINE +
            "2. create_xml [path to exclude list] [exclude list version]" +
            Util.NEWLINE +
            "3. [command_name] help";
    
    
    /** Creates a new instance of Etool */
    public Etool() {
    }
    
    public static void main(String args[]) {
        props = new Properties();
        String cf = PROPS_FILE_LOCATION;
        File acf = new File(cf);
        FileHandler handler;
        
        if(Util.ETOOLHOME == null){
        	System.out.println("You need to set ETOOL_HOME in your env!");
        	System.exit(1);
        }
        
        try {
            props.load(new FileInputStream(acf));
        } catch (IOException e) {
            System.out.println("ERROR ==> " + cf + " NOT FOUND!!!");
            System.exit(1);
        }
        if(args.length < 1 || args.length > 3) {
            System.err.println(USAGE);
            System.exit(1);
            
        } else if("test_list".equalsIgnoreCase(args[0]) && args.length == 1){
        	TestList tl = new TestList();
            System.out.println(tl.getUsage());
            
        } else if("test_list".equalsIgnoreCase(args[0]) &&
                "help".equalsIgnoreCase(args[1])){
        	TestList tl = new TestList();
            System.out.println(tl.getUsage());
            
        } else if("test_list".equalsIgnoreCase(args[0]) && args.length == 3){
        	TestList tl = new TestList();
            tl.runTestList(args[1], args[2]);
            
        } else if("create_xml".equalsIgnoreCase(args[0]) && args.length == 1) {
        	Extext et = new Extext();
            System.out.println(et.getUsage());
            
        } else if("create_xml".equalsIgnoreCase(args[0]) &&
                "help".equalsIgnoreCase(args[1])){
        	Extext et = new Extext();
            System.out.println(et.getUsage());
            
        } else if("create_xml".equalsIgnoreCase(args[0]) && args.length == 3){
        	Extext et = new Extext();
            et.createXML(args[1], args[2]);
            
        } else {
            System.out.println("Invalid Usage");
            System.out.println(USAGE);
        }
        
    }

}
