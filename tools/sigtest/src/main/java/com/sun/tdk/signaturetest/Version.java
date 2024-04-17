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

import java.io.InputStream;
import java.util.Properties;

/**
 * Version information for the SignatureTest tool.
 */
public class Version {

    private static I18NResourceBundle i18n =
            I18NResourceBundle.getBundleForClass(Version.class);

    // the following constants are based on the maven build producing a git.properties file
    // which is read by the static initializer below
    public static final String Number;
    public static final String build_time;
    public static final String build_os;
    public static final String build_user;

    static {
        // Defaults
        String number = "Unknown";
        String buildOS = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String buildUser = System.getProperty("user.name");
        String buildTime = "Unknown";
        // Look for build info from the jar manifest
        try {
            // Read in the git.properties file
            Properties properties = new Properties();
            try(InputStream is = Version.class.getResourceAsStream("/META-INF/git.properties")) {
                if(is != null) {
                    properties.load(is);
                    number = properties.getProperty("git.build.version");
                    buildTime = properties.getProperty("git.commit.id.full");
                    buildUser = properties.getProperty("git.build.user.name");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Set the values read from manifest or the defaults
            Number = number;
            build_os = buildOS;
            build_time = buildTime;
            build_user = buildUser;
        }
    }

    public static String getVersionInfo() {
        StringBuffer sb = new StringBuffer();
        sb.append(i18n.getString("Main.usage.version", Version.Number) + '\n');
        sb.append("=========================\n");
        sb.append(i18n.getString("Version.version.build", Version.build_time) + '\n');
        sb.append(i18n.getString("Version.version.build_on", Version.build_os) + '\n');
        sb.append(i18n.getString("Version.version.build_by", Version.build_user));
        return sb.toString();
    }

}	    
	    
