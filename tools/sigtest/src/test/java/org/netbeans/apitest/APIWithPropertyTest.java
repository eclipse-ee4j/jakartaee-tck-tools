/*
 * Copyright 2007 Sun Microsystems, Inc.  All Rights Reserved.
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
package org.netbeans.apitest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.Test;
import org.netbeans.junit.NbTestSuite;

/**
 *
 * @author Jaroslav Tulach
 */
public class APIWithPropertyTest extends APITest {
    private static File workDir;
    
    public APIWithPropertyTest(String s) {
        super(s);
    }
    
    public static Test suite() {
        Test t = null;
//        t = new APIWithPropertyTest("testNoFailuresIfInXMLIf");
        if (t == null) {
            t = new NbTestSuite(APIWithPropertyTest.class);
        }
        return t;
    }
    
    @Override
    protected void compareAPIs(int slotFirst, int slotSecond, String... additionalArgs) throws Exception {
        File d1 = new File(getWorkDir(), "dir" + slotFirst);
        File d2 = new File(getWorkDir(), "dir" + slotSecond);
        
        File build = new File(getWorkDir(), "build.xml");
        extractResource("build.xml", build);
        
        List<String> args = new ArrayList<String>();
        args.add("all-property");
        args.addAll(Arrays.asList(additionalArgs));
        args.add("-Ddir1=" + d1);
        args.add("-Ddir2=" + d2);
        ExecuteUtils.execute(build, args.toArray(new String[0]));
    }
   
    @Override
    public void testGenerateVersionNumberAsJunit() throws Exception {}
    @Override
    public void testGenerateVersionNumber() throws Exception {}
    @Override
    public void testNoFailuresIfInXMLIf() {}
    
}