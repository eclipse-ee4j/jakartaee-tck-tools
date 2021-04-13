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
package com.sun.tdk.signaturetest.classpath;

import com.sun.tdk.signaturetest.loaders.BinaryClassDescrLoader;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MethodDescr;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReleaseTest {

    public ReleaseTest() {
    }

    @Test
    public void testFindJDK8() throws ClassNotFoundException {
        Release jdk8 = Release.find(8);
        assertNotNull(jdk8.findClass("java.lang.Object"));
        assertNull(jdk8.findClass("java.lang.Module"));

        BinaryClassDescrLoader loader = new BinaryClassDescrLoader(new ClasspathImpl(jdk8, null), 4096);
        ClassDescription deprecatedClass = loader.load("java.lang.Deprecated");
        assertMethods(deprecatedClass);
    }

    @Test
    public void testFindJDK9() throws ClassNotFoundException {
        Release jdk9 = Release.find(9);
        assertNotNull(jdk9.findClass("java.lang.Object"));
        assertNotNull(jdk9.findClass("java.lang.Module"));
        assertNull(jdk9.findClass("java.lang.Record"));

        BinaryClassDescrLoader loader = new BinaryClassDescrLoader(new ClasspathImpl(jdk9, null), 4096);
        ClassDescription deprecatedClass = loader.load("java.lang.Deprecated");
        assertMethods(deprecatedClass, "forRemoval", "since");
    }

    @Test
    public void testFindJDK13() throws ClassNotFoundException {
        Release jdk13 = Release.find(13);
        assertNotNull(jdk13.findClass("java.lang.Object"));
        assertNotNull(jdk13.findClass("java.lang.Module"));
        assertNull(jdk13.findClass("java.lang.Record"));

        BinaryClassDescrLoader loader = new BinaryClassDescrLoader(new ClasspathImpl(jdk13, null), 4096);
        ClassDescription deprecatedClass = loader.load("java.lang.Deprecated");
        assertMethods(deprecatedClass, "forRemoval", "since");
    }

    @Test
    public void testFindJDK14() {
        Release jdk14 = Release.find(14);
        assertNotNull(jdk14.findClass("java.lang.Object"));
        assertNotNull(jdk14.findClass("java.lang.Module"));
        assertNotNull(jdk14.findClass("java.lang.Record"));
    }

    @Test
    public void testFindJDK15() throws ClassNotFoundException {
        Release jdk15 = Release.find(15);
        assertNotNull(jdk15.findClass("java.lang.Object"));
        assertNotNull(jdk15.findClass("java.lang.Module"));
        assertNotNull(jdk15.findClass("java.lang.Record"));
        BinaryClassDescrLoader loader = new BinaryClassDescrLoader(new ClasspathImpl(jdk15, null), 4096);
        ClassDescription deprecatedClass = loader.load("java.lang.Deprecated");
        assertMethods(deprecatedClass, "forRemoval", "since");
    }

    private void assertMethods(ClassDescription deprecatedClass, String... names) {
        MethodDescr[] arr = deprecatedClass.getDeclaredMethods();
        assertEquals("Same number of methods: " + Arrays.toString(arr), names.length, arr.length);

        Set<String> all = new HashSet<>(Arrays.asList(names));
        for (int i = 0; i < arr.length; i++) {
            MethodDescr m = arr[i];
            all.remove(m.getName());
        }

        assertEquals("Not found methods " + all, 0, all.size());
    }


}
