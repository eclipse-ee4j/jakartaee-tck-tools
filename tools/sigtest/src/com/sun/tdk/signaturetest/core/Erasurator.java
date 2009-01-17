/*
 * $Id: Erasurator.java 4504 2008-03-13 16:12:22Z sg215604 $
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
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class <b>Erasurator</b> performs "Type erasure" (see JLS Third Edition, p. 4.6)
 * Type erasure is a mapping from types (possibly including parameterized types and
 * type variables) to types (that are never parameterized types or type variables). We
 * write |T| for the erasure of type T. The erasure mapping is defined as follows.
 * <li>The erasure of a parameterized type (p3.5) G&lt;T1, ... ,Tn&gt; is |G|.</li>
 * <li>The erasure of a nested type T.C is |T|.C.</li>
 * <li>The erasure of an array type T[] is |T|[].</li>
 * <li>The erasure of a type variable (p3.4) is the erasure of its leftmost bound.</li>
 * <li>The erasure of every other type is the type itself.
 * The erasure of a method signature s is a signature consisting of the same
 * name as s, and the erasures of all the formal parameter types given in s.</li>
 * <p/>
 * <br>When the <b>Erasurator</b> finds the definition of a generic type or method, it removes
 * all occurrences of the type parameters and replaces them by their leftmost bound,
 * or type Object if no bound had been specified.
 *
 * @author Mikhail Ershov
 * @author Roman Makarchuk
 */
public class Erasurator {

    private Map globalParameters = new HashMap();
    private Map localParameters = new HashMap();

    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Erasurator.class);

    public ClassDescription erasure(ClassDescription clz) {

        ClassDescription result = (ClassDescription) clz.clone();

        globalParameters.clear();

        if (result.getTypeParameters() != null) {
            parseTypeParameters(result);
            result.setTypeParameters(null);
        }
        processMembers(result);
        return result;
    }

    public ClassDescription fullErasure(ClassDescription clz) {

        ClassDescription result = (ClassDescription) clz.clone();

        globalParameters.clear();

        if (result.getTypeParameters() != null) {
            parseTypeParameters(result);
            result.setTypeParameters(null);
        }
        processDeclaredMembers(result);
        return result;
    }

    private static String convert(String s, Map globalParameters, Map localParameters) {

        Matcher m;
        String newS = s;

        while (newS.indexOf('<') != -1) {
            assert newS.indexOf('>') != -1;
            m = simpleParamUsage.matcher(newS);
            newS = m.replaceAll("");
        }

        if (globalParameters.size() == 0 && localParameters.size() == 0)
            return newS;  // nothing to do

        m = replaceParamUsage.matcher(newS);
        while (m.find()) {
            String param = m.group();
            if (globalParameters.containsKey(param)) {
                newS = m.replaceFirst((String) globalParameters.get(param));
                m = replaceParamUsage.matcher(newS);
                continue;
            }
            if (localParameters.containsKey(param)) {
                newS = m.replaceFirst((String) localParameters.get(param));
                m = replaceParamUsage.matcher(newS);
                continue;
            }

            System.out.println(i18n.getString("Erasurator.error.unresolved", param));
        }

        return newS;
    }

    public void parseTypeParameters(ClassDescription classDescr) {
        if (classDescr.getTypeParameters() != null)
            parseTypeParameters(classDescr, globalParameters);
    }


    private static void parseTypeParameters(MemberDescription member, Map parameters) {

        StringTokenizer st = new StringTokenizer(member.getTypeParameters(), "<>,");
        final String ext = " extends ";

        boolean isClass = member.isClass();

        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            Matcher m = simpleParamName.matcher(token);
            if (m.lookingAt()) {
                String name = token.substring(m.start(), m.end());
                String key;

                if (isClass)
                    key = "{" + member.getQualifiedName() + name + "}";
                else
                    key = "{%" + name + "}";

                if (token.length() > m.end() && token.substring(m.end()).startsWith(ext)) {
                    String val = token.substring(m.end() + ext.length()).trim();

                    if (val.indexOf(' ') >= 0)
                        val = val.substring(0, val.indexOf(' '));

                    parameters.put(key, maskDollar(val));
                }
            }
        }
    }

    private void processDeclaredMembers(ClassDescription clz) {

        ConstructorDescr[] constrs = clz.getDeclaredConstructors();
        clz.setConstructors(new ConstructorDescr[constrs.length]);
        for (int i = 0; i < constrs.length; i++) {
            clz.setConstructor(i, (ConstructorDescr) processMember(constrs[i], false));
        }

        MethodDescr[] meths = clz.getDeclaredMethods();
        clz.setMethods(new MethodDescr[meths.length]);
        for (int i = 0; i < meths.length; i++) {
            clz.setMethod(i, (MethodDescr) processMember(meths[i], false));
        }

        FieldDescr[] flds = clz.getDeclaredFields();
        clz.setFields(new FieldDescr[flds.length]);
        for (int i = 0; i < flds.length; i++) {
            clz.setField(i, (FieldDescr) processMember(flds[i], false));
        }

        InnerDescr[] inners = clz.getDeclaredClasses();
        clz.setNestedClasses(new InnerDescr[inners.length]);
        for (int i = 0; i < inners.length; i++) {
            clz.setNested(i, (InnerDescr) processMember(inners[i], false));
        }
    }

    private void processMembers(ClassDescription clz) {

        MemberCollection newMembers = new MemberCollection();

        for (Iterator e = clz.getMembersIterator(); e.hasNext();) {
            newMembers.addMember(processMember((MemberDescription) e.next()));
        }

        clz.setMembers(newMembers);
    }

    public MemberDescription processMember(MemberDescription mr) {
        return processMember(mr, false);
    }

    private MemberDescription processMember(MemberDescription mr, boolean dontClone) {

        assert !mr.isClass();

//        if (globalParameters.size() == 0 && mr.getTypeParameters() == null)
//            return mr;

        localParameters.clear();

        if (mr.getTypeParameters() != null)
            parseTypeParameters(mr, localParameters);

        MemberDescription cloned_m = mr;
        if (!dontClone)
            cloned_m = (MemberDescription) mr.clone();

        cloned_m.setTypeParameters(null);

        String args = cloned_m.getArgs();

        if (!MemberDescription.NO_ARGS.equals(args))
            cloned_m.setArgs(convert(args, globalParameters, localParameters));

        String type = cloned_m.getType();
        if (!MemberDescription.NO_TYPE.equals(type))
            cloned_m.setType(convert(type, globalParameters, localParameters));

        // TODO (Roman Makarchuk) just for debugging
        if (SigTest.debug) {
            String s = cloned_m.toString();
            assert s.indexOf('<') == -1 && s.indexOf('?') == -1 && s.indexOf('{') == -1;
        }

        return cloned_m;
    }


    // TODO weak performance; use indexOf instead of charAt()
    public static ArrayList splitParameters(String actualTypeParams) {
        ArrayList paramList = new ArrayList();
        int startPos = 1;
        int level = 0;
        int len = actualTypeParams.length() - 1;
        for (int i = 1; i < len; ++i) {
            switch (actualTypeParams.charAt(i)) {
                case'<':
                    ++level;
                    break;
                case'>':
                    --level;
                    break;
                case',': {
                    if (level == 0) {
                        paramList.add(actualTypeParams.substring(startPos, i));
                        startPos = i + 1;
                    }
                }
            }
        }

        // add the last param!
        paramList.add(actualTypeParams.substring(startPos, len));

        return paramList;
    }


    public static MemberDescription[] replaceFormalParameters(String fqn, MemberDescription[] members, List actualTypeParamList, boolean skipRawTypes) {

        MemberDescription[] result = new MemberDescription[members.length];

        for (int i = 0; i < members.length; ++i)
            result[i] = replaceFormalParameters(fqn, members[i], actualTypeParamList, skipRawTypes);

        return result;
    }

    public static Collection replaceFormalParameters(String fqn, Collection members, List actualTypeParamList, boolean skipRawTypes) {

        assert actualTypeParamList.size() != 0;

        Collection result = new ArrayList();

        for (Iterator it = members.iterator(); it.hasNext();) {
            MemberDescription newFid = replaceFormalParameters(fqn, (MemberDescription) it.next(), actualTypeParamList, skipRawTypes);
            result.add(newFid);
        }
        return result;

    }


    private static MemberDescription replaceFormalParameters(String fqn, MemberDescription fid, List actualTypeParamList, boolean skipRawTypes) {

        MemberDescription newFid = (MemberDescription) fid.clone();
        for (int i = 0; i < actualTypeParamList.size(); ++i) {
            String actual = (String) actualTypeParamList.get(i);

            if (skipRawTypes && actual.indexOf('%') == -1)
                continue;

            String key = "\\{" + fqn + "%" + i + "\\}";
            replaceFormalParameters(newFid, key, actual);
        }

        return newFid;
    }


    private static void replaceFormalParameters(MemberDescription mr, String formalParam, String actualParam) {

        String actual = maskDollar(actualParam);
        Pattern p = Pattern.compile(maskDollar(formalParam));
        Matcher m;

        String args = mr.getArgs();

        if (!MemberDescription.NO_ARGS.equals(args)) {
            m = p.matcher(args);
            mr.setArgs(m.replaceAll(actual));
        }

        String type = mr.getType();
        if (!MemberDescription.NO_TYPE.equals(type)) {
            m = p.matcher(type);
            mr.setType(m.replaceAll(actual));
        }            

        if (mr.isSuperInterface() || mr.isSuperClass()) {
            String typeParams = mr.getTypeParameters();
            if (typeParams != null) {
                m = p.matcher(typeParams);
                mr.setTypeParameters(m.replaceAll(actual));
            }
        }
    }

    private static String maskDollar(String str) {
        int pos;
        String tmp = str, result = "";

        do {
            pos = tmp.indexOf('$');
            if (pos != -1) {
                result += tmp.substring(0, pos);
                result += "\\$";
                tmp = tmp.substring(pos + 1);
            }
        } while (pos != -1);

        result += tmp;

        return result;
    }


    // begin of line + % + any numbers
    private static Pattern simpleParamName = Pattern.compile("^%\\d+?");
    //    private static Pattern simpleParamUsage = Pattern.compile("<.+?>");
    private static Pattern simpleParamUsage = Pattern.compile("<[^<>]+?>");

    private static Pattern replaceParamUsage = Pattern.compile("\\{.+?\\}");

}
