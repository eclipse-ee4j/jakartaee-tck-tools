/*
 * Copyright 1998-1999 Sun Microsystems, Inc.  All Rights Reserved.
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

public interface ClassConstants {
    /* Class File Constants */
    int JAVA_MAGIC                   = 0xcafebabe;
    int JAVA_VERSION                 = 45;
    int JAVA_MINOR_VERSION           = 3;

    /* Constant table */
    int CONSTANT_UTF8                = 1;
    int CONSTANT_UNICODE             = 2;
    int CONSTANT_INTEGER             = 3;
    int CONSTANT_FLOAT               = 4;
    int CONSTANT_LONG                = 5;
    int CONSTANT_DOUBLE              = 6;
    int CONSTANT_CLASS               = 7;
    int CONSTANT_STRING              = 8;
    int CONSTANT_FIELD               = 9;
    int CONSTANT_METHOD              = 10;
    int CONSTANT_INTERFACEMETHOD     = 11;
    int CONSTANT_NAMEANDTYPE         = 12;

    /* Access and modifier flags */
    int ACC_PUBLIC                   = 0x00000001;
    int ACC_PRIVATE                  = 0x00000002;
    int ACC_PROTECTED                = 0x00000004;
    int ACC_STATIC                   = 0x00000008;
    int ACC_FINAL                    = 0x00000010;
    int ACC_SYNCHRONIZED             = 0x00000020;
    int ACC_VOLATILE                 = 0x00000040;
    int ACC_TRANSIENT                = 0x00000080;
    int ACC_NATIVE                   = 0x00000100;
    int ACC_INTERFACE                = 0x00000200;
    int ACC_ABSTRACT                 = 0x00000400;
    int ACC_SUPER                    = 0x00000020;
    int ACC_STRICT		     = 0x00000800;
    int ACC_EXPLICIT		     = 0x00001000;
}
