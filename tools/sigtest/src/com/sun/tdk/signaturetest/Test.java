/*
 * $Id: Test.java 4504 2008-03-13 16:12:22Z sg215604 $
 *
 * Copyright 1996-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tdk.signaturetest;

import java.io.PrintWriter;

/**
 * This is a simple wrapper for SignatureTest class that implements
 * required by JavaTest interface.
 *
 * @author Serguei Ivashin
 *
 * @test
 * @executeClass com.sun.tdk.signaturetest.Test
 */
public class Test implements com.sun.javatest.Test {

    /**
     * Run the test using command-line; return status via numeric exit code.
     */
    public static void main(String[] args) {
        Test t = new Test();
        t.run(args, new PrintWriter(System.err, true), new PrintWriter(System.out, true)).exit();
    }


    /**
     * This is the gate to run the test with the JavaTest application.
     *
     * @param log This log-file is used for error messages.
     * @param ref This reference-file is ignored here.
     */
    public com.sun.javatest.Status run(String[] args, PrintWriter log, PrintWriter ref) {

        SignatureTest t = SignatureTest.getInstance();
        t.run(args, log, ref);
        return com.sun.javatest.Status.parse(t.toString().substring(7));
    }

}
