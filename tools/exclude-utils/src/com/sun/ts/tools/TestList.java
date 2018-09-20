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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


/**
 * This class is used for testing the CTS exclude list for false entries.
 * The basic functionality is that for each test on the exclude list will
 * be tested against the master test list.
 */

public class TestList implements Ecommand{
    private static final String USAGE = "usage: test_list " +
            "[path to exclude list] " + "[version of the exclude list] (141), (5), (6)";
    
    public TestList(){
    }
    
    public String getUsage(){
        return USAGE;
    }
    
    private static List<String> buildList(File file, String s, boolean b) {
        FileReader listfilereader;
        String testname;
        String strtln = s;
        boolean removeWhtSpace = b;
        List<String> alist = new ArrayList<String>();
        
        try {
            listfilereader = new FileReader(file);
            BufferedReader bufferedreader = new BufferedReader(listfilereader);
            
            while((testname = bufferedreader.readLine()) != null) {
                StringTokenizer stringtokenizer = new StringTokenizer(testname,
                        " \t\n\r\f");
                
                if(!(removeWhtSpace) && testname.trim().
                        startsWith(strtln))
                    continue;
                else
                    alist.add(testname);
                
                if(removeWhtSpace && testname.trim().
                        startsWith(strtln)){
                    alist.add(testname);
                }
            }
        } catch(FileNotFoundException f) {
            System.out.println("ERROR:   " + file + "   File Not Found");
            System.exit(2);
        } catch(IOException e) {
            System.out.println("ERROR:   " + file + "   File is zero length " +
                    "or you do not have permisions to read it");
            System.exit(3);
        }
        return alist;
    }
    
    public void runTestList(String excludelist, String version) {
        File excl_list = new File(excludelist);
        String mlName = "cts" + version + "_master_testlist.txt";
        File mast_list = null;
        
        if(!mlName.equals(null)){
        	mast_list = new File(Util.MASTERS + mlName);
        }else{
        	System.out.println(USAGE);
        	System.exit(1);
        }
        
        // Shave all white space at the beginning & end of each line.
        Shave shave = new Shave();
        shave.execute(excludelist);
        
        List exclude_list = buildList(excl_list, "#", false);
        List master_list = buildList(mast_list, "com/sun/ts/tests", 
        		true);
        
        Collections.sort(master_list);
        Object testName;
        
        int tfail = 0;
        int tpass = 0;
        int trun = 0;
        int esize = exclude_list.size();
        
        for(int ei=0; ei != esize;){
            testName = exclude_list.get(ei);
            
            // Validate that the Test exists in the CTS Master List.
            if(Collections.binarySearch(master_list, testName) >= 0){
                if(exclude_list.indexOf(testName) !=
                        exclude_list.lastIndexOf(testName)){
                    tfail = (tfail + 1);
                    System.out.println("FAILURE==> Duplicate Entry For: " +
                            testName);
                }else{
                    tpass = (tpass + 1);
                }
            }else if(exclude_list.get(ei).toString().equalsIgnoreCase("")){
                // do nothing
            } else {
                tfail = (tfail + 1);
                System.out.println("FAILED:   " + exclude_list.get(ei));
            }
            ei++;
            
        }
        trun = (tfail + tpass);
        
        System.out.println("");
        System.out.println("Total PASSED:  " + tpass);
        System.out.println("Total FAILED:  " + tfail);
        System.out.println("              --------");
        System.out.println("Total " + "RUN:     " + trun);
    }
}
