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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.netbeans.apitest.APITest.copy;
import org.netbeans.junit.NbTestCase;

public class GenerateJUnitReportTest extends NbTestCase {

    public GenerateJUnitReportTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        String c1
                = "package ahoj;"
                + "interface Private {"
                + "}"
                + "public interface I {"
                + "  public Private get();"
                + "}";
        createFile(1, "I.java", c1);
    }

    public void testFailWhenGenerating() throws Exception {
        File xml = new File(getWorkDir(), "junit.xml");
        File sig = new File(getWorkDir(), "api.sig");
        try {
            generateAPIs(1, "-Dcheck.package=ahoj.*", "-Dfail.on.error=true", "-Dcheck.report=" + xml, "-Dapi.out=" + sig);
            fail("Generating of sigtest files should fail");
        } catch (ExecuteUtils.ExecutionError err) {
            // OK
        }
        assertTrue("File " + xml + " exists", xml.exists());
        assertJUnitFailure(xml);
        assertSigfileGenerated(sig);
    }

    public void testReportFailureWhenGenerating() throws Exception {
        File xml = new File(getWorkDir(), "junit.xml");
        File sig = new File(getWorkDir(), "api.sig");
        generateAPIs(1, "-Dcheck.package=ahoj.*", "-Dfail.on.error=false", "-Dcheck.report=" + xml, "-Dapi.out=" + sig);
        assertJUnitFailure(xml);
        assertSigfileGenerated(sig);
    }
    protected void assertSigfileGenerated(File sig) throws IOException {
        assertTrue("Signatures generated in " + sig, sig.exists());
        final String sigContent = APITest.readFile(sig);
        if (sigContent.contains("interface ahoj.I")) {
            return;
        }
        fail(sigContent);
    }


    private void assertJUnitFailure(File xml) throws IOException {
        assertTrue("File " + xml + " exists", xml.exists());
        String failureReport = APITest.readFile(xml);
        if (failureReport.contains("failures=\"1\"")) {
            return;
        }
        fail("There should be one failure: " + xml + " but was:\n" + failureReport);
    }

    protected final void createFile(int slot, String name, String content) throws Exception {
        File d1 = new File(getWorkDir(), "dir" + slot);
        File c1 = new File(d1, name);
        copy(content, c1);
    }

    protected void generateAPIs(int slotFirst, String... additionalArgs) throws Exception {
        File d1 = new File(getWorkDir(), "dir" + slotFirst);

        File build = new File(getWorkDir(), "build.xml");
        extractResource("build.xml", build);

        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(additionalArgs));
        args.add("generate");
        args.add("-Ddir1=" + d1);
        ExecuteUtils.execute(build, args.toArray(new String[0]));
    }

    final File extractResource(String res, File f) throws Exception {
        URL u = APITest.class.getResource(res);
        assertNotNull("Resource should be found " + res, u);

        FileOutputStream os = new FileOutputStream(f);
        InputStream is = u.openStream();
        for (;;) {
            int ch = is.read();
            if (ch == -1) {
                break;
            }
            os.write(ch);
        }
        os.close();

        return f;
    }

}
