/*
 * $Id:$
 *
 * Copyright 2021 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tdk.signaturetest.core;

/**
 * JDKExclude represents the JDK classes to be excluded from signature testing.
 *
 * @author Scott Marlow
 */
public interface JDKExclude {

    /**
     * A default filter which excludes classes that start with {@code java} and {@code javax}.
     */
    JDKExclude JDK_CLASSES = (name) -> {
        final PackageGroup excludedJdkClasses = new PackageGroup(true);
        excludedJdkClasses.addPackages(new String[] {"java", "javax" });
        return excludedJdkClasses.checkName(name);
    };

    /**
     * Check for JDK classes that should be excluded from signature testing.
     * @param name is class name (with typical dot separators) to check.
     * @return true if the class should be excluded from signature testing.
     */
    boolean isJdkClass(String name);

}
