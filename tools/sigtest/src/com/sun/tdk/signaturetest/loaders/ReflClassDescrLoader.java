/*
 * $Id: ReflClassDescrLoader.java 4504 2008-03-13 16:12:22Z sg215604 $
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

package com.sun.tdk.signaturetest.loaders;

import com.sun.tdk.signaturetest.core.ClassDescriptionLoader;
import com.sun.tdk.signaturetest.core.PrimitiveTypes;
import com.sun.tdk.signaturetest.model.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;

/**
 * <b>ReflClassDescrLoader</b> is intended to compile information about
 * classes via reflections package. If available, the advanced method
 * <b>Class</b>.<code>forName</code>(<b>String</b>,<code>boolean</code>,<b>ClassLoader</b>)
 * is involved, which enables to do not initialize classes being loaded.
 * (This helps to avoid irrelevant security problems.)
 *
 * @author Maxim Sokolnikov
 * @version 05/03/22
 * @see com.sun.tdk.signaturetest.model.ClassDescription
 */
public class ReflClassDescrLoader implements ClassDescriptionLoader, LoadingHints {

    public static boolean debug = false;

    /**
     * Reference to advanced <code>forName()</code> method available only for
     * newer Java implementations. If only simpler <code>forName()</code> method
     * is available, then this field is incarnated with <code>null</code>.
     *
     * @see Class#forName(String,boolean,ClassLoader)
     */
    private Method forName;
    /**
     * Arguments prepared to invoke advanced <code>forName()</code> method via
     * reflection.
     *
     * @see Method#invoke(Object,Object[])
     */
    private Object[] args;

    /**
     * Adjust new <b>ReflClassDescLoader</b> instance. In particular, detect
     * if advanced method <b>Class</b>.<code>forName()</code> is implemented,
     * which enables to do not initialize the class being loaded. Otherwise,
     * tune <code>this</code> instance to use simpler version of the method
     * <code>forName()</code>, which must be available anyway.
     *
     * @see Class#forName(String,boolean,ClassLoader)
     * @see Class#forName(String)
     */
    public ReflClassDescrLoader() {

        args = new Object[]{
                "",
                Boolean.FALSE,
                this.getClass().getClassLoader()
        };

        Class[] param = {
                String.class,
                Boolean.TYPE,
                ClassLoader.class
        };
        try {
            forName = Class.class.getDeclaredMethod("forName", param);
        } catch (NoSuchMethodException e) {
            forName = null;
        }
    }

    /**
     * Return new <b>ClassDescription</b> for that class found by the given
     * <code>name</code>.
     *
     * @throws ClassNotFoundException If <b>Class</b>.<code>forName(name)</code>
     *                                fails to load the required class.
     * @see Class#forName(String)
     */
    public ClassDescription load(String name) throws ClassNotFoundException {

        if (forName == null)
            return loadClass(Class.forName(name));

        try {
            args[0] = name;
            return loadClass((Class) forName.invoke(null, args));
        }
        catch (InvocationTargetException e) {

            Throwable t = e.getTargetException();

            if (t instanceof LinkageError)
                throw (LinkageError) t;
            else if (t instanceof ClassNotFoundException)
                throw (ClassNotFoundException) t;
        } catch (IllegalAccessException e) {
        }

        return loadClass(Class.forName(name));
    }


    private ClassDescription loadClass(Class classObject) {
        ClassDescription c = new ClassDescription();

        c.setTiger(false);
        c.setModifiers(classObject.getModifiers());
        c.setupClassName(classObject.getName());
        setupMethods(c, classObject);
        setupFields(c, classObject);
        setupConstructors(c, classObject);
        setupInterfaces(c, classObject);
        setupNested(c, classObject);
        setupSuperClass(c, classObject);

        return c;

    }

    private void setupMethods(ClassDescription cd, Class classObject) {
        Method[] methods = classObject.getDeclaredMethods();
        cd.createMethods(methods.length);
        for (int i = 0; i < methods.length; i++)
            cd.setMethod(i, createMember(methods[i]));
    }

    private void setupFields(ClassDescription cd, Class classObject) {
        Field[] fields = classObject.getDeclaredFields();
        cd.createFields(fields.length);

        FieldDescr fld;
        for (int i = 0; i < fields.length; i++) {
            fld = createMember(fields[i]);

            cd.setField(i, fld);

            String type = fld.getType();

            if (fld.isFinal() && (PrimitiveTypes.isPrimitive(type) || "java.lang.String".equals(type))) {
                if (!hasHint(LoadingHints.DONT_READ_VALUES)) {
                    try {
                        fields[i].setAccessible(true);
                        Object v = fields[i].get(null);
                        String val = MemberDescription.valueToString(v);
                        fld.setConstantValue(val) /*valueToString(v)*/;
                    } catch (Throwable e) {
                        // catch error or exception that may be thrown during static class initialization
                        if (debug) {
                            System.err.println("Error during reading field value " + fld.toString());
                            e.printStackTrace(System.err);
                        }
                    }
                }
            }
        }
    }


    private void setupNested(ClassDescription cd, Class classObject) {
        Class nested[] = classObject.getDeclaredClasses();
        cd.createNested(nested.length);
        for (int i = 0; i < nested.length; i++) {
            InnerDescr m = new InnerDescr();
            cd.setNested(i, m);
            m.setModifiers(nested[i].getModifiers());

            // -----
            // workaround of a problem with obsfucated inner classes whith no dollar sign in name
            m.setupInnerClassName(cd.getQualifiedName(), nested[i].getName());
            // -----

            m.setupClassName(nested[i].getName());
        }
    }


    private void setupConstructors(ClassDescription cd, Class classObject) {
        Constructor[] ctors = classObject.getDeclaredConstructors();
        cd.createConstructors(ctors.length);
        for (int i = 0; i < ctors.length; i++)
            cd.setConstructor(i, createMember(ctors[i]));
    }


    private void setupInterfaces(ClassDescription cd, Class classObject) {
        Class interfaces[] = classObject.getInterfaces();
        cd.createInterfaces(interfaces.length);
        for (int i = 0; i < interfaces.length; i++) {
            SuperInterface intf = new SuperInterface();
            cd.setInterface(i, intf);
            intf.setupClassName(interfaces[i].getName());
        }
    }


    private void setupSuperClass(ClassDescription cd, Class classObject) {
        Class spr = classObject.getSuperclass();
        if (spr != null) {
            SuperClass sc = new SuperClass();
            sc.setupClassName(spr.getName());
            cd.setSuperClass(sc);
        }
    }

    private static FieldDescr createMember(Field field) {

        FieldDescr member = new FieldDescr(field.getName(),
                field.getDeclaringClass().getName(), field.getModifiers());

        member.setType(MemberDescription.getTypeName(field.getType()));
        return member;
    }

    /**
     * Create description for the given method.
     */
    private static MethodDescr createMember(Method meth) {

        MethodDescr member = new MethodDescr(meth.getName(),
                meth.getDeclaringClass().getName(), meth.getModifiers());

        member.setType(MemberDescription.getTypeName(meth.getReturnType()));

        // create args
        member.setArgs(ReflClassDescrLoader.getArgs(meth.getParameterTypes()));

        // create throws clause
        member.setThrowables(getThrows(meth.getExceptionTypes()));

        return member;
    }


    /**
     * Create description for the given constructor.
     */
    private static ConstructorDescr createMember(Constructor ctor) {

        ConstructorDescr member = new ConstructorDescr(ctor.getDeclaringClass().getName(),
                ctor.getModifiers());

        // create args
        member.setArgs(ReflClassDescrLoader.getArgs(ctor.getParameterTypes()));

        // create throws clause
        member.setThrowables(getThrows(ctor.getExceptionTypes()));

        return member;
    }

    private static String getThrows(Class[] exceptionClasses) {

        if (exceptionClasses.length == 0)
            return MemberDescription.EMPTY_THROW_LIST;

        String[] exceptionNames = new String[exceptionClasses.length];

        for (int j = 0; j < exceptionClasses.length; ++j)
            exceptionNames[j] = exceptionClasses[j].getName();

        // in fact, Arrays.sort() is slow, because clone the whole array passed as input parameter        
        if (exceptionClasses.length > 1)
            Arrays.sort(exceptionNames);

        StringBuffer throwables = new StringBuffer(exceptionNames[0]);

        for (int i = 1; i < exceptionNames.length; ++i)
            throwables.append(MemberDescription.THROWS_DELIMITER).append(exceptionNames[i]);

        return throwables.toString();
    }


    private static String getArgs(Class[] args) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < args.length; i++) {
            if (i != 0)
                sb.append(MemberDescription.ARGS_DELIMITER);
            sb.append(MemberDescription.getTypeName(args[i]));
        }

        return sb.toString();
    }

    private HashSet hints = new HashSet();

    public void addLoadingHint(Hint hint) {
        hints.add(hint);
    }

    private boolean hasHint(Hint hint) {
        return hints.contains(hint);
    }
}
