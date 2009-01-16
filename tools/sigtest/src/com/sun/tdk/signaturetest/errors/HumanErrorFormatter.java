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


import com.sun.tdk.signaturetest.model.*;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Level;

import com.sun.tdk.signaturetest.util.I18NResourceBundle;

public class HumanErrorFormatter extends SortedErrorFormatter {

    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(SortedErrorFormatter.class);
    private Level level;

    /**
     * Assign the given <b>PrintWriter</b> to print error messages.
     */
    public HumanErrorFormatter(PrintWriter out, boolean isv, Level l) {
        super(out, isv);
        level = l;
    }

    public void printErrors() {

        sortErrors();

        ErrorComparator ec = new ErrorComparator();

        int length = failedMessages.size();
        Chain ch = new Chain(failedMessages);

        for (int i = 0; i < length ; i++) {
            Message e1 = (Message) failedMessages.get(i);
            if (e1 == null)
                continue;

            int j = i;

            while (j+1 < failedMessages.size()) {
                Message e2 = (Message) failedMessages.get(j+1);
                if (ec.compare(e1, e2) == 0)
                    j++;
                else
                    break;
            }

            List currentGroup = failedMessages.subList(i, j+1);

            Handler h = constructHandlerChain();

            h.process(currentGroup, ch);

            i = j;

        }

        ch.finishProcessing();
        Iterator it = failedMessages.iterator();
        numErrors = 0;
        numWarnings = 0;

        while(it.hasNext()) {
            Message m = (Message) it.next();
            if (level.intValue() <= m.getLevel().intValue()) {
                numErrors++;
            } else {
                numWarnings++;
            }
        }

        sortErrorsForOutput();

        outProcessedErrors();

    }

    protected void outProcessedErrors() {
        boolean hasHeader = false;
        MessageType lastType = null;
        String cl = "";

        for (int i = 0; i < failedMessages.size(); i++) {
            Message current = (Message) failedMessages.get(i);
            if (current == null)
                continue;

            String ccl = current.className;

            if (current.messageType == MessageType.ADD_CLASSES) {
                lastType = current.messageType;
                out.println("\n+ Class " + ccl);
                cl = ccl;
                continue;
            }

            if (current.messageType == MessageType.MISS_CLASSES) {
                lastType = current.messageType;
                out.println("\n- Class " + ccl);
                cl = ccl;
                continue;
            }

            if (!cl.equals(ccl)) {
                cl = ccl;
                lastType = null;
                out.println("\nClass " + cl);
            }

            if (current.messageType != lastType) {
                hasHeader = true;

                out.println("  " + current.messageType.getLocMessage());
                lastType = current.messageType;
            }
            if (hasHeader) {
                if (current.definition.equals(""))
                    out.println(current.className);
                else {
                    StringBuffer name = new StringBuffer();
                    if (current.messageType != MessageType.CHNG_CLASSES_MEMBERS) {
                        out.println("    " + current.definition);
                        if (isVerbose() && current.tail.length() != 0)
                            out.println(i18n.getString("SortedErrorFormatter.error.affected", current.tail));
                    } else {

                        if (current.errorObject.getMemberType() != MemberType.CLASS) {
                            name.append("    ");
                            name.append(current.errorObject);
                            out.println(name);
                        }

                        out.print(current.definition);
                    }
                }
            } else {
                out.println(current);
            }
        }
        if (failedMessages.size() > 0)
            out.println("");
    }

    protected Handler constructHandlerChain() {
        //AnnotationHandler must be last but one
        //Other *Handler may be in any order
        return new ModifiersHandler().setNext(
                new ReturnTypeHandler().setNext(
                        new TypeParametersHandler().setNext(
                                new ThrowsHandler().setNext(
                                        new ConstantValueHandler().setNext(
                                            new AnnotationHandler())))));
    }


    private void sortErrorsForOutput() {
        Collections.sort(failedMessages, new Comparator() {

            // 1 - By class
            // 2 - By object (CLSS, method, fiels, other)
            // 3 - By message type
            // 4 - By defenition

            public int compare(Object o1, Object o2) {
                Message m1 = (Message) o1;
                Message m2 = (Message) o2;

                if (m1==null && m2==null) return 0;
                if (m1==null && m2!=null) return -1;
                if (m1!=null && m2==null) return 1;

                int comp = m1.className.compareTo(m2.className);

                if (comp == 0) {
                    comp = m1.errorObject.getMemberType().compareTo(m2.errorObject.getMemberType());
                    if (comp == 0) {
                        comp = m1.messageType.compareTo(m2.messageType);
                        if (comp == 0) {
                            comp = m1.definition.compareTo(m2.definition);
                            if (comp == 0) {
                                if (m1.tail != null && m2.tail != null)
                                    comp = m1.tail.compareTo(m2.tail);
                                else {
                                    if (m1.tail == null)
                                        comp = -1;
                                    else
                                        comp = 1;
                                }
                            }
                        }
                        return comp;
                    }
                }
                return comp;
            }

        }
        );
    }

    protected void sortErrors() {
        Collections.sort(failedMessages, new ErrorComparator()
        );
    }

    private static class ErrorComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Message msg1 = (Message) o1;
            Message msg2 = (Message) o2;
            MemberDescription md1 = msg1.errorObject;
            MemberDescription md2 = msg2.errorObject;

            int comp = md1.getQualifiedName().compareTo(md2.getQualifiedName());

            if (comp == 0) {
                comp = md1.getMemberType().compareTo(md2.getMemberType());
                if (comp == 0 && (md1.getMemberType() == MemberType.METHOD || md1.getMemberType() == MemberType.CONSTRUCTOR)) {

                    if (md1 instanceof MethodDescr && md2 instanceof MethodDescr ) {

                        MethodDescr mth1 = (MethodDescr) md1;
                        MethodDescr mth2 = (MethodDescr) md2;
                        comp = mth1.getSignature().compareTo(mth2.getSignature());

                    } else if (md1 instanceof ConstructorDescr && md2 instanceof ConstructorDescr ) {

                        ConstructorDescr co1 = (ConstructorDescr) md1;
                        ConstructorDescr co2 = (ConstructorDescr) md2;
                        comp = co1.getSignature().compareTo(co2.getSignature());

                    }

                }
            }

            return comp;
        }
    }
}