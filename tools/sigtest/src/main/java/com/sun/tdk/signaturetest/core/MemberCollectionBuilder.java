/*
 * $Id: MemberCollectionBuilder.java 4504 2008-03-13 16:12:22Z sg215604 $
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

import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;



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
 */
public class MemberCollectionBuilder {

    private ClassCorrector cc;
    private Erasurator erasurator = new Erasurator();
    private Transformer defaultTransformer = new DefaultAfterBuildMembersTransformer();
    private Log log;
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(MemberCollectionBuilder.class);
    private BuildMode mode = BuildMode.NORMAL;
    private ClassHierarchy secondCH;

    /**
     * Selftracing can be turned on by setting FINER level
     * for logger com.sun.tdk.signaturetest.core.MemberCollectionBuilder
     * It can be done via custom logging config file, for example:
     * java -Djava.util.logging.config.file=/home/ersh/wrk/st/trunk_prj/logging.properties -jar sigtest.jar
     * where logging.properties context is:
     * -------------------------------------------------------------------------
     * handlers= java.util.logging.FileHandler, java.util.logging.ConsoleHandler
     * java.util.logging.FileHandler.pattern = sigtest.log.xml
     * java.util.logging.FileHandler.formatter = java.util.logging.XMLFormatter
     * com.sun.tdk.signaturetest.core.MemberCollectionBuilder.level = FINER
     * -------------------------------------------------------------------------
     * In this case any java.util compatible log viewer can be used, for instance
     * Apache Chainsaw (http://logging.apache.org/chainsaw)
     */
    static Logger logger = Logger.getLogger(MemberCollectionBuilder.class.getName());

    public MemberCollectionBuilder(Log log, JDKExclude jdkExclude) {
        this.cc = jdkExclude == null ? new ClassCorrector(log) : new ClassCorrector(log, jdkExclude);
        this.log = log;

        // not configured externally
        if (logger.getLevel() == null) {
            logger.setLevel(Level.OFF);
        }
    }
    
    public MemberCollectionBuilder(Log log) {
        this.cc = new ClassCorrector(log);
        this.log = log;

        // not configured externally
        if (logger.getLevel() == null) {
            logger.setLevel(Level.OFF);
        }
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

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(
                    "**** createMembers for " + cl.getName() + "\n" +
                            "** addInherited=" + addInherited + "\n" +
                            "** fixClass=" + fixClass + "\n" +
                            "** checkHidding=" + checkHidding + "\n" +
                            "** BuildMode=" + mode);
        }

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

        // for APICheck
        // we have to remove members inherited from superclasses outside the scope
        // (not included to not transitively closed signature file)
        // or superclasses where inherited chain was interrupted
        //
        // example 1: signature file contains only java.util classes
        // that means that all members from java.lang superclasses (including java.lang.Object)
        // must be removed because them are not existed for signature file
        //
        // example 2: signature file contains only java.lang classes
        // consider by java.lang.RuntimePermission hierarcy:
        // java.lang.Object
        //   extended by java.security.Permission
        //       extended by java.security.BasicPermission
        //           extended by java.lang.RuntimePermission
        // Object is in the scope but inheritance chain was interrupted by the classes
        // outside the java.lang package. So in this case RuntimePermission should have no inherited members
        if (mode == BuildMode.TESTABLE) {
            MemberCollection cleaned = new MemberCollection();
            int memcount = 0;
            for (Iterator e = cl.getMembersIterator(); e.hasNext();) {
                memcount++;
                MemberDescription mr = (MemberDescription) e.next();
                MemberType mt = mr.getMemberType();
                if (mt != MemberType.SUPERCLASS) {
                    if (!isAccessible(mr.getDeclaringClassName())) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("BuildMode.TESTABLE - removing " + mr);
                        }
                        continue;
                    }
                    if (!mr.getDeclaringClassName().equals(cl.getQualifiedName())) {
                        String cn = cl.getQualifiedName();
                        String dcn = mr.getDeclaringClassName();
                        if (!isAncestor(cn, dcn)) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.fine("BuildMode.TESTABLE - removing " + mr);
                            }
                            continue;
                        }
                    }
                }
                cleaned.addMember(mr);
            }
            if (cleaned.getAllMembers().size() != memcount) {
                cl.setMembers(cleaned);
            }
        }


        t = PluginAPI.AFTER_CLASS_CORRECTOR.getTransformer();
        if (t != null)
            t.transform(cl);
    }

    // gently find ancestors.
    // don't use here ClassHierarchy's methods
    // because they are not stateless!
    private boolean isAncestor(String clName, String superClName) {
        try {
            ClassDescription c = secondCH.load(clName);
            SuperClass superCl = c.getSuperClass();
            if (superCl != null && superClName.equals(superCl.getQualifiedName())) {
                return true;
            }
            SuperInterface[] sis = c.getInterfaces();
            for (int i = 0; i < sis.length; i++) {
                if (superClName.equals(sis[i].getQualifiedName())) {
                    return true;
                }
            }
            if (superCl != null && isAncestor(superCl.getQualifiedName(), superClName)) {
                return true;
            }
            for (int i = 0; i < sis.length; i++) {
                if (isAncestor(sis[i].getQualifiedName(), superClName)) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
        return false;
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

        logger.finer("Getting members for " + cl.getQualifiedName());
        // required for correct overriding checking
        erasurator.parseTypeParameters(cl);

        List paramList = null;
        MemberCollection retVal = new MemberCollection();

        // creates declared members
        MemberDescription[] methods = cl.getDeclaredMethods();
        MemberDescription[] fields = cl.getDeclaredFields();
        MemberDescription[] classes = cl.getDeclaredClasses();
        MemberDescription[] intrfs = cl.getInterfaces();

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
        if (paramList != null) {
            intrfs = Erasurator.replaceFormalParameters(clsName, intrfs, paramList, skipRawTypes);
        }

        MethodOverridingChecker overridingChecker = new MethodOverridingChecker(erasurator);
        overridingChecker.addMethods(methods);
        retVal = addSuperMembers(methods, retVal);
        retVal = addSuperMembers(fields, retVal);
        retVal = addSuperMembers(classes, retVal);

        logger.finer(" direct interfaces");
        for (int i = 0; i < intrfs.length; ++i) {
            SuperInterface s = (SuperInterface) intrfs[i];
            s.setDirect(true);
            s.setDeclaringClass(cl.getQualifiedName());
        }
        retVal = addSuperMembers(intrfs, retVal);

        if (addInherited) {
            addInherited(checkHidding, cl, hierarchy, paramList, skipRawTypes,
                    overridingChecker, retVal);
        } else {
            fixAnnotations(cl, hierarchy);
        }

        return retVal;
    }


    private void addInherited(boolean checkHidding, ClassDescription cl, ClassHierarchy hierarchy, List paramList, boolean skipRawTypes, MethodOverridingChecker overridingChecker, MemberCollection retVal) throws ClassNotFoundException {

        String clsName = cl.getQualifiedName();
        logger.finer(" adding inherited members - superclasses");
        Set internalClasses = Collections.EMPTY_SET;
        if (checkHidding) {
            internalClasses = cl.getInternalClasses();
        }
        Map inheritedFields = new HashMap();
        SuperClass superClassDescr = cl.getSuperClass();
        if (superClassDescr != null) {
            try {
                // creates members inherited from superclass
                ClassDescription superClass = hierarchy.load(superClassDescr.getQualifiedName());
                MemberCollection superMembers = getMembers(superClass, superClassDescr.getTypeParameters(), false, true, true, checkHidding);
                findInheritableAnnotations(cl, superClass);
                //exclude non-accessible members
                superMembers = getAccessibleMembers(superMembers, cl, superClass);
                // process superclass methods
                Collection coll = superMembers.getAllMembers();
                if (paramList != null) {
                    coll = Erasurator.replaceFormalParameters(clsName, coll, paramList, skipRawTypes);
                }
                for (Iterator it = coll.iterator(); it.hasNext();) {
                    MemberDescription supMD = (MemberDescription) it.next();
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(" ? " + supMD);
                    }
                    if (supMD.isMethod()) {
                        if (addInheritedMethod(supMD, overridingChecker, retVal, hierarchy, superClass, cl)) continue;
                    } else if (supMD.isField()) {
                        // store fields in temporary collection
                        supMD.unmark();
                        inheritedFields.put(supMD.getName(), supMD);
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(" store inherited field for further processing " + supMD);
                        }
                    } else if (supMD.isSuperInterface()) {
                        SuperInterface si = (SuperInterface) supMD.clone();
                        si.setDirect(false);
                        retVal.addMember(si);
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(" added inherited superinterface " + si);
                        }
                    } else if (supMD.isInner()) {
                        if (!internalClasses.contains(supMD.getName())) {
                            retVal.addMember(supMD);
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(" added inherited inner class " + supMD);
                            }
                        }
                    } else {
                        retVal.addMember(supMD);
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(" added inherited member " + supMD);
                        }
                    }
                }
            } catch (ClassNotFoundException ex) {
                if (mode != BuildMode.SIGFILE && mode != BuildMode.APICOV_REAL ) {
                    logger.log(Level.FINER, "Class not found", ex);
                    throw ex;
                }
            }
        }
        addInheritedFromInterfaces(cl, hierarchy, checkHidding, paramList,
                skipRawTypes, overridingChecker,
                retVal, inheritedFields, internalClasses);
    }

    private void addInheritedFromInterfaces(ClassDescription cl,
            ClassHierarchy hierarchy, boolean checkHidding,
            List paramList, boolean skipRawTypes,
            MethodOverridingChecker overridingChecker,
            MemberCollection retVal, Map inheritedFields,
            Set internalClasses) throws ClassNotFoundException {

        String clsName = cl.getQualifiedName();
        logger.finer(" adding inherited members - superinterfaces");
        // findMember direct interfaces
        SuperInterface[] interfaces = cl.getInterfaces();
        HashSet xfCan = new HashSet();
        for (int i = 0; i < interfaces.length; i++) {
            try {
                ClassDescription intf = hierarchy.load(interfaces[i].getQualifiedName());
                MemberCollection h = getMembers(intf, interfaces[i].getTypeParameters(), false, true, true, checkHidding);
                Collection coll = h.getAllMembers();
                if (paramList != null) {
                    coll = Erasurator.replaceFormalParameters(clsName, coll, paramList, skipRawTypes);
                }
                for (Iterator it = coll.iterator(); it.hasNext();) {
                    MemberDescription memb = (MemberDescription) it.next();
                    if (memb.isMethod()) {
                        MethodDescr m = (MethodDescr) memb;
                        MethodDescr overriden = overridingChecker.getOverridingMethod(m, true);
                        MemberDescription erased = erasurator.processMember(memb);
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(" ? consider interface method " + m);
                        }
                        if (overriden != null && mode == BuildMode.TESTABLE) {
                            if (!isAccessible(overriden.getDeclaringClassName())) {
                                boolean doFix = false;
                                int mods = 0;
                                try {
                                    mods = hierarchy.getClassModifiers(overriden.getDeclaringClassName());
                                } catch (ClassNotFoundException ex) {
                                    doFix = true;
                                }
                                if (doFix && Modifier.hasModifier(mods, Modifier.PUBLIC) || Modifier.hasModifier(mods, Modifier.PROTECTED)) {
                                    if (logger.isLoggable(Level.FINER)) {
                                        logger.finer(" ? change interface method from " + overriden + " to " + memb);
                                    }
                                    retVal.changeMember(overriden, memb);
                                    overriden = null;
                                }
                            }
                        }
                        if (overriden == null) {
                            retVal.addMember(m);
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(" added " + m);
                            }
                        } else if (!PrimitiveTypes.isPrimitive(m.getType()) && !m.getType().endsWith("]")) {
                            try {
                                String existReturnType = overriden.getType();
                                String newReturnType = erased.getType();
                                if (!existReturnType.equals(newReturnType) && (cl.getClassHierarchy().getSuperClasses(newReturnType).contains(existReturnType) || cl.getClassHierarchy().getAllImplementedInterfaces(newReturnType).contains(existReturnType))) {
                                    retVal.updateMember(memb);
                                    if (logger.isLoggable(Level.FINER)) {
                                        logger.finer(" updated " + memb);
                                    }
                                }
                            } catch (ClassNotFoundException e) {
                                logger.log(Level.FINER, " returntype not found " + m.getType(), e);
                                log.storeWarning(i18n.getString("MemberCollectionBuilder.warn.returntype.notresolved", m.getType()), null);
                            }
                        } else {
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(" didn't added because of overriding " + m + " conflicts with " + overriden);
                            }
                        }
                    } else if (memb.isField()) {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(" ? consider interface field " + memb);
                        }
                        MemberDescription storedFid = (MemberDescription) inheritedFields.get(memb.getName());
                        if (storedFid != null) {
                            // the same constant can processed several times (e.g. if the same interface is extended/implemented twice)
                            if (!storedFid.getQualifiedName().equals(memb.getQualifiedName())) {
                                storedFid.mark();
                                if (!hierarchy.isClassVisibleOutside(memb.getDeclaringClassName())) {
                                    xfCan.add(memb.getName());
                                }
                            }
                        } else {
                            memb.unmark();
                            inheritedFields.put(memb.getName(), memb);
                        }
                    } else if (memb.isSuperInterface()) {
                        SuperInterface si = (SuperInterface) memb.clone();
                        si.setDirect(false);
                        retVal.addMember(si);
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(" added superinterface " + si);
                        }
                    } else if (memb.isInner()) {

                        if (!internalClasses.contains(memb.getName()) && retVal.findSimilar(memb) == null) {
       //                     retVal.addMember(memb);
                            if (logger.isLoggable(Level.FINER)) {
                                logger.finer(" added inner class " + memb);
                            }
                        } else {
                            //System.err.println("Artefact class found " + memb.getName());
                            if (!hierarchy.isClassVisibleOutside(memb.getDeclaringClassName())) {
                                cl.addXClasses(memb.getName());
                            }
                        }

                    } else {
                        retVal.addMember(memb);
                        if (logger.isLoggable(Level.FINER)) {
                            logger.finer(" added " + memb);
                        }
                    }
                }
            } catch (ClassNotFoundException ex) {
                if (mode != BuildMode.SIGFILE && mode != BuildMode.APICOV_REAL) {
                    logger.log(Level.FINER, " not found class", ex);
                    throw ex;
                }
            }
        }
        Set internalFields = Collections.EMPTY_SET;
        Set xFields = Collections.EMPTY_SET;
        if (checkHidding) {
            internalFields = cl.getInternalFields();
            xFields = cl.getXFields();
        }
        // add inherited fields that have no conflicts with each other
        for (Iterator it = inheritedFields.values().iterator(); it.hasNext();) {
            MemberDescription field = (MemberDescription) it.next();
            String fiName = field.getName();
            if (!field.isMarked() && !internalFields.contains(fiName) && !xFields.contains(fiName)) {
                retVal.addMember(field);
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer(" added interface field " + field);
                }
            } else {
                if (xfCan.contains(field.getName())) {
                    // this code must be in ClassCorrector - here is wrong place !
                    System.err.println("Phantom field found " + field.getQualifiedName());
                    if (logger.isLoggable(Level.FINER)) {
                        // this is actually very serious design error -
                        // we have to print warning or even error
                        logger.finer("added x-hider for " + cl.getQualifiedName() + " is " + fiName);
                    }
                    cl.addXFields(fiName);
                }
            }
        }
        if (!cl.getXClasses().isEmpty()) {
            Iterator it = cl.getXClasses().iterator();
            while (it.hasNext()) {
                String xClass = (String) it.next();
                Iterator rvi = retVal.iterator();
                while (rvi.hasNext()) {
                    MemberDescription rm = (MemberDescription) rvi.next();
                    if (rm.isInner() && rm.getName().equals(xClass)) {
                        System.err.println("Phantom class found " + rm.getQualifiedName());
                        rvi.remove();
                    }
                }
            }
        }

    }


    private boolean addInheritedMethod(MemberDescription supMD,
            MethodOverridingChecker overridingChecker,
            MemberCollection retVal,
            ClassHierarchy hierarchy,
            ClassDescription superClass,
            ClassDescription cl) {
        MethodDescr m = (MethodDescr) supMD;
        MethodDescr overriden = overridingChecker.getOverridingMethod(m, true);
        MemberDescription erased = erasurator.processMember(supMD);
        if (overriden == null) {
            retVal.addMember(m);
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(" added inherited method " + m);
            }
        } else if (!PrimitiveTypes.isPrimitive(m.getType()) && !m.getType().endsWith("]")) {
            try {
                if (!hierarchy.isAccessible(superClass)) {
                    return true;
                }
                String existReturnType = overriden.getType();
                String newReturnType = erased.getType();
                if (!existReturnType.equals(newReturnType) && (cl.getClassHierarchy().getSuperClasses(newReturnType).contains(existReturnType) || cl.getClassHierarchy().getAllImplementedInterfaces(newReturnType).contains(existReturnType))) {
                    retVal.updateMember(supMD);
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(" added inherited method " + supMD);
                    }
                }
            } catch (ClassNotFoundException e) {
                if (mode != BuildMode.SIGFILE && mode != BuildMode.APICOV_REAL) {
                    log.storeWarning(i18n.getString("MemberCollectionBuilder.warn.returntype.notresolved", m.getType()), null);
                    logger.log(Level.FINER, " " + i18n.getString("MemberCollectionBuilder.warn.returntype.notresolved", m.getType()), e);
                }
            }
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(" didn't added because of overriding " + m + " conflicts with " + overriden);
            }
        }
        return false;
    }



    private void fixAnnotations(ClassDescription cl, ClassHierarchy hierarchy) throws ClassNotFoundException {
        // TODO (Roman Makarchuk) temporary solution !!!
        // see UseAnnotClss025 test. ClassCorrector should also move annotations
        // from invisible superclass
        SuperClass superClassDescr = cl.getSuperClass();
        if (superClassDescr != null) {
            try {
                ClassDescription superClass = hierarchy.load(superClassDescr.getQualifiedName());
                findInheritableAnnotations(cl, superClass);
            } catch (ClassNotFoundException ex) {
                if (mode != BuildMode.SIGFILE && mode != BuildMode.APICOV_REAL) {
                    logger.log(Level.FINER, " not found class", ex);
                    throw ex;
                }
            }
        }
    }


    private MemberCollection addSuperMembers(MemberDescription[] from,
            MemberCollection to) {
        logger.finer(" adding members");
        for (int i = 0; i < from.length; i++) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("  +" + from[i]);
            }
            to.addMember(from[i]);
        }
        return to;
    }

    public static class BuildMode {
        public static final BuildMode NORMAL = new BuildMode("NORMAL");
        public static final BuildMode SIGFILE = new BuildMode("SIGFILE");
        public static final BuildMode TESTABLE = new BuildMode("TESTABLE");
        public static final BuildMode APICOV_REAL = new BuildMode("APICOV_REAL");
        private String name;

        private BuildMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private boolean isAccessible(String qualifiedName) {
        try {
            secondCH.load(qualifiedName);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
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

    public void setBuildMode(BuildMode bm) {
        mode = bm;
    }

    public void setSecondClassHierarchy(ClassHierarchy signatureClassesHierarchy) {
        secondCH = signatureClassesHierarchy;
    }

    class DefaultAfterBuildMembersTransformer implements Transformer {

        public ClassDescription transform(ClassDescription cls) {

            for (Iterator it = cls.getMembersIterator(); it.hasNext();) {
                MemberDescription mr = (MemberDescription) it.next();

                boolean isBridgeMethod = mr.hasModifier(Modifier.BRIDGE);
                boolean isSynthetic = mr.hasModifier(Modifier.ACC_SYNTHETIC);

                // skip synthetic methods and constructors
                if (isSynthetic) {
                    if (logger.isLoggable(Level.INFO)) {
                        if (mr.isConstructor()) {
                            logger.info(i18n.getString("MemberCollectionBuilder.message.synthetic_constr_skipped",
                                    mr.getQualifiedName() + "(" + mr.getArgs() + ")"));
                        } else if (mr.isMethod()) {
                            String signature = mr.getType() + " " + mr.getQualifiedName() + "(" + mr.getArgs() + ")";
                            if (isBridgeMethod) {
                                logger.info(i18n.getString("MemberCollectionBuilder.message.bridge", signature));
                            } else {
                                logger.info(i18n.getString("MemberCollectionBuilder.message.synthetic_method_skipped",
                                        signature));
                            }
                        }
                    }
                    it.remove();
                    continue;
                }

                // includes only public and protected constructors, methods, classes, fields
                if (!(mr.isPublic() || mr.isProtected() || mr.isSuperInterface() || mr.isSuperClass()))
                    it.remove();
            }
            return cls;
        }
    }


}
/**
 * @author Maxim Sokolnikov
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 * @version 05/03/22
 */
class MethodOverridingChecker {

    private Map /*<String, MethodDescr>*/ methodSignatures = new HashMap();
    private Erasurator erasurator;

    public MethodOverridingChecker(Erasurator er) {
        erasurator = er;
    }

    public void addMethod(MethodDescr m) {
        MethodDescr cloned_m = (MethodDescr) erasurator.processMember(m);
        methodSignatures.put(cloned_m.getSignature(), cloned_m);
    }

    public MethodDescr getOverridingMethod(MethodDescr m, boolean autoAdd) {
        MethodDescr cloned_m = (MethodDescr) erasurator.processMember(m);
        String signature = cloned_m.getSignature();
        MethodDescr isOverriding = (MethodDescr) methodSignatures.get(signature);
        if (isOverriding == null && autoAdd)
            methodSignatures.put(signature, cloned_m);
        return isOverriding;
    }

    public void addMethods(MemberDescription[] methods) {
        for (int i = 0; i < methods.length; ++i)
            addMethod((MethodDescr) methods[i]);
    }
}
