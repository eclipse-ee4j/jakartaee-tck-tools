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


package com.sun.tdk.signaturetest.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * This is formatter for java.util.logging's ConsoleHandler
 * prints file name and line number where log occuresd
 * Can be turned on in different ways, for example via property file:
 * -Djava.util.logging.config.file=/home/ersh/wrk/st/trunk_prj/logging.properties
 * where logging.properties contains
 * --------------------------------
 * handlers= java.util.logging.ConsoleHandler
 * java.util.logging.ConsoleHandler.formatter = com.sun.tdk.signaturetest.util.LogFormatter
 * --------------------------------
 *
 * @author Mikhail Ershov
 */
public class LogFormatter extends Formatter {
    public String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();
        sb.append(record.getLevel().getLocalizedName());
        sb.append(':');
        String s = formatMessage(record);
        if (s.indexOf("append") >= 0)
            sb.append(s);
        else
            return "";

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {
            }
        }
        StackTraceElement se = findCaller();
        if (se != null) {
            sb.append(" . . . . . .  . . . . . . . . . . . . . . . . . . . . . . . . ");
            sb.append(se.toString());
        }
        sb.append('\n');
        return sb.toString();
    }


    private StackTraceElement findCaller() {
        StackTraceElement stack[] = (new Throwable()).getStackTrace();
        int ix = 0;
        while (ix < stack.length) {
            StackTraceElement frame = stack[ix];
            String cname = frame.getClassName();
            if (cname.equals("java.util.logging.Logger")) {
                break;
            }
            ix++;
        }
        while (ix < stack.length) {
            StackTraceElement frame = stack[ix];
            String cname = frame.getClassName();
            if (!cname.equals("java.util.logging.Logger")) {
                return frame;
            }
            ix++;
        }
        return null;
    }
}
