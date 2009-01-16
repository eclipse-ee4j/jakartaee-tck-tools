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

package com.sun.tdk.signaturetest.merge;

import com.sun.tdk.signaturetest.Result;
import com.sun.tdk.signaturetest.core.Erasurator;
import com.sun.tdk.signaturetest.core.Log;
import com.sun.tdk.signaturetest.loaders.VirtualClassDescriptionLoader;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Merges some APIs according JSR68 rules
 *
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 */
public class JSR68Merger extends FeaturesHolder {

    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(JSR68Merger.class);
    private Log log;
    private Result result;
    private Erasurator erasurator;

    public JSR68Merger(Log log, Result result) {
        this.log = log;
        this.result = result;
        this.addSupportedFeature(FeaturesHolder.ConstInfo);
        this.addSupportedFeature(FeaturesHolder.BuildMembers);
        this.addSupportedFeature(FeaturesHolder.TigerInfo);
        erasurator = new Erasurator();
    }

    public VirtualClassDescriptionLoader merge(MergedSigFile[] files, int mode) {
        this.mode = mode;
        VirtualClassDescriptionLoader result = new VirtualClassDescriptionLoader();
        setLogger();
        for (int i = 0; i < files.length; i++) {
            MergedSigFile mf = files[i];
            if (!mf.getLoader().hasFeature(FeaturesHolder.ConstInfo))
                this.removeSupportedFeature(FeaturesHolder.ConstInfo);
            if (!mf.getLoader().hasFeature(FeaturesHolder.TigerInfo))
                this.removeSupportedFeature(FeaturesHolder.TigerInfo);
            if (!mf.getLoader().hasFeature(FeaturesHolder.BuildMembers))
                this.removeSupportedFeature(FeaturesHolder.BuildMembers);
            Iterator it = mf.getClassSet().values().iterator();
            while (it.hasNext()) {
                ClassDescription cd = (ClassDescription) it.next();
                // If one of input APIs contain an element and other doesn't,
                // this element goes to the result API without modification except for the following case :

                // 1) is it unique?
                boolean unique = true;
                ArrayList sameClasses = new ArrayList();
                ArrayList filesForSameClasses = new ArrayList();
                sameClasses.add(cd);
                filesForSameClasses.add(mf);
                for (int j = 0; j < files.length; j++) {
                    if (i == j) continue;
                    MergedSigFile mfOther = files[j];
                    if (mfOther.getClassSet().containsKey(cd.getQualifiedName())) {
                        unique = false;
                        sameClasses.add(mfOther.getClassSet().get(cd.getQualifiedName()));
                        filesForSameClasses.add(mfOther);
                    }
                }

                // TODO !!!
                // If this element is first declared class member in inheritance
                // chain and the other API inherits the same element, then this element doesn't
                // go to the result API.
                if (unique) {
                    result.add(cd);
                } else {
                    //                    logger.fine("Not unique, to merge " + cd.getQualifiedName());
                    ClassDescription resultedClass = new ClassDescription();
                    resultedClass.setupClassName(cd.getQualifiedName());

                    ClassDescription[] classes = (ClassDescription[]) sameClasses.toArray(new ClassDescription[0]);
                    MergedSigFile[] filesForClasses = (MergedSigFile[]) filesForSameClasses.toArray(new MergedSigFile[0]);
                    if (merge(classes, resultedClass, filesForClasses) && merge2(classes, resultedClass)) {
                        result.add(resultedClass);
                    }
                }
            }
        }

        Iterator it = result.getClassIterator();
        ArrayList innersToRemove = new ArrayList();
        nextClass:
        while (it.hasNext()) {
            ClassDescription cd = (ClassDescription) it.next();
            try {
                result.load(cd.getPackageName());
                error(i18n.getString("Merger.error.packageconflict", cd.getPackageName()));
            } catch (ClassNotFoundException e) {
                // this is normal that there is no classes
                // with package name
            }
            if (cd.getQualifiedName().indexOf("$") >= 0) {
                try {
                    ClassDescription outer = result.load(cd.getDeclaringClassName());
                    InnerDescr[] dc = outer.getDeclaredClasses();
                    for (int i = 0; i < dc.length; i++) {
                        if (dc[i].getQualifiedName().equals(cd.getQualifiedName())) {
                            continue nextClass;
                        }
                    }

                    for (Iterator it2 = result.getClassIterator(); it2.hasNext();) {
                        ClassDescription similarInner = (ClassDescription) it2.next();
                        if (similarInner.getQualifiedName().indexOf("$") >= 0 &&
                                similarInner.getName().equals(cd.getName())) {
                            ClassDescription parent = outer;
                            while (true) {
                                try {
                                    parent = result.load(parent.getSuperClass().getQualifiedName());
                                    if (similarInner.getDeclaringClassName().equals(parent.getQualifiedName())) {
                                        innersToRemove.add(cd);
                                        continue nextClass;
                                    }
                                } catch (Exception e) {
                                    // no parents
                                    break;
                                }
                            }

                        }
                    }

                    InnerDescr d = new InnerDescr();
                    d.setupClassName(cd.getQualifiedName());
                    d.setModifiers(cd.getModifiers());
                    InnerDescr[] newInners = new InnerDescr[dc.length + 1];
                    System.arraycopy(dc, 0, newInners, 0, dc.length);
                    newInners[dc.length] = d;
                    outer.setNestedClasses(newInners);

                } catch (ClassNotFoundException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
        it = result.getClassIterator();
        while (it.hasNext()) {
            if (innersToRemove.contains(it.next()))
                it.remove();
        }

        return result;
    }


    private boolean merge(ClassDescription[] similarClasses, ClassDescription result, MergedSigFile[] sigfiles) {

        boolean mAbs = similarClasses[0].isAbstract();
        for (int i = 1; i < similarClasses.length; i++) {
            if (similarClasses[i].isAbstract() != mAbs) {
                error(similarClasses[0].getQualifiedName() + " " + i18n.getString("Merger.error.modifierconfict", "abstract"));
                return false;
            }
        }

        boolean mInt = similarClasses[0].isInterface();
        for (int i = 1; i < similarClasses.length; i++) {
            if (similarClasses[i].isInterface() != mInt) {
                error(similarClasses[0].getQualifiedName() + i18n.getString("Merger.error.classwithinterface"));
                return false;
            }
        }

        if (!mergeMod(similarClasses, result)) {
            return false;
        }


        if (!mergeSuprs(similarClasses, result, sigfiles)) {
            return false;
        }

        return mergeInterfaces(similarClasses, result);

    }

    private boolean merge2(ClassDescription[] similarClasses, ClassDescription result) {

        if (prepareGenerics(similarClasses, result)) {

            mergeAnnotations(similarClasses, result);
            mergeTypeParameters(similarClasses, result);

            if (!mergeMembers(similarClasses, result)) {
                return false;
            }

            checkGenerics(result);
        }

        return true;
    }


    private void checkGenerics(ClassDescription result) {
        ClassDescription eResult = erasurator.fullErasure(result);
        ConstructorDescr[] genCostr = eResult.getDeclaredConstructors();
        MethodDescr[] genMeth = eResult.getDeclaredMethods();
        FieldDescr[] genFld = eResult.getDeclaredFields();

        HashSet sims = new HashSet();

        for (int i = 0; i < genCostr.length; i++) {
            String s = genCostr[i].getSignature();
            if (sims.contains(s)) {
                error(show(genCostr[i]) + i18n.getString("Merger.error.typeconflict"), new Object[]{result.getDeclaredConstructors()[i], s});
            }
            sims.add(s);
        }

        for (int i = 0; i < genMeth.length; i++) {
            String s = genMeth[i].getSignature();
            if (sims.contains(s)) {
                error(show(genCostr[i]) + i18n.getString("Merger.error.typeconflict"), new Object[]{result.getDeclaredMethods()[i], s});
            }
            sims.add(s);
        }

        for (int i = 0; i < genFld.length; i++) {
            String s = genFld[i].getName();
            if (sims.contains(s)) {
                error(show(genCostr[i]) + i18n.getString("Merger.error.typeconflict"), new Object[]{result.getDeclaredFields()[i], s});
            }
            sims.add(s);
        }


    }

    private boolean prepareGenerics(ClassDescription[] similarClasses, ClassDescription result) {
        ArrayList hasGen = new ArrayList();
        ArrayList noGen = new ArrayList();
        int noGenPos = -1;
        int genPos = -1;
        for (int i = 0; i < similarClasses.length; i++) {
            if (isGeneralized(similarClasses[i])) {
                hasGen.add(similarClasses[i]);
                genPos = i;
            } else {
                noGen.add(similarClasses[i]);
                noGenPos = i;
            }
        }
        if (hasGen.size() == 0 || noGen.size() == 0) {
            return true;
        }

        if (hasGen.size() == 1 && noGen.size() == 1) {
            ClassDescription hasGenCD = similarClasses[genPos];

            ConstructorDescr[] genCostr = hasGenCD.getDeclaredConstructors();
            MethodDescr[] genMeth = hasGenCD.getDeclaredMethods();
            FieldDescr[] genFld = hasGenCD.getDeclaredFields();

            ClassDescription hasGenEraCD = erasurator.fullErasure(hasGenCD);
            ClassDescription noGenCD = similarClasses[noGenPos];
            if (noGenCD.equals(hasGenEraCD)) {
                noGenCD.setTypeParameters(hasGenCD.getTypeParameters());
            }

            ConstructorDescr[] noGenCostr = noGenCD.getDeclaredConstructors();
            ConstructorDescr[] eraCostr = hasGenEraCD.getDeclaredConstructors();

            MethodDescr[] noGenMeth = noGenCD.getDeclaredMethods();
            MethodDescr[] eraMeth = hasGenEraCD.getDeclaredMethods();

            FieldDescr[] noGenFld = noGenCD.getDeclaredFields();
            FieldDescr[] eraFld = hasGenEraCD.getDeclaredFields();

            // constructors
            for (int i = 0; i < noGenCostr.length; i++) {
                ConstructorDescr c = noGenCostr[i];
                for (int j = 0; j < eraCostr.length; j++) {
                    ConstructorDescr c2 = eraCostr[j];
                    ConstructorDescr c3 = genCostr[j];
                    if (c.equals(c2) && !c.equals(c3)) {
                        noGenCD.setConstructor(i, (ConstructorDescr) c3.clone());
                        break;
                    }
                }
            }

            // methods
            for (int i = 0; i < noGenMeth.length; i++) {
                MethodDescr m = noGenMeth[i];
                for (int j = 0; j < eraMeth.length; j++) {
                    MethodDescr m2 = eraMeth[j];
                    MethodDescr m3 = genMeth[j];
                    if (m.getSignature().equals(m2.getSignature()) && m.getType().equals(m2.getType())) {
                        noGenCD.setMethod(i, (MethodDescr) m3.clone());
                        break;
                    }
                }
            }

            // fields
            for (int i = 0; i < noGenFld.length; i++) {
                FieldDescr f = noGenFld[i];
                for (int j = 0; j < eraFld.length; j++) {
                    FieldDescr f2 = eraFld[j];
                    FieldDescr f3 = genFld[j];
                    if (f.equals(f2) && f.getType().equals(f2.getType())) {
                        noGenCD.setField(i, (FieldDescr) f3.clone());
                        break;
                    }
                }
            }
            return true;
        }


        if (hasGen.size() >= 1 || noGen.size() >= 1) {
            ClassDescription res1 = (ClassDescription) result.clone();
            ClassDescription res2 = (ClassDescription) result.clone();
            if (noGen.size() >= 0) {
                merge2((ClassDescription[])noGen.toArray(new ClassDescription[0]), res1);
            } else {
                res1 = (ClassDescription) noGen.get(0);
            }
            if (hasGen.size() >= 0) {
                merge2((ClassDescription[])hasGen.toArray(new ClassDescription[0]), res2);
            } else {
                res1 = (ClassDescription) hasGen.get(0);
            }
            merge2(new ClassDescription[]{res1, res2} , result);
            return false;
        }

        return true;

    }

    private boolean isGeneralized(ClassDescription clazz) {
        if (clazz.getTypeParameters() != null) return true;
        Iterator it = clazz.getMembersIterator();
        while (it.hasNext()) {
            MemberDescription md = (MemberDescription) it.next();
            if (!md.getDeclaringClassName().equals(clazz.getQualifiedName())) {
                continue;
            }
            if (md.getTypeParameters() != null) {
                return true;
            }
        }
        return false;
    }


    //  Merge modifiers for two class members and report error, if any.
    //
    private boolean mergeMod(MemberDescription[] x, MemberDescription z) {
        for (int i = 0; i < x.length; i++) {
            z.setModifiers(z.getModifiers() | x[i].getModifiers());
        }

        // access modifiers - set more visible
        // clean up
        int visibilityBits = Modifier.PUBLIC.getValue() | Modifier.PROTECTED.getValue() | Modifier.PRIVATE.getValue();
        z.setModifiers(z.getModifiers() & ~(visibilityBits));

        int vis = 0;
        for (int i = 0; i < x.length; i++) {
            vis = vis | (x[i].getModifiers() & visibilityBits);
        }

        if ((vis & Modifier.PUBLIC.getValue()) != 0) {
            z.setModifiers(z.getModifiers() | Modifier.PUBLIC.getValue());
        } else if ((vis & Modifier.PROTECTED.getValue()) != 0) {
            z.setModifiers(z.getModifiers() | Modifier.PROTECTED.getValue());
        } else if ((vis & Modifier.PRIVATE.getValue()) != 0) {
            /* nothing */
        } else {
            z.setModifiers(z.getModifiers() | Modifier.PRIVATE.getValue());
        }

        // "final" modifier
        // If the elements differ in the "final" modifier, don't include it.
        // Note that if class is final, then all its methods are implicitly final (JLS II, 8.4.3.3).
        for (int i = 0; i < x.length; i++) {
            if (!x[i].isFinal()) {
                z.setModifiers(z.getModifiers() & ~Modifier.FINAL.getValue());
                break;
            }
        }

        // "static" modifier
        // If the elements differ in the "static" modifier, declare conflict.
        boolean mStat = x[0].isStatic();
        for (int i = 1; i < x.length; i++) {
            if (x[i].isStatic() != mStat) {
                error(show(x[0]) + " " + i18n.getString("Merger.error.modifierconfict", "static"));
                return false;
            }
        }

        return true;
    }

    // If superclass of c1 is subclass of superclass of c2,
    // use superclass of c1 as superclass for the new element.
    // Otherwise, if superclass of c2 is subclass of superclass
    // of c1, use superclass of c2 as superclass for the new
    // element.
    // Otherwise declare conflict

    private boolean mergeSuprs(ClassDescription[] similarClasses, ClassDescription result, MergedSigFile[] sigfiles) {
        ArrayList superclasses = new ArrayList();
        // collect superclasses
        for (int i = 0; i < similarClasses.length; i++) {
            SuperClass sc = similarClasses[i].getSuperClass();
            if (sc != null && !superclasses.contains(sc)) {
                superclasses.add(sc);
            }
        }

        // not superclasses at all (Object)
        if (superclasses.isEmpty()) {
            return true;
        }

        // similar or (null and similar)
        if (superclasses.size() == 1) {
            result.setSuperClass((SuperClass) superclasses.iterator().next());
            return true;
        }

        for (int i = 0; i < sigfiles.length; i++) {
            MergedSigFile file = sigfiles[i];
            // 1) find a file which contains all superclasses
            boolean all = true;
            for (int j = 0; j < superclasses.size(); j++) {
                SuperClass sc = (SuperClass) superclasses.get(j);
                if (!file.getClassSet().containsKey(sc.getQualifiedName())) {
                    all = false;
                    break;
                }
            }
            if (!all) {
                continue;  // try the next sigfile
            }
            for (int j = 0; j < superclasses.size(); j++) {
                SuperClass scSub = (SuperClass) superclasses.get(j);
                boolean subSuperFound = true;
                for (int k = 0; k < superclasses.size(); k++) {
                    try {
                        if (k == j)
                            continue;
                        SuperClass scSuper = (SuperClass) superclasses.get(k);

                        if (!file.getClassHierarchy().isSubclass(scSub.getQualifiedName(),
                                scSuper.getQualifiedName())) {
                            subSuperFound = false;
                            break;
                        }
                    } catch (ClassNotFoundException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
                if (subSuperFound) {
                    result.setSuperClass(scSub);
                    return true;
                }
            }
        }

        error(show(result) + " " + i18n.getString("Merger.error.superclassesnotrelated"));
        logger.severe("Can't merge superclasses");
        return false;
    }


    private boolean mergeInterfaces(ClassDescription[] similarClasses, ClassDescription result) {

        TreeSet ts = new TreeSet(new Comparator() {
            public int compare(Object o1, Object o2) {
                SuperInterface s1 = (SuperInterface) o1;
                SuperInterface s2 = (SuperInterface) o2;
                return s1.getQualifiedName().compareTo(s2.getQualifiedName());
            }
        });

        ts.addAll(Arrays.asList(similarClasses[0].getInterfaces()));
        for (int i = 1; i < similarClasses.length; i++) {
            ts.addAll(Arrays.asList(similarClasses[i].getInterfaces()));
        }

        result.createInterfaces(ts.size());
        Iterator it = ts.iterator();
        int i = 0;
        while (it.hasNext()) {
            SuperInterface si = (SuperInterface) ((SuperInterface) it.next()).clone();
            si.setDirect(true);
            result.setInterface(i++, si);
        }

        return true;
    }


    private boolean mergeMembers(ClassDescription[] similarClasses, ClassDescription result) {

        // methods
        makeMethods(similarClasses, result);

        // constructors
        makeCtors(similarClasses, result);

        // fields
        makeFields(similarClasses, result);

        // annotations

        // hiders
        makeHiders(similarClasses, result);

        return true;
    }


    private boolean makeHiders(ClassDescription[] similarClasses, ClassDescription result) {
        // result should be intersection
        HashSet internalFields = new HashSet(similarClasses[0].getInternalFields());
        HashSet internalClasses = new HashSet(similarClasses[0].getInternalClasses());
        for (int i = 1; i < similarClasses.length; i++) {
            internalFields.retainAll(similarClasses[i].getInternalFields());
            internalClasses.retainAll(similarClasses[i].getInternalClasses());
        }
        result.setInternalClasses(internalClasses);
        result.setInternalFields(internalFields);

        return true;
    }

    private boolean makeFields(ClassDescription[] similarClasses, ClassDescription result) {
        ArrayList fields = new ArrayList();
        HashSet h = new HashSet();
        for (int i = 0; i < similarClasses.length; i++) {
            FieldDescr [] fds = similarClasses[i].getDeclaredFields();
            for (int j = 0; j < fds.length; j++) {
                FieldDescr fd = fds[j];
                ArrayList sameFields = new ArrayList();
                sameFields.add(fd);
                boolean isUnique = true;
                for (int k = 0; k < similarClasses.length; k++) {
                    if (k == i) continue;
                    FieldDescr [] fd2 = similarClasses[k].getDeclaredFields();
                    for (int l = 0; l < fd2.length; l++) {
                        if (fd2[l].getName().equals(fd.getName())) {
                            isUnique = false;
                            sameFields.add(fd2[l]);
                        }
                    }
                }
                if (isUnique) {
                    if (!h.contains(fd.getName()))
                        fields.add(fd);
                    h.add(fd.getName());
                } else {
                    // some merge
                    FieldDescr f = new FieldDescr();
                    if (mergeFields((FieldDescr[]) sameFields.toArray(new FieldDescr[0]), f)) {
                        if (!h.contains(f.getName()))
                            fields.add(f);
                        h.add(f.getName());
                    } else {
                        // ??
                    }
                }
            }
        }

        result.setFields((FieldDescr[]) fields.toArray(new FieldDescr[0]));
        return true;
    }


    private boolean mergeFields(FieldDescr[] similarFileds, FieldDescr result) {
        String type = similarFileds[0].getType();
        String value = similarFileds[0].getConstantValue();
        for (int i = 1; i < similarFileds.length; i++) {
            if (!type.equals(similarFileds[i].getType())) {
                error(show(similarFileds[0]) + i18n.getString("Merger.error.typeconflict"), new Object[]{type, similarFileds[i].getType()});
                return false;
            }
            String anotherValue = similarFileds[i].getConstantValue();
            if (!(value == null && anotherValue == null)) {
                if ((value == null || anotherValue == null) || !value.equals(anotherValue)) {
                    error(show(similarFileds[0]) + i18n.getString("Merger.error.differentvalues"), new Object[]{value, anotherValue});
                    return false;
                }
            }

        }

        if (!mergeMod(similarFileds, result)) {
            return false;
        }

        result.setupMemberName(similarFileds[0].getQualifiedName());
        result.setType(type);
        result.setConstantValue(value);
        mergeAnnotations(similarFileds, result);
        mergeTypeParameters(similarFileds, result);

        return true;
    }

    private boolean makeMethods(ClassDescription[] similarClasses, ClassDescription result) {

        // methods
        ArrayList methods = new ArrayList();
        HashSet h = new HashSet();

        for (int i = 0; i < similarClasses.length; i++) {
            MethodDescr [] mfs = similarClasses[i].getDeclaredMethods();
            for (int j = 0; j < mfs.length; j++) {
                MethodDescr mf = mfs[j];
                ArrayList sameMethods = new ArrayList();
                ArrayList finalMods = new ArrayList();
                sameMethods.add(mf);
                finalMods.add(new Boolean(mf.isFinal() || similarClasses[i].isFinal()));
                boolean isUnique = true;
                for (int k = 0; k < similarClasses.length; k++) {
                    if (k == i) continue;
                    MethodDescr [] mfs2 = similarClasses[k].getDeclaredMethods();
                    for (int l = 0; l < mfs2.length; l++) {
                        if (mfs2[l].getSignature().equals(mf.getSignature())) {
                            isUnique = false;
                            sameMethods.add(mfs2[l]);
                            finalMods.add(new Boolean(mfs2[l].isFinal() || similarClasses[k].isFinal()));
                        }
                    }
                }
                if (isUnique) {
                    if (!h.contains(mf.getSignature()))
                        methods.add(mf);
                    h.add(mf.getSignature());
                } else {
                    // some merge
                    MethodDescr m = new MethodDescr();
                    if (mergeMethods((MethodDescr[]) sameMethods.toArray(new MethodDescr[0]), m, finalMods)) {
                        if (!h.contains(m.getSignature()))
                            methods.add(m);
                        h.add(m.getSignature());
                    } else {
                        // ??
                    }
                }
            }
        }

        result.setMethods((MethodDescr[]) methods.toArray(new MethodDescr[0]));

        return true;
    }

    private boolean makeCtors(ClassDescription[] similarClasses, ClassDescription result) {
        ArrayList constr = new ArrayList();
        HashSet h = new HashSet();
        for (int i = 0; i < similarClasses.length; i++) {
            ConstructorDescr [] cds = similarClasses[i].getDeclaredConstructors();
            for (int j = 0; j < cds.length; j++) {
                ConstructorDescr cd = cds[j];
                ArrayList sameConstr = new ArrayList();
                sameConstr.add(cd);
                boolean isUnique = true;
                for (int k = 0; k < similarClasses.length; k++) {
                    if (k == i) continue;
                    ConstructorDescr [] cds2 = similarClasses[k].getDeclaredConstructors();
                    for (int l = 0; l < cds2.length; l++) {
                        if (cds2[l].getSignature().equals(cd.getSignature())) {
                            isUnique = false;
                            sameConstr.add(cds2[l]);
                        }
                    }
                }
                if (isUnique) {
                    if (!h.contains(cd.getSignature()))
                        constr.add(cd);
                    h.add(cd.getSignature());
                } else {
                    // some merge
                    ConstructorDescr c = new ConstructorDescr();

                    if (mergeConstructors((ConstructorDescr[]) sameConstr.toArray(new ConstructorDescr[0]), c)) {
                        if (!h.contains(c.getSignature()))
                            constr.add(c);
                        h.add(c.getSignature());
                    } else {
                        // ??
                    }
                }
            }
        }

        result.setConstructors((ConstructorDescr[]) constr.toArray(new ConstructorDescr[0]));
        return true;
    }


    private boolean mergeMethods(MemberDescription[] similarMethods, MemberDescription result, ArrayList finalMods) {
        String type = similarMethods[0].getType();
        for (int i = 1; i < similarMethods.length; i++) {
            if (!type.equals(similarMethods[i].getType())) {
                error(show(similarMethods[0]) + i18n.getString("Merger.error.typeconflict"), new Object[]{type, similarMethods[i].getType()});
                return false;
            }
        }

        if (!mergeMod(similarMethods, result)) {
            return false;
        }

        result.setupMemberName(similarMethods[0].getQualifiedName());
        result.setType(similarMethods[0].getType());
        mergeAnnotations(similarMethods, result);

        // if there is not abstract , clean abstract
        boolean notAbstract = false;
        boolean hasFinal = true;
        for (int i = 0; i < similarMethods.length; i++) {
            if (!similarMethods[i].isAbstract()) {
                notAbstract = true;
            }
            if (!((Boolean) finalMods.get(i)).booleanValue()) {
                hasFinal = false;
            }
        }
        if (notAbstract) {
            result.setModifiers(result.getModifiers() & ~Modifier.ABSTRACT.getValue());
        }
        if (hasFinal) {
            result.setModifiers(result.getModifiers() | Modifier.FINAL.getValue());
        }

        result.setArgs(similarMethods[0].getArgs());
        result.setType(similarMethods[0].getType());

        mergeTypeParameters(similarMethods, result);


        if (!mergeThrows(similarMethods, result)) {
            return false;
        }

        return true;
    }

    private void mergeTypeParameters(MemberDescription[] similarMembers, MemberDescription result) {
        String tp = null;
        boolean unique = true;
        for (int i = 0; i < similarMembers.length; i++) {
            String tpp = similarMembers[i].getTypeParameters();
            if (tpp != null) {
                if (tp == null || tpp.equals(tp)) {
                    tp = tpp;
                } else {
                    unique = false;
                }
            }
        }
        if (unique) {
            result.setTypeParameters(tp);
        } else {
            error(show(similarMembers[0]) + i18n.getString("Merger.error.typeconflict"), new Object[]{tp, similarMembers[0].getTypeParameters()});
        }
    }

    private void mergeAnnotations(MemberDescription[] similarMembers, MemberDescription result) {
        TreeSet annotations = new TreeSet();
        for (int i = 0; i < similarMembers.length; i++) {
            AnnotationItem[] annos = similarMembers[i].getAnnoList();
            for (int j = 0; j < annos.length; j++) {
                annotations.add(annos[j]);
            }
        }
        result.setAnnoList((AnnotationItem[]) annotations.toArray(new AnnotationItem[0]));

    }

    private boolean mergeConstructors(ConstructorDescr[] similarCtors, ConstructorDescr result) {
        String type = similarCtors[0].getType();
        for (int i = 1; i < similarCtors.length; i++) {
            if (!type.equals(similarCtors[i].getType())) {
                error(show(similarCtors[0]) + i18n.getString("Merger.error.typeconflict"), new Object[]{type, similarCtors[i].getType()});
                return false;
            }
        }

        if (!mergeMod(similarCtors, result)) {
            return false;
        }

        result.setupMemberName(similarCtors[0].getQualifiedName());
        result.setType(similarCtors[0].getType());
        result.setArgs(similarCtors[0].getArgs());
        mergeAnnotations(similarCtors, result);
        mergeTypeParameters(similarCtors, result);

        if (!mergeThrows(similarCtors, result)) {
            return false;
        }

        return true;
    }


    private boolean mergeThrows(MemberDescription[] similarMethods, MemberDescription result) {

        if (mode == BINARY_MODE) {
            result.setThrowables("");
            return true;
        } else {
            // merge normalized throw list
            // in this version we won't do it - the lists are already normalized
            String throwList = similarMethods[0].getThrowables();
            for (int i = 1; i < similarMethods.length; i++) {
                if (!throwList.equals(similarMethods[1].getThrowables())) {
                    Object[] tlist = {show(throwList), show(similarMethods[1].getThrowables())};
                    error(show(similarMethods[0]) + i18n.getString("Merger.error.throwconflict"), tlist);
                    return false;
                }
            }
            result.setThrowables(throwList);
        }

        return true;
    }

    private void error(String msg, Object [] params) {
        error(MessageFormat.format(msg, params));
    }

    private void error(String msg) {
        log.storeError(msg);
        result.error(i18n.getString("Merger.error"));
    }

    private static String show(MemberDescription x) {
        return x.getQualifiedName();
    }

    private static String show(String x) {
        return x;
    }


    private void setLogger() {
        logger = Logger.getLogger("merge");
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            public void publish(LogRecord arg0) {
                System.out.println(arg0.getMessage());
            }

            public void flush() {
            }

            public void close() throws SecurityException {
            }
        });
        logger.setLevel(Level.SEVERE);

    }


    // ME - ACC_STRICT ?
    private static final int flagclass = Modifier.PUBLIC.getValue() | Modifier.FINAL.getValue() |
            Modifier.INTERFACE.getValue() | Modifier.ABSTRACT.getValue();

    private static final int flagfield = Modifier.PUBLIC.getValue() | Modifier.PRIVATE.getValue() |
            Modifier.PROTECTED.getValue() | Modifier.STATIC.getValue() |
            Modifier.FINAL.getValue() | Modifier.VOLATILE.getValue() |
            Modifier.TRANSIENT.getValue();

    private static final int flagmethod = Modifier.PUBLIC.getValue() | Modifier.PRIVATE.getValue() |
            Modifier.PROTECTED.getValue() | Modifier.STATIC.getValue() |
            Modifier.FINAL.getValue() | Modifier.ABSTRACT.getValue() |
            Modifier.ACC_STRICT.getValue() | Modifier.NATIVE.getValue() |
            Modifier.SYNCHRONIZED.getValue();

    private static final int flaginner = Modifier.PUBLIC.getValue() | Modifier.PRIVATE.getValue() |
            Modifier.PROTECTED.getValue() | Modifier.STATIC.getValue() |
            Modifier.FINAL.getValue() | Modifier.INTERFACE.getValue() | Modifier.ABSTRACT.getValue();


    public static final int SOURCE_MODE = 0;
    public static final int BINARY_MODE = 1;
    private int mode;

    private Logger logger;
    private boolean debug = true;

}
