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
    
    private ExecuteUtils() {
    }
/*    
    final static void execute (String res, String[] args) throws Exception {
        execute (extractResource (res), args);
    }
  */  
    private static ByteArrayOutputStream out;
    private static ByteArrayOutputStream err;
    
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
        
        List<String> arr = new ArrayList<String>();
        arr.add ("-f");
        arr.add (f.toString ());
        arr.addAll(Arrays.asList(args));
        //arr.add ("-verbose");
        
        
        out.reset ();
        err.reset ();
        
        try {
            sec.setActive(true);
            org.apache.tools.ant.Main.main (arr.toArray(new String[0]));
        } catch (MySecExc ex) {
            Assert.assertNotNull ("The only one to throw security exception is MySecMan and should set exitCode", sec.exitCode);
            ExecutionError.assertExitCode (
                "Execution has to finish without problems",
                sec.exitCode.intValue ()
            );
        } finally {
            sec.setActive(false);
        }
    }
    
    static class ExecutionError extends AssertionFailedError {
        public final int exitCode;
        
        public ExecutionError (String msg, int e) {
            super (msg);
            this.exitCode = e;
        }
        
        public static void assertExitCode (String msg, int e) {
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
        public void printStackTrace(PrintWriter ps) {
        }
    }
    
    private static class MySecMan extends SecurityManager {
        public Integer exitCode;
        
        private boolean active;
        
        public void checkExit (int status) {
            if (active) {
                exitCode = new Integer (status);
                throw new MySecExc ();
            }
        }

        public void checkPermission(Permission perm, Object context) {
        }

        public void checkPermission(Permission perm) {
        /*
            if (perm instanceof RuntimePermission) {
                if (perm.getName ().equals ("setIO")) {
                    throw new MySecExc ();
                }
            }
         */
        }

        public void checkMulticast(InetAddress maddr) {
        }

        public void checkAccess (ThreadGroup g) {
        }

        public void checkWrite (String file) {
        }

        public void checkLink (String lib) {
        }

        public void checkExec (String cmd) {
        }

        public void checkDelete (String file) {
        }

        public void checkPackageAccess (String pkg) {
        }

        public void checkPackageDefinition (String pkg) {
        }

        public void checkPropertyAccess (String key) {
        }

        public void checkRead (String file) {
        }

        public void checkSecurityAccess (String target) {
        }

        public void checkWrite(FileDescriptor fd) {
        }

        public void checkListen (int port) {
        }

        public void checkRead(FileDescriptor fd) {
        }

        public void checkMulticast(InetAddress maddr, byte ttl) {
        }

        public void checkAccess (Thread t) {
        }

        public void checkConnect (String host, int port, Object context) {
        }

        public void checkRead (String file, Object context) {
        }

        public void checkConnect (String host, int port) {
        }

        public void checkAccept (String host, int port) {
        }

        public void checkMemberAccess (Class clazz, int which) {
        }

        public void checkSystemClipboardAccess () {
        }

        public void checkSetFactory () {
        }

        public void checkCreateClassLoader () {
        }

        public void checkAwtEventQueueAccess () {
        }

        public void checkPrintJobAccess () {
        }

        public void checkPropertiesAccess () {
        }

        void setActive(boolean b) {
            active = b;
        }
    } // end of MySecMan

}
