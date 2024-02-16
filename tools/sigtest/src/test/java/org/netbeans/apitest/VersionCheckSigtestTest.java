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

/** Runs all the tests as for compatibility check, but in case we check
 * that nothing changed, it does "strictcheck" e.g. the full check for 
 * mutual compatibility.
 *
 * @author Jaroslav Tulach
 */
public class VersionCheckSigtestTest extends CheckNewSigtestTest {
    public VersionCheckSigtestTest(String s) {
        super(s);
    }
    
    public static Test suite() {
        Test t = null;
//        t = new VersionCheckSigtestTest("testConvertingNonFinalClassToAbstract");
        return t != null ? t : new NbTestSuite(VersionCheckSigtestTest.class);
    }
    
    @Override
    protected void compareAPIs(int slotFirst, int slotSecond, String... additionalArgs) throws Exception {
        File d1 = new File(getWorkDir(), "dir" + slotFirst);
        File d2 = new File(getWorkDir(), "dir" + slotSecond);
        
        File build = new File(getWorkDir(), "build.xml");
        extractResource("build.xml", build);
        
        List<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList(additionalArgs));
        args.add("with-version");
        args.add("-Ddir1=" + d1);
        args.add("-Ddir2=" + d2);
        
        // these three lines will skip the check
        args.add("-Dv1=1.0");
        if (
            getName().equals("testMissingFieldInAbstractClassIsDetected") ||
            getName().equals("testMissingStaticFieldDetected") ||
            getName().equals("testMissingStaticFieldSubpackageDetected") ||
            getName().equals("testMissingConstructorInAbstractClassIsDetected") ||
            getName().equals("testConvertingNonFinalClassToAbstract") ||
            getName().equals("testAddInterfaceMethod")
        ) {
            // do the test of signatures for these special tests
            args.add("-Dv2=1.0");
        } else {
            args.add("-Dv2=2.0");
        }
        args.add("-Dcheck.type=versioncheck");
        
        ExecuteUtils.execute(getLog(), build, args.toArray(new String[0]));
    }
    
    @Override
    protected final void checkAPIsEqual(String... additionalArgs) throws Exception {
        File d1 = new File(getWorkDir(), "dir1");
        
        File build = new File(getWorkDir(), "build.xml");
        extractResource("build.xml", build);
        
        List<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList(additionalArgs));
        args.add("with-version");
        args.add("-Ddir1=" + d1);
        args.add("-Dv1=1.0");
        args.add("-Ddir2=" + d1);
        args.add("-Dv2=1.0");
        args.add("-Dcheck.type=versioncheck");
        ExecuteUtils.execute(getLog(), build, args.toArray(new String[0]));
    }
   
}