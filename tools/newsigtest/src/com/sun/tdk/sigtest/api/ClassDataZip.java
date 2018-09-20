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

import java.io.*;
import java.util.zip.*;


public class ClassDataZip extends ZipFile implements ClassData 
{
    ZipEntry ze;


    public ClassDataZip (File f) throws IOException
    {
        super(f);
    }
    
    
    public void set (ZipEntry z)
    {
        ze = z;
    }


    public void close ()
    {
        ze = null;

    	try {
	        super.close();
    	}
	    catch (IOException e) {
	    }
    }


    public String getName ()
    {
        return super.getName() + " " + ze.getName();
    }


    public InputStream getStream ()  throws IOException
    {
        return getInputStream(ze);
    }


    public int getCount () 
    {
        return (int)ze.getSize();
    }
}
