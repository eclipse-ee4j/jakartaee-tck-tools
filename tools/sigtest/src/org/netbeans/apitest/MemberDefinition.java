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

import java.util.Vector;

/** This is class for analysis of the member definitions. **/
final class MemberDefinition implements SignatureConstants {
    static final int PRIVATE   = 0;
    static final int DEFAULT   = 1;
    static final int PROTECTED = 2;
    static final int PUBLIC    = 3;

    /** words from definition. **/
    Vector definitions;
    /** original member definition. **/
    String stringDefinition;
    /** name of the enclosing class. **/
    String name;

    /** creates MemberDefinition for given definition and enclosing class.
     *  @param name name of the enclosing class
     *  @param def member definition. **/
    MemberDefinition(String name, String def) {
        definitions = new Vector();
	int fromIndex;
	for (fromIndex = 0; def.indexOf(' ', fromIndex) >= 0;) {
	    int pos = def.indexOf(' ', fromIndex);
	    definitions.addElement(def.substring(fromIndex, pos));
	    fromIndex = pos + 1;
	}
	if (fromIndex < def.length())
	    definitions.addElement(def.substring(fromIndex));
        this.stringDefinition = def;
        this.name = name;
    }

    /** Returns member signature with local name **/
    String getShortSignature() {
        String sign = getSignature();
        int pos = sign.lastIndexOf('(');
        pos = (pos < 0) ? (sign.length() - 1) : (pos - 1);
        pos = Math.max(sign.lastIndexOf('.', pos), 
                       Math.max(sign.lastIndexOf('$', pos), 
                                sign.lastIndexOf(' ', pos)));
        return sign.substring(pos + 1);
    }

    /** Returns qualified name of the class which declares member. **/
    String getDeclaringClass() {
        String sign = getSignature();
        int pos = sign.lastIndexOf('(');
        pos = (pos < 0) ? (sign.length() - 1) : (pos - 1);
        pos = Math.max(sign.lastIndexOf('.', pos), sign.lastIndexOf('$', pos));
        return sign.substring(0, pos);
    }   
            

    /** Returns type of the fields or return type of the methods. **/
    String getType() {
        if (stringDefinition.startsWith(METHOD) ||
            stringDefinition.startsWith(FIELD)) {
            int pos = definitions.lastIndexOf("throws");
            if (pos >= 0) 
                return (String)definitions.elementAt(pos - 2);
            else
                return (String)definitions.elementAt(definitions.size() - 2);
        } else {
            return null;
        }
    }

    /** Returns int code of the access modifier. **/
    int getAccesModifier() {
        if (definitions.contains("private"))
            return PRIVATE;
        if (definitions.contains("protected"))
            return PROTECTED;
        if (definitions.contains("public"))
            return PUBLIC;
        return DEFAULT;
    }

    /** Returns member signature with qualified name **/
    private String getSignature() {
        int pos = definitions.indexOf("throws");
        pos = (pos < 0) ? definitions.size() : pos;
        return (String)definitions.elementAt(pos - 1);
    }
}
