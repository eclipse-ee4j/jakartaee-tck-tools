/*
 * $Id: ClassDescriptionLoader.java 4504 2008-03-13 16:12:22Z sg215604 $
 *
 * Copyright 1996-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tdk.signaturetest.loaders;

/**
 * This is optional interface which ClassDescriptionLoader can implement
 * Allows to pass some additional modes to loader for fine tunung  
 */
public interface LoadingHints {
    void addLoadingHint(Hint hint);

    /**
     * Says that loader should not read constant values for
     * preventing unnecesary class initialization
     * Can be used in reflection loaders without constant checking
     */
    Hint DONT_READ_VALUES = new Hint("DONT_READ_VALUES");

    /**
     * Says that loader should read synthetic elements
     */
    Hint READ_SYNTETHIC = new Hint("RAED_SYNTHETIC");

    /**
     * Says that loader should read bridge methods
     */
    Hint READ_BRIDGE = new Hint("READ_BRIDGE");

    /**
     * Read annotations even inside methods bodies
     */
    Hint READ_ANY_ANNOTATIONS = new Hint("READ_ANY_ANNOTATIONS");

    public static class Hint {
        private String name;

        Hint(String name) {
            this.name = name;
        }

        public String toString() {
            return "Hint:" + name;
        }
    }
}
