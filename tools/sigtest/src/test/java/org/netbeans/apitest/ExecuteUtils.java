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
import org.junit.Assert;

/**
 *
 * @author Jaroslav Tulach
 */
final class ExecuteUtils {
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

    final static void execute(File f, String[] args) throws Exception {
        // we need security manager to prevent System.exit
        if (! (System.getSecurityManager () instanceof MySecMan)) {
            out = new java.io.ByteArrayOutputStream ();
            err = new java.io.ByteArrayOutputStream ();
            System.setOut (new java.io.PrintStream (out));
            System.setErr (new java.io.PrintStream (err));

            System.setSecurityManager (new MySecMan ());
        }

        MySecMan sec = (MySecMan)System.getSecurityManager();

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

        List<String> arr = new ArrayList<>();
        arr.add ("-f");
        arr.add (f.toString ());
        arr.addAll(Arrays.asList(args));
        arr.add ("-verbose");
        if (System.getProperty("java.version").startsWith("1.8")) {
            arr.add ("-Dbuild.compiler=extjavac");
        }

        out.reset ();
        err.reset ();

        try {
            sec.setActive(true);
            org.apache.tools.ant.Main.main (arr.toArray(new String[0]));
        } catch (MySecExc ex) {
            Assert.assertNotNull ("The only one to throw security exception is MySecMan and should set exitCode", sec.exitCode);
            ExecutionError.assertExitCode ("Execution has to finish without problems", sec.exitCode);
        } finally {
            sec.setActive(false);
        }
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

    private static class MySecExc extends SecurityException {
        @Override
        public void printStackTrace() {
        }
        @Override
        public void printStackTrace(PrintStream ps) {
        }
        @Override
        public void printStackTrace(PrintWriter ps) {
        }
    }

    private static class MySecMan extends SecurityManager {
        public Integer exitCode;

        private boolean active;

        @Override
        public void checkExit (int status) {
            if (active) {
                exitCode = status;
                throw new MySecExc ();
            }
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
        }

        @Override
        public void checkPermission(Permission perm) {
        /*
            if (perm instanceof RuntimePermission) {
                if (perm.getName ().equals ("setIO")) {
                    throw new MySecExc ();
                }
            }
         */
        }

        @Override
        public void checkMulticast(InetAddress maddr) {
        }

        @Override
        public void checkAccess (ThreadGroup g) {
        }

        @Override
        public void checkWrite (String file) {
        }

        @Override
        public void checkLink (String lib) {
        }

        @Override
        public void checkExec (String cmd) {
        }

        @Override
        public void checkDelete (String file) {
        }

        @Override
        public void checkPackageAccess (String pkg) {
        }

        @Override
        public void checkPackageDefinition (String pkg) {
        }

        @Override
        public void checkPropertyAccess (String key) {
        }

        @Override
        public void checkRead (String file) {
        }

        @Override
        public void checkSecurityAccess (String target) {
        }

        @Override
        public void checkWrite(FileDescriptor fd) {
        }

        @Override
        public void checkListen (int port) {
        }

        @Override
        public void checkRead(FileDescriptor fd) {
        }

        @Override
        @SuppressWarnings("deprecation")
        public void checkMulticast(InetAddress maddr, byte ttl) {
        }

        @Override
        public void checkAccess (Thread t) {
        }

        @Override
        public void checkConnect (String host, int port, Object context) {
        }

        @Override
        public void checkRead (String file, Object context) {
        }

        @Override
        public void checkConnect (String host, int port) {
        }

        @Override
        public void checkAccept (String host, int port) {
        }

        @SuppressWarnings("deprecation")
        public void checkMemberAccess (Class clazz, int which) {
        }

        @SuppressWarnings("deprecation")
        public void checkSystemClipboardAccess () {
        }

        @Override
        public void checkSetFactory () {
        }

        @Override
        public void checkCreateClassLoader () {
        }

        @SuppressWarnings("deprecation")
        public void checkAwtEventQueueAccess () {
        }

        @Override
        public void checkPrintJobAccess () {
        }

        @Override
        public void checkPropertiesAccess () {
        }

        void setActive(boolean b) {
            active = b;
        }
    } // end of MySecMan

}
