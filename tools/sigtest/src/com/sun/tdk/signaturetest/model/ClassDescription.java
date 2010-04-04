/*
 * $Id: ClassDescription.java 4504 2008-03-13 16:12:22Z sg215604 $
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

import com.sun.tdk.signaturetest.core.ClassHierarchy;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.SwissKnife;

import java.io.Serializable;
import java.util.*;

/**
 * <b>ClassDescription</b> lists all public or protected members of some class
 * or interface. Given some <b>MemberDescription</b> instance, it is possible to
 * findMember a <b>List</b> of <b>MemberDescription</b> references for all members
 * characterized with that shorter <b>MemberDescription</b>.
 * <p/>
 * <p>Several abstract methods for finding nested classes are declared in
 * <b>ClassDescription</b>, which are implemented in the following two classes: <ul>
 * <li> <b>ReflClassDescription</b> using reflection, and
 * <li> <b>SigFileClassDescription</b> using previously created table of nested classes.
 * </ul>
 *
 * @author Maxim Sokolnikov
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 * @version 05/03/22
 */

public class ClassDescription extends MemberDescription implements Serializable {

    private static final String PACKAGE_INFO_CLASS = ".package-info";

    public static final String OUTER_PREFIX = "outer";

    // NOTE: Change this method carefully if you changed the code,
    // please, update the method isCompatible() in order it works as previously
    public boolean equals(Object o) {
        // == used instead of equals() because name is always assigned via String.intern() call
        return o instanceof ClassDescription && name == ((ClassDescription) o).name;
    }

    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }


    public String getQualifiedName() {
        return name;
    }

    public String getName() {
        return getClassShortName(name);
    }

    public boolean isCompatible(MemberDescription m) {

        if (!equals(m))
            throw new IllegalArgumentException("Only equal members can be checked for compatibility!");

        return memberType.isCompatible(getModifiers(), m.getModifiers()) &&
                SwissKnife.equals(typeParameters, m.typeParameters);
    }

    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(ClassDescription.class);

    public boolean containsMember(MemberDescription newMember) {
        return members.contains(newMember);
    }

    public MemberDescription findMember(MemberDescription requiredMember) {
        return members.find(requiredMember);
    }

    public int getMembersCount(MemberType memberType, String fqname) {
        return members.getMembersCount(memberType, fqname);
    }

    public boolean isAnonymousClass() {
        if (name.charAt(name.length()-1) == CLASS_DELIMITER) {
            return false;
        }
        return !declaringClass.equals(NO_DECLARING_CLASS) && isAsciiDigit(name.charAt(name.lastIndexOf(CLASS_DELIMITER) +1));
    }

    private static boolean isAsciiDigit(char c) {
        return '0' <= c && c <= '9';
    }


    // TODO low performance!
    public ArrayList getTypeBounds() {
//        assert typeParameters != null;
        ArrayList bounds = new ArrayList();
        int startPos = 1;
        int endPos;
        do {
            endPos = typeParameters.indexOf(',', startPos);
            if (endPos == -1)
                endPos = typeParameters.length() - 1;

            String type = typeParameters.substring(startPos, endPos);

            final String ext = "extends";

            int extPos = type.indexOf(ext);
//            assert extPos != -1;
            int ampPos = type.indexOf('&');
            if (ampPos == -1) ampPos = type.length();

            String bound = type.substring(extPos + ext.length(), ampPos);

            bounds.add(bound.trim());
            startPos = endPos + 1;

        } while (startPos != typeParameters.length());

        return bounds;
    }

    public void setMembers(MemberCollection members) {
        this.members = members;
    }

    public static String getPackageInfo(String packageName) {
        return packageName + PACKAGE_INFO_CLASS;
    }

    public static class TypeParam {


        public TypeParam(int seqnb, String ident, String declared) {
            this.seqnb = seqnb;
            this.ident = ident;
            this.declared = declared;
        }

        final int seqnb;
        final String ident;
        final String declared;

        public int getSeqnb() {
            return seqnb;
        }

        public String getIdent() {
            return ident;
        }

        public String getDeclared() {
            return declared;
        }

        TypeParam hidden = null; // hidden type parameter
    }


    public static class TypeParameterList {
        Map/*String,TypeParam*/ tab = new HashMap();
        int seqnb = 0;

        public TypeParameterList(TypeParameterList enclosing) {
            if (enclosing != null && !enclosing.tab.isEmpty())
                tab.putAll(enclosing.tab);
        }

        public void reset_count() {
            seqnb = 0;
        }

        public Map getTab() {
            return tab;
        }

        public void add(String id, String declared) {
            TypeParam e = new TypeParam(seqnb++, id, declared);

            TypeParam o = (TypeParam) tab.put(id, e);
            if (o == null)
                return;

            if (o.declared.equals(e.declared)) {
                String[] invargs = {id, e.declared, o.declared};
                throw new Error(i18n.getString("ClassDescription.error.declaredtwice", invargs));
            }

            e.hidden = o;
        }

        public void clear(String declared) {
            Collection/*TypeParam*/ tmp = new ArrayList();

            //  Get list of parameters to be removed
            for (Iterator it = tab.entrySet().iterator(); it.hasNext();) {
                Map.Entry ent = (Map.Entry) it.next();
                TypeParam e = (TypeParam) ent.getValue();
                if (e.declared.equals(declared))
                    tmp.add(e);
            }

            //  Remove parameter or replace it with previously hidden
            for (Iterator it = tmp.iterator(); it.hasNext();) {
                TypeParam e = (TypeParam) it.next();
                if (e.hidden == null)
                    tab.remove(e.ident);
                else
                    tab.put(e.ident, e.hidden);
            }
        }


        public String replace(String id) {
            TypeParam e = (TypeParam) tab.get(id);
            if (e == null)
                return replaceNone(id);
            else
                return "{" + e.declared + '%' + String.valueOf(e.seqnb) + "}";
        }


        public static String replaceNone(String id) {
            return "{?" + id + "}";
        }


        //  Replace all the links having the form ...{?id}...
        //
        public String replaceForwards(String s) {
            int i, k;
            while ((i = s.indexOf("{?")) != -1 && (k = s.indexOf('}', i)) != -1) {

                String r = replace(s.substring(i + 2, k));
                if (!r.startsWith("{?"))
                    s = s.substring(0, i) + r + s.substring(k + 1);
                else
                    throw new Error(i18n.getString("ClassDescription.error.undefined", s));
            }

            return s;
        }
    }

    //  List of type parameters (type variables) declared by the class
    // this member must be used only loaders!
    // TODO remove this member out of ClassDescription!
    protected TypeParameterList typeparamList = null;


    public TypeParameterList getTypeparamList() {
        return typeparamList;
    }

    public void setTypeparamList(TypeParameterList typeparamList) {
        this.typeparamList = typeparamList;
    }


    /**
     * List of members of that class described by <code>this</code> instance.
     * This is a hashtable indexed by <b>MemberDescription</b> instances, and
     * containing a <b>List</b> of <b>MemberDescription</b> instances
     * describing all members characterized with the same <b>MemberDescription</b>.
     *
     * @see MemberDescription
     */
    protected transient MemberCollection members;


    public boolean isPackageInfo() {
        return name.endsWith(PACKAGE_INFO_CLASS);
    }

    public String getPackageName() {
        return getPackageName(name);
    }

    /**
     * Given the (qualified) class <code>name</code>, extract its package name.
     */
    public static String getPackageName(String name) {
        int pos = name.lastIndexOf(".");
        if (pos > 0)
            return name.substring(0, pos);

        return "";
    }


    public boolean isTiger() {
        return isTiger;
    }

    private boolean isTiger = false;

    public void setTiger(boolean tiger) {
        isTiger = tiger;
    }


    /**
     * Description for empty class containing no members.
     *
     * @see #members
     */


    public ClassDescription() {
        super(MemberType.CLASS, CLASS_DELIMITER);
        members = new MemberCollection();
    }


    /**
     * Adds new class member.
     */
    public void add(MemberDescription x) {
        members.addMember(x);
    }


    /**
     * Return <b>Enumeration</b> of <b>MemberDescription</b> getMembersIterator for all
     * members of that class described by <code>this</code> instance.
     *
     * @see #members
     * @see MemberDescription
     */
    public Iterator getMembersIterator() {
        return members.iterator();
    }


    //  Remove the throws list completely for 'bin' mode
    public void removeThrows() {
        //  For every methods and constructors ...
        for (Iterator e = members.iterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isMethod() || mr.isConstructor())
                mr.setThrowables(MemberDescription.EMPTY_THROW_LIST);
        }
    }

    public ConstructorDescr[] getDeclaredConstructors() {
        return declaredConstructors;
    }

    public FieldDescr[] getDeclaredFields() {
        return declaredFields;
    }

    public MethodDescr[] getDeclaredMethods() {
        return declaredMethods;
    }

    public SuperInterface[] getInterfaces() {
        return interfaces;
    }

    public String getOuterClass() {
        return declaringClass;
    }

    public InnerDescr[] getDeclaredClasses() {
        return nestedClasses;
    }


    public void createInterfaces(int size) {
        interfaces = SuperInterface.EMPTY_ARRAY;
        if (size > 0)
            interfaces = new SuperInterface[size];
    }

    public void setInterface(int i, SuperInterface interf) {
        interfaces[i] = interf;
    }
    public void setInterfaces(SuperInterface[] interfs) {
    	interfaces = interfs;
    }
    

    public void createFields(int size) {
        declaredFields = FieldDescr.EMPTY_ARRAY;
        if (size > 0)
            declaredFields = new FieldDescr[size];
    }

    public void createMethods(int size) {
        declaredMethods = MethodDescr.EMPTY_ARRAY;
        if (size > 0)
            declaredMethods = new MethodDescr[size];
    }

    public void createConstructors(int size) {
        declaredConstructors = ConstructorDescr.EMPTY_ARRAY;
        if (size > 0)
            declaredConstructors = new ConstructorDescr[size];
    }

    public void createNested(int size) {
        nestedClasses = InnerDescr.EMPTY_ARRAY;
        if (size > 0)
            nestedClasses = new InnerDescr[size];
    }


    public void setConstructor(int i, ConstructorDescr c) {
        declaredConstructors[i] = c;
    }

    public void setConstructors(ConstructorDescr[] ctors) {
        declaredConstructors = ctors;
    }

    public void setMethods(MethodDescr[] methods) {
        declaredMethods = methods;
    }


    public void setMethod(int i, MethodDescr m) {
        declaredMethods[i] = m;
    }

    public void setField(int i, FieldDescr f) {
        declaredFields[i] = f;
    }

    public void setFields(FieldDescr[] fields) {
        declaredFields = fields;
    }


    public FieldDescr getField(int i) {
        return declaredFields[i];
    }

    public MethodDescr getMethod(int i) {
        return declaredMethods[i];
    }

    public ConstructorDescr getConstructor(int i) {
        return declaredConstructors[i];
    }

    private MethodDescr[] declaredMethods = MethodDescr.EMPTY_ARRAY;
    private FieldDescr[] declaredFields = FieldDescr.EMPTY_ARRAY;
    private ConstructorDescr[] declaredConstructors = ConstructorDescr.EMPTY_ARRAY;
    private SuperInterface[] interfaces = SuperInterface.EMPTY_ARRAY;
    private InnerDescr[] nestedClasses = InnerDescr.EMPTY_ARRAY;

    // these class's members initialized only if class description loaded from signature file
    private Set internalFields = null;  // contains private and package access fields
    private Set internalClasses = null; // contains private and package access nested classes
    private Set xFields = null;  // contains fields that prevent resolve other fields by simple name
    private Set xClasses = null;  // contains inner that prevent resolve other classes by simple name

    public void setNestedClasses(InnerDescr[] ncls) {
        nestedClasses = ncls;
    }

    public void setNested(int i, InnerDescr m) {
        nestedClasses[i] = m;
    }

    private SuperClass superClass = null;

    public void setSuperClass(SuperClass m) {
        superClass = m;
    }

    public SuperClass getSuperClass() {
        return superClass;
    }

    public boolean isClass() {
        return true;
    }


    private boolean isInternalMember(MemberDescription m) {
        return m.hasModifier(Modifier.PRIVATE) || (!m.hasModifier(Modifier.PUBLIC) && !m.hasModifier(Modifier.PROTECTED));
    }


    public void setInternalFields(Set fields) {
        internalFields = fields;
    }

    public void setInternalClasses(Set classes) {
        internalClasses = classes;
    }


    public Set getInternalFields() {

        Set result = internalFields;
        if (result == null) {
            // try to find private field in the declared fields
            for (int i = 0; i < declaredFields.length; ++i) {
                MemberDescription m = declaredFields[i];
                if (isInternalMember(m)) {
                    if (result == null)
                        result = new HashSet();
                    result.add(m.getName());
                }
            }
            if (result == null)
                result = Collections.EMPTY_SET;
        }
        return result;
    }

    public Set getXFields() {
        if (xFields == null) {
            return Collections.EMPTY_SET;
        } else {
            return xFields;
        }
    }

    public void setXFields(Set fileds) {
        xFields = fileds;
    }

    public void addXFields(String name) {
        if (xFields == null) {
            xFields = new HashSet();
        }
        xFields.add(name);
    }

    public Set getXClasses() {
        if (xClasses == null) {
            return Collections.EMPTY_SET;
        } else {
            return xClasses;
        }
    }

    public void setXClasses(Set classes) {
        xClasses = classes;
    }

    public void addXClasses(String name) {
        if (xClasses == null) {
            xClasses = new HashSet();
        }
        xClasses.add(name);
    }

    public Set getInternalClasses() {

        Set result = internalClasses;
        if (result == null) {
            // try to find private class in the declared classes
            for (int i = 0; i < nestedClasses.length; ++i) {
                MemberDescription m = nestedClasses[i];
                if (isInternalMember(m)) {
                    if (result == null)
                        result = new HashSet();
                    result.add(m.getName());
                }
            }
            if (result == null)
                result = Collections.EMPTY_SET;
        }
        return result;
    }

    public String toString() {

        StringBuffer buf = new StringBuffer();

        buf.append("CLASS");

        String modifiers = Modifier.toString(memberType, getModifiers(), true);
        if (modifiers.length() != 0) {
            buf.append(' ');
            buf.append(modifiers);
        }

        buf.append(' ');
        buf.append(name);

        if (typeParameters != null)
            buf.append(typeParameters);

        AnnotationItem[] annoList = getAnnoList();
        for (int i = 0; i < annoList.length; ++i) {
            buf.append("\n ");
            buf.append(annoList[i]);
        }

        return buf.toString();
    }

    public boolean isTopClass() {
        return NO_DECLARING_CLASS.equals(declaringClass);
    }

    public Set getDependences() {
        Set set = new HashSet();
        populateDependences(set);
        return set;
    }

    protected void populateDependences(Set dependences) {

        // Note! Nested classes do NOT tracked!
        populateDependences(getInterfaces(), dependences);

        AnnotationItem[] annots = getAnnoList();
        for (int i = 0; i < annots.length; ++i) {

            // skip dependency to itself
            if (annots[i].getName().equals(name))
                continue;

            addDependency(dependences, annots[i].getName());
        }

        if (superClass != null)
            addDependency(dependences, superClass.getQualifiedName());

        // add outer class for nested class
        if (!isTopClass())
            addDependency(dependences, declaringClass);
    }

    public void populateDependences(MemberDescription[] members, Set dependences) {
        for (int i = 0; i < members.length; ++i)
            if (members[i].isPublic() || members[i].isProtected()) {
                members[i].populateDependences(dependences);
            } else if (members[i].isSuperInterface())
                addDependency(dependences, members[i].getQualifiedName());
    }

    public boolean isDocumentedAnnotation() {
        if (hasModifier(Modifier.ANNOTATION)) {
            AnnotationItem[] annots = getAnnoList();
            for (int i = 0; i < annots.length; ++i) {
                if (AnnotationItem.ANNOTATION_DOCUMENTED.equals(annots[i].getName()))
                    return true;
            }
        }
        return false;
    }

    private ClassHierarchy hierarchy;

    public ClassHierarchy getClassHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }
    
    static final long serialVersionUID = -3431812619261695131L;
}
