/*
 * $Id$
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

import com.sun.tdk.signaturetest.model.MemberType;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

public class MessageType implements Comparable {
    private String text;
    private int id;
    private boolean thisIsWarning;

    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(MessageType.class);

    public int compareTo(Object o) {
        if (o instanceof MessageType) {
            MessageType mt = (MessageType) o;
            if (id == mt.id) return 0;
            return (id - mt.id) > 0 ? 1 : -1 ;
        }
        return 0;
    }

    public boolean isWarning() {
        return thisIsWarning;
    }

    private MessageType(String key, int id) {
        this(key, id, false);
    }

    private MessageType(String key, int id, boolean isWarning) {
        this.text = key;
        this.id = id;
        this.thisIsWarning = isWarning;
    }

    public String toString() {
        return getLocMessage();
    }

    String getLocMessage() {
        return text;
    }

    final public static MessageType ERROR_UNKNOWN           = new MessageType(i18n.getString("ErrorFormatter.error.unknown"), -1);
    final public static MessageType MISS_CLASSES            = new MessageType(i18n.getString("ErrorFormatter.error.missing.classes"), 1);
    final public static MessageType MISS_SUPERCLASSES       = new MessageType(i18n.getString("ErrorFormatter.error.missing.superclasses"), 2);
    final public static MessageType MISS_FIELDS             = new MessageType(i18n.getString("ErrorFormatter.error.missing.field"), 3);
    final public static MessageType MISS_CONSTRUCTORS       = new MessageType(i18n.getString("ErrorFormatter.error.missing.construct"), 4);
    final public static MessageType MISS_METHS              = new MessageType(i18n.getString("ErrorFormatter.error.missing.methods"), 5);
    final public static MessageType ADD_CLASSES             = new MessageType(i18n.getString("ErrorFormatter.error.added.classes"), 7); // Added nested Classes or class definitions
    final public static MessageType ADD_SUPCLASSES          = new MessageType(i18n.getString("ErrorFormatter.error.added.superclasses"), 8); // Added Superclasses or Superinterfaces
    final public static MessageType ADD_FLD                 = new MessageType(i18n.getString("ErrorFormatter.error.added.field"), 9); // Added Fields
    final public static MessageType ADD_CONSTRUCTORS        = new MessageType(i18n.getString("ErrorFormatter.error.added.construct"), 10); // Added Constructors
    final public static MessageType ADD_METHS               = new MessageType(i18n.getString("ErrorFormatter.error.added.methods"), 11); // Added Methods
    final public static MessageType ERROR_LINKERR           = new MessageType(i18n.getString("ErrorFormatter.error.linkerror"), 18); // LinkageError
    final public static MessageType MISS_ANNO               = new MessageType(i18n.getString("ErrorFormatter.error.missinganno.defn"), 21); // 21 annoMissed
    final public static MessageType ADD_ANNO                = new MessageType(i18n.getString("ErrorFormatter.error.addedanno.defn"), 22); // 22 annoAdded
    final public static MessageType CHNG_SUPCLASSES_TOPOL   = new MessageType(i18n.getString("ErrorFormatter.error.changedsuperclasses.topol"), 23); // 23 "Topology of Interface Inheritance"

    public static MessageType getMissingMessageType(MemberType type) {
        if (type == MemberType.CLASS || type == MemberType.INNER) return MISS_CLASSES;
        if (type == MemberType.SUPERCLASS || type == MemberType.SUPERINTERFACE) return MISS_SUPERCLASSES;
        if (type == MemberType.FIELD) return MISS_FIELDS;
        if (type == MemberType.CONSTRUCTOR) return MISS_CONSTRUCTORS;
        if (type == MemberType.METHOD) return MISS_METHS;
        return ERROR_UNKNOWN;
    }

    public static MessageType getAddedMessageType(MemberType type) {
        if (type == MemberType.CLASS || type == MemberType.INNER) return ADD_CLASSES;
        if (type == MemberType.SUPERCLASS || type == MemberType.SUPERINTERFACE) return ADD_SUPCLASSES;
        if (type == MemberType.FIELD) return ADD_FLD;
        if (type == MemberType.CONSTRUCTOR) return ADD_CONSTRUCTORS;
        if (type == MemberType.METHOD) return ADD_METHS;
        return ERROR_UNKNOWN;
    }

}
