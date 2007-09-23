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

import java.io.PrintWriter;
import java.util.Vector;

/** This is class for formatting error messages which are created by
 *  SignatureTest and APIChangesTest. This class returns messages in
 *  the sorted format. The messages are grouped by error type and
 *  alphabetized by class then name **/
class SortedErrorFormatter extends ErrorFormatter {
    /** the headers of the group with the same error type **/
    String[]  headers = {
        "Missing Classes",
        "Missing Class Definitions",
        "Missing Superclasses or Superinterfaces",
        "Missing Fields",
        "Missing Constructors",
        "Missing Methods",
        "Added Classes",
        "Added Class Definitions",
        "Added Superclasses or Superinterfaces",
        "Added Fields",
        "Added Constructors",
        "Added Methods",
        "LinkageError"
    };
    /** Vector which store ErrorMessage **/
    Vector failedMessages;
    /** the size of the tabulation. **/
    int tabSize = 20;

    /** Creates new SortedErrorFormatter with given PrintWriter and headers.
     *  @param out the PrintWriter which prints error messages
     *  @param headers headers which heads group of the ErrorMessages
     *  with the same errorType. **/
    public SortedErrorFormatter(PrintWriter out, String headers[]) {
        super(out);
        this.headers = headers;
        failedMessages = new Vector();
    }

    /** Creates new SortedErrorFormatter with given PrintWriter.
     *  @param out the PrintWriter which prints error messages.**/
    public SortedErrorFormatter(PrintWriter out) {
        super(out);
        failedMessages = new Vector();
    }
        
    /** adds new error to the SortedErrorFormatter.
     *  @param errorType type of the error message
     *  @param className name of the class where is error is found
     *  @param def short error message
     *  @param tail the tail of the error message which will be added to
     *  the message in the plain format **/
    public void addError(String errorTyp, String className, String def,
                         String tail) {
        failedMessages.addElement(createError(errorTyp, className, def, tail));
        size++;
    }

    /** Returns number of the error messages and prints errors. **/
    public int printErrors() {
	int currentErrorType = 0;
        boolean hasHeader = false;
	while (!failedMessages.isEmpty()) {
	    int pos = 0;
            Vector currentTypeErrors = new Vector();
	    for (int i = 0; i < failedMessages.size(); i++) {
                if (i < 0)
                    break;
		ErrorMessage temp = (ErrorMessage)failedMessages.elementAt(i);
                if (temp.errorType == currentErrorType) {
                    currentTypeErrors.addElement(temp);
                    failedMessages.removeElementAt(i--);
                }
            }
            if (currentTypeErrors.isEmpty()) {
                currentErrorType++;
                continue;
            }
            if ((headers != null) && (currentErrorType < headers.length)) {
                hasHeader = true;
                out.println(headers[currentErrorType] + "\n" +
                            space('-', headers[currentErrorType].length()) + "\n");
            } else {
                hasHeader = false;
            }
            while (!currentTypeErrors.isEmpty()) {
                ErrorMessage min = (ErrorMessage)currentTypeErrors.elementAt(0);
                pos = 0;
                for (int i = 0; i < currentTypeErrors.size(); i++) {
                    ErrorMessage temp = (ErrorMessage)currentTypeErrors.elementAt(i);
                    if (min.compareTo(temp) > 0) {
                        min = temp;
                        pos = i;
                    }
                }
                currentTypeErrors.removeElementAt(pos);
                if (hasHeader) {
                    if (min.definition.equals(""))
                        out.println(min.className);
                    else {
                        int currentTab = (min.className.length() + 1) / tabSize;
                        if ((min.className.length() + 1) % tabSize != 0)
                            currentTab++;
                        currentTab = currentTab * tabSize;
                        out.println(min.className + ":" +
                                    space(' ', currentTab -
                                          min.className.length() - 1) +
                                    toString(min.definition));
                    }
                } else {
                    out.println(min);
                }
            }
            out.println("");
            currentErrorType++;
	}
        return size;
    }

    /** returns String which is sequence of the given char
     *  @param c given char
     *  @param len length of the returned String **/
    protected static String space(char c, int len) {
        char buff[] = new char[len];
        for (int i = 0; i < len; i++)
            buff[i] = c;
        return new String(buff);
    }
}
