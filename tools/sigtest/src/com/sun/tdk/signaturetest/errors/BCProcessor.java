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


package com.sun.tdk.signaturetest.errors;

import com.sun.tdk.signaturetest.core.ClassHierarchy;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * This is backward compatibility postprocessor.
 *
 * @author Mikhail Ershov
 */
public class BCProcessor extends HumanErrorFormatter {
    // is it bin mode?
    private boolean bin;
    private boolean extensibleInterfaces;
    private ClassHierarchy clHier, sfHier;
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(BCProcessor.class);

    /**
     * Assign the given <b>PrintWriter</b> to print error messages.
     */
    public BCProcessor(PrintWriter out, boolean isv, boolean binMode, ClassHierarchy classHierarchy, ClassHierarchy sigFileClassHierarchy, Level l, boolean extensibleInterfaces) {
        super(out, isv, l);
        this.bin = binMode;
        this.clHier = classHierarchy;
        this.sfHier = sigFileClassHierarchy;
        this.extensibleInterfaces = extensibleInterfaces;
    }

    protected Handler constructHandlerChain() {

        Handler[] handlers = {
                new Rule1_1(), new Rule1_2(), new Rule1_3(), new Rule2_1(),
                new Rule2_2(), new Rule2_3(), new Rule2_4(), new Rule2_5(),
                new Rule2_7(), new Rule2_8(), new Rule3_1(), new Rule3_3(), new Rule3_4(),
                new Rule3_6(), new Rule3_8(), new Rule3_10(), new Rule3_11(),
                new Rule3_12(), new Rule4_1(), new Rule4_2(), new Rule4_6(),
                new Rule4_7(), new Rule4_8(), new Rule5_1_2(), new Rule5_2_3(),
                new Rule5_4(), new Rule5_6(), new Rule5_12(), new Rule5_14(),
                new Terminator()
        };

        // link them
        for (int i = 0; i < handlers.length - 1; i++) {
            handlers[i].setNext(handlers[i + 1]);
        }

        return handlers[0];
    }


    protected void outProcessedErrors() {
        String cl = "";

        for (int i = 0; i < failedMessages.size(); i++) {
            Message current = (Message) failedMessages.get(i);
            if (current == null)
                continue;

            String ccl = current.className;

            if (!cl.equals(ccl)) {
                cl = ccl;
                out.println("\nClass " + cl);
            }

            String prefix = "";
            if (current.getLevel().intValue() < Level.SEVERE.intValue())
                prefix = "warn: ";
            out.println("  " + prefix + current.definition + " : " + current.errorObject);
        }
        if (failedMessages.size() > 0)
            out.println("");
    }

    /*
    API (public or protected) type (class, interface, enum, annotation type) or member
    (method, field) added
    */
    class Rule1_1 extends Handler {

        Rule1_1() {
            super();
            setLevel(Level.WARNING);
        }

        private ErrorFormatter.Message m;
        private Handler r5 = new Rule5_2_3();

        boolean acceptMessageList(List l) {

            if (l.size() != 1) {
                return false;
            }

            m = (ErrorFormatter.Message) l.get(0);

            // annotation fields are exceptions.
            // checked by the others rules depends on their default value
            if (m.messageType == MessageType.ADD_METHS && m.errorObject instanceof MethodDescr) {
                MethodDescr md = (MethodDescr) m.errorObject;
                try {
                    String dcn = md.getDeclaringClassName();
                    if (clHier.isInterface(dcn) && clHier.isAnnotation(dcn))
                        return false;
                } catch (ClassNotFoundException e) {
                    return false;
                }
            }

            if (r5.acceptMessageList(l))
                return false;

            // ignore added constructors - this another rules
            if (m.messageType == MessageType.ADD_CONSTRUCTORS) {
                return false;
            }

            return (m.messageType == MessageType.ADD_CLASSES) ||
                    (m.messageType == MessageType.ADD_METHS);
        }


        protected void writeMessage(List l, Chain ch) {
            if (!bin) {
                m.definition = i18n.getString("BCProcessor.error.1_1"); //"W1.1 - API type added";
                setMessageLevel(m);
                ch.addMessage(m);
            }

        }
    }


    /*
    API (public or protected) type (class, interface, enum, annotation type) or member
    (method, field) removed
    */
    class Rule1_2 extends Handler {

        private ErrorFormatter.Message m;

        boolean acceptMessageList(List l) {

            if (l.size() != 1) {
                return false;
            }

            m = (ErrorFormatter.Message) l.get(0);

            boolean retval = (m.messageType == MessageType.MISS_CLASSES) ||
                    (m.messageType == MessageType.MISS_FIELDS) ||
                    (m.messageType == MessageType.MISS_METHS);

            if (retval && m.errorObject.isProtected()) {
                if (!canBeSubclassed(m.className, sfHier)) {
                    retval = false;
                }
            }
            return retval;
        }

        protected void writeMessage(List l, Chain ch) {
            m.definition = i18n.getString("BCProcessor.error.1_2");  //"E1.2 - API type removed";
            setMessageLevel(m);
            ch.addMessage(m);
        }
    }


    /*
    1.3 Narrowing type or member visibility - from public to non-public,
    from protected to package or private (actually it means type or member disappearing)

    Breaks - Both

    UPD 28.08.2008 It looks like this is safety change visibility from protected
    to invisible for final classes

    UPD 16.09.2009 For abstract classes constructors can change an access
    from public to protected

    */
    static class Rule1_3 extends PairedHandler {

        protected boolean proc() {
            boolean problem = m1.hasModifier(Modifier.PUBLIC) && !m2.hasModifier(Modifier.PUBLIC);
            if (problem) {

                newM.definition = i18n.getString("BCProcessor.error.1_3"); // "E1.3 - Narrowing type or member visibility";
                setMessageLevel(newM);
                return true;
            }
            return false;
        }
    }

    /*
    2.1 Interfaces but NOT annotation types - Adding API method
    Breaks - both
    */
    class Rule2_1 extends Handler {
        private ErrorFormatter.Message m;

        boolean acceptMessageList(List l) {

            if (l.size() != 1) {
                return false;
            }

            m = (ErrorFormatter.Message) l.get(0);
            if (m.messageType == MessageType.ADD_METHS && m.errorObject instanceof MethodDescr) {
                MethodDescr md = (MethodDescr) m.errorObject;
                try {
                    String dcn = md.getDeclaringClassName();
                    if (!extensibleInterfaces && clHier.isInterface(dcn) && !clHier.isAnnotation(dcn)) {
                        if (!clHier.isInterface(m.className) && !canBeSubclassed(m.className, clHier)) {
                            return false;
                        }
                        return true;
                    }
                } catch (ClassNotFoundException e) {
                }
            } 
            return false;
        }

        protected void writeMessage(List l, Chain ch) {
            m.definition = i18n.getString("BCProcessor.error.2_1"); //"E2.1 - Interface method added";
            setMessageLevel(m);
            ch.addMessage(m);
        }
    }


    /*
    2.2 Interfaces and annotation types - Adding API field
    Breaks - both
    */
    class Rule2_2 extends Handler {
        private ErrorFormatter.Message m;

        boolean acceptMessageList(List l) {

            if (l.size() != 1) {
                return false;
            }

            m = (ErrorFormatter.Message) l.get(0);
            if (m.messageType == MessageType.ADD_FLD && m.errorObject instanceof FieldDescr) {
                FieldDescr md = (FieldDescr) m.errorObject;
                // see 2.3 for other case
                if (m.className.equals(md.getDeclaringClassName()))
                    try {
                        return clHier.isAnnotation(md.getDeclaringClassName());
                    } catch (ClassNotFoundException e) {
                        return false;
                    }
            }
            return false;
        }

        protected void writeMessage(List l, Chain ch) {
            m.definition = i18n.getString("BCProcessor.error.2_2"); //"E2.2 - Interface field added";
            setMessageLevel(m);
            ch.addMessage(m);
        }
    }


    /*
    2.3 Expanding set of superinterfaces (direct or indirect) with constants
    breaks - source
    */
    class Rule2_3 extends Handler {
        private ErrorFormatter.Message m;

        boolean acceptMessageList(List l) {
            if (l.size() != 1) {
                return false;
            }

            if (bin) return false;

            m = (ErrorFormatter.Message) l.get(0);
            if (m.messageType == MessageType.ADD_FLD && m.errorObject instanceof FieldDescr) {
                FieldDescr md = (FieldDescr) m.errorObject;
                // see 2.2 for other case
                if (!m.className.equals(md.getDeclaringClassName()))
                    try {
                        return clHier.isInterface(md.getDeclaringClassName());
                    } catch (ClassNotFoundException e) {
                        return false;
                    }
            }
            return false;
        }

        protected void writeMessage(List l, Chain ch) {
            m.definition = i18n.getString("BCProcessor.error.2_3");  //"W2.3 - Adding interfaces with constants ";
            setMessageLevel(m);
            ch.addMessage(m);
        }
    }


    /*
    2.4 Contracting superinterface set (direct or inherited)
    breaks - both
    */
    static class Rule2_4 extends Handler {
        private ErrorFormatter.Message m;

        boolean acceptMessageList(List l) {
            if (l.size() != 1) {
                return false;
            }

            m = (ErrorFormatter.Message) l.get(0);
            return m.messageType == MessageType.MISS_SUPERCLASSES && m.errorObject instanceof SuperInterface;
        }

        protected void writeMessage(List l, Chain ch) {
            m.definition = i18n.getString("BCProcessor.error.2_4"); //"E2.4 - Contracting superinterface set (direct or inherited) ";
            setMessageLevel(m);
            ch.addMessage(m);
        }
    }

    class Rule2_5 extends Handler {
        private ErrorFormatter.Message m;

        boolean acceptMessageList(List l) {

            if (l.size() != 1 || bin) {
                return false;
            }

            m = (ErrorFormatter.Message) l.get(0);
            if (m.messageType == MessageType.ADD_METHS && m.errorObject instanceof MethodDescr) {
                MethodDescr md = (MethodDescr) m.errorObject;
                try {
                    String dcn = md.getDeclaringClassName();
                    return clHier.isInterface(dcn) &&
                            clHier.isAnnotation(dcn) &&
                            !md.hasModifier(Modifier.HASDEFAULT);
                } catch (ClassNotFoundException e) {
                    return false;
                }

            } else
                return false;
        }

        protected void writeMessage(List l, Chain ch) {
            m.definition = i18n.getString("BCProcessor.error.2_5"); //"E2.5 - Adding member without default value to annotation type";
            setMessageLevel(m);
            ch.addMessage(m);
        }
    }

    class Rule2_7 extends Handler {
        private ErrorFormatter.Message m;

        boolean acceptMessageList(List l) {

            if (l.size() != 1) {
                return false;
            }

            m = (ErrorFormatter.Message) l.get(0);
            if (m.messageType == MessageType.MISS_METHS && m.errorObject instanceof MethodDescr) {
                MethodDescr md = (MethodDescr) m.errorObject;
                try {
                    String dcn = md.getDeclaringClassName();
                    return clHier.isInterface(dcn) &&
                            clHier.isAnnotation(dcn);
                } catch (ClassNotFoundException e) {
                    return false;
                }

            } else
                return false;
        }

        protected void writeMessage(List l, Chain ch) {
            m.definition = i18n.getString("BCProcessor.error.2_7"); //"E2.7 - Removing member from annotation type";
            setMessageLevel(m);
            ch.addMessage(m);
        }
    }

    static class Rule2_8 extends MethodPairedHandler {
        protected boolean proc() {
            if (me1.messageType == MessageType.MISS_METHS && me2.messageType == MessageType.ADD_METHS) {
                if (meth1.getSignature().equals(meth2.getSignature())
                        && meth1.hasModifier(Modifier.HASDEFAULT) && !meth2.hasModifier(Modifier.HASDEFAULT)) {
                    newM.definition = i18n.getString("BCProcessor.error.2_8"); //  Removing default value from member of annotation type
                    return true;
                }
            }
            return false;
        }
    }

    class Rule3_1 extends MethodPairedHandler {
        protected boolean proc() {
            if (!meth1.getType().equals(meth2.getType())
                    || !meth1.getSignature().equals(meth2.getSignature())) {

                if (!isAssignableTo(meth1.getType(), meth2.getType(), sfHier)) {
                    newM.definition = i18n.getString("BCProcessor.error.3_1"); //"E3.1 - Changing method signature and/or return type";
                    return true;
                }
            }
            return false;
        }
    }

    class Rule3_3 extends MethodPairedHandler {

        boolean acceptMessageList(List l) {
            return !bin && super.acceptMessageList(l);
        }

        protected boolean proc() {
            boolean problem = meth1.hasModifier(Modifier.VARARGS) && !meth2.hasModifier(Modifier.VARARGS);
            if (problem) {
                newM.definition = i18n.getString("BCProcessor.error.3_3"); // "E3.3 - Changing parameter from arity T... to array type T[]";
                setMessageLevel(newM);
                return true;
            }
            return false;
        }
    }

    class Rule3_4 extends PairedHandler {
        boolean acceptMessageList(List l) {
            return super.acceptMessageList(l) && !bin;
        }

        protected boolean proc() {
            Collection c1 = Handler.stringToArrayList(m1.getThrowables(), ",");
            Collection c2 = Handler.stringToArrayList(m2.getThrowables(), ",");

            if (!c1.equals(c2)) {
                newM.definition = i18n.getString("BCProcessor.error.3_4"); //"E3.4 - Changing normalized throw list";
                setMessageLevel(newM);
                return true;
            }
            return false;
        }
    }

    class Rule3_6 extends MethodPairedHandler {

        Rule3_6() {
            super();
            setLevel(Level.WARNING);
        }

        protected boolean proc() {
            boolean problem = meth1.isProtected() && meth2.isPublic() && !meth1.isFinal();
            if (problem && !bin && canBeSubclassed(me1.className, sfHier)) {
                newM.definition = i18n.getString("BCProcessor.error.3_6"); //"W3.6 - Increase access, from protected to public if the class is subclassable";
                setMessageLevel(newM);
                return true;
            }
            return false;
        }
    }

    class Rule3_8 extends MethodPairedHandler {
        protected boolean proc() {
            if (!meth1.isAbstract() && meth2.isAbstract()) {
                if (canBeSubclassed(me1.className, sfHier)) {
                    newM.definition = i18n.getString("BCProcessor.error.3_8"); //"E3.8 - Changing method from non-abstract to abstract";
                    setMessageLevel(newM);
                    return true;
                }
            }
            return false;
        }
    }

    class Rule3_10 extends MethodPairedHandler {
        protected boolean proc() {
            if (!meth1.isFinal() && meth2.isFinal()) {
                if (canBeSubclassed(me1.className, sfHier)) {
                    newM.definition = i18n.getString("BCProcessor.error.3_10"); //"E3.10 - Changing method from non-final to final";
                    setMessageLevel(newM);
                    return true;
                }
            }
            return false;
        }
    }

    static class Rule3_11 extends MethodPairedHandler {
        protected boolean proc() {
            if (meth1.isStatic() && !meth2.isStatic()) {
                newM.definition = i18n.getString("BCProcessor.error.3_11"); // "E3.11 - Changing method from static to non-static";
                setMessageLevel(newM);
                return true;
            }
            return false;
        }
    }

    static class Rule3_12 extends MethodPairedHandler {
        protected boolean proc() {
            if (!meth1.isStatic() && meth2.isStatic()) {
                newM.definition = i18n.getString("BCProcessor.error.3_12"); //"E3.12 - Changing method from non-static to static";
                setMessageLevel(newM);
                return true;
            }
            return false;
        }
    }

    /*
     4.1 Interface and class fields - Changing type
     Breaks - Both
    */
    static class Rule4_1 extends PairedHandler {
        protected boolean proc() {
            if (m1 instanceof FieldDescr && m2 instanceof FieldDescr) {
                String t1 = m1.getType();
                String t2 = m2.getType();
                if (!t1.equals(t2)) {
                    newM.definition = i18n.getString("BCProcessor.error.4_1"); //"E4.1 - Changing field type";
                    setMessageLevel(newM);
                    return true;
                }
            }
            return false;
        }
    }

    static class Rule4_2 extends FieldPairedHandler {
        Handler r46 = new Rule4_6();
        Handler r47 = new Rule4_7();
        Handler r48 = new Rule4_8();

        Rule4_2() {
            super();
            setLevel(Level.WARNING);
        }

        boolean acceptMessageList(List l) {
            if (!super.acceptMessageList(l)
                    || r46.acceptMessageList(l)
                    || r47.acceptMessageList(l)
                    || r48.acceptMessageList(l))
                return false;
            return f1.getConstantValue() != null || f2.getConstantValue() != null;
        }

        protected boolean proc() {
            if (!conValEquals(f1, f2)) {
                newM.definition = i18n.getString("BCProcessor.error.4_2"); //"W4.2 - Changing field constant value";
                setMessageLevel(newM);
                return true;
            }
            return false;
        }

        private boolean conValEquals(FieldDescr f1, FieldDescr f2) {
            String v1 = f1.getConstantValue() == null ? "" : f1.getConstantValue();
            String v2 = f2.getConstantValue() == null ? "" : f2.getConstantValue();
            if (!f1.getType().equals(f2.getType())) return true;
            return v1.equals(v2);
        }
    }

    static class Rule4_6 extends FieldPairedHandler {

        boolean acceptMessageList(List l) {
            if (!super.acceptMessageList(l)) return false;
            init(l);
            return !f1.hasModifier(Modifier.FINAL) && f2.hasModifier(Modifier.FINAL);
        }

        protected boolean proc() {
            newM.definition = i18n.getString("BCProcessor.error.4_6"); // "E4.6 - Changing field from non-final to final";
            setMessageLevel(newM);
            return true;
        }
    }

    static class Rule4_7 extends FieldPairedHandler {

        boolean acceptMessageList(List l) {
            if (!super.acceptMessageList(l)) return false;
            init(l);
            return f1.hasModifier(Modifier.STATIC) && !f2.hasModifier(Modifier.STATIC);
        }

        protected boolean proc() {
            newM.definition = i18n.getString("BCProcessor.error.4_7"); // "E4.7 - Changing field from static to non-static";
            setMessageLevel(newM);
            return true;
        }
    }

    static class Rule4_8 extends FieldPairedHandler {

        boolean acceptMessageList(List l) {
            if (!super.acceptMessageList(l)) return false;
            init(l);
            return !f1.hasModifier(Modifier.STATIC) && f2.hasModifier(Modifier.STATIC);
        }


        protected boolean proc() {
            newM.definition = i18n.getString("BCProcessor.error.4_8"); // "E4.8 - Changing field from non-static to static";
            setMessageLevel(newM);
            return true;
        }
    }

    /*
    Adding non-abstract and non-static methods to classes
    5.1.2 	   The class is not final 	Source
    */
    class Rule5_1_2 extends Handler {
        private ErrorFormatter.Message m;

        Rule5_1_2() {
            super();
            setLevel(Level.WARNING);
        }

        boolean acceptMessageList(List l) {
            if (l.size() != 1) {
                return false;
            }
            m = (ErrorFormatter.Message) l.get(0);
            if (m.messageType == MessageType.ADD_METHS && m.errorObject instanceof MethodDescr) {
                try {
                    return !(clHier.isInterface(m.className) || clHier.isAnnotation(m.className));

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        protected void writeMessage(List l, Chain ch) {
            try {
                ClassDescription cd = clHier.load(m.className);
                MethodDescr md = (MethodDescr) m.errorObject;
                if (!md.isStatic() && !md.isAbstract()) {
                    if (cd.hasModifier(Modifier.FINAL)) return;
                    if (bin) return;
                    m.definition = i18n.getString("BCProcessor.error.5_1_2"); // "W5.1.2 - Adding methods";
                    ch.addMessage(m);
                    setMessageLevel(m);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    class Rule5_2_3 extends Handler {
        private ErrorFormatter.Message m;


        boolean acceptMessageList(List l) {
            if (l.size() != 1) {
                return false;
            }
            m = (ErrorFormatter.Message) l.get(0);
            if (m.messageType == MessageType.ADD_METHS && m.errorObject instanceof MethodDescr) {
                try {
                    return !(clHier.isInterface(m.className) || clHier.isAnnotation(m.className));

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        protected void writeMessage(List l, Chain ch) {
            try {
                ClassDescription cd = clHier.load(m.className);
                MethodDescr md = (MethodDescr) m.errorObject;

                if (canBeSubclassed(m.className, sfHier)) {
                    if (md.isAbstract()) {
                        m.definition = i18n.getString("BCProcessor.error.5_2"); //"E5.2 - Adding abstract methods";
                        ch.addMessage(m);
                        setMessageLevel(m);
                        return;
                    }

                    if (md.isStatic() && !cd.hasModifier(Modifier.FINAL) && !bin) {
                        m.definition = i18n.getString("BCProcessor.error.5_3"); // "E5.3 - Adding static methods";
                        ch.addMessage(m);
                        setMessageLevel(m);
                    }
                }


            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    /*
     5.4 Removing constructors
     Breaks - Both
    */
    static class Rule5_4 extends Handler {
        private ErrorFormatter.Message m;

        boolean acceptMessageList(List l) {
            if (l.size() != 1) {
                return false;
            }
            m = (ErrorFormatter.Message) l.get(0);
            return m.messageType == MessageType.MISS_CONSTRUCTORS;
        }

        protected void writeMessage(List l, Chain ch) {
            m.definition = i18n.getString("BCProcessor.error.5_4"); // "E5.4 - Removing constructor";
            setMessageLevel(m);
            ch.addMessage(m);
        }
    }

    /*
     5.6 Adding fields
     Breaks - Both
    */
    class Rule5_6 extends Handler {

        Rule5_6() {
            super();
            setLevel(Level.WARNING);
        }

        private ErrorFormatter.Message m;

        boolean acceptMessageList(List l) {
            if (l.size() != 1) {
                return false;
            }
            m = (ErrorFormatter.Message) l.get(0);
            if (m.messageType == MessageType.ADD_FLD) {
                try {
                    ClassDescription cd = clHier.load(m.className);
                    if (cd.isClass() && !cd.isFinal() && !cd.isInterface()) {
                        return true;
                    }
                } catch (ClassNotFoundException e) {

                }
            }
            return false;
        }

        protected void writeMessage(List l, Chain ch) {
            m.definition = i18n.getString("BCProcessor.error.5_6"); // "W5.6 - Adding fields";
            setMessageLevel(m);
            ch.addMessage(m);
        }
    }

    class Rule5_12 extends ClassPairedHandler {
        protected boolean proc() {
            // TODO - recheck enum can't be subclassed in jdk7
            if (!c1.isAbstract() && !c1.hasModifier(Modifier.ENUM) && c2.isAbstract()) {
                if (canBeSubclassed(c1.getQualifiedName(), sfHier)) {
                    newM.definition = i18n.getString("BCProcessor.error.5_12"); // "E5.12 - Changing class from non-abstract to abstract";
                    setMessageLevel(newM);
                    return true;
                }
            }
            return false;
        }
    }

    static class Rule5_14 extends ClassPairedHandler {
        protected boolean proc() {
            if (!c1.isFinal() && c2.isFinal()) {
                newM.definition = i18n.getString("BCProcessor.error.5_14"); // "E5.14 - Changing class from non-final to final";
                setMessageLevel(newM);
                return true;
            }
            return false;
        }
    }

    static class Terminator extends Handler {
        boolean acceptMessageList(List l) {
            return true;
        }

        protected void writeMessage(List l, Chain ch) {
            ch.setMessagesProcessed(l);
        }
    }

}

