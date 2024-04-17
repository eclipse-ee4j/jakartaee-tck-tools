/*
 * Copyright (c) 2023,2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package com.sun.tdk.signaturetest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Validate that the version information in the jar matches the expected maven build values.
 */
public class VersionIT {
    @Test
    public void testVersionDefaults() throws IOException {
        // Load the generated git.properties file for the maven build timestamp and project version
        URL versionInfo = VersionIT.class.getResource("/META-INF/git.properties");
        InputStream is = versionInfo.openStream();
        assertNotNull("/META-INF/git.properties not found", is);
        Properties props = new Properties();
        try (is){
            props.load(versionInfo.openStream());
        }

        String number = props.getProperty("git.build.version");
        String buildTime = props.getProperty("git.commit.id.full");
        String buildUser = props.getProperty("git.build.user.name");
        String buildOS = System.getProperty("os.name") + " " + System.getProperty("os.version");

        assertEquals(number, Version.Number);
        assertEquals(buildOS, Version.build_os);
        assertEquals(buildUser, Version.build_user);
        assertEquals(buildTime, Version.build_time);
    }
}
