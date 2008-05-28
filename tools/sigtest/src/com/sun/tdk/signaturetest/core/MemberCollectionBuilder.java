/*
 * $Id: MemberCollectionBuilder.java 4504 2008-03-13 16:12:22Z sg215604 $
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

package com.sun.tdk.signaturetest.core;

import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.util.*;

/**
 * This class provides methods to findByName an load a class and to compile
 * a <b>ClassDescription</b> for it. The method <b>Class</b>.<code>forName()</code>
 * is used to findByName a <code>Class</code> object. If the advanced method
 * <code>forName</code>(<b>String</b>,<code>boolean</code>,<b>ClassLoader</b>)
 * is unavailable, the rougher method <code>forName</code>(<b>String</b>) is used.
 *
 * @author Maxim Sokolnikov
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 * @version 05/03/22
 */
public class MemberCollectionBuilder {

    private ClassCorrector cc;
    private Erasurator erasurator = new Erasurator();
    private Transformer defaultTransformer = new DefaultAfterBuildMembersTransformer();
    private Log log;
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(MemberCollectionBuilder.class);

    public MemberCollectionBuilder(Log log) {
        this.cc = new ClassCorrector(log);
        this.log = log;
    }

    /**
     * Generate <code>members</code> field for the given <b>ClassDescription</b>
     * <code>cl</code>. Recursively findByName all inherited fields, methods, nested
     * classes, and interfaces for the class having the name prescribed by
     * <code>cl</code>.
     *
     * @see MemberDescription
     */
    public void createMembers(ClassDescription cl, boolean addInherited, boolean fixClass, boolean checkHidding) throws ClassNotFoundException {

        MemberCollection members = getMembers(cl, addInherited, checkHidding);

        // add super class
        SuperClass spr = cl.getSuperClass();

        if (spr != null)
            members.addMember(spr);

        //add constructors
        ConstructorDescr[] constr = cl.getDeclaredConstructors();
        for (int i = 0; i < constr.length; i++) {
            members.addMember(constr[i]);
        }

        cl.setMembers(members);

        Transformer t = PluginAPI.AFTER_BUILD_MEMBERS.getTransformer();
        if (t == null)
            t = defaultTransformer;

        t.transform(cl);

        if (fixClass) {
            t = PluginAPI.CLASS_CORRECTOR.getTransformer();
            if (t == null)
                t = cc;
            t.transform(cl);
        }

        t = PluginAPI.AFTER_CLASS_CORRECTOR.getTransformer();
        if (t != null)
            t.transform(cl);
    }


    /**
     * Collect <b>MemberDescription</b>s for all fields, methods, and nested
     * classes of the given class described by <code>cl</code>. Recursively findByName
     * all inherited members, as far as members declared by the class
     * <code>cl</code>.
     *
     * @see com.sun.tdk.signaturetest.model.MemberDescription
     */

    private MemberCollection getMembers(ClassDescription cl, boolean addInherited, boolean checkHidding) throws ClassNotFoundException {
        return getMembers(cl, null, true, false, addInherited, checkHidding);
    }

    private MemberCollection getMembers(ClassDescription cl, String actualTypeParams,
                                        boolean skipRawTypes, boolean callErasurator, boolean addInherited, boolean checkHidding) throws ClassNotFoundException {

        assert cl != null;

        // required for correct overriding checking
        erasurator.parseTypeParameters(cl);

        List paramList = null;
        MemberCollection retVal = new MemberCollection();

        // creates declared members
        MemberDescription[] methods = cl.getDeclaredMethods();
        MemberDescription[] fields = cl.getDeclaredFields();
        MemberDescription[] classes = cl.getDeclaredClasses();

        String clsName = cl.getQualifiedName();
        ClassHierarchy hierarchy = cl.getClassHierarchy();

        if (actualTypeParams != null) {
            paramList = Erasurator.splitParameters(actualTypeParams);
            methods = Erasurator.replaceFormalParameters(clsName, methods, paramList, skipRawTypes);
            fields = Erasurator.replaceFormalParameters(clsName, fields, paramList, skipRawTypes);
            classes = Erasurator.replaceFormalParameters(clsName, classes, paramList, skipRawTypes);

        } else if (callErasurator && cl.getTypeParameters() != null) {

            List boundsList = cl.getTypeBounds();
            methods = Erasurator.replaceFormalParameters(clsName, methods, boundsList, false);
            fields = Erasurator.replaceFormalParameters(clsName, fields, boundsList, false);
            classes = Erasurator.replaceFormalParameters(clsName, classes, boundsList, false);
        }

        MethodOverridingChecker overridingChecker = new MethodOverridingChecker(erasurator);

        overridingChecker.addMethods(methods);

        for (int i = 0; i < methods.length; i++)
            retVal.addMember(methods[i]);

        for (int i = 0; i < fields.length; i++)
            retVal.addMember(fields[i]);

        for (int i = 0; i < classes.length; i++)
            retVal.addMember(classes[i]);


        MemberDescription[] intrfs = cl.getInterfaces();

        if (paramList != null)
            intrfs = Erasurator.replaceFormalParameters(clsName, intrfs, paramList, skipRawTypes);

        for (int i = 0; i < intrfs.length; ++i) {
            SuperInterface s = (SuperInterface) intrfs[i];
            s.setDirect(true);
            retVal.addMember(s);
        }

        if (addInherited) {

            Set internalClasses = Collections.EMPTY_SET;
            if (checkHidding)
                internalClasses = cl.getInternalClasses();

            Map inheritedFields = new HashMap();


            SuperClass superClassDescr = cl.getSuperClass();
            if (superClassDescr != null) {
                // creates members inherited from superclass
                ClassDescription superClass = hierarchy.load(superClassDescr.getQualifiedName());

                MemberCollection superMembers = getMembers(superClass, superClassDescr.getTypeParameters(), false, true, addInherited, checkHidding);

                findInheritableAnnotations(cl, superClass);

                //exclude non-accessible members
                superMembers = getAccessibleMembers(superMembers, cl, superClass);

                // process superclass methods
                Collection coll = superMembers.getAllMembers();
                if (paramList != null)
                    coll = Erasurator.replaceFormalParameters(clsName, coll, paramList, skipRawTypes);

                for (Iterator it = coll.iterator(); it.hasNext();) {
                    MemberDescription fid = (MemberDescription) it.next();

                    if (fid.isMethod()) {
                        MethodDescr m = (MethodDescr) fid;
                        MethodDescr overriden = overridingChecker.getOverridingMethod(m, true);
                        MemberDescription erased = erasurator.processMember(fid);
                        if (overriden == null) {
                            retVal.addMember(m);
                        } else if (!PrimitiveTypes.isPrimitive(m.getType())
                                && !m.getType().endsWith("]")) {
                            try {
                                String existReturnType = overriden.getType();                            
                                String newReturnType = erased.getType();                            
                                if (!existReturnType.equals(newReturnType) 
                                        && (cl.getClassHierarchy().getSuperClasses(newReturnType).contains(existReturnType)
                                        || cl.getClassHierarchy().getAllImplementedInterfaces(newReturnType).contains(existReturnType))) {
                                    retVal.updateMember(fid);
                                } 
                            }
                            catch (ClassNotFoundException e) {
                                log.storeWarning(i18n.getString("MemberCollectionBuilder.warn.returntype.notresolved", m.getType()));
                            }                                        
                        }

                    } else if (fid.isField()) {
                        // store fields in temporary collection
                        fid.unmark();
                        inheritedFields.put(fid.getName(), fid);
                    } else if (fid.isSuperInterface()) {
                        SuperInterface si = (SuperInterface) fid.clone();
                        si.setDirect(false);
                        retVal.addMember(si);
                    } else if (fid.isInner()) {
                        if (!internalClasses.contains(fid.getName()))
                            retVal.addMember(fid);
                    } else retVal.addMember(fid);
                }

            }

            // findMember direct interfaces
            SuperInterface interfaces[] = cl.getInterfaces();

            for (int i = 0; i < interfaces.length; i++) {

                ClassDescription intf = hierarchy.load(interfaces[i].getQualifiedName());

                MemberCollection h = getMembers(intf, interfaces[i].getTypeParameters(), false, true, addInherited, checkHidding);

                Collection coll = h.getAllMembers();

                if (paramList != null)
                    coll = Erasurator.replaceFormalParameters(clsName, coll, paramList, skipRawTypes);

                for (Iterator it = coll.iterator(); it.hasNext();) {
                    MemberDescription fid = (MemberDescription) it.next();

                    if (fid.isMethod()) {
                        MethodDescr m = (MethodDescr) fid;
                        MethodDescr overriden = overridingChecker.getOverridingMethod(m, true);
                        MemberDescription erased = erasurator.processMember(fid);
                        if (overriden == null) {
                            retVal.addMember(m);
                        } else if (!PrimitiveTypes.isPrimitive(m.getType())
                                && !m.getType().endsWith("]")) {
                            try {
                                String existReturnType = overriden.getType();                            
                                String newReturnType = erased.getType();                            
                                if (!existReturnType.equals(newReturnType) 
                                        && (cl.getClassHierarchy().getSuperClasses(newReturnType).contains(existReturnType)
                                        || cl.getClassHierarchy().getAllImplementedInterfaces(newReturnType).contains(existReturnType))) {
                                    retVal.updateMember(fid);
                                } 
                            }
                            catch (ClassNotFoundException e) {
                                log.storeWarning(i18n.getString("MemberCollectionBuilder.warn.returntype.notresolved", m.getType()));
                            }                        
                        }


                    } else if (fid.isField()) {
                        MemberDescription storedFid = (MemberDescription) inheritedFields.get(fid.getName());
                        if (storedFid != null) {
                            // the same constant can processed several times (e.g. if the same interface is extended/implemented twice)
                            if (!storedFid.getQualifiedName().equals(fid.getQualifiedName()))
                                storedFid.mark();
                        } else {
                            fid.unmark();
                            inheritedFields.put(fid.getName(), fid);
                        }
                    } else if (fid.isSuperInterface()) {
                        SuperInterface si = (SuperInterface) fid.clone();
                        si.setDirect(false);
                        retVal.addMember(si);
                    } else if (fid.isInner()) {
                        if (!internalClasses.contains(fid.getName()))
                            retVal.addMember(fid);
                    } else retVal.addMember(fid);
                }

            }

            Set internalFields = Collections.EMPTY_SET;
            if (checkHidding)
                internalFields = cl.getInternalFields();

            // add inherited fields that have no conflicts with each other
            for (Iterator it = inheritedFields.values().iterator(); it.hasNext();) {
                MemberDescription field = (MemberDescription) it.next();
                if (!field.isMarked() && !internalFields.contains(field.getName()))
                    retVal.addMember(field);
            }

        } else {

            // TODO (Roman Makarchuk) temporary solution !!!
            // see UseAnnotClss025 test. ClassCorrector should also move annotations
            // from invisible superclass
            SuperClass superClassDescr = cl.getSuperClass();
            if (superClassDescr != null) {
                ClassDescription superClass = hierarchy.load(superClassDescr.getQualifiedName());
                findInheritableAnnotations(cl, superClass);
            }
        }

        return retVal;
    }

    /**
     * Filter those <b>MemberDescription</b> instances found inside the
     * given <code>members</code> collection available for use by the given
     * <code>subclass</code>, provided they are members of the given
     * <code>superClass</code>.
     *
     * @see MemberDescription
     */
    private MemberCollection getAccessibleMembers(MemberCollection members,
                                                  ClassDescription subclass,
                                                  ClassDescription superClass) {

        String pkg = subclass.getPackageName();
        boolean isSamePackage = pkg.equals(superClass.getPackageName());
        MemberCollection retVal = new MemberCollection();

        for (Iterator e = members.iterator(); e.hasNext();) {
            MemberDescription mbr = (MemberDescription) e.next();
            if ((mbr.isPublic() || mbr.isProtected() || isSamePackage || mbr.isSuperInterface()) && !mbr.isPrivate())
                retVal.addMember(mbr);
        }

        return retVal;
    }

    //  Find all inheritable annotations
    private void findInheritableAnnotations(ClassDescription subclass, ClassDescription superClass) {

        AnnotationItem[] superClassAnnoList = superClass.getAnnoList();

        if (superClassAnnoList.length != 0) {

            Set tmp = new TreeSet();

            AnnotationItem[] subClassAnnoList = subclass.getAnnoList();

            for (int i = 0; i < superClassAnnoList.length; i++)
                if (superClassAnnoList[i].isInheritable())
                    tmp.add(superClassAnnoList[i]);

            for (int i = 0; i < subClassAnnoList.length; i++)
                tmp.add(subClassAnnoList[i]);

            if (tmp.size() != subClassAnnoList.length) {
                AnnotationItem[] newAnnoList = new AnnotationItem[tmp.size()];
                tmp.toArray(newAnnoList);
                subclass.setAnnoList(newAnnoList);
            }
        }
    }
}

/**
 * @version 05/03/22
 * @author Maxim Sokolnikov
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 */
class DefaultAfterBuildMembersTransformer implements Transformer {

    public ClassDescription transform(ClassDescription cls) {

        for (Iterator it = cls.getMembersIterator(); it.hasNext();) {
            MemberDescription mr = (MemberDescription) it.next();

            // includes only public and protected constructors, methods, classes, fields
            if (!(mr.isPublic() || mr.isProtected() || mr.isSuperInterface() || mr.isSuperClass()))
                it.remove();
        }

        return cls;
    }
}

/**
 * @version 05/03/22
 * @author Maxim Sokolnikov
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 */
class MethodOverridingChecker {

    private Map /*<String, MethodDescr>*/ methodSignatures = new HashMap();
    private Erasurator erasurator;

    public MethodOverridingChecker(Erasurator er) {
        erasurator = er;
    }

    public void addMethod(MethodDescr m) {
        MethodDescr cloned_m = (MethodDescr)erasurator.processMember(m);
        methodSignatures.put(cloned_m.getSignature(), cloned_m);
    }

    public MethodDescr getOverridingMethod(MethodDescr m, boolean autoAdd) {
        MethodDescr cloned_m  = (MethodDescr)erasurator.processMember(m);
        String signature = cloned_m.getSignature();
        MethodDescr isOverriding = (MethodDescr)methodSignatures.get(signature);
        if (isOverriding == null && autoAdd)
            methodSignatures.put(signature, cloned_m);
        return isOverriding;
    }

    public void addMethods(MemberDescription[] methods) {
        for(int i=0; i<methods.length; ++i)
            addMethod((MethodDescr)methods[i]);
    }
}