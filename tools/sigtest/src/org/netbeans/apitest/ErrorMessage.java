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

/** This class represents Error message created
  *  by APIChanges or SignatureTest **/
public class ErrorMessage {
    /** messages for current error message. If for current error type this
     *  message is not exist, than ErrorFormatter.messages is used **/
    String messages[];
    /** name of the class where is error is found **/
    String className;
    /** failed definition **/
    String definition;
    /** the tail of the error message which will be added to the message
     *  in the plain format **/
    String tail;
    /** type of the error message **/
    int errorType;
    /** creates new error message 
     *  @param errorType type of the error message
     *  @param className name of the class where is error is found
     *  @param definition failed definition
     *  @param tail the tail of the error message which will be added to
     *  the message in the plain format **/
    public ErrorMessage(int errorType, String className, String definition,
                        String tail) {
        this.errorType = errorType;
        this.className = className;
        this.definition = (definition == null) ? "" : definition ;
        this.tail = (tail == null) ? "" : tail ;
        
    }

    /** set the new messages
     *  @param messages the new messages **/
    public void setMessages(String messages []) {
        this.messages = messages;
    }


    /** Compares this ErrorMessage with the given ErrorMessage for order.
     *  The ordering rules are following:
     *  <p> 1. compare errorType as integer
     *  <p> 2. if errorTypes are equal, than compare classNames alphabetically.
     *  <p> 3. if ClassNames are equals than compare definition <p>
     *  Returns a negative integer, zero, or a positive integer as this
     *  ErrorMessage is less than, equal to, or greater than the given
     *  ErrorMessage. **/
    public int compareTo(ErrorMessage ob) {
        if (ob.errorType == this.errorType) {
            if (this.className.equals(ob.className))
                return (getShortName(this.definition)).compareTo(
                    getShortName(ob.definition));
            else 
                return this.className.compareTo(ob.className);
        } else {
            return this.errorType - ob.errorType;
        }
    }

    /** Returns short name for missing or added member
     *  @param def definition of the given member **/
    public String getShortName(String def) {
        String retVal;
        int pos = def.lastIndexOf(" throws ");
        if (pos >= 0)
            retVal = def.substring(0, pos);
        else
            retVal = def;
        return retVal.substring(retVal.lastIndexOf(' ') + 1);
    }

    /** returns String representation of current ErrorMessage **/
    public String toString() {
        return messages[errorType] + className + 
            "\n    " + ErrorFormatter.toString(definition) + tail;
    }
}



