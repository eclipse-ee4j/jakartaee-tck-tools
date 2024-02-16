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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.AssertionFailedError;
import org.apache.tools.ant.Main;
import org.junit.Assert;

/**
 *
 * @author Jaroslav Tulach
 */
final class ExecuteUtils {
    private static PrintStream origSystemOut = System.out;
    private static PrintStream origSystemErr = System.err;
    private static ByteArrayOutputStream out;
    private static ByteArrayOutputStream err;

    private ExecuteUtils() {
    }

    final static String getStdOut() {
        return out.toString();
    }
    final static String getStdErr() {
        return err.toString();
    }

    final static void execute(PrintStream testLog, File f, String[] args) throws Exception {


        // Jesse claims that this is not the right way how the execution
        // of an ant script should be invoked:
        //
        // better IMHO to just run the task directly
        // (setProject() and similar, configure its bean properties, and call
        // execute()), or just make a new Project and initialize it.
        // ant.Main.main is not intended for embedded use. Then you could get rid
        // of the SecurityManager stuff, would be cleaner I think.
        //
        // If I had to write this once again, I would try to follow the
        // "just make a new Project and initialize it", but as this works
        // for me now, I leave it for the time when somebody really
        // needs that...

        if(out == null) {
            out = new java.io.ByteArrayOutputStream ();
            err = new java.io.ByteArrayOutputStream ();
            System.setOut (new java.io.PrintStream (out));
            System.setErr (new java.io.PrintStream (err));
        }

        List<String> arr = new ArrayList<>();
        arr.add ("-f");
        arr.add (f.toString ());
        arr.addAll(Arrays.asList(args));
        arr.add ("-verbose");
        if (System.getProperty("java.version").startsWith("1.8")) {
            arr.add ("-Dbuild.compiler=extjavac");
        }

        out.reset();
        err.reset();

        // Call our ant main and then assert on the captured exit status
        MyAntMain antMain = new MyAntMain();
        antMain.startAnt(arr.toArray(new String[0]), null, null);
        testLog.println("ExecuteUtils.finished ant call, exit="+antMain.getExitCode());
        testLog.println("---System.out");
        testLog.println(ExecuteUtils.getStdOut());
        testLog.println("---System.err");
        testLog.println(ExecuteUtils.getStdErr());
        origSystemOut.println("---System.out");
        origSystemOut.println(ExecuteUtils.getStdOut());
        origSystemOut.flush();
        origSystemErr.println("---System.err");
        origSystemErr.println(ExecuteUtils.getStdErr());
        origSystemErr.flush();
        ExecutionError.assertExitCode ("Execution has to finish without problems", antMain.getExitCode());
    }

    final static class ExecutionError extends AssertionFailedError {
        public final int exitCode;

        public ExecutionError (String msg, int e) {
            super (msg);
            this.exitCode = e;
        }

        public String getStdErr() {
            return ExecuteUtils.getStdErr();
        }

        static void assertExitCode (String msg, int e) {
            if (e != 0) {
                throw new ExecutionError (
                    msg + " was: " + e + "\nOutput: " + out.toString () +
                    "\nError: " + err.toString (),
                    e
                );
            }
        }
    }

    /**
     * Override the ant Main class to capture the exitCode call and do not call the
     * super method as it calls {@linkplain System#exit(int)}
     */
    private static class MyAntMain extends Main {
        private int exitCode;


        @Override
        protected void exit(int exitCode) {
            this.exitCode = exitCode;
        }
        public int getExitCode() {
            return exitCode;
        }
    }
}
