/*
 * Copyright 1998-1999 Sun Microsystems, Inc.  All Rights Reserved.
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

import java.io.PrintWriter;

/**
 * This interface is implemented by tests to be run under the test harness
 * of javasoft.sqe.javatest. Information about the test is normally contained
 * in the parameters of a table in an HTML file that is read by the
 * test harness.
 *
 * A test should also define `main' as follows:
 * <pre>
 * <code>
 * 	public static void main(String[] args) {
 * 	    Test t = new <em>test-class-name</em>();
 * 	    Status s = t.run(args, new PrintWriter(System.err), new PrintWriter(System.out));
 * 	    s.exit();
 * 	}
 * </code>
 * </pre>
 * Defining `main' like this means that the test can also be run standalone, 
 * independent of the harness.
 *
 * @author Jonathan J Gibbons
 */
public interface Test
{
    /**
     * Runs the test embodied by the implementation.
     * @param args 	These are supplied from the `executeArgs'
     *		   	values in the corresponding test description
     *             	and permit an implementation to be used for a variety of tests.
     * @param log  	A stream to which to report messages and errors.
     * @param ref  	A stream to which to write reference output.
     *			The file may subsequently be used to determine if the test 
     *			succeeded by comparing the contents against a golden file.
     */
    public Status run(String[] args, PrintWriter log, PrintWriter ref);

}
