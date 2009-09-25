/*
 * $Id: SignatureTest.java 4549 2008-03-24 08:03:34Z me155718 $
 *
 * Copyright 1996-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tdk.signaturetest.ant;

import com.sun.tdk.signaturetest.Merge;
import com.sun.tdk.signaturetest.Result;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * <pre>
 * Ant wrapper for merge command
 * Required parameters:
 *   nested "file" element defines input files
 *     Corresponds to -files option
 *     Samples -
 *     &lt;megre ...
 *       &lt;file name="a1.sig" /&gt;
 *       &lt;file name="a2.sig" /&gt;
 *       ...
 *     &lt;/merge&gt;
 *   "classpath" attribute or nested "classpath" element is required.
 *     Corresponds to -classpath option
 *
 *   "write" attribute is required. Specifies output file name
 *     Corresponds to -write option
 *
 * Optional parameters:
 *   "binary" - Sets binary merge mode. Corresponds to "-Binary" option. Deafule is false.
 *   "failonerror" - Stop the build process if the command exits with an error. Default is "false".
 *   "negative" - inverts result (that is passed status treats as faild and vice versa, default is "false"
 *
 *
 * @author Mikhail Ershov
 */
public class AMerge extends ASuperBase {

    private ArrayList files = new ArrayList();
    private String write;
    private boolean binary = false;



    public void execute() throws BuildException {
        checkParams();
        Merge m = new Merge();
        System.setProperty(Result.NO_EXIT, "true");
        m.run(createParams(), new PrintWriter(System.out, true), null);
        if (negative ? m.isPassed() : !m.isPassed()) {
            if (failOnError) {
                throw new BuildException(m.toString());
            } else {
                getProject().log(m.toString(), Project.MSG_ERR);
            }
        }
    }

    private String[] createParams() {
        ArrayList params = new ArrayList();

        params.add(Merge.FILES_OPTION);

        Iterator it = files.iterator();
        StringBuffer files = new StringBuffer();
        while (it.hasNext()) {
            AFile af = (AFile) it.next();
            files.append(af.value);
            if (it.hasNext()) {
                files.append(File.pathSeparatorChar);
            }
        }
        params.add(files.toString());

        params.add(Merge.WRITE_OPTION);

        params.add(write);

        if (binary) {
            params.add(Merge.BINARY_OPTION);
        }

        return (String[]) params.toArray(new String[]{});
    }

    private void checkParams() throws BuildException {
        if (files == null || files.size() == 0) {
            throw new BuildException("Files are not specified");
        }

        // write
        if (write == null || "".equals(write)) {
            throw new BuildException("Output file is not specified");
        }
    }

    public AFile createFile() {
        AFile af = new AFile();
        files.add(af);
        return af;
    }

    public void setWrite(String write) {
        this.write = write;
    }

    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    public static class AFile extends DataType {
        String value;

        public void setName(String p) {
            value = p;
        }
    }


}
