/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 1999 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
 
package com.sun.ant.taskdefs.common;

import java.io.File;
import java.util.Arrays;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

/**
 * A helper class which performs the actual work of the ddcreator task.
 *
 * This class is run with a classpath which includes the Sun RI tools
 */
public class Props2Xml {
    /**
     * The root directory of the tree containing the textual deployment desciptors. 
     */
    private File descriptorDirectory;
    
    /**
     * The directory where generated serialised desployment descriptors are written.
     */
    private File generatedFilesDirectory;

    /**
     * The descriptor text files for which a serialised descriptor is to be created.
     */
    String[] descriptors; 

	/**
     * The directory where generated serialised desployment descriptors are written.
     */
    private String name;


    /**
     * The main method.
     *
     * The main method creates an instance of the Props2Xml, passing it the 
     * args which it then processes.
     */    
    public static void main(String[] args) throws Exception {
        //Props2Xml helper = new Props2Xml(args);
        //helper.process();
    }
  
    /**
     * Initialise the helper with the command arguments.
     *
     */
	 //This util file is not used and hence commented
	 //keeping commented for future reference
    /*private Props2Xml(String[] args) {
        int index = 0;
        descriptorDirectory = new File(args[index++]);
        generatedFilesDirectory = new File(args[index++]);
        name = args[index++];
        
        descriptors = new String[args.length - index];
        for (int i = 0; index < args.length; ++i) {
            descriptors[i] = args[index++];
        }
    }*/
    
    /**
     * Do the actual work.
     *
     * The work proceeds by examining each descriptor given. If the xml
     * file does not exist or is older than the props file, the Sun RI
     * XML converter tool is invoked directly to create xml & runtime.xml files.
     */    
    /*private void process() throws Exception {
        for (int i = 0; i < descriptors.length; ++i) {
            String descriptorName = descriptors[i];
            File descriptorFile = new File(descriptorDirectory, descriptorName);

            int extIndex = descriptorName.lastIndexOf(".");
            
			//set defaults in case there is no extension
			String xmlName = name + ".xml";
            String runtimeXmlName = name + ".runtime.xml";
            String descNameMinusExt = descriptorName;
			File xmlFile = null;            
            
                
            if(descriptorName.endsWith(".props"))
			{
				xmlName = name + "_ejb.xml";
        		runtimeXmlName = name + "_ejb.runtime.xml";
        		descNameMinusExt = descriptorName;
				xmlFile = new File(generatedFilesDirectory, xmlName);
            
        		// do we need to regenerate the file
        		if (!xmlFile.exists() || xmlFile.lastModified() < descriptorFile.lastModified())
				{
					System.out.println("calling ejb props converter!");
					//call ejb props converter
					String[] args = {descriptorFile.getPath(), 
									 generatedFilesDirectory.getPath(),
									 generatedFilesDirectory.getPath() + File.separator + descriptorName + ".xml",
									 generatedFilesDirectory.getPath() + File.separator + xmlName,
									 generatedFilesDirectory.getPath() + File.separator + runtimeXmlName};
		            try
					{
		                com.sun.enterprise.util.PropsToXmlConverter.main(args);
		            }
		            catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			
			}
			else if(descriptorName.endsWith(".webprops"))
			{
				xmlName = name + "_web.xml";
        		runtimeXmlName = name + "_web.runtime.xml";
        		descNameMinusExt = descriptorName;
				xmlFile = new File(generatedFilesDirectory, xmlName);
            
        		// do we need to regenerate the file
        		if (!xmlFile.exists() || xmlFile.lastModified() < descriptorFile.lastModified())
				{
					System.out.println("calling web props converter!");
					
					//call web props converter
					String[] args = {descriptorFile.getPath(), 
									 generatedFilesDirectory + File.separator + xmlName,
									 generatedFilesDirectory + File.separator + runtimeXmlName};
		            try
					{
		                com.sun.enterprise.util.WebPropertiesFileConverter.main(args);
		            }
		            catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
			else if(descriptorName.endsWith(".cprops"))
			{
				
				xmlName = name + "_client.xml";
        		runtimeXmlName = name + "_client.runtime.xml";
        		descNameMinusExt = descriptorName;
				xmlFile = new File(generatedFilesDirectory, xmlName);
            
        		// do we need to regenerate the file
        		if (!xmlFile.exists() || xmlFile.lastModified() < descriptorFile.lastModified())
				{
					System.out.println("calling appclient props converter!");
					System.out.println(descriptorFile.getPath() + "," + 
                                       generatedFilesDirectory + File.separator + xmlName + ","
									   + generatedFilesDirectory + File.separator + runtimeXmlName);
					//call appclient props converter
					String[] args = {descriptorFile.getPath(), 
									 generatedFilesDirectory + File.separator + xmlName,
									 generatedFilesDirectory + File.separator + runtimeXmlName};
									 
		            try
					{
		                com.sun.enterprise.util.ApplicationClientPropertiesFileConverter.main(args);
		            }
		            catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			} 
        }
    }*/
}
