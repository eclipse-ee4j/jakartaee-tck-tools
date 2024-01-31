/*
 * $Id: ClassHierarchyImpl.java 4504 2008-03-13 16:12:22Z sg215604 $
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

package com.sun.tdk.signaturetest.core;

import com.sun.tdk.signaturetest.SigTest;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.plugin.Filter;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.plugin.Transformer;

import java.util.*;

/**
 * @author Roman Makarchuk
 */
public class ClassHierarchyImpl implements ClassHierarchy {

    private static String[] EMPTY_STRING_ARRAY = new String[0];
    private ClassDescriptionLoader loader;

    /**
     * If the <code>trackMode</code> field equals to <code>ALL_PUBLIC</code>,
     * every <code>public</code> or <code>protected</code> class is considered
     * to be accessible. Otherwise, ordinal accessibility rules are applied.
     * These rules imply, that <code>public</code> or <code>protected</code> nested
     * class may become inaccessible because of stronger accessibility limitations
     * assigned to its declaring class, or to class declaring its declaring class,
     * and so on.
     *
     * @see #trackMode
     */
    private int trackMode;
    private Filter defaultFilter = new DefaultIsAccessibleFilter();

    public ClassHierarchyImpl(ClassDescriptionLoader loader, int trackMode) {
        this.loader = loader;
        this.trackMode = trackMode;
    }

    public String getSuperClass(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return info.superClass;
    }

    public List /* String */ getSuperClasses(String fqClassName) throws ClassNotFoundException {
        List superclasses = new ArrayList();
        findSuperclasses(fqClassName, superclasses);
        return superclasses;
    }

    public String[] getSuperInterfaces(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return info.superInterfaces;
    }

    public Set /* String */ getAllImplementedInterfaces(String fqClassName) throws ClassNotFoundException {
        Set intfs = new HashSet();
        findAllImplementedInterfaces(fqClassName, intfs);
        return intfs;
    }

    private void findSuperclasses(String fqname, List supers) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqname);
        String supr = info.superClass;
        if (supr != null) {
            supers.add(supr);
            findSuperclasses(supr, supers);
        }
    }

    private void findAllImplementedInterfaces(String fqname, Set implementedInterfaces) throws ClassNotFoundException {

        List superClasses = new ArrayList();

        ClassInfo info = getClassInfo(fqname);

        String[] intfs = info.superInterfaces;
        for (int j = 0; j < intfs.length; ++j) {
            implementedInterfaces.add(intfs[j]);
            superClasses.add(intfs[j]);
        }

        findSuperclasses(fqname, superClasses);

        for (int i = 0; i < superClasses.size(); ++i) {
            findSuperInterfaces((String) superClasses.get(i), implementedInterfaces);
        }
    }

    private void findSuperInterfaces(String fqname, Set supers) throws ClassNotFoundException {

        ClassInfo info = getClassInfo(fqname);

        String[] intf = info.superInterfaces;
        for (int i = 0; i < intf.length; i++) {
            supers.add(intf[i]);
            findSuperInterfaces(intf[i], supers);
        }
    }


    public String[] getDirectSubclasses(String fqClassName) {

        String[] result = EMPTY_STRING_ARRAY;

        List subClasses = (List) directSubClasses.get(fqClassName);
        if (subClasses != null)
            result = (String[]) subClasses.toArray(EMPTY_STRING_ARRAY);

        return result;
    }

    public String[] getAllSubclasses(String fqClassName) {
        throw new UnsupportedOperationException("This method is not implemented");
    }

    public String[] getNestedClasses(String fqClassName) {
        throw new UnsupportedOperationException("This method is not implemented");
    }

    public boolean isSubclass(String subClassName, String superClassName) throws ClassNotFoundException {

        assert subClassName != null && superClassName != null;

        String name = subClassName;
        do {
            ClassInfo info = getClassInfo(name);
            if (superClassName.equals(info.superClass))
                return true;
            name = info.superClass;

        } while (name != null);

        return false;
    }


    public ClassDescription load(String name) throws ClassNotFoundException {
        return load(name, false);
    }

    public boolean isMethodOverriden(MethodDescr md) throws ClassNotFoundException {
        Erasurator erasurator = new Erasurator();
        MethodOverridingChecker moc = new MethodOverridingChecker(erasurator);
        List scs = getSuperClasses(md.getDeclaringClassName());
        Iterator it = scs.iterator();
        while(it.hasNext()) {
            ClassDescription sc = load((String)it.next());
            moc.addMethods(sc.getDeclaredMethods());
        }
        return moc.getOverridingMethod(md, false) != null;        
    }

    private ClassDescription load(String name, boolean no_cache) throws ClassNotFoundException {
        ClassDescription c = loader.load(name);

        Transformer t = PluginAPI.ON_CLASS_LOAD.getTransformer();
        if (t != null) {
            t.transform(c);
        }

        if (!no_cache) {
            // store class info!
            getClassInfo(name);
        }
        c.setHierarchy(this);
        return c;

    }


    /**
     * Check if the class described by <code>c</code> is to be traced accordingly
     * to <code>trackMode</code> set for <code>this</code> instance.
     * Every <code>public</code> or <code>protected</code> class is accessible,
     * if it is not nested to another class having stronger accessibility limitations.
     * However, if <code>trackMode</code> is set to <code>ALL_PUBLIC</code> for
     * <code>this</code> instance, every <code>public</code> or
     * <code>protected</code> class is considered to be accessible despite of
     * its accessibility limitations possibly inherited.
     */
    public boolean isAccessible(ClassDescription c) {
        return isAccessible(c, false);
    }


    public boolean isDocumentedAnnotation(String fqname) throws ClassNotFoundException {

        ClassInfo info = (ClassInfo) processedClasses.get(fqname);
        if (info != null)
            return info.isDocumentedAnnotation;

        ClassDescription c = load(fqname);
        return c.isDocumentedAnnotation();
    }


    public boolean isAccessible(String fqname) throws ClassNotFoundException {

        if (fqname == null)
            throw new NullPointerException("Parameter fqname can't be null!");

        ClassInfo info = (ClassInfo) processedClasses.get(fqname);
        if (info != null)
            return info.accessable;

        ClassDescription c = load(fqname);
        return isAccessible(c, false);
    }


    private boolean isAccessible(ClassDescription c, boolean no_cache) {

        if (!no_cache) {
            ClassInfo info = (ClassInfo) processedClasses.get(c.getQualifiedName());
            if (info != null)
                return info.accessable;
        }

        // Anonymous class can't be part of any API! 
        if (c.isAnonymousClass())
            return false;

        Filter f = PluginAPI.IS_CLASS_ACCESSIBLE.getFilter();
        if (f == null)
            f = defaultFilter;
        return f.accept(c);
    }

    // returns true if the class is visible outside the package
    public boolean isClassVisibleOutside(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return info.isVisibleOutside;
    }

    public boolean isClassVisibleOutside(ClassDescription cls) throws ClassNotFoundException {

        boolean visible = cls.hasModifier(Modifier.PUBLIC) || cls.hasModifier(Modifier.PROTECTED);

        if (visible && !cls.isTopClass()) {
            visible = isClassVisibleOutside(cls.getDeclaringClassName());
        }
        return visible;
    }

    public boolean isInterface(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return Modifier.hasModifier(info.modifiers, Modifier.INTERFACE);
    }

    public boolean isAnnotation(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return Modifier.hasModifier(info.modifiers, Modifier.ANNOTATION);
    }

    public int getClassModifiers(String fqClassName) throws ClassNotFoundException {
        ClassInfo info = getClassInfo(fqClassName);
        return info.modifiers;

    }

    private ClassInfo getClassInfo(String fqname) throws ClassNotFoundException {

        ClassInfo info = (ClassInfo) processedClasses.get(fqname);
        if (info == null) {

            ClassDescription c = load(fqname, true);
            info = new ClassInfo(c, isAccessible(c, true), isClassVisibleOutside(c));

            if (info.superClass != null)
                addSubClass(info.superClass, fqname);

            if(info.superInterfaces != null && info.superInterfaces.length > 0) {
                for ( int i=0; i < info.superInterfaces.length; i++) {
                    addSubClass(info.superInterfaces[i], fqname);
                }
            }

            processedClasses.put(fqname, info);
        }
        return info;
    }


    private Map directSubClasses = new HashMap();

    private void addSubClass(String superClass, String subClass) {

        List subClasses = (List) directSubClasses.get(superClass);

        if (subClasses == null) {
            subClasses = new ArrayList(3);
            directSubClasses.put(superClass, subClasses);
        }

        subClasses.add(subClass);
    }


    private static class ClassInfo {

        private static final String[] EMPTY_INTERFACES = new String[0];

        String superClass = null;
        String[] superInterfaces = EMPTY_INTERFACES;
        boolean accessable = false;
        boolean isDocumentedAnnotation = false;
        int modifiers = 0;
        boolean isVisibleOutside;

        public ClassInfo(ClassDescription c, boolean accessable, boolean visible) {

            modifiers = c.getModifiers();

            SuperClass sc = c.getSuperClass();
            if (sc != null)
                superClass = sc.getQualifiedName();

            SuperInterface[] intfs = c.getInterfaces();
            int len = intfs.length;
            if (len > 0) {
                superInterfaces = new String[len];
                for (int i = 0; i < len; ++i)
                    superInterfaces[i] = intfs[i].getQualifiedName();
            }

            this.accessable = accessable;
            this.isVisibleOutside = visible;
            this.isDocumentedAnnotation = c.isDocumentedAnnotation();
        }
    }


    public int getTrackMode() {
        return trackMode;
    }


    private HashMap processedClasses = new HashMap();

    class DefaultIsAccessibleFilter implements Filter {

        private boolean isAccessible(ClassDescription c) {

            if (c.isPackageInfo())
                return true;

            if (trackMode == ALL_PUBLIC)
                return c.isPublic() || c.isProtected();

            boolean result = false;

            try {
                result = c.getClassHierarchy().isClassVisibleOutside(c);
            }
            catch (ClassNotFoundException e) {
                if (SigTest.debug)
                    e.printStackTrace();
            }
            return result;
        }

        public boolean accept(ClassDescription cls) {
            return isAccessible(cls);
        }
    }

}


