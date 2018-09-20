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
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Shave implements Ecommand {
    
    private static final String USAGE = "usage: run [filesname]";
    
    public Shave() {
    }
    
    public String getUsage(){
        return USAGE;
    }
    
    public void execute(String input_file) {
        String infile = input_file;
        String backup = infile + ".orig";
        FileReader filereader;
        FileReader backupfilereader;
        
        try {
            filereader = new FileReader(infile);
        } catch(FileNotFoundException filenotfoundexception) {
            System.out.println(infile + " File Not Found");
            return;
        }
        
        // make a backup of the file.
        try {
            String bline;
            BufferedReader backupreader = new BufferedReader(filereader);
            BufferedWriter backupwriter = new BufferedWriter
                    (new FileWriter(backup));
            while((bline = backupreader.readLine()) != null) {
                backupwriter.write(bline);
                backupwriter.newLine();
            }
            filereader.close();
            backupwriter.close();
            System.out.println("Created a backup file called: " + backup);
        } catch(IOException ioe){
            ioe.printStackTrace();
        }
        
        // Use backup version to correct for any whitespace.
        try {
            backupfilereader = new FileReader(backup);
        } catch(FileNotFoundException filenotfoundexception) {
            System.out.println(backup + " File Not Found");
            return;
        }
        
        try {
            BufferedReader bufferedreader = new BufferedReader(backupfilereader);
            BufferedWriter bufferedwriter = new BufferedWriter
                    (new FileWriter(infile));
            int i = 0;
            String s1;
            
            while((s1 = bufferedreader.readLine()) != null) {
                bufferedwriter.write(s1.trim());
                bufferedwriter.newLine();
            }
            
            backupfilereader.close();
            bufferedwriter.close();
            System.out.println("Removed all blank spaces at beginning and end " +
                    "of each line from file " + infile);
        } catch(IOException ioe){
            System.out.println("File might not be writeable!");
            System.out.println("Try checking the file out of source control!");
            System.out.println("exiting... ");
            ioe.printStackTrace();
            System.exit(1);
        }
    }
}
