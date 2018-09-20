/*
 * Copyright (c) 2000, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

/*
 * This utility class adds some new functionality to API Check to
 * handle inheritance and overridding issues within the CTS signature
 * tests.
 */

package javasoft.sqe.apiCheck;

import java.util.*;
import java.lang.reflect.*;

public class InheritedUtil {

    private static InheritedUtil instance = new InheritedUtil();

    public static InheritedUtil instance() {
	return instance;
    }

    private Map primMap;

    private InheritedUtil() {
	primMap = new HashMap();
	// void will never be mapped as a parameter or throws type,
	// only mapped as a return type and we don't care about those
	primMap.put("V", "void");
	primMap.put("Z", java.lang.Boolean.TYPE);
	primMap.put("B", java.lang.Byte.TYPE);
	primMap.put("S", java.lang.Short.TYPE);
	primMap.put("I", java.lang.Integer.TYPE);
	primMap.put("J", java.lang.Long.TYPE);
	primMap.put("C", java.lang.Character.TYPE);
	primMap.put("F", java.lang.Float.TYPE);
	primMap.put("D", java.lang.Double.TYPE);
    }

    private String createDimString(int numDims) {
	StringBuffer buf = new StringBuffer(numDims + 1);
	for (int i = 0; i < numDims; i++) {
	    buf.append("[");
	}
	return buf.toString();
    }

    private Class mapPrimitive(String prim) {
	Class result = null;
	if (primMap.containsKey(prim)) {
	    result = (Class)primMap.get(prim);
	}
	return result;
    }

    private Class[] createClassArray(Vector xclasses) throws Exception {
	int numClasses = (xclasses == null) ? 0 : xclasses.size();
	Class[] result = new Class[xclasses.size()];
	for (int i = 0; i < numClasses; i++) {
	    XType xtype = (XType)xclasses.elementAt(i);
	    String dimString = "";
	    String typeString = "";
	    if (xtype instanceof XTypeArray) {
		dimString = createDimString(((XTypeArray)xtype).dims);
		xtype = ((XTypeArray)xtype).ref;
	    }
	    if (xtype instanceof XTypePrimitive) {
		String prim = Character.toString(((XTypePrimitive)xtype).chr);
		if (dimString.length() > 0) {
		    typeString = prim;
		} else {
		   result[i] = mapPrimitive(prim);
		   System.err.println("### Mapped Class \"" + result[i].getName() + "\"");
		   continue;
		}
	    } else if (xtype instanceof XTypeClass) {
		typeString = ((XTypeClass)xtype).toString();
		if (dimString.length() > 0) {
		    typeString = "L" + typeString + ";";
		}
	    } else {
		System.err.println("Unrecognized XType \"" + xtype + "\"");
	    }
	    System.err.println("#### Creating Class \"" + dimString + typeString + "\"");
	    Class newType = Class.forName(dimString + typeString);
	    result[i] = newType;
	}
	return result;
    }

    private boolean methodModifiersOK (int mods, Class declaringClass, Class originalClass) {
	boolean result = false;
	/*
	 * If the method is declared abstract, static or private we could
	 * not have inheritted it.  If the method is public or protected it was
	 * inheritted.  If the method is not public, protected or private
	 * it must be package so we may have inheritted the method if the original 
	 * class and the declaring class are in the same package.
	 */
	if (Modifier.isAbstract(mods) || Modifier.isStatic(mods) || Modifier.isPrivate(mods)) {
	    result = false;
	} else if (Modifier.isPublic(mods) || Modifier.isProtected(mods) ) {
	    result = true;
	} else if (declaringClass.getPackage().getName().equals(originalClass.getPackage().getName())) {
	    result = true;
	}
	return result;
    }

    private void dumpThrows (Class[] inherited, Class[] original) {
	System.err.println("### Expected throws \"" + Arrays.asList(original) + "\"");
	System.err.println("### Found throws    \"" + Arrays.asList(inherited) + "\"");
    }

    private boolean throwsFound(String className, Class[] classes) {
	boolean result = false;
	for (int i = 0; i < classes.length; i++) {
	    if (className.equals(classes[i].getName())) {
		result = true;
		break;
	    }
	}
	return result;
    }

    private boolean throwsMatch(Class[] inherited, Class[] original) {
	boolean result = true;	
	if (inherited.length != original.length) {
	    System.err.println("### Throws list have differing length.");
	    result = false;   
	} else {
	    for (int i = 0; i < inherited.length; i++) {
		String className = inherited[i].getName();
		if (!throwsFound(className, original)) {
		    result = false;
		    System.err.println("### Exception not found \"" + className + "\" in throws list.");
		    break;
		}
	    }
	}
	if (!result) {
	    dumpThrows(inherited, original);
	}
	return result;
    }

    private boolean isMethodInherited(Class clazz, String methodName, Class[] args, Class originalClass, Class[] originalThrows) {
	if (clazz == null) {
	    System.err.println("### Method NOT inherited \"" + methodName + "()\"");
	    return false;
	}
	Method m = null;
	System.err.println("### Checking class \"" + clazz.getName() +  "\" for inherited method \"" + methodName + "()\"");
	try {
	    m = clazz.getDeclaredMethod(methodName, args);
	    Class[] inheritedThrows = m.getExceptionTypes();
	    boolean result = methodModifiersOK(m.getModifiers(), clazz, originalClass) &&
		throwsMatch(inheritedThrows, originalThrows);
	    if (result) {
		System.err.println("### Method inherited from \"" + clazz.getName() + "." + methodName + "()\"");
	    } else {
		System.err.println("### Method NOT inherited \"" + methodName + "()\"");
	    }
	    return result;
	} catch (NoSuchMethodException nsme) {
	    System.err.println("### NOT FOUND \"" + clazz.getName() + "." + methodName + "()\"");
	}
	return isMethodInherited(clazz.getSuperclass(), methodName, args, originalClass, originalThrows);
    }

    public boolean methodIsInherited(XClassMethod method) throws Exception {
	System.err.println("### Calling methodIsInherited method name \"" + method.name + "()\"");
	String  methodName     = method.name;
        Class   clazz          = normalizedForName(method.home.FullName());
	Class   superClass     = clazz.getSuperclass();
	Class[] args           = createClassArray(method.args);
	Class[] originalThrows = createClassArray(method.xthrows);
	return isMethodInherited(superClass, methodName, args, clazz, originalThrows);
    }

    public boolean methodIsOverridden(XClassMethod method) throws Exception {
	System.err.println("### Calling methodIsOverridden method name \"" + method.name + "()\"");
	return methodIsInherited(method);
    }

    private boolean fieldModifiersOK (int mods, Class declaringClass, Class originalClass) {
	boolean result = false;
	/*
	 * If the field is declared static or private we could
	 * not have inherited it from this class or any super class above
	 * this class in the heirarchy.  This is true because fields with the same
	 * name that are declared private or static hide fields of the same name
	 * in super classes.  So if we find a field with these modifiers we know
	 * we didn't inherit the field.  If the field is public or protected it
	 * has been inherited.  If the field is not public, protected or private
	 * it must be package so we may have inherited the field if the original 
	 * class and the declaring class are in the same package.
	 */
	if (Modifier.isStatic(mods) || Modifier.isPrivate(mods)) {
	    result = false; // both hide an inherited member
	} else if (Modifier.isPublic(mods) || Modifier.isProtected(mods)) {
	    result = true;
	} else if (declaringClass.getPackage().getName().equals(originalClass.getPackage().getName())) {
	    result = true;
	}
	return result;
    }

    private boolean isFieldInherited(Class clazz, String fieldName, Class originalClass) throws Exception {
	if (clazz == null) {
	    System.err.println("### Field NOT inherited \"" +  fieldName + "\"");
	    return false;
	}
	Field f = null;
	try {
	    f = clazz.getDeclaredField(fieldName);
	    int mods = f.getModifiers();
	    boolean result = fieldModifiersOK(mods, clazz, originalClass);
	    if (result) {
		System.err.println("### Field inherited from \"" + clazz.getName() + "." + fieldName + "\"");
	    } else {
		System.err.println("### Field NOT inherited from \"" + clazz.getName() + "." + fieldName + "\"");
	    }
	    return result;
	} catch (NoSuchFieldException nsfe) {
	}
	return isFieldInherited(clazz.getSuperclass(), fieldName, originalClass);
    }

    public boolean fieldIsInherited(XClassField field) throws Exception {
	System.err.println("### Calling fieldIsInherited field name \"" + field.name + "\"");
	String  fieldName  = field.name;
	Class   clazz      = normalizedForName(field.home.FullName());
	Class   superClass = clazz.getSuperclass();
	return isFieldInherited(superClass, fieldName, clazz);
    }

    public boolean fieldIsOverridden(XClassField field) throws Exception {
	System.err.println("### Calling fieldIsOverridden field name \"" + field.name + "\"");
	return fieldIsInherited(field);
    }

    private Class normalizedForName(String clazzName) throws Exception {
	Class clazz = null;
        try {
	    clazz = Class.forName(clazzName);
	} catch (ClassNotFoundException e) {
	    System.err.println("Could not load class \"" + clazzName +"\"");
	    System.err.println("Trying a nested version of the class name");
	    int lastDot = clazzName.lastIndexOf(".");
	    String normalizedClazzName = clazzName.substring(0, lastDot) +
		"$" + clazzName.substring(lastDot + 1);
	    System.err.println("Normalized \"" + clazzName + " to \"" + normalizedClazzName + "\"");
	    clazz = Class.forName(normalizedClazzName);	    
	}
	return clazz;
    }

}
