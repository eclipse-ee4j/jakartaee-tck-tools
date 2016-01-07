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

import com.sun.tdk.signaturetest.SigTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Base class for ant wrappers such as ASetup and ATest
 * @author Mikhail Ershov
 */
public class ABase extends ASuperBase {

    Path classpath;
    ArrayList pac = new ArrayList();
    private ArrayList exclude = new ArrayList();
    String fileName;
    private String apiVersion;


    void createBaseParameters(ArrayList params) {
        params.add(SigTest.FILENAME_OPTION);
        params.add(fileName);
        params.add(SigTest.CLASSPATH_OPTION);
        String [] cp = classpath.list();
        StringBuffer cpb = new StringBuffer();
        for (int i = 0; i < cp.length; i++) {
            cpb.append(cp[i]);
            if (i != cp.length - 1) {
                cpb.append(File.pathSeparatorChar);
            }
        }
        params.add(cpb.toString());
        if (apiVersion != null) {
            params.add(SigTest.APIVERSION_OPTION);
            params.add(apiVersion);
        }
        Iterator it = pac.iterator();
        while (it.hasNext()) {
            params.add(SigTest.PACKAGE_OPTION);
            APackage ap = (APackage) it.next();
            params.add(ap.value);
        }
        it = exclude.iterator();
        while (it.hasNext()) {
            params.add(SigTest.EXCLUDE_OPTION);
            AExclude ae = (AExclude) it.next();
            params.add(ae.value);
        }
    }


    // classpath
    public void setClasspath(Path s) {
        createClasspath().append(s);
    }

    public Path createClasspath() {
        return createClasspath(getProject()).createPath();
    }

    private Path createClasspath(Project p) {
        if (classpath == null) {
            classpath = new Path(p);
        }
        return classpath;
    }


    // exclude
    public void setExclude(String s) {
        AExclude ae = new AExclude();
        exclude.add(ae);
        ae.setPackage(s);
    }

    public AExclude createExclude() {
        AExclude ae = new AExclude();
        exclude.add(ae);
        return ae;
    }

    // package
    public void setPackage(String s) {
        APackage ae = new APackage();
        pac.add(ae);
        ae.setName(s);
    }

    public APackage createPackage() {
        APackage ap = new APackage();
        pac.add(ap);
        return ap;
    }

    // filename
    public void setFilename(String s) {
        fileName = s;
    }

    // APIVersion
    public void setApiVersion(String s) {
        apiVersion = s;
    }

    public static class AExclude extends DataType {
        String value;

        public void setPackage(String p) {
            value = p;
        }

        public void setClass(String p) {
            value = p;
        }
    }

    public static class APackage extends DataType {
        String value;

        public void setName(String p) {
            value = p;
        }
    }

}
