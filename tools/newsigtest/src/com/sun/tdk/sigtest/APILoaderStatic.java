/*
 * Copyright (c) 2003, 2018 Oracle and/or its affiliates. All Rights Reserved.
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

package com.sun.tdk.sigtest;

import com.sun.tdk.sigtest.api.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;


class APILoaderStatic extends API 
{
    static final String separator     = File.separator,
                        pathSeparator = File.pathSeparator;
    static final char   separatorChar     = File.separatorChar,
                        pathSeparatorChar = File.pathSeparatorChar;
    

    String classpath;
    List elements;
    StaticClassReader rdr;


    APILoaderStatic (String cp, boolean vm)
    {
        classpath = cp;
        elements  = new ArrayList();
        
	    StringTokenizer st = new StringTokenizer(cp, pathSeparator, false);
    	while (st.hasMoreElements())  {
	        String s = (String)st.nextElement();
            if (s.length() == 0)
                continue;
                
            File f = new File(s);
       	    if (!f.exists()) {
              	System.err.println("Ignored: " + s);
	            continue;
            }
			
        	if (f.isDirectory()) {
                elements.add(new ClassPathDir(s));
        	}
		
        	else {
                try {
            		ClassPathZip z = new ClassPathZip(f);
                    elements.add(z);
	            }
        	    catch (IOException e) {
	            	System.err.println("Not a zip file: " + s);
		            return;
    	        }
            }
        }
        
        rdr = new StaticClassReader(this);
        rdr.valuemode = vm;
    }

    
    //  Implementation of API interface
    
    public void close ()
    {
        rdr = null;
    
        if (elements != null) {
            for (Iterator it = elements.iterator(); it.hasNext();) {
                ClassPathElement pe = (ClassPathElement)it.next();
                pe.close();
            }
            
            elements = null;
        }
        
        classpath = null;
    }
    
    
    //  Implementation of API interface
    
    public ClassIterator getClassIterator ()
    {
        return new ClassIteratorPath(classpath);
    }
    
    
    //  Implementation of API interface
    //  Sequental-access method not supported
    
    public XClass getXClass ()
    {
        throw new UnsupportedOperationException();
    }


    //  Implementation of API interface
    //  Sequental-access method not supported
    
    public void rewind ()
    {
        throw new UnsupportedOperationException();
    }


    //  Implementation of API interface
    //  Random-access (by fully qulified name) method
    
    public XClass getXClass (String fqn)
    {
        int idx = fqn.indexOf('$');
        String topfqn = (idx == -1 ) ? fqn : fqn.substring(0, idx);
        
        XClass x = make(topfqn);
        x.packname = Utils.getPackClassName(topfqn, x.name);
        
        XClass xclass;        
        if (idx == -1)
            xclass = x;
        else {
            xclass = x.findInner(fqn);
            if (xclass == null) 
                System.err.println("Problem with inner class: " + fqn);
        }
        return xclass;
    }
    
    
    XClass make (String fqn)
    {
        for (Iterator it = elements.iterator(); it.hasNext();) {
            ClassPathElement pe = (ClassPathElement)it.next();
            ClassData cd = pe.find(fqn);
            if (cd != null) {
                rdr.read(fqn, cd);
                
                XClass outer = rdr.outer;
                Collection inners = rdr.inners;
                if (inners != null) 
                    for (Iterator i = inners.iterator(); i.hasNext();) {
                        String s = (String)i.next();
                        XClass inner = make(s);
                        if (inner != null)
                            inner.link(outer);
                        else
                            System.err.println("MISSING required inner class " + s);
                    }
                  
                outer.setDefaults();  
                return outer;
            }
        }
        //System.out.println("missing " + fqn);        
        return null;
    }
    
    
    interface ClassPathElement
    {
        ClassData find (String fqn);

        void close ();
    }


    static class ClassPathDir implements ClassPathElement
    {
        String path;


        ClassPathDir (String s)
        {
            path = s + separator;
        }


        public ClassData find (String fqn)
        {
            ClassDataFile f = new ClassDataFile(path + fqn.replace('.', separatorChar) + ".class");
            return f.exists() ? f : null;        
        }


        public void close ()
        {
        }
    }


    static class ClassPathZip extends ClassDataZip implements ClassPathElement
    {
        ClassPathZip (File f) throws IOException
        {
            super(f);
        }


        public ClassData find (String fqn)
        {
            ZipEntry ze = getEntry(fqn.replace('.', '/') + ".class");
            set(ze);
            return ze != null ? this : null;
        }
    }
 
}


    
class StaticClassReader
{    
    APILoaderStatic api;
    boolean valuemode;
    
    XClass     outer;
    Collection inners;

    ClassData classdata;
    String givenname,
           ownname;

    DataInputStream in;

    Constant[] constants;

    static final int
        CONSTANT_Class              = 7,
        CONSTANT_Fieldref           = 9,
        CONSTANT_Methodref          = 10,
        CONSTANT_InterfaceMethodref = 11,
        CONSTANT_String             = 8,
        CONSTANT_Integer            = 3,
        CONSTANT_Float              = 4,
        CONSTANT_Long               = 5,
        CONSTANT_Double             = 6,
        CONSTANT_NameAndType        = 12,
        CONSTANT_Utf8               = 1;


    StaticClassReader (APILoaderStatic a)
    {
        api = a;
    }


	void read (String fullname, ClassData cd)
	{
        outer  = null;
        inners = null;
        
        givenname = fullname;
        InputStream is = null;
        ByteArrayInputStream bs;

        try {
            is = cd.getStream();
            byte data[] = new byte[cd.getCount()];

            for (int total = 0; total < data.length;)
                total += is.read(data, total, data.length - total);

            bs = new ByteArrayInputStream(data);
            in = new DataInputStream(bs);

            readClass();
        }
        catch (IOException e) {
            System.err.println(e);
        }
        catch (Error e) {
            System.err.println(e.getMessage());
        }

        constants = null;

        try {
            if (in != null)
                in.close();
                
            if (is != null)
                is = null;
        }
        catch (IOException e) {
            System.err.println(e);
        }

        in = null;
        is = null;
        givenname = null;
        classdata = null;
    }


    void readClass () throws IOException
    {
        if (in.readInt() != 0xCAFEBABE) err("Not a class file");

        int minor = in.readUnsignedShort();
        int major = in.readUnsignedShort();

        readConstants();

        int access = in.readUnsignedShort();

        ownname = getClassName(in.readUnsignedShort());
        if (ownname == null || !ownname.equals(givenname))
            err(null);

        outer = api.newXClass();
        outer.defined = true;
        outer.name = Utils.getSimpleClassName(ownname);
        //outer.packname = Utils.getPackClassName(ownname, outer.name);

        outer.extend = getClassName(in.readUnsignedShort());

        int n = in.readUnsignedShort();
        outer.implement = new String[n];
        for (int i = 0; i < n; i++)
            outer.implement[i] = getClassName(in.readUnsignedShort());

        readFields(outer);

        readMethods(outer);

        ClassAttrs attrs = new ClassAttrs();
        attrs.read();

        if (attrs.access != -1)
            outer.modifiers = getModifiers(attrs.access & ~0x20, XModifier.flaginner);
        else
            outer.modifiers = getModifiers(access & ~0x20, XModifier.flagclass);

        if (outer.isInterface())
            outer.extend = null;
    }


    class ClassAttrs extends AttrsIter
    {
        int access = -1;


        void check (String s) throws IOException
        {
            if (s.equals("InnerClasses"))
            {
                int n = is.readUnsignedShort();
                for (int i = 0; i < n; i++)
                {
                    String inner = getClassName(is.readUnsignedShort());
                    String outer = getClassName(is.readUnsignedShort());
                    int k = is.readUnsignedShort();
                    String simple = (k == 0 ? null : getName(k));
                    int x = is.readUnsignedShort();

                    if (inner != null && inner.equals(ownname))
                    {
                        if (access != -1) err(null);
                        access = x;
                    }
                    
                    if (outer != null && outer.equals(ownname)) {
                        if (inners == null)
                            inners = new ArrayList();
                        inners.add(inner);
                    }
                }
            }
        }
    }


    //  Read and store constat pool
    //
    void readConstants () throws IOException
    {
        int n = in.readUnsignedShort();
        constants = new Constant[n];

        constants[0] = null;

        for (int i = 1; i < n; i++)
        {
            Constant c = new Constant();
            constants[i] = c;           

            c.tag = in.readByte();
            switch (c.tag)
            {
                case CONSTANT_Class:
                    c.info = new Short(in.readShort());
                    break;

                case CONSTANT_Fieldref:
                case CONSTANT_Methodref:
                case CONSTANT_InterfaceMethodref:
                    c.info = new Integer(in.readInt());
                    break;

                case CONSTANT_String:
                    c.info = new Short(in.readShort());
                    break;

                case CONSTANT_Integer:
                    c.info = new Integer(in.readInt());
                    break;

                case CONSTANT_Float:
                    c.info = new Float(in.readFloat());
                    break;

                case CONSTANT_Long:
                    c.info = new Long(in.readLong());
                    i++;
                    break;

                case CONSTANT_Double:
                    c.info = new Double(in.readDouble());
                    i++;
                    break;

                case CONSTANT_NameAndType:
                    c.info = new Integer(in.readInt());
                    break;

                case CONSTANT_Utf8:
                    c.info = in.readUTF();
                    break;

                default:
                    //System.err.println("class "+givensname);
                    //System.err.println(String.valueOf(i)+") tag? "+String.valueOf(c.tag));
                    err(null);
            }
        }
    }


    //  Read and store class fields
    //
    void readFields (XClass xclass) throws IOException
    {
        int n = in.readUnsignedShort();
        for (int i = 0; i < n; i++)
        {
            XClassField xfield = api.newXClassField();
            xfield.modifiers = getModifiers(in.readUnsignedShort(), XModifier.flagfield);
            xfield.name = getName(in.readUnsignedShort());
            xfield.type = Utils.convertVMType(new StringBuffer(getName(in.readUnsignedShort())));

            FieldAttrs attrs = new FieldAttrs();
            attrs.read();

            if ((xfield.modifiers & XModifier.xstatic) != 0)
                xfield.value = attrs.value;

            if (xfield.value != null)
            {
                if (xfield.type.equals("boolean"))
                    xfield.value = new Boolean(((Integer)xfield.value).intValue() != 0);

                else if (xfield.type.equals("byte"))
                    xfield.value = new Byte(((Integer)xfield.value).byteValue());

                else if (xfield.type.equals("char"))
                    xfield.value = new Character((char)((Integer)xfield.value).shortValue());
            }

            xfield.link(xclass);
        }
    }


    class FieldAttrs extends AttrsIter
    {
        Object value = null;


        void check (String s) throws IOException
        {
            if (s.equals("ConstantValue"))
            {
                if (value != null) err(null);

                if (valuemode)
                {
                    Constant c = getConstant(is.readUnsignedShort());
                    switch (c.tag)
                    {
                        case CONSTANT_Long:
                        case CONSTANT_Float:
                        case CONSTANT_Double:
                        case CONSTANT_Integer:
                            value = c.info;
                            break;

                        case CONSTANT_String:
                            value = getName(((Short)c.info).intValue() & 0xFFFF);
                            break;

                        default:
                            err(null);
                    }
                }
            }
        }
    }


    //  Read and store class methods and constructors
    //
    void readMethods (XClass xclass) throws IOException
    {
        int n = in.readUnsignedShort();
        for (int i = 0; i < n; i++)
        {
            int modifiers = getModifiers(in.readUnsignedShort(), XModifier.flagmethod);
            String name = getName(in.readUnsignedShort());

            StringBuffer descr = new StringBuffer(getName(in.readUnsignedShort()));
            String[] args = getArgs(descr);
            String   type = Utils.convertVMType(descr);

            MethodAttrs attrs = new MethodAttrs();
            attrs.read();

            if (name.equals("<init>"))
            {
                XClassCtor xctor = api.newXClassCtor();
                xctor.modifiers = modifiers;
                xctor.name = xclass.name;
                xctor.args = args;
                xctor.xthrows = attrs.xthrows;
                xctor.link(xclass);
            }
            else if (!name.equals("<clinit>"))
            {                   
                XClassMethod xmethod = api.newXClassMethod();
                xmethod.modifiers = modifiers;
                xmethod.name = name;
                xmethod.args = args;
                xmethod.type = type;
                xmethod.xthrows = attrs.xthrows;
                xmethod.link(xclass);
            }
        }              
    }


    class MethodAttrs extends AttrsIter
    {
        String[] xthrows;

        void check (String s) throws IOException
        {
            if (s.equals("Exceptions")) {
                int n = is.readUnsignedShort();
                xthrows = new String[n];
                for (int i = 0; i < n; i++)
                    xthrows[i] = getClassName(is.readUnsignedShort());
            }
        }
    }


    String[] getArgs (StringBuffer s)
    {
        List tt = new ArrayList();

        if (s.length() == 0 || s.charAt(0) != '(') err(null);
        s.deleteCharAt(0);

        while (s.length() != 0 && s.charAt(0) != ')')
            tt.add(Utils.convertVMType(s));

        if (s.length() == 0 || s.charAt(0) != ')') err(null);

        s.deleteCharAt(0);
        if (s.length() == 0) err(null);

        return (String[])tt.toArray(new String[tt.size()]);
    }


    class AttrsIter
    {
        ByteArrayInputStream bs;
        DataInputStream is;

        boolean synthetic = false,
               deprecated = false;
        String  signature = null;


        void read () throws IOException
        {
            int n = in.readUnsignedShort();

            for (int i = 0; i < n; i++) {
                String name = getName(in.readUnsignedShort());
                int count = in.readInt();
                //System.err.println("  attribute: "+name+" count: "+count);

                if (count != 0) {
                    byte[] info = new byte[count];
                    in.readFully(info);
                    bs = new ByteArrayInputStream(info);
                    is = new DataInputStream(bs);
                }

                if (name.equals("Synthetic"))
                    synthetic = true;

                else if (name.equals("Deprecated"))
                    deprecated = true;

                else if (name.equals("Signature")) 
                    signature = getName(is.readUnsignedShort());
                
                else
                    check(name);

                if (count != 0) {
                    try {
                        is.close();
                    }
                    catch (IOException x) {
                    }

                    is = null;
                    bs = null;
                }
            }
        }


        void check (String s)  throws IOException
        {
        }
    }


    String getClassName (int i) throws IOException
    {
        if (i == 0)
            return null;

        Constant c = getConstant(i);
        if (c.tag != CONSTANT_Class) err(null);

        c = getConstant(((Short)c.info).shortValue());
        if (c.tag != CONSTANT_Utf8) err(null);

        return ((String)c.info).replace('/', '.');
    }


    String getName (int i) throws IOException
    {           
        Constant c = getConstant(i);
        if (c.tag != CONSTANT_Utf8) err(null);
        return (String)c.info;
    }


    Constant getConstant (short i)
    {
        return getConstant(((int)i) & 0xFFFF);
    }


    Constant getConstant (int i)
    {
        if (i <= 0 || i >= constants.length) err(null);
        return constants[i];
    }


    static int getModifiers (int flags, int allowed)
    {
        int x = 0;

        if ((flags & 0x001) != 0) x |= XModifier.xpublic;
        if ((flags & 0x002) != 0) x |= XModifier.xprivate;
        if ((flags & 0x004) != 0) x |= XModifier.xprotected;
        if ((flags & 0x008) != 0) x |= XModifier.xstatic;
        if ((flags & 0x010) != 0) x |= XModifier.xfinal;
        if ((flags & 0x020) != 0) x |= XModifier.xsynchronized;
        if ((flags & 0x040) != 0) x |= XModifier.xvolatile;
        if ((flags & 0x080) != 0) x |= XModifier.xtransient;
        if ((flags & 0x100) != 0) x |= XModifier.xnative;
        if ((flags & 0x200) != 0) x |= XModifier.xinterface;
        if ((flags & 0x400) != 0) x |= XModifier.xabstract;
        if ((flags & 0x800) != 0) x |= XModifier.xstrictfp;

        //if ((x & ~allowed) != 0) {
        //    System.err.println("not allowed modifier(s): \""+
        //                       XModifier.toString(x & ~allowed)+"\"");
        //    x &= allowed;
        //}

        return x;
    }


    void err (String s)
    {
        StringBuffer msg = new StringBuffer();
        msg.append("Invalid class file: ").append(classdata.getName());

        if (s != null)
            msg.append("  \n").append(s);

        throw new Error(msg.toString()); 
    }


    static class Constant
    {
        byte tag;
        Object info;
    }

}

