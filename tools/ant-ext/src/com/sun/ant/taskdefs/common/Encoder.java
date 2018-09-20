/*
 * Copyright (c) 2003, 2018 Oracle and/or its affiliates. All rights reserved.
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
 * $Id$
 */

package com.sun.ant.taskdefs.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class Encoder extends Task {
    /**
     * The 'in' file.
     */
    private File in;
    
    /**
     * The 'out' file.
     */
    private File out;
    
    /**
     * The encoding.
     */
    private String encoding;
    
    /**
     * Constructs a new, default Encoder instance.
     */
    public Encoder() {
        super();
    }
    
    /**
     * Sets the 'in' file.
     * 
     * @param in the 'in' file.
     */
    public void setIn(File in) {
        this.in = in;
    }
    
    /**
     * Sets the 'out' file.
     * 
     * @param out the 'out' file.
     */
    public void setOut(File out) {
        this.out = out;
    }
    
    /**
     * Sets the encoding.
     * 
     * @param encoding the encoding.
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    /* (non-Javadoc)
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException {
        try {
            Charset cs = Charset.forName(encoding);
            InputStreamReader reader = new InputStreamReader(new FileInputStream(in));
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(out), cs);
            char[] buffer = new char[1024];
            int length;
            do {
                length = reader.read(buffer);
                if (length > 0) {
                    writer.write(buffer, 0, length);
                }
            } while (length > 0);
            reader.close();
            writer.close();
        } catch (Exception e) {
            throw new BuildException("Unable to encode '" + in + "' to '" + out + "' as '" + encoding + "'", e);
        }
    }
}
