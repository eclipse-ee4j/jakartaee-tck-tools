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

import junit.framework.Test;
import org.netbeans.junit.NbTestSuite;

/**
 *
 * @author Jaroslav Tulach
 */
public class APIWithReleaseTest extends APITest {
    public APIWithReleaseTest(String s) {
        super(s);
    }

    public static Test suite() {
        Test t = null;
//        t = new APIWithReleaseTest("testStrictNestedInterfaces");
        if (t == null) {
            t = new NbTestSuite(APIWithReleaseTest.class);
        }
        return t;
    }

    @Override
    protected String buildScript() {
        return "build-with-release.xml";
    }

    @Override
    protected String generateRelease() {
        return "14";
    }

    @Override
    protected String checkRelease() {
        return "15";
    }

    public void testDetectChangeInCharSequence() throws Exception {
        String c1 =
            "package ahoj;" +
            "public interface I extends CharSequence {" +
            "  public void get();" +
            "}";
        createFile(1, "I.java", c1);


        String c2 =
            "package ahoj;" +
            "public interface I extends CharSequence {" +
            "  public void get();" +
            "}";
        createFile(2, "I.java", c2);

        try {
            compareAPIs(1, 2, "-Dcheck.package=ahoj.*", "-Dcheck.type=strictcheck");
            fail("CharSequence.isEmpty() was added in JDK15 and that should be detected");
        } catch (ExecuteUtils.ExecutionError ex) {
            String out = ex.getStdErr().replace('\n', ' ');
            assertTrue(out, out.matches(".*Added Methods.*ahoj.I.*method public boolean java.lang.CharSequence.isEmpty().*"));
        }
    }

}