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
 *      This module builds class description from its class file.
 */


package javasoft.sqe.apiCheck;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;



class ReadStatic implements ReadClasses
{
	XProg prog;
    boolean valuemode;


    ReadStatic (boolean v)
    {
        valuemode = v;
    }

	
    public
	boolean ReadPath (API api, String classpath, PackageSet packs)
	{
        long t0 = Main.GetTimer();

		api.xprog = prog = new XProg();

        api.props.Add("date", new Date().toString());

        if (classpath == null || classpath.length() == 0)
        {
            classpath = System.getProperty("java.class.path");
        }
        else if (classpath.equals("$") || classpath.equals("@"))
        {
            classpath = System.getProperty("sun.boot.class.path");
            api.props.Add("version", System.getProperty("java.version")); 
        }
        else
        {
        }

        api.props.Add("classpath", classpath);
        //prog.props.Add("static", null);

        if (!packs.IsEmpty())
            api.props.Add("package",  packs.toString());

        PathWalk path = new PathWalk(packs, new Processor());
		
		StringTokenizer st = new StringTokenizer(classpath, 
                                                 File.pathSeparator,
                                                 false); 
        
        int errs = 0;
        
        while (st.hasMoreElements())
            if (!path.Walk((String)st.nextElement()))
                errs++;
		
		path.Close();	
        prog = null;

        Main.PrintTimer("ReadStatic "+classpath, t0);
        return errs == 0;
	}


    class Processor implements ClassProcessor
    {

        String filename,
               packname,
               classname,
               ownname;

        DataInputStream in;

        Constant[] constants;

        static final 
        int CONSTANT_Class              = 7,
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


		public 
        void ProcessClass (String pack, String name, Object src, Object sub)
		{
            //System.err.println("ProcessClass "+pack+" + "+name);	
            packname  = pack;
            classname = name;

            try
            {
                Process(src, sub);
            }
            catch (Throwable x)
            {
            }
            
        }


        void Process (Object src, Object sub)
		{
            InputStream ins = null;
            int bcount = 0;

            if (src instanceof File)
            {
                File f = (File)src;
                filename  = f.toString();
                try
                {
                    ins = new FileInputStream(f);
                    bcount = (int)f.length();
                }
                catch (FileNotFoundException x)
                {
                }
            }
            else if (src instanceof ZipFile)
            {
                ZipFile  zf = (ZipFile)src;
                ZipEntry ze = (ZipEntry)sub;
                filename = zf.getName(); 
                try
                {
                    ins = zf.getInputStream(ze);
                    bcount = (int)ze.getSize();
                }
                catch (IOException x)
                {
                }
            }
            else
            {
                System.err.println("Invalid class source " + src.toString());
            }

            if (ins == null)
            {
                System.err.println("Class not found ? " + packname + classname);
            }
            else
            {
                ByteArrayInputStream bs;

                try
                {
                    byte data[] = new byte[bcount];

                    for (int total = 0; total < data.length;)
                        total += ins.read(data, total, data.length - total);

                    bs = new ByteArrayInputStream(data);
                    in = new DataInputStream(bs);
                    
                    ReadClass();
                }
                catch (IOException x)
                {
                    System.err.println(x);
                }

                constants = null;

                try
                {
                    in.close();
                }
                catch (IOException x)
                {
                    System.err.println(x);
                }

                in = null;
                ins = null;
            }
		}


        void ReadClass () throws IOException
        {
            if (in.readInt() != 0xCAFEBABE) 
                Err("Not a class file");

            int minor = in.readUnsignedShort();
            int major = in.readUnsignedShort();

            ReadConstants();
            
            int access = in.readUnsignedShort();

            ownname = GetClassName(in.readUnsignedShort());
            if (ownname == null) 
                Err(null);

            if (!ownname.equals(packname + classname))
                Err("Invalid class name \"" + ownname + "\"");

            XClass xclass = prog.DefineClass(ownname);
            if (xclass.defined)
                Err("Duplicate class \"" + ownname + "\"");

            //System.err.println("CLASS:"+ownname);
                
            String s = GetClassName(in.readUnsignedShort());
            if (s != null)
                xclass.extend = prog.DefineClass(s);

            int n = in.readUnsignedShort();
            for (int i = 0; i < n; i++)
            {
                s = GetClassName(in.readUnsignedShort());
                xclass.implement.Add(prog.DefineClass(s));
            }

            ReadFields(xclass);

            ReadMethods(xclass);

            ClassAttrs attrs = new ClassAttrs();
            attrs.Read();

            if (attrs.access != -1)
                xclass.modifier = GetModifier(attrs.access & ~0x20, XModifier.flaginner);
            else
                xclass.modifier = GetModifier(access & ~0x20, XModifier.flagclass);

            if (xclass.IsInterface())
                xclass.extend = null;

            xclass.defined = true;
        }


        class ClassAttrs extends AttrsIter
        {
            int access = -1;


            void Check (String s) throws IOException
            {
                if (s.equals("InnerClasses"))
                {
                    int n = is.readUnsignedShort();
                    for (int i = 0; i < n; i++)
                    {
                        String inner = GetClassName(is.readUnsignedShort());
                        String outer = GetClassName(is.readUnsignedShort());
                        int k = is.readUnsignedShort();
                        String simple = (k == 0 ? null : GetName(k));
                        int x = is.readUnsignedShort();

                        if (inner != null && inner.equals(ownname))
                        {
                            if (access != -1) Err(null);
                            access = x;
                        }
                    }
                }
            }
        }


        //  Read and store constat pool
        //
        void ReadConstants () throws IOException
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
                        //System.err.println("class "+packname+classname);
                        //System.err.println(String.valueOf(i)+") tag? "+String.valueOf(c.tag));
                        Err(null);
                }
            }
        }


        //  Read and store class fields
        //
        void ReadFields (XClass xclass) throws IOException
        {
            int n = in.readUnsignedShort();
            for (int i = 0; i < n; i++)
            {
                XClassField xfield = new XClassField();
                xfield.modifier = GetModifier(in.readUnsignedShort(), XModifier.flagfield);
                xfield.name = GetName(in.readUnsignedShort());
                xfield.type = prog.DefineVMType(new StringBuffer(GetName(in.readUnsignedShort())));

                FieldAttrs attrs = new FieldAttrs();
                attrs.Read();

                if ((xfield.modifier & XModifier.xstatic) != 0)
                    xfield.value = attrs.value;

                if (xfield.value != null)
                {
                    String t = xfield.type.toString();

                    if (t.equals("boolean"))
                        xfield.value = new Boolean(((Integer)xfield.value).intValue() != 0);

                    else if (t.equals("byte"))
                        xfield.value = new Byte(((Integer)xfield.value).byteValue());

                    else if (t.equals("char"))
                        xfield.value = new Character((char)((Integer)xfield.value).shortValue());
                }

                xfield.Link(xclass);
            }
        }


        class FieldAttrs extends AttrsIter
        {
            Object value = null;


            void Check (String s) throws IOException
            {
                if (s.equals("ConstantValue"))
                {
                    if (value != null) Err(null);

                    if (valuemode)
                    {
                        Constant c = GetConstant(is.readUnsignedShort());
                        switch (c.tag)
                        {
                            case CONSTANT_Long:
                            case CONSTANT_Float:
                            case CONSTANT_Double:
                            case CONSTANT_Integer:
                                value = c.info;
                                break;
                                
                            case CONSTANT_String:
                                value = GetName(((Short)c.info).intValue() & 0xFFFF);
                                break;

                            default:
                                Err(null);
                        }
                    }
                }
            }
        }


        //  Read and store class methods and constructors
        //
        void ReadMethods (XClass xclass) throws IOException
        {
            int n = in.readUnsignedShort();
            for (int i = 0; i < n; i++)
            {
                int modifier = GetModifier(in.readUnsignedShort(), XModifier.flagmethod);
                String name = GetName(in.readUnsignedShort());

                StringBuffer descr = new StringBuffer(GetName(in.readUnsignedShort()));
                XTypes args = GetArgs(descr);
                XType  type = prog.DefineVMType(descr);
                XTypes xthrows = new XTypes();

                MethodAttrs attrs = new MethodAttrs();
                attrs.Read();

                if (name.equals("<init>"))
                {
                    XClassConstructor xconstructor = new XClassConstructor();
                    xconstructor.modifier = modifier;
                    xconstructor.name = xclass.name;
                    xconstructor.args = args;
                    xconstructor.xthrows = attrs.xthrows;
                    xconstructor.Link(xclass);
                }
                else if (!name.equals("<clinit>"))
                {                   
                    XClassMethod xmethod = new XClassMethod();
                    xmethod.modifier = modifier;
                    xmethod.name = name;
                    xmethod.args = args;
                    xmethod.type = type;
                    xmethod.xthrows = attrs.xthrows;
                    xmethod.Link(xclass);
                }
            }              
        }


        class MethodAttrs extends AttrsIter
        {
            XTypes xthrows = new XTypes();

            void Check (String s) throws IOException
            {
                if (s.equals("Exceptions"))
                {
                    int n = is.readUnsignedShort();
                    for (int i = 0; i < n; i++)
                    {
                        String x = GetClassName(is.readUnsignedShort());
                        xthrows.Add(prog.DefineType(x));
                    }
                }
            }
        }


        XTypes GetArgs (StringBuffer s)
        {
            XTypes tt = new XTypes();

            if (s.length() == 0 || s.charAt(0) != '(') Err(null);
            s.deleteCharAt(0);

            while (s.length() != 0 && s.charAt(0) != ')')
                tt.Add(prog.DefineVMType(s));

            if (s.length() == 0 || s.charAt(0) != ')') Err(null);

            s.deleteCharAt(0);
            if (s.length() == 0) Err(null);

            return tt;
        }


        class AttrsIter
        {
            ByteArrayInputStream bs;
            DataInputStream is;

            boolean synthetic  = false,
                    deprecated = false;


            void Read () throws IOException
            {
                int n = in.readUnsignedShort();

                for (int i = 0; i < n; i++)
                {
                    String name = GetName(in.readUnsignedShort());
                    int count = in.readInt();
                    //System.err.println("  attribute: "+name+" count: "+count);

                    if (count != 0)
                    {
                        byte[] info = new byte[count];
                        in.readFully(info);
                        bs = new ByteArrayInputStream(info);
                        is = new DataInputStream(bs);
                    }

                    if (name.equals("Synthetic"))
                        synthetic = true;

                    else if (name.equals("Deprecated"))
                        deprecated = true;

                    else
                        Check(name);

                    if (count != 0)
                    {
                        try
                        {
                            is.close();
                        }
                        catch (IOException x)
                        {
                        }

                        is = null;
                        bs = null;
                    }
                }
            }


            void Check (String s) throws IOException
            {
            }
        }


        String GetClassName (int i) throws IOException
        {
            if (i == 0)
                return null;

            Constant c = GetConstant(i);
            if (c.tag != CONSTANT_Class) Err(null);

            c = GetConstant(((Short)c.info).shortValue());
            if (c.tag != CONSTANT_Utf8) Err(null);

            return ((String)c.info).replace('/', '.');
        }


        String GetName (int i) throws IOException
        {           
            Constant c = GetConstant(i);
            if (c.tag != CONSTANT_Utf8) Err(null);
            return (String)c.info;
        }


        Constant GetConstant (short i)
        {
            return GetConstant(((int)i) & 0xFFFF);
        }


        Constant GetConstant (int i)
        {
            if (i <= 0 || i >= constants.length) Err(null);
            return constants[i];
        }


        //static
        int GetModifier (int flags, int allowed)
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

            //if ((x & ~allowed) != 0)
            //    System.err.println("not allowed modifier(s): \""+XModifier.toString(x & ~allowed)+"\"");

            return x & allowed;
        }


        void Err (String s)
        {
            if (s != null)
                System.err.println(s);

            System.err.println("Invalid class file "+filename); 
            throw new Error(); 
        }
	}
}


class Constant
{
    byte tag;
    Object info;
}


