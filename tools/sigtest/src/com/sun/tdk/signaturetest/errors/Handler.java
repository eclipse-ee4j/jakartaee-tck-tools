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

import com.sun.tdk.signaturetest.errors.ErrorFormatter.Message;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.core.ClassHierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.PatternSyntaxException;
import java.util.logging.Level;



/**
 * @author Sergey Glazyrin
 * @author Mikhail Ershov
 */
abstract class Handler {

    private Handler next;

    // Get the level specifying which messages will be processed by this Handler.
    // Message levels lower than this level will be discarded.
    private Level level = Level.SEVERE;

    public Handler setNext(Handler h) {
        next = h;
        return this;
    }

    public void process(List l, Chain ch) {
        if (acceptMessageList(l)) {
            writeMessage(l, ch);
        }
        if (next != null) {
            next.process(l, ch);
        }
    }


    /*
    *  First filtering method. By default we don't process added/missed annotation
    *  messages and "short" lists
    *  handler which have to process such cases have to override this method.
    */
    boolean acceptMessageList(List l) {
        if (l.size() < 2) {
            return false;
        }
        Message e1 = (Message) l.get(0);
        Message e2 = (Message) l.get(1);

        return !(isAnnotationMessage(e1) || isAnnotationMessage(e2));

    }

    protected abstract void writeMessage(List l, Chain ch);

    boolean isAnnotationMessage(Message m) {
        return (m.messageType == MessageType.ADD_ANNO ||
                m.messageType == MessageType.MISS_ANNO);
    }

    protected static final ArrayList EMPTY_ARRAY_LIST = new ArrayList();

    protected static ArrayList stringToArrayList(String source, String delimiter) {
        if ((source == null) || source.length() == 0) {
            return EMPTY_ARRAY_LIST;
        }

        String[] strA;
        ArrayList result = new ArrayList();
        try {
            strA = source.split(delimiter);
        } catch (PatternSyntaxException e) {
            result.add(source);
            return result;
        }
        for (int i = 0; i < strA.length; i++) {
            result.add(strA[i]);
        }
        return result;
    }

    protected boolean canBeSubclassed(String className, ClassHierarchy clHier) {
        try {
            ClassDescription cd = clHier.load(className);
            ConstructorDescr[] constrs = cd.getDeclaredConstructors();
            if (cd.isFinal() || constrs == null || constrs.length == 0) {
                return false;
            }
            for (int i = 0; i < constrs.length; i++) {
                if (constrs[i].isPublic() || constrs[i].isProtected()) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
        return false;

    }

    protected Level getLevel() {
        return level;
    }

    protected void setLevel(Level level) {
        this.level = level;
    }

    protected void setMessageLevel(Message m) {
        m.setLevel(level);
    }
}
abstract class PairedHandler extends Handler {

    protected Message me1;
    protected Message me2;

    protected MemberDescription m1;
    protected MemberDescription m2;

    protected Message newM;

    final protected void writeMessage(List l, Chain ch) {

        init(l);

        newM = new Message(MessageType.CHNG_CLASSES_MEMBERS, m1.getDeclaringClassName(), "", "", m1);

        if (proc()) {
            ch.setMessageProcessed(me1);
            ch.setMessageProcessed(me2);
            ch.addMessage(newM);
        }

    }

    protected void init(List l) {
        me1 = (Message) l.get(0);
        me2 = (Message) l.get(1);

        m1 = me1.errorObject;
        m2 = me2.errorObject;
    }

    protected void addDef(String def) {
        newM.definition += def + "\n";
    }

    abstract protected boolean proc();

}



class ModifiersHandler extends PairedHandler {
    protected boolean proc() {

        Collection c1 = Handler.stringToArrayList(Modifier.toString(m1.getMemberType(), m1.getModifiers(), true), " ");
        Collection c2 = Handler.stringToArrayList(Modifier.toString(m2.getMemberType(), m2.getModifiers(), true), " ");

        if (!c1.equals(c2)) {
            Collection c3 = new ArrayList(c2);
            c2.removeAll(c1);
            c1.removeAll(c3);

            if (c1.size() != 0) {
                addDef("    - " + c1.toString());
            }
            if (c2.size() != 0) {
                addDef("    + " + c2.toString());
            }
            return true;
        }
        return false;
    }
}

class ReturnTypeHandler extends PairedHandler {
    protected boolean proc() {
        String t1 = m1.getType();
        String t2 = m2.getType();

        if (t1 != null && !t1.equals(t2)) {
            if (!t1.equals("")) {
                addDef("    - type: " + t1);
            }
            if ((t2 != null) && (!t2.equals(""))) {
                addDef("    + type: " + t2);
            }
            return true;
        }
        return false;
    }
}

class ConstantValueHandler extends PairedHandler {

    boolean acceptMessageList(List l) {
        if (l.size() >= 2) {
            MemberDescription m1 = ((Message) l.get(0)).errorObject;
            MemberDescription m2 = ((Message) l.get(1)).errorObject;

            if (m1 instanceof FieldDescr && m2 instanceof FieldDescr) {
                FieldDescr f1 = (FieldDescr) m1;
                FieldDescr f2 = (FieldDescr) m2;

                return f1.getConstantValue() != null || f2.getConstantValue() != null;
            }
        }
        return false;
    }

    protected boolean proc() {

        FieldDescr f1 = (FieldDescr) m1;
        FieldDescr f2 = (FieldDescr) m2;

        String v1 = f1.getConstantValue() == null ? "" : f1.getConstantValue() ;
        String v2 = f2.getConstantValue() == null ? "" : f2.getConstantValue();
        if (!v1.equals(v2)) {
            if (!"".equals(v1))
                addDef("    - value: " + v1);
            if (!"".equals(v2))
                addDef("    + value: " + v2);

            return true;
        }
        return false;
    }
}


class TypeParametersHandler extends PairedHandler {

    protected boolean proc() {

        Collection c1 = Handler.stringToArrayList(trimTypeParameter(m1.getTypeParameters()), ", ");
        Collection c2 = Handler.stringToArrayList(trimTypeParameter(m2.getTypeParameters()), ", ");

        if (!c1.equals(c2)) {
            Collection c3 = new ArrayList(c2);
            c2.removeAll(c1);
            c1.removeAll(c3);

            if ((c1 != null) && (c1.size() != 0)) {
                addDef("    - Type parameters: " + c1.toString().trim());
            }
            if ((c2 != null) && (c2.size() != 0)) {
                addDef("    + Type parameters: " + c2.toString().trim());
            }
            return true;
        }
        return false;
    }

    private String trimTypeParameter(String s) {
        if ((s == null) || s.length() == 0)
            return "";
        StringBuffer sb = new StringBuffer(s);

        if (sb.charAt(0) == '<') {
            sb.deleteCharAt(0);
        }

        if (sb.charAt(sb.length() - 1) == '>') {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString().trim();
    }
}

class ThrowsHandler extends PairedHandler {

    protected boolean proc() {

        Collection c1 = Handler.stringToArrayList(m1.getThrowables(), ",");
        Collection c2 = Handler.stringToArrayList(m2.getThrowables(), ",");

        if (c1 != null && !c1.equals(c2)) {

            Collection c3 = new ArrayList(c2);
            c2.removeAll(c1);
            c1.removeAll(c3);

            if (c1.size() != 0) {
                addDef("    - Throws: " + c1.toString());
            }
            if (c2.size() != 0) {
                addDef("    + Throws: " + c2.toString());
            }
            return true;
        }
        return false;
    }
}

class AnnotationHandler extends PairedHandler {

    protected boolean proc() {

        Collection c1 = annotationListToArrayList(m1.getAnnoList());
        Collection c2 = annotationListToArrayList(m2.getAnnoList());

        if (!c1.equals(c2)) {
            Collection c3 = new ArrayList(c2);
            c2.removeAll(c1);
            c1.removeAll(c3);

            if ((c1 != null) && (c1.size() != 0)) {
                addDef("    - Anno: " + c1.toString());
            }
            if ((c2 != null) && (c2.size() != 0)) {
                addDef("    + Anno: " + c2.toString());
            }
            return true;
        }
        return false;

    }

    private ArrayList annotationListToArrayList(AnnotationItem[] a) {
        if (a == null) {
            return EMPTY_ARRAY_LIST;
        }
        ArrayList result = new ArrayList();
        for (int i = 0; i < a.length; i++) {
            result.add(a[i].getName());
        }
        return result;
    }


}

