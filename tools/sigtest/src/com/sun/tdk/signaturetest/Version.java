/*
 * $Id$
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

package com.sun.tdk.signaturetest;

import com.sun.tdk.signaturetest.util.I18NResourceBundle;

public class Version {

    private static I18NResourceBundle i18n =
            I18NResourceBundle.getBundleForClass(Version.class);

    // the following constatnts should be filled in by the build script
    public static final String Number="2.2";
    public static final String build_time="";
    public static final String build_os="";
    public static final String build_user="";

    public static String getVersionInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(i18n.getString("Main.usage.version", Version.Number) + '\n');
        sb.append("=========================\n");
        sb.append(i18n.getString("Version.version.build", Version.build_time) + '\n');
        sb.append(i18n.getString("Version.version.build_on", Version.build_os) + '\n');
        sb.append(i18n.getString("Version.version.build_by", Version.build_user));
        return sb.toString();
    }

}	    
	    
