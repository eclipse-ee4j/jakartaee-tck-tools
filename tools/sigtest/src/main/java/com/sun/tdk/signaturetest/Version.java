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

import java.net.JarURLConnection;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class Version {

    private static I18NResourceBundle i18n =
            I18NResourceBundle.getBundleForClass(Version.class);

    // the following constants should be filled in by the build script
    public static final String Number;
    public static final String build_time;
    public static final String build_os;
    public static final String build_user;

    public static final String IMPLEMENTATION_BUILD_OS = "Implementation-Build-OS";

    public static final String IMPLEMENTATION_BUILD_TIME = "Implementation-Build-Time";

    static {
        // Defaults
        String number = "Unknown";
        String buildOS = System.getProperty("os.name") + " " + System.getProperty("os.version");
        String buildUser = System.getProperty("user.name");
        ZonedDateTime gmt = ZonedDateTime.now(ZoneId.of("GMT"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String buildTime = gmt.format(formatter);
        // Look for build info from the jar manifest
        try {
            // Get the URL of the jar file from the CodeSource
            ProtectionDomain pd = Version.class.getProtectionDomain();
            CodeSource cs = pd.getCodeSource();
            URL url = cs.getLocation();
            // Open a JarURLConnection to the jar file's URL
            if(url.getProtocol().equals("file")) {
                // If the URL is a file URL, convert it to a jar URL if it is actually a jar file
                String path = url.getPath();
                if(path.endsWith(".jar")) {
                    url = new URL("jar:file:" + path + "!/");
                } else {
                    throw new IllegalStateException(String.format("Warning: %s is not a jar file\n", path));
                }
            }
            JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
            // Get the Manifest from the JarURLConnection
            Manifest mf = jarConnection.getManifest();
            // Read the attributes from the Manifest
            Attributes attrs = mf.getMainAttributes();
            if(attrs.containsKey(Attributes.Name.IMPLEMENTATION_VERSION)) {
                number = attrs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }
            if(attrs.containsKey(Attributes.Name.IMPLEMENTATION_VENDOR)) {
                buildUser = attrs.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);
            }
            if(attrs.containsKey(IMPLEMENTATION_BUILD_OS)) {
                buildOS = attrs.getValue(IMPLEMENTATION_BUILD_OS);
            }
            if(attrs.containsKey(IMPLEMENTATION_BUILD_TIME)) {
                buildTime = attrs.getValue(IMPLEMENTATION_BUILD_TIME);
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
	    
