/*
 * $Id: ClassCorrector.java 4504 2008-03-13 16:12:22Z sg215604 $
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
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <b>ClassCorrector</b> is the main part of solving problems related with hidden language elements<p>
 * 
 * <li><b>public class (interface) extends package local class (interface)</b><br>
 * Sigtest should ignore base class and/or implemented interfaces
 * and move all visible base's members to the nearest visible SUBclass like Javadoc
 * do since version 1.5.</li>
 * <li><b>public inner class extends private inner class</b><br>
 * Similar solution. But Javadoc ignores such classes and it looks like a bug in Javadoc
 * </li>
 * <li><b>public method throws private exception</b><br>
 * Sigtest should substitute invisible exception to the nearest visible SUPERclass.
 * Javadoc doesn't do it and as result it generates insufficient documentation </li>
 *
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 */
public class ClassCorrector implements Transformer {

    protected ClassHierarchy classHierarchy = null;
    private Log log;
    private JDKExclude jdkExclude = new JDKExclude() {
        @Override
        public boolean isJdkClass(String name) {
            return false;
        }
    };

    /**
     * Selftracing can be turned on by setting FINER level
     * for logger com.sun.tdk.signaturetest.core.ClassCorrector
     * It can be done via custom logging config file, for example:
     * java -Djava.util.logging.config.file=/home/ersh/wrk/st/trunk_prj/logging.properties -jar sigtest.jar
     * where logging.properties context is:
     * -------------------------------------------------------------------------
     * handlers= java.util.logging.FileHandler, java.util.logging.ConsoleHandler
     * java.util.logging.FileHandler.pattern = sigtest.log.xml
     * java.util.logging.FileHandler.formatter = java.util.logging.XMLFormatter
     * com.sun.tdk.signaturetest.core.ClassCorrector.level = FINER
     * -------------------------------------------------------------------------
     * In this case any java.util compatible log viewer can be used, for instance
     * Apache Chainsaw (http://logging.apache.org/chainsaw)
     */
    private static Logger logger = Logger.getLogger(ClassCorrector.class.getName());


    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(ClassCorrector.class);

    public ClassCorrector(Log log, JDKExclude jdkExclude) {
        this.jdkExclude = jdkExclude != null ? jdkExclude :
                new JDKExclude() {
                    @Override
                    public boolean isJdkClass(String name) {
                        return false;
                    }
                };
        this.log = log;
        // not configured externally
        if(logger.getLevel() == null) {
            logger.setLevel(Level.OFF);
        }
        
    }
    
    public ClassCorrector(Log log) {
        this(log, null);
    }


    public ClassDescription transform(ClassDescription cl) throws ClassNotFoundException {

        classHierarchy = cl.getClassHierarchy();
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(">>>> ClassCorrector for class " + cl.getQualifiedName());
        }

        replaceInvisibleExceptions(cl);
        replaceInvisibleInMembers(cl);
        // 1)replace invisible return-types
        // 2)fix invisible parameter types
        fixMethods(cl);
        removeInvisibleInterfaces(cl);
        fixInvisibleSuperclasses(cl);
        removeDuplicatedConstants(cl);
        checkClassTypeParameters(cl);
        removeInvisibleAnnotations(cl);
        additionalChecks(cl);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("<<<< ClassCorrector for class " + cl.getQualifiedName());
        }
        return cl;
    }


    private void additionalChecks(ClassDescription cl) throws ClassNotFoundException {
        if (classHierarchy.isClassVisibleOutside(cl)) {

            if (!cl.hasModifier(Modifier.ABSTRACT))
                return;

            boolean ctorExists = false;

            ConstructorDescr[] ctors = cl.getDeclaredConstructors();
            for (int i = 0; i < ctors.length; ++i) {
                ConstructorDescr c = ctors[i];
                if (c.hasModifier(Modifier.PUBLIC) || c.hasModifier(Modifier.PROTECTED))
                {
                    ctorExists = true;
                    break;
                }
            }

            if (!ctorExists)
                return;

            MethodDescr[] methods = cl.getDeclaredMethods();
            for (int i = 0; i < methods.length; ++i) {
                MethodDescr mr = methods[i];
                if ((mr.isMethod()) && !mr.hasModifier(Modifier.PUBLIC) &&
                        !mr.hasModifier(Modifier.PROTECTED) && mr.hasModifier(Modifier.ABSTRACT)) {
                    String invargs[] = {cl.getQualifiedName(), mr.toString()};
                    log.storeWarning(i18n.getString("ClassCorrector.error.class.useless_abst_public_class", invargs), null);
                }
            }
        }
    }

    /**
     * Sigtest should substitute invisible exception to the nearest visible SUPERclass.
     */
    private void replaceInvisibleExceptions(ClassDescription c) throws ClassNotFoundException {

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isMethod() || mr.isConstructor()) {
                replaceInvisibleExceptions(mr);
            }
        }
    }


    private void replaceInvisibleExceptions(MemberDescription mr) throws ClassNotFoundException {

        String throwables = mr.getThrowables();

        if (!MemberDescription.EMPTY_THROW_LIST.equals(throwables)) {
            boolean mustCorrect = false;
            StringBuffer sb = new StringBuffer();

            int startPos = 0, pos;
            do {
                if (sb.length() != 0)
                    sb.append(MemberDescription.THROWS_DELIMITER);

                String exceptionName;
                pos = throwables.indexOf(MemberDescription.THROWS_DELIMITER, startPos);
                if (pos != -1) {
                    exceptionName = throwables.substring(startPos, pos);
                    startPos = pos + 1;
                } else
                    exceptionName = throwables.substring(startPos);

                if (!jdkExclude.isJdkClass(exceptionName) && isInvisibleClass(exceptionName)) {
                    List supers = classHierarchy.getSuperClasses(exceptionName);
                    exceptionName = findVisibleReplacement(exceptionName, supers, "java.lang.Throwable", true);
                    mustCorrect = true;
                }

                sb.append(exceptionName);

            } while (pos != -1);

            if (mustCorrect) {
                String invargs[] = {mr.getQualifiedName(), throwables, sb.toString()};
                log.storeWarning(i18n.getString("ClassCorrector.message.throwslist.changed", invargs), null);

                mr.setThrowables(sb.toString());
            }
        }
    }

    private String findVisibleReplacementAndCheckInterfaces(String clName, List supers, String replaceWithClassName) throws ClassNotFoundException {

        // is it public inner class of hidden outer?
        if (isPublicInner(clName))
            return null;

        String replacement = findVisibleReplacement(clName, supers, replaceWithClassName, true);

        Set oldInt = classHierarchy.getAllImplementedInterfaces(clName);

        if (oldInt.size() != 0) {

            Set newInt = classHierarchy.getAllImplementedInterfaces(replacement);

            oldInt.removeAll(newInt); // diff

            // remove all superinterfaces from the diff
            removeSuperInterfaces(oldInt);
            int visibleInterfaces = 0;
            String iName = null;

            for (Iterator it = oldInt.iterator(); it.hasNext();) {
                String nextInt = (String) it.next();
                if (!isInvisibleClass(nextInt)) {
                    visibleInterfaces++;
                    iName = nextInt;
                }
            }

            if ("java.lang.Object".equals(replacement) && visibleInterfaces == 1)
                return iName;

            if (visibleInterfaces > 0)
                return null;
        }

        return replacement;
    }


    private String findVisibleReplacement(String clName, List supers, String replaceWithClassName, boolean findToSuper) {
        // supers sorted from analyzed class to superclass
        if (supers.size() > 0) {
            // used for members - finds nearest visible subclass
            if (!findToSuper) {
                int i = supers.indexOf(clName);
                if (i <= 0)
                    return replaceWithClassName;

                for (int pos = i - 1; pos >= 0; pos--) {
                    String name = (String) supers.get(pos);
                    if (!isInvisibleClass(name))
                        return name;
                }

            } else {
                // used for exception - finds nearest visible superclass
                for (int pos = 0; pos < supers.size(); pos++) {
                    String name = (String) supers.get(pos);
                    if (!isInvisibleClass(name))
                        return name;
                }
            }
        }
        return replaceWithClassName;
    }


    /**
     * 1) replaces invisible return-types
     * 2) fixes invisible parameter types
     * 3) fixes invisible attribute types
     */
    private void fixMethods(ClassDescription cl) throws ClassNotFoundException {

        for (Iterator e = cl.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isMethod() || mr.isField()) {
                fixType(cl, mr);
            }
            if (mr.isConstructor() || mr.isMethod())
                checkMethodParameters(cl, mr);
        }
    }


    private void fixType(ClassDescription cl, MemberDescription mr) throws ClassNotFoundException {
        String returnType = mr.getType();
        if (!MemberDescription.NO_TYPE.equals(returnType) && isInvisibleClass(returnType)) {

            returnType = stripArrays(returnType);

            List supers = Collections.EMPTY_LIST;

            // is it interface or class ? If invisible interface found replace it with java.lang.Object!!!
            if (!classHierarchy.isInterface(returnType)) {
                supers = classHierarchy.getSuperClasses(returnType);
            }

            String newName = findVisibleReplacementAndCheckInterfaces(returnType, supers, "java.lang.Object");

            if (newName != null) {

                newName = wrapArray(returnType, newName);

                mr.setType(newName);

//                if (verboseCorrector) {
                    if (!mr.isField()) {
                        String invargs[] = {cl.getName(), mr.getName(), returnType, newName};
                        log.storeWarning(i18n.getString("ClassCorrector.message.returntype.changed", invargs), logger);
                    } else {
                        String invargs[] = {cl.getName(), mr.getName(), returnType, newName};
                        log.storeWarning(i18n.getString("ClassCorrector.message.fieldtype.changed", invargs), logger);
                    }
//                }
            } else {
                if (!mr.isField()) {
                    String invargs[] = {returnType, mr.toString()};
                    log.storeError(i18n.getString("ClassCorrector.error.returntype.hidden", invargs), logger);
                } else {
                    String invargs[] = {returnType, mr.toString()};
                    log.storeError(i18n.getString("ClassCorrector.error.fieldtype.hidden", invargs), logger);
                }

            }
        }
        checkType(cl, mr);
    }


    private void checkMethodParameters(ClassDescription cl, MemberDescription mr) {
        String args = mr.getArgs();
        if (MemberDescription.NO_ARGS.equals(args))
            return;

        checkActualParameters(cl, mr, args);
    }


    private void checkType(ClassDescription cl, MemberDescription mr) {
        String type = mr.getType();

        int pos = type.indexOf('<');
        if (pos != -1)
            checkActualParameters(cl, mr, type.substring(pos));
    }

    private void checkActualParameters(ClassDescription cl, MemberDescription mr, String actualParameters) {
        StringTokenizer tz = new StringTokenizer(actualParameters, ",<>[]&", false);

        boolean firstParameter = true;

        while (tz.hasMoreTokens()) {
            String param = tz.nextToken().trim();

            if (param.length() > 0) {

                String prefix = "? super ";
                if (param.indexOf(prefix) == 0)
                    param = param.substring(prefix.length());

                prefix = "? extends ";
                if (param.indexOf(prefix) == 0)
                    param = param.substring(prefix.length());

                if (isInvisibleClass(param)) {

                    // let's ignore first synthetic parameter in nested class' constructor
                    // -allpublic option allows tracking classes like the following:
                    // class A {
                    //     public class B {}  // this class
                    // }
                    //
                    boolean isInner = cl.getQualifiedName().indexOf('$') >= 0;
                    if (mr.isConstructor() && isInner && !cl.hasModifier(Modifier.STATIC) && firstParameter) {
                        // it's ok. well, it's almost ok :-)
                        firstParameter = false;
                        continue;
                    }

                    String invargs[] = {param, mr.toString(), cl.getQualifiedName()};
                    log.storeError(i18n.getString("ClassCorrector.error.parametertype.hidden", invargs), logger);
                }
            }

        }
    }


    /**
     * This method changes "declared class" for merged visible class members,
     * which are declared in invisible superclasses.
     */
    private void replaceInvisibleInMembers(ClassDescription c) throws ClassNotFoundException {

        String className = c.getQualifiedName();

        List supers = classHierarchy.getSuperClasses(c.getQualifiedName());

        ArrayList newMembers = new ArrayList();

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {

            MemberDescription mr = (MemberDescription) e.next();

            // process methods, constructors and fields only
            if (mr.isSuperClass() || mr.isSuperInterface())
                continue;

            if (isInvisibleClass(mr.getDeclaringClassName())) {
                String newPar = findVisibleReplacement(mr.getDeclaringClassName(), supers, className, false);
                MemberDescription newMember = (MemberDescription) mr.clone();
                newMember.setDeclaringClass(newPar);

                e.remove();

                // check for existing the same. For example:
                // public interface I extends hidden { void foo(); }
                // interface hidden { void foo(); }
                if (!c.containsMember(newMember)) {

                    newMembers.add(newMember);

                    if (logger.isLoggable(Level.FINE)) {
                        String invargs[] = {mr.getQualifiedName(), mr.getDeclaringClassName(), newMember.getDeclaringClassName()};
                        logger.fine(i18n.getString("ClassCorrector.message.member.moved", invargs));
                    }


                } else {
                    if (logger.isLoggable(Level.FINE)) {
                        String invargs[] = {mr.getQualifiedName(), mr.getDeclaringClassName(), newMember.getDeclaringClassName()};
                        logger.fine(i18n.getString("ClassCorrector.message.member.removed", invargs));
                    }
                }
            }
        }

        for (int i = 0; i < newMembers.size(); ++i)
            c.add((MemberDescription) newMembers.get(i));
    }


    private void removeInvisibleInterfaces(ClassDescription c) throws ClassNotFoundException {

        List makeThemDirect = null;

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isSuperInterface()) {

                SuperInterface si = (SuperInterface) mr;
                String siName = si.getQualifiedName();

                if (isInvisibleClass(siName)) {
                    if (logger.isLoggable(Level.FINE)) {
                        String invargs[] = {mr.getQualifiedName(), c.getQualifiedName()};
                        logger.fine(i18n.getString("ClassCorrector.message.interface.removed", invargs));
                    }
                    e.remove();

                    if (si.isDirect()) {

                        if (makeThemDirect == null)
                            makeThemDirect = new ArrayList();

                        String[] intfs = classHierarchy.getSuperInterfaces(siName);

                        for (int i = 0; i < intfs.length; ++i)
                            makeThemDirect.add(intfs[i]);
                    }
                }

                if (mr.getTypeParameters() != null)
                    checkActualParameters(c, mr, mr.getTypeParameters());
            }
        }

        if (makeThemDirect != null) {

            for (Iterator it = c.getMembersIterator(); it.hasNext();) {
                MemberDescription mr = (MemberDescription) it.next();
                if (mr.isSuperInterface() && makeThemDirect.contains(mr.getQualifiedName())) {
                    // NOTE: clone not required here, because MemberCollectionBuilder clone
                    // all non-direct superinterfaces!
                    ((SuperInterface) mr).setDirect(true);
                }
            }
        }
    }

    private void fixInvisibleSuperclasses(ClassDescription c) throws ClassNotFoundException {

        SuperInterface[] intfs = null;
        MemberDescription newMember = null;

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isSuperClass()) {
                if (isInvisibleClass(mr.getQualifiedName())) {

                    ClassDescription cS = classHierarchy.load(mr.getQualifiedName());

                    List supers = classHierarchy.getSuperClasses(cS.getQualifiedName());
                    String newName = findVisibleReplacement(mr.getQualifiedName(), supers, "java.lang.Object", true);
                    newMember = (MemberDescription) mr.clone();
                    newMember.setupClassName(newName);

                    if (logger.isLoggable(Level.FINE)) {
                        String invargs[] = {c.getQualifiedName(), mr.getQualifiedName(), newName};
                        logger.fine(i18n.getString("ClassCorrector.message.super.changed", invargs));
                    }
                    e.remove();

                    intfs = cS.getInterfaces();
                }

                if (mr.getTypeParameters() != null)
                    checkActualParameters(c, mr, mr.getTypeParameters());

                break;  // only one superclass may exist!
            }
        }


        if (newMember != null)
            c.add(newMember);

        if (intfs != null) {

            for (int i = 0; i < intfs.length; ++i) {
                SuperInterface m = (SuperInterface) c.findMember(intfs[i]);
                if (m != null) {
                    m.setDirect(true);
                    m.setDeclaringClass(c.getQualifiedName());
                }
            }
        }
    }


    protected void removeSuperInterfaces(Set interfaces) throws ClassNotFoundException {

        List intfs = new ArrayList(interfaces);
        List su = new ArrayList();

        for (int i = 0; i < intfs.size(); i++) {
            String intfName = (String) intfs.get(i);

            if (intfName == null || isInvisibleClass(intfName))
                continue;

            su.clear();

            su.addAll(classHierarchy.getAllImplementedInterfaces(intfName));

            for (int j = 0; j < su.size(); j++) {

                String sui = (String) su.get(j);

                if (sui.equals(intfName))
                    continue;

                int pos;
                while ((pos = intfs.indexOf(sui)) >= 0) {
                    intfs.set(pos, null);
                }
            }
        }

        interfaces.clear();
        // remove nulls
        for (int i = 0; i < intfs.size(); i++) {
            if (intfs.get(i) != null && !interfaces.contains(intfs.get(i)))
                interfaces.add(intfs.get(i));
        }
    }


    /*
     * After removing invisible interfaces we can have duplicated constants
     * public class P implements I1, I2 {}
     * interface I1 { int I = 0; }
     * interface I2 { int I = 1; }
     * in this case we must remove constants I from resulted set because
     * reference by simple name is impossible due to ambiguity,
     * and reference by qualified name is impossible also
     * due to I1 and I2 are invisible outside the package
     */
    private void removeDuplicatedConstants(ClassDescription c) {

        Set constantNames = new HashSet();

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isField() && mr.isPublic())
                if (((FieldDescr) mr).isConstant()) {
                    String constName = mr.getQualifiedName();
                    if (c.getMembersCount(MemberType.FIELD, constName) > 1)
                        constantNames.add(constName);
                }
        }

        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isField())
                if (((FieldDescr) mr).isConstant() && constantNames.contains(mr.getQualifiedName())) {
                    e.remove();
                    if (logger.isLoggable(Level.FINE)) {
                        String invargs[] = {mr.getQualifiedName(), c.getQualifiedName()};
                        logger.fine(i18n.getString("ClassCorrector.message.const.removed", invargs));
                    }
                }
        }
    }


    private void checkClassTypeParameters(ClassDescription cl) {
        checkTypeParameters(cl, cl);
        for (Iterator e = cl.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            if (mr.isMethod() || mr.isConstructor())
                checkTypeParameters(cl, mr);
        }
    }

    private void checkTypeParameters(ClassDescription cl, MemberDescription mr) {

        final String ext = "extends";
        String typeparams = mr.getTypeParameters();

        if (typeparams != null) {
            ArrayList params = Erasurator.splitParameters(typeparams);
            for (int i = 0; i < params.size(); ++i) {

                String param = (String) params.get(i);
                String temp = param.substring(param.indexOf(ext) + ext.length());
                StringTokenizer st = new StringTokenizer(temp, "&");

                while (st.hasMoreTokens()) {
                    String className = st.nextToken().trim();
                    int pos = className.indexOf('<');
                    if (pos != -1)
                        checkActualParameters(cl, mr, className.substring(pos));
                    if (isInvisibleClass(className) && !className.equals(mr.getDeclaringClassName()))
                        if (mr.isMethod() || mr.isConstructor()) {
                            String invargs[] = {className, mr.toString(), mr.getDeclaringClassName()};
                            log.storeError(i18n.getString("ClassCorrector.error.parametertype.hidden", invargs), logger);
                        } else {
                            String invargs[] = {className, mr.getQualifiedName()};
                            log.storeError(i18n.getString("ClassCorrector.error.parametertype.hidden2", invargs), logger);
                        }
                }
            }
        }
    }

    private static String wrapArray(String oldT, String newT) {
        int pos = oldT.indexOf('[');
        if (pos != -1)
            return newT + oldT.substring(pos);
        return newT;
    }

    private static String stripArrays(String name) {
        int pos = name.indexOf('[');
        if (pos != -1)
            return name.substring(0, pos);
        return name;
    }

    private static String stripGenerics(String name) {
        int pos = name.indexOf('<');
        if (pos != -1)
            return name.substring(0, pos);
        return name;
    }


    // TODO review this method usage
    private boolean isPublicInner(String clName) throws ClassNotFoundException {
        if (clName.indexOf('$') < 0)
            return false;

        ClassDescription cd = classHierarchy.load(clName);
        return cd.isPublic() || cd.isProtected();
    }


    private boolean isInvisibleClass(String fqname) {

        if (fqname.length() == 0)    // constructors' return type
            return false;

        // Is this a type parameter ?
        if (fqname.startsWith("{"))
            return false;

        if (fqname.startsWith("?"))
            return false;
        if (jdkExclude.isJdkClass(fqname)) 
            return false;
        String pname = ClassCorrector.stripArrays(ClassCorrector.stripGenerics(fqname));

        if (PrimitiveTypes.isPrimitive(pname))
            return false;

        boolean accessible = true;

        try {
            accessible = classHierarchy.isAccessible(pname);
        }
        catch (ClassNotFoundException e) {
            log.storeError(i18n.getString("ClassCorrector.error.missingclass", new String[]{pname}), logger);
        }

        return !accessible;
    }


    private void removeInvisibleAnnotations(ClassDescription cl) throws ClassNotFoundException {

        int count = 0;
        AnnotationItem[] annotations = cl.getAnnoList();

        int len = annotations.length;

        if (len == 0)
            return;

        for (int i = 0; i < len; ++i) {
            String annoName = annotations[i].getName();

            boolean documented = classHierarchy.isDocumentedAnnotation(annoName);

            if (isInvisibleClass(annoName)) {
                if (documented && logger.isLoggable(Level.WARNING))
                    logger.warning(i18n.getString("ClassCorrector.error.invisible_documented_annotation", annoName));
                annotations[i] = null;
            } else
                ++count;
        }

        if (count == len)
            return;   // nothing to do

        AnnotationItem[] visibleAnnotations = AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY;

        if (count != 0) {
            visibleAnnotations = new AnnotationItem[count];
            count = 0;
            for (int i = 0; i < len; ++i) {
                if (annotations[i] != null)
                    visibleAnnotations[count++] = annotations[i];
            }
        }

        cl.setAnnoList(visibleAnnotations);
    }

}
