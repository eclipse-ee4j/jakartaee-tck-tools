/*
 * $Id: ErrorFormatter.java 4504 2008-03-13 16:12:22Z sg215604 $
 *
 * Copyright 1996-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tdk.signaturetest.errors;

import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.model.MemberType;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.io.PrintWriter;


/**
 * This is class formats error messages reported by <b>SignatureTest</b> or
 * <b>APIChangesTest</b>. This class prints messages using the ``plain'' format
 * without any sorting and grouping of error messages.
 *
 * @version 05/03/22
 * @author Maxim Sokolnikov
 * @author Mikhail Ershov
 */
public class ErrorFormatter {
    
    // this string is not used in output
    public static final String annoAdded = "Added Annotations",
            annoMissed = "Missed Annotations";



    /**
     * Output stream intended to print error messages.
     */
    PrintWriter out;

    /**
     * Number of added errors.
     */
    int size;

    /**
     * Assign new <b>ErrorFormatter</b> to the given <b>PrintWriter</b>.
     */
    public ErrorFormatter(PrintWriter out) {
        this.out = out;
    }


    /**
     * Print new error message, and increment errors counter.
     *
     */
    public void addError(MessageType kind, String className, MemberType type, String def, MemberDescription errorObject) {
        addError(kind, className, type, def, null, errorObject);
    }    

    
    public void addError(MessageType kind, String className, MemberType type, String def, String tail, MemberDescription errorObject) {
        out.println(createError(kind, className, type, def, tail, errorObject));
        if (!kind.isWarning()) size++;
    }


    /**
     * Create new error message.
     *
     */
    protected Message createError(MessageType kind, String className, MemberType type, String def, String tail, MemberDescription errorObject) {
        return new Message(kind, className, def, tail, errorObject);
    }

    /**
     * Print errors. This method is dummy, because no errors buffering is
     * implemented by this class. However, subclasses could overload this
     * method.
     */
    public void printErrors() {
    }

    /**
     * Return number of found errors.
     */
    public int getNumErrors() {
        return size;
    }
    
    /**
     * This class formats some error message reported by
     * <b>SignatureTest</b> or other similar tests.
     */
    public static class Message implements Comparable {

        /**
         * Message templates for different error types.
         *
         */
        
        public MemberDescription errorObject;

        /**
         * Name of the class affected by <code>this</code> error message.
         */
        public String className;

        /**
         * Class or class member affected by <code>this</code> error message.
         */
        public String definition;        

        /**
         * The tail to append to <code>this</code> error message.
         */
        public String tail;

        /**
         * Type of <code>this</code> error message.
         */
        public MessageType messageType;

        /**
         * Create new error message.
         *
         */

        public Message(MessageType error, String className, String definition, String tail, MemberDescription errorObject) {
            this.messageType = error;
            this.className = className;
            this.definition = (definition == null) ? "" : definition;
            this.tail = (tail == null) ? "" : tail;
            this.errorObject = errorObject;
            
        }


        /**
         * Compare <code>this</code> <b>Message</b> to the given
         * <b>Message</b> <code>ob</code>.
         * Messages ordering is the following: <br>
         * &nbsp; 1. Compare <code>errorType</code> fields as integers. <br>
         * &nbsp; 2. If <code>errorType</code> fields are equal, than compare
         * <code>className</code> fields. <br>
         * &nbsp; 3. If <code>className</code> fields also equals, than compare
         * <code>definition</code> fields.
         */
        public int compareTo(Object o) {
            Message ob = (Message) o;
            int comp = 0;
            if (ob.messageType == this.messageType) {
                comp = this.className.compareTo(ob.className);
                if (comp == 0)
                    comp = (getShortName(this.definition)).compareTo(getShortName(ob.definition));
	    }
            return comp;
        }

        /**
         * Cut ``<code>throws</code>'' clause out off the given member
         * description <code>def</code>.
         */
        public String getShortName(String def) {
            String retVal = def;
            int pos = def.lastIndexOf(" throws ");
            if (pos >= 0)
                retVal = def.substring(0, pos);
            return retVal.substring(retVal.lastIndexOf(' ') + 1);
        }

        /**
         * Return string representing <code>this</code> <b>Message</b>.
         */
        public String toString() {
            String retVal = messageType.getLocMessage();
            return retVal + " " + className + "\n    " + definition + tail;
        }
    }
}
