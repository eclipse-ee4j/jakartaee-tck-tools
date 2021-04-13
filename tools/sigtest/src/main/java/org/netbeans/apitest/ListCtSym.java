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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

final class ListCtSym {
    public static void main(String... args) throws Exception {
        File lsR = new File(args[0]);
        File dir = new File(args[1]);
        int depth = Integer.parseInt(args[2]);

        Writer w = new FileWriter(lsR);
        dumpDir(w, dir, null, depth);
        w.close();
    }

    private static void dumpDir(Writer w, File dir, String prefix, int depth) throws IOException {
        File[] children = dir.listFiles();
        if (depth <= 0 || children == null) {
            return;
        }
        int minusOneDepth = depth - 1;
        for (File ch : children) {
            String newPrefix = prefix == null ? ch.getName() : prefix + "/" + ch.getName();
            if (minusOneDepth == 0) {
                w.append(newPrefix).append("\n");
            } else {
                if (ch.isDirectory()) {
                    dumpDir(w, ch, newPrefix, minusOneDepth);
                }
            }
        }
    }

    static Integer parseReleaseInteger(String release) {
        if (release == null) {
            return null;
        }
        String r;
        if (release.startsWith("1.")) {
            r = release.substring(2);
        } else {
            r = release;
        }
        try {
            return Integer.parseInt(r);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
