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
 *  APIChangesTest. This class returns messages in the sorted format.
 *  The messages are grouped by error type and alphabetized by class 
 *  then name **/
class APISortedErrorFormatter extends SortedErrorFormatter {

    /** Creates new APISortedErrorFormatter with given PrintWriter and headers.
     *  @param out the PrintWriter which prints error messages
     *  @param headers headers which heads group of the ErrorMessages
     *  with the same errorType. **/
    public APISortedErrorFormatter(PrintWriter out, String headers[]) {
        super(out);
        this.headers = headers;
        failedMessages = new Vector();
    }

    /** Creates new APISortedErrorFormatter with given PrintWriter.
     *  @param out the PrintWriter which prints error messages.**/
    public APISortedErrorFormatter(PrintWriter out) {
        super(out);
        failedMessages = new Vector();
        String temp [] = {
            "Missing Classes",
            "Missing Classes",
            "Missing Superclasses or Superinterfaces",
            "Missing Fields",
            "Missing Constructors",
            "Missing Methods",
            "Incompatible change: Class is not public in the new implementation",
            "Incompatible change: Class is not protected in the new implementation",
            "Incompatible change: Class is less accessible in the new implementation",
            "Incompatible change: Class is final in the new implementation",
            "Incompatible change: Class is abstract in the new implementation",
            "Incompatible change: Class is interface in the new implementation",
            "Incompatible change: Interface is class in the new implementation",
            "Incompatible change: Field is not public in the new implementation",
            "Incompatible change: Field is not protected in the new implementation",
            "Incompatible change: Field is less accessible in the new implementation",
            "Incompatible change: Field is final in the new implementation",
            "Incompatible change: Field is change type in the new implementation",
            "Incompatible change: Field is static in the new implementation",
            "Incompatible change: Field is not static in the new implementation",
            "Incompatible change: Field is volatile in the new implementation",
            "Incompatible change: Field is not volatile in the new implementation",
            "Incompatible change: Constructor is not public in the new implementation",
            "Incompatible change: Constructor is not protected in the new implementation",
            "Incompatible change: Constructor is less accessible in the new implementation",
            "Incompatible change: Method is not public in the new implementation",
            "Incompatible change: Method is not protected in the new implementation",
            "Incompatible change: Method is less accessible in the new implementation",
            "Incompatible change: Method is final in the new implementation",
            "Incompatible change: Method is change type in the new implementation",
            "Incompatible change: Method is static in the new implementation",
            "Incompatible change: Method is not static in the new implementation",
            "Incompatible change: Method is abstract in the new implementation",
            "LinkageError thrown during tracking of the definition"
        };
        headers = temp;
    }
        
    
    /** adds new error to the APISortedErrorFormatter.
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

    /** create new error
     *  @param errorType type of the error message
     *  @param className name of the class where is error is found
     *  @param def short error message
     *  @param tail the tail of the error message which will be added to
     *  the message in the plain format **/        
    protected ErrorMessage createError(String error, String className,
                                       String def, String tail) {
        ErrorMessage problem = super.createError(error, className, def,
                                                 tail);
        int errorType = problem.errorType;
        int errorSubtype = errorType;
	if ((error != null) && !error.equals("not found") && !error.equals("Missing")) {
            if ((errorType == 0) || (errorType == 1)) {
                if (error.equals("is not public"))
                    errorSubtype = 6;
                else if (error.equals("is not protected"))
                    errorSubtype = 7;
                else if (error.equals("less accessible"))
                    errorSubtype = 8;
                else if (error.equals("is final"))
                    errorSubtype = 9;
                else if (error.equals("is abstract"))
                    errorSubtype = 10;
                else if (error.equals("is interface"))
                    errorSubtype = 11;
                else if (error.equals("is not interface"))
                    errorSubtype = 12;
            } else if (errorType == 3) {
                if (error.equals("is not public"))
                    errorSubtype = 13;
                else if (error.equals("is not protected"))
                    errorSubtype = 14;
                else if (error.equals("less accessible"))
                    errorSubtype = 15;
                else if (error.equals("is final"))
                    errorSubtype = 16;
                else if (error.endsWith("type."))
                    errorSubtype = 17;
                else if (error.equals("is static"))
                    errorSubtype = 18;
                else if (error.equals("is not static"))
                    errorSubtype = 19;
                else if (error.equals("is volatile"))
                    errorSubtype = 20;
                else if (error.equals("is not volatile"))
                    errorSubtype = 21;
            } else if (errorType == 4) {
                if (error.equals("is not public"))
                    errorSubtype = 22;
                else if (error.equals("is not protected"))
                    errorSubtype = 23;
                else if (error.equals("less accessible"))
                    errorSubtype = 24;
            } else if (errorType == 5) {
                if (error.equals("is not public"))
                    errorSubtype = 25;
                else if (error.equals("is not protected"))
                    errorSubtype = 26;
                else if (error.equals("less accessible"))
                    errorSubtype = 27;
                else if (error.equals("is final"))
                    errorSubtype = 28;
                else if (error.startsWith(" return value of "))
                    errorSubtype = 29;
                else if (error.equals("is static"))
                    errorSubtype = 30;
                else if (error.equals("is not static"))
                    errorSubtype = 31;
                else if (error.equals("is abstract"))
                    errorSubtype = 32;
            } else if (errorType == 12) {
                errorSubtype = 33;
            } 
        }
        problem.errorType = errorSubtype;
        return problem;
    }
}
