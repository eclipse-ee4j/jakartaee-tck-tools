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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.sun.tdk.signaturetest.loaders.BinaryClassDescrLoader;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MethodDescr;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ReleaseTest {

    @ParameterizedTest
    @MethodSource("jdkParameters")
    public void verifyJdk(final int version, final List<String> expectedTypes, final List<String> unexpectedTypes, final Map<String, List<String>> methodChecks) throws Exception {
        final Release release = Release.find(version);
        Assertions.assertNotNull(release, () -> "Failed to find release version " + version);
        for (String expectedType : expectedTypes) {
            assertNotNull(release.findClass(expectedType), () -> String.format("Failed to find class %s in Java %d", expectedType, version));
        }
        for (String unexpectedType : unexpectedTypes) {
            assertNull(release.findClass(unexpectedType), () -> String.format("Did not expect to find type %s in Java %d", unexpectedType, version));
        }
        if (!methodChecks.isEmpty()) {
            final BinaryClassDescrLoader loader = new BinaryClassDescrLoader(new ClasspathImpl(release, null), 4096);
            for (var entry : methodChecks.entrySet()) {
                final ClassDescription classDescription = loader.load(entry.getKey());
                Assertions.assertNotNull(classDescription, () -> String.format("Failed to find class %s in Java %d", entry.getKey(), version));
                assertMethods(classDescription, entry.getValue());
            }
        }
    }

    private static Stream<Arguments> jdkParameters() {
        final List<Arguments> jdkParameters = new ArrayList<>();
        final int currentVersion = Runtime.version().feature();
        final int minVersion = discoverMinimumVersion();
        for (int i = currentVersion; i >= minVersion; i--) {
            final List<String> expectedTypes = new ArrayList<>();
            final List<String> unexpectedTypes = new ArrayList<>();
            final Map<String, List<String>> methodChecks = new HashMap<>();
            expectedTypes.add("java.lang.Object");
            if (i == 8) {
                unexpectedTypes.add("java.lang.Module");
                methodChecks.put("java.lang.Deprecated", List.of());
            } else {
                expectedTypes.add("java.lang.Module");
                methodChecks.put("java.lang.Deprecated", List.of("forRemoval", "since"));
            }
            if (i > 13) {
                expectedTypes.add("java.lang.Record");
            } else {
                unexpectedTypes.add("java.lang.Record");
            }

            if (i > 16) {
                expectedTypes.add("java.util.random.RandomGeneratorFactory");
            } else {
                unexpectedTypes.add("java.util.random.RandomGeneratorFactory");
            }

            if (i > 20) {
                expectedTypes.add("java.util.SequencedCollection");
            } else {
                unexpectedTypes.add("java.util.SequencedCollection");
            }


            jdkParameters.add(Arguments.of(i, List.copyOf(expectedTypes), List.copyOf(unexpectedTypes), Map.copyOf(methodChecks)));
        }
        return jdkParameters.stream();
    }

    private void assertMethods(final ClassDescription deprecatedClass, final List<String> names) {
        MethodDescr[] arr = deprecatedClass.getDeclaredMethods();
        assertEquals(names.size(), arr.length, () -> "Same number of methods: " + Arrays.toString(arr));

        Set<String> all = new HashSet<>(names);
        for (MethodDescr m : arr) {
            all.remove(m.getName());
        }

        assertEquals(0, all.size(), () -> "Not found methods " + all);
    }

    private static int discoverMinimumVersion() {
        for (int version = 8; version <= Runtime.version().feature(); version++) {
            if (isAvailableRelease(version)) {
                return version;
            }
        }
        return Runtime.version().feature();
    }

    private static boolean isAvailableRelease(final int version) {
        // Use the process builder to invoke javac to ensure the --release version is available
        final ProcessBuilder pb = new ProcessBuilder("javac", "--release", Integer.toString(version), "--version")
                .redirectErrorStream(true);
        Process process;
        try  {
            process = pb.start();
            final InputStream in = pb.start().getInputStream();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final Thread consumer = new Thread(() -> {
                try {
                    final byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                } catch (IOException ignore) {}
            });
            consumer.setDaemon(true);
            consumer.start();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException ignored) {
            return false;
        }
    }

}
