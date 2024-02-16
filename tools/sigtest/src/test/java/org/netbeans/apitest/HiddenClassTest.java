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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.Test;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.NbTestSuite;

/**
 *
 * @author Jaroslav Tulach
 */
public class HiddenClassTest extends NbTestCase {
    private static File workDir;
    
    public HiddenClassTest(String s) {
        super(s);
    }
    
    public static Test suite() {
        return new NbTestSuite(HiddenClassTest.class);
        //return new HiddenClassTest("testOkToHaveInnerclass");
    }

    @Override
    protected void setUp() throws Exception {
        clearWorkDir();
    }

    public void testArgument() throws Exception {
        String c1 =
            "package ahoj;" +
            "public interface I {" +
            "  public void get(Arg a);" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "I.java", c1);
        
        
        try {
            compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
            fail("Should fail on generating as Arg is not public");
        } catch (ExecuteUtils.ExecutionError ex) {
            // ok
        }
    }
    
    public void testChecksOnlyVisiblePackages() throws Exception {
        String c1 =
            "package ahoj;" +
            "public abstract class I {" +
            "  public abstract void get(Integer a);" +
            "  hidden.H get() { return null; };" +
            "}";
        createFile(1, "I.java", c1);
        String c2 =
            "package hidden;" +
            "public interface H {" +
            "  public void get(Arg a);" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "H.java", c2);
        
        
        compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
    }
    
    public void testChecksOnlyVisiblePackagesForFields() throws Exception {
        String c1 =
            "package ahoj;" +
            "public abstract class I {" +
            "  public abstract void get(Integer a);" +
            "  hidden.H hhh;" +
            "}";
        createFile(1, "I.java", c1);
        String c2 =
            "package hidden;" +
            "public class H {" +
            "  public static final Arg arg = new Arg();" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "H.java", c2);
        
        
        compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
    }
    
    public void testChecksOnlyVisiblePackagesForInnerclass() throws Exception {
        String c1 =
            "package ahoj;" +
            "public abstract class I {" +
            "  public abstract void get(Integer a);" +
            "  private static class X implements hidden.H {" +
            "  }" +
            "}";
        createFile(1, "I.java", c1);
        String c2 =
            "package hidden;" +
            "public interface H {" +
            "  public static final Arg arg = new Arg();\n" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "H.java", c2);
        
        
        compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
    }

    public void testOkToHaveInnerclass() throws Exception {
        String c1 =
            "package ahoj2;" +
            "public class I {" +
            "  class T { }" +
            "}";
        createFile(1, "I.java", c1);
        
        
        compareAPIs(1, 1, "-Dcheck.package=ahoj2.*");
    }

    public void testReturnType() throws Exception {
        String c1 =
            "package ahoj;" +
            "public interface I {" +
            "  public Arg get();" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "I.java", c1);
        
        
        try {
            compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
            fail("Should fail on generating as Arg is not public");
        } catch (ExecuteUtils.ExecutionError ex) {
            // ok
        }
    }

    public void testGenericReturnType() throws Exception {
        String c1 =
            "package ahoj;" +
            "import java.util.List;" +
            "public interface I {" +
            "  public List<Arg> get();" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "I.java", c1);
        
        
        try {
            compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
            fail("Should fail on generating as Arg is not public");
        } catch (ExecuteUtils.ExecutionError ex) {
            // ok
        }
    }
    public void testGenericWildReturnType() throws Exception {
        String c1 =
            "package ahoj;" +
            "import java.util.List;" +
            "public interface I {" +
            "  public List<? extends Arg> get();" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "I.java", c1);
        
        
        try {
            compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
            fail("Should fail on generating as Arg is not public");
        } catch (ExecuteUtils.ExecutionError ex) {
            // ok
        }
    }
    public void testGenericWildArg() throws Exception {
        String c1 =
            "package ahoj;" +
            "import java.util.List;" +
            "public interface I {" +
            "  public void get(List<? extends Arg> a);" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "I.java", c1);
        
        
        try {
            compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
            fail("Should fail on generating as Arg is not public");
        } catch (ExecuteUtils.ExecutionError ex) {
            // ok
        }
    }
    public void testGenericArg() throws Exception {
        String c1 =
            "package ahoj;" +
            "import java.util.List;" +
            "public interface I {" +
            "  public void get(List<Arg> a);" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "I.java", c1);
        
        
        try {
            compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
            fail("Should fail on generating as Arg is not public");
        } catch (ExecuteUtils.ExecutionError ex) {
            // ok
        }
    }

    public void testFieldType() throws Exception {
        String c1 =
            "package ahoj;" +
            "public interface I {" +
            "  public static final Arg get = null;" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "I.java", c1);
        
        
        try {
            compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
            fail("Should fail on generating as Arg is not public");
        } catch (ExecuteUtils.ExecutionError ex) {
            // ok
        }
    }

    public void testGenericFieldType() throws Exception {
        String c1 =
            "package ahoj;" +
            "import java.util.List;" +
            "public interface I {" +
            "  public static final List<Arg> get = null;" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "I.java", c1);
        
        
        try {
            compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
            fail("Should fail on generating as Arg is not public");
        } catch (ExecuteUtils.ExecutionError ex) {
            // ok
        }
    }
    public void testGenericWildFieldType() throws Exception {
        String c1 =
            "package ahoj;" +
            "import java.util.List;" +
            "public interface I {" +
            "  public static final List<? extends Arg> get = null;" +
            "}" +
            "class Arg { }" +
            "";
        createFile(1, "I.java", c1);
        
        
        try {
            compareAPIs(1, 1, "-Dcheck.package=ahoj.*");
            fail("Should fail on generating as Arg is not public");
        } catch (ExecuteUtils.ExecutionError ex) {
            // ok
        }
    }
   
    protected final void createFile(int slot, String name, String content) throws Exception {
        File d1 = new File(getWorkDir(), "dir" + slot);
        File c1 = new File(d1, name);
        copy(content, c1);
    }
    
    protected final void compareAPIs(int slotFirst, int slotSecond, String... additionalArgs) throws Exception {
        File d1 = new File(getWorkDir(), "dir" + slotFirst);
        File d2 = new File(getWorkDir(), "dir" + slotSecond);
        
        File build = new File(getWorkDir(), "build.xml");
        extractResource("build.xml", build);
        
        List<String> args = new ArrayList<String>();
        args.addAll(Arrays.asList(additionalArgs));
        args.add("-Ddir1=" + d1);
        args.add("-Ddir2=" + d2);
        ExecuteUtils.execute(getLog(), build, args.toArray(new String[0]));
    }
    
    private static final void copy(String txt, File f) throws Exception {
        f.getParentFile().mkdirs();
        FileWriter w = new FileWriter(f);
        w.append(txt);
        w.close();
    }

    final File extractResource(String res, File f) throws Exception {
        URL u = HiddenClassTest.class.getResource(res);
        assertNotNull ("Resource should be found " + res, u);
        
        FileOutputStream os = new FileOutputStream(f);
        InputStream is = u.openStream();
        for (;;) {
            int ch = is.read ();
            if (ch == -1) {
                break;
            }
            os.write (ch);
        }
        os.close ();
            
        return f;
    }
    
    final static String readFile (java.io.File f) throws java.io.IOException {
        int s = (int)f.length ();
        byte[] data = new byte[s];
        assertEquals ("Read all data", s, new java.io.FileInputStream (f).read (data));
        
        return new String (data);
    }
    
    final File extractString (String res, String nameExt) throws Exception {
        File f = new File(getWorkDir(), nameExt);
        f.deleteOnExit ();
        
        FileOutputStream os = new FileOutputStream(f);
        InputStream is = new ByteArrayInputStream(res.getBytes("UTF-8"));
        for (;;) {
            int ch = is.read ();
            if (ch == -1) {
                break;
            }
            os.write (ch);
        }
        os.close ();
            
        return f;
    }
    
}