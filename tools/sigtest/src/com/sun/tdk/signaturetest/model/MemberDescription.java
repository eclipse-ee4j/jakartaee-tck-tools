/*
 * $Id: MemberDescription.java 4516 2008-03-17 18:48:27Z eg216457 $
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

package com.sun.tdk.signaturetest.model;

import com.sun.tdk.signaturetest.core.PrimitiveTypes;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Set;

/**
 * <b>MemberDescription</b> describes a class member, such as
 * field, or method, or constructor, or nested class or interface. It contains
 * all modifiers, type for field or returned type for method, name, types of
 * method's or constructor's arguments, and declared exceptions. It does not keep
 * any ``<I><b>extends</b></I> ...'' nor ``<I><b>implements</b></I> ...''
 * information for nested class.
 *
 * @author Maxim Sokolnikov
 */
public abstract class MemberDescription implements Cloneable, Serializable {

    public static final String EMPTY_THROW_LIST = "";
    public static final String NO_ARGS = "";
    public static final String NO_TYPE = "";
    public static final String NO_DECLARING_CLASS = "";

    // String is used only for convenience. it must be 1 char length.
    public static final String THROWS_DELIMITER = ",";
    public static final String ARGS_DELIMITER = ",";

    public static final char CLASS_DELIMITER = '$';
    public static final char MEMBER_DELIMITER = '.';

    protected MemberDescription(MemberType memberType, char delimiter) {
        this.memberType = memberType;
        this.delimiter = delimiter;
    }

    protected final char delimiter;

    /**
     * All modifiers assigned to <code>this</code> item.
     *
     * @see com.sun.tdk.signaturetest.model.Modifier
     *      Direct access to this field not allowed because
     *      method setModifiers(int) changes incoming modifiers!
     * @see #setModifiers(int)
     */
    private int modifiers = 0;

    //  For classes, methods and constructors: generic type parameters or null
    String typeParameters;

    /**
     * FieldDescr type, if <code>this</code> item describes some field, or
     * return type, if <code>this</code> item describes some methods.
     * null value not allowed!
     */
    String type = NO_TYPE;

    /**
     * Qualified name of the class or interface, where
     * <code>this</code> item is declared.
     * null value not allowed!
     */
    String declaringClass = NO_DECLARING_CLASS;

    /**
     * If <code>this</code> item describes some method or constructor,
     * <code>args</code> lists types of its arguments. Type names are
     * separated by commas, and the whole <code>args</code> list is
     * embraced inside matching parentheses. in form: (arg,arg,...)
     * null value not allowed!
     */
    String args = NO_ARGS;

    /**
     * Contains <I><b>throws</b></I> clause, if <code>this</code> item
     * describes some method or constructor. in form: throws t,t,...t
     * null value not allowed!
     */
    String throwables = EMPTY_THROW_LIST;


    //  Sorted list of annotations present on this item or null
    private AnnotationItem[] annoList = AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY;

    /**
     * Sort of entity referred by <code>this</code> item. It should be either field,
     * or method, or constructor, or class or inner class, or interface being
     * implemented by some class, or <I>superclass</I> being extended by some class.
     *
     * @see #isField()
     * @see #isMethod()
     * @see #isConstructor()
     * @see #isClass()
     * @see #isSuperClass()
     * @see #isSuperInterface()
     * @see #isInner()
     */

    MemberType memberType;

    //  For classes, superclasses and superinterfaces: fully-qualified class name
    //  For other members: short name including inners
    // all names are interned. this helps to save memory, specially in binary mode!
    // Note! since all names interned it's possible to use == instead of equals()
    String name = "";


    // TODO using this method is a bad practice! 
    public final Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int hashCode() {
        return memberType.hashCode() + name.hashCode();
    }

    // this method must have package access !!!
    public MemberType getMemberType() {
        return memberType;
    }

    public String getName() {
        return name;
    }


    /**
     * Check if <code>this</code> is a class item.
     */
    public boolean isClass() {
        return false;
    }

    /**
     * Check if <code>this</code> item describes superclass for some class.
     * (I.e., check if <code>this</code> describes ``<I><b>extends</b></I> ...''
     * suffix for some <b>ClassDescription</b>.)
     */
    public boolean isSuperClass() {
        return false;
    }

    /**
     * Check if <code>this</code> item describes interface class for some class.
     * (I.e., check if <code>this</code> describes some of interface name(s) in
     * ``<I><b>implements</b></I> ...'' suffix for some <b>ClassDescription</b>.)
     */
    public boolean isSuperInterface() {
        return false;
    }

    /**
     * Check if <code>this</code> item describes some field.
     */
    public boolean isField() {
        return false;
    }

    /**
     * Check if <code>this</code> item describes some method.
     */
    public boolean isMethod() {
        return false;
    }

    /**
     * Check if <code>this</code> item describes some constructor.
     */
    public boolean isConstructor() {
        return false;
    }

    public boolean isInner() {
        return false;
    }


    /**
     * Return <b>Set</b> of Modifier
     * assigned to <code>this</code> item.
     *
     * @see Modifier
     */
    public int getModifiers() {
        return modifiers;
    }

    /**
     * Display return-type if <code>this</code> describes some method,
     * or type of the field if <code>this</code> describes some field.
     *
     */
    public String getType() {
        return type;
    }

    public String getTypeParameters() {
        return typeParameters;
    }

    // SuperInterface, SuperClass, ClassDescription,   
    public void setupGenericClassName(String superClassName) {

        int pos = superClassName.indexOf('<');
        String tmp = superClassName;
        if (pos != -1) {
            tmp = superClassName.substring(0, pos);
            typeParameters = superClassName.substring(pos);
        } else typeParameters = null;

        setupClassName(tmp);
    }


    // ClassDescription, InnerDescr and all members.
    public void setupClassName(String fqn) {

        int delimPos = fqn.lastIndexOf(delimiter);

        if (memberType == MemberType.CLASS || memberType == MemberType.SUPERCLASS ||
                memberType == MemberType.SUPERINTERFACE) {

            name = fqn.intern();

            if (delimPos != -1) {
                declaringClass = fqn.substring(0, delimPos).intern();
            } else {
                declaringClass = NO_DECLARING_CLASS;
            }

        } else {
            declaringClass = fqn.substring(0, delimPos).intern();
            name = fqn.substring(delimPos + 1).intern();
        }
    }

    // only inner in F40Parser
    public void setupInnerClassName(String name, String declaringClassName) {
        declaringClass = declaringClassName.intern();
        this.name = name.intern();
    }

    // only field and method
    public void setupMemberName(String own, String dcl) {
        declaringClass = dcl.intern();
        name = own.intern();
    }

    // only field, method and constructor
    public void setupMemberName(String fqn) {
        int pos = fqn.lastIndexOf(delimiter);

        declaringClass = fqn.substring(0, pos).intern();
        name = fqn.substring(pos + 1).intern();
    }


    /**
     * Display qualified name of the class or interface declaring
     * <code>this</code> item. Empty string is returned if <code>this</code>
     * item describes top-level class or interface, which is not inner
     * class or interface.
     */
    public String getDeclaringClassName() {
        return declaringClass;
    }


    public AnnotationItem[] getAnnoList() {
        return annoList;
    }

    // default implementation.
    // For ClassDescription, SuperClass, SuperInteraface this method must be overriden!
    public String getQualifiedName() {
        return declaringClass + delimiter + name;
    }

    /**
     * Returns list of exception names separated by commas declared
     * in the <I><b>throws</b></I> clause for that method or constructor
     * described by <code>this</code> item.
     *
     */
    public String getThrowables() {
        return throwables;
    }

    private boolean marked = false;

    public void mark() {
        marked = true;
    }

    public void unmark() {
        marked = false;
    }

    public boolean isMarked() {
        return marked;
    }


    /**
     * Check if modifiers list for <code>this</code> item contains
     * the <code>"protected"</code> string.
     *
     */
    public boolean isProtected() {
        return Modifier.hasModifier(modifiers, Modifier.PROTECTED);
    }

    /**
     * Check if modifiers list for <code>this</code> item contains
     * the <code>"public"</code> string.
     *
     */
    public boolean isPublic() {
        return Modifier.hasModifier(modifiers, Modifier.PUBLIC);
    }

    /**
     * Check if modifiers list for <code>this</code> item contains
     * the <code>"private"</code> string.
     *
     */
    public boolean isPrivate() {
        return Modifier.hasModifier(modifiers, Modifier.PRIVATE);
    }

    /**
     * Check if modifiers list for <code>this</code> item contains
     * the <code>"abstract"</code> string.
     *
     */
    public boolean isAbstract() {
        return Modifier.hasModifier(modifiers, Modifier.ABSTRACT);
    }

    /**
     * Check if modifiers list for <code>this</code> item contains
     * the <code>"static"</code> string.
     *
     */
    public boolean isStatic() {
        return Modifier.hasModifier(modifiers, Modifier.STATIC);
    }

    public boolean isFinal() {
        return Modifier.hasModifier(modifiers, Modifier.FINAL);
    }

    /**
     * Check if modifiers list for <code>this</code> item contains
     * the <code>"interface"</code> string.
     *
     */
    public boolean isInterface() {
        return Modifier.hasModifier(modifiers, Modifier.INTERFACE);
    }

    //  Convert constant value to string representation
    //  used in sigfile.
    //
    public static String valueToString(Object value) {

        if (value == null)
            return "";

        if (value.getClass().isArray()) {
            StringBuffer sb = new StringBuffer();
            sb.append('[');
            int n = Array.getLength(value);
            for (int i = 0; i < n; i++) {
                if (i != 0)
                    sb.append(", ");
                sb.append(valueToString(Array.get(value, i)));
            }
            sb.append(']');
            return sb.toString();
        } else if (value instanceof Character)
            return "\'" + stuffOut(value.toString()) + "\'";

        else if (value instanceof String)
            return "\"" + stuffOut(value.toString()) + "\"";

        else if (value instanceof Long)
            return value.toString(); // + "L";

        else if (value instanceof Float) {
            Float f = (Float) value;
            return f.toString();
//            if (!f.isNaN() && !f.isInfinite())
//                s += "f";
        } else if (value instanceof Double) {
            Double d = (Double) value;
            return d.toString();
//            if (!d.isNaN() && !d.isInfinite())
//                s += "d";
        } else // boolean, byte, short, int
            return value.toString();
    }

    private static String stuffOut(String s) {
        StringBuffer x = new StringBuffer();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if ((0x20 <= c && c <= 0x7E) && c != '\"' && c != '\\')
                x.append(c);

            else if (c == '\r')
                x.append("\\r");

            else if (c == '\n')
                x.append("\\n");

            else
                x.append(esc(c));
        }

        return x.toString();
    }

    private static String esc(char c) {
        String s = Integer.toHexString(c);
        int n = s.length();

        if (n == 1)
            return "\\u000" + s;
        else if (n == 2)
            return "\\u00" + s;
        else if (n == 3)
            return "\\u0" + s;
        else
            return "\\u" + s;
    }


    public void setModifiers(int access) {

        int mask = memberType.getModifiersMask();

        if ((access & mask) != access)
            throw new IllegalArgumentException("Unknown modifier(s) found " + (access & ~mask));

        modifiers = access;

        if (Modifier.hasModifier(modifiers, Modifier.INTERFACE))
            modifiers = Modifier.addModifier(modifiers, Modifier.ABSTRACT);

        // ===== end of workaround =====
    }

    public void addModifier(Modifier mod) {
        modifiers = Modifier.addModifier(modifiers, mod);
    }

    public void removeModifier(Modifier mod) {
        modifiers = Modifier.removeModifier(modifiers, mod);        
    }


    public static String getTypeName(Class c) {
        String className = c.getName();

        if (!className.startsWith("["))
            return className;

        return getTypeName(className);
    }


    public static String getTypeName(String className) {

        StringBuffer sb = new StringBuffer();

        int dims = 0;
        while (className.charAt(dims) == '[')
            dims++;

        String type;

        if (className.charAt(dims) == 'L')
            type = className.substring(dims + 1, className.length() - 1);
        else
            type = PrimitiveTypes.getPrimitiveType(className.charAt(dims));

        sb.append(type);
        for (int i = 0; i < dims; ++i)
            sb.append("[]");

        return sb.toString();
    }


    public String getArgs() {
        return args;
    }

    public boolean setType(String type) {
        if (this.type.equals(type))
            return false;

        this.type = type.intern();
        return true;
    }

    public boolean setArgs(String args) {
        if (this.args.equals(args))
            return false;

        // this is just memory usage optimization
        if (args.indexOf(ARGS_DELIMITER) == -1)
            this.args = args.intern();
        else
            this.args = args;

        return true;
    }

    public boolean setThrowables(String throwables) {
        if (this.throwables.equals(throwables))
            return false;

        // this is just memory usage optimization
        if (throwables.indexOf(THROWS_DELIMITER) == -1)
            this.throwables = throwables.intern();
        else
            this.throwables = throwables;

        return true;
    }

    public void setAnnoList(AnnotationItem[] annoList) {
        this.annoList = annoList;
        // in fact, Arrays.sort() is slow, because clone the whole array passed as input parameter        
        if (annoList.length>1)
            Arrays.sort(this.annoList);
    }

    public void setTypeParameters(String typeParameters) {
        this.typeParameters = typeParameters;
    }


    // should be used for members only
    public void setDeclaringClass(String declaringClass) {
        if (declaringClass == null || NO_DECLARING_CLASS.equals(declaringClass))
            throw new IllegalArgumentException();

        this.declaringClass = declaringClass.intern();
    }

    public void setNoDeclaringClass() {
        this.declaringClass = NO_DECLARING_CLASS;
    }

    //  Pack exception lists in the following way:
    //      throws <e1>, ... <eN>
    //  one blank follows 'throw' keyword, exceptions are separated by ',' without blanks.
    //  If the exception list is empty, empty string is returned.
    //  Exceptions are sorted alphabetically
    //
    //  Note: this method has side-effect - its parameter (xthrows) gets sorted.
    //
    public static String getThrows(String[] xthrows) {
        if (xthrows == null || xthrows.length == 0)
            return EMPTY_THROW_LIST;

        // in fact, Arrays.sort() is slow, because clone the whole array passed as input parameter
        if (xthrows.length > 1)
            Arrays.sort(xthrows);

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < xthrows.length; i++) {
            if (i != 0)
                sb.append(THROWS_DELIMITER);
            sb.append(xthrows[i]);
        }

        return sb.toString();
    }

    public boolean hasModifier(Modifier mod) {
        return Modifier.hasModifier(modifiers, mod);
    }

    public abstract boolean isCompatible(MemberDescription m);

    protected void populateDependences(Set dependences) {
    }

    protected void addDependency(Set dependences, String newDependency) {

        if (newDependency.charAt(0) == '{')
            return;

        String temp = newDependency;

        int pos = temp.indexOf('<');
        if (pos != -1)
            temp = temp.substring(0, pos);

        pos = temp.indexOf('[');
        if (pos != -1)
            temp = temp.substring(0, pos);

        if (PrimitiveTypes.isPrimitive(newDependency))
            return;

        dependences.add(temp);
    }

    protected static String getClassShortName(String fqn) {
        String result = fqn;
        int pos = Math.max(fqn.lastIndexOf(MEMBER_DELIMITER), fqn.lastIndexOf(CLASS_DELIMITER));
        if (pos != -1)
            result = fqn.substring(pos + 1);
        return result;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        memberType = MemberType.getItemType(memberType.toString());

        // intern fields after deserialization
        setupMemberName(name, declaringClass);
        setArgs(args);
        setThrowables(throwables);
    }

}
