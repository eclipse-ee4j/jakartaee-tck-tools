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
 *      This module writes signature file from class set.
 */


package javasoft.sqe.apiCheck;

import java.io.*;
import java.util.*;

import com.sun.ts.lib.util.SigLogIntf;
import com.sun.ts.lib.util.SigLogAdapter;

class WriteJh
{
    static final 
    String magic = "//Java signature file";

    SigLogIntf os;
    //    PrintWriter os;
    String  defpack = null;
    int     defpacklength;
    String  indent;
    boolean allflag = false;
    int     acclevel = 0;


    WriteJh ()
    {
        defpack = Main.args.getProperty("defpack");
        if (defpack != null)
        {
            if (defpack.length() == 0)
                defpack = null;
            else
            {
                if (!defpack.endsWith("."))
                    defpack += ".";

                defpacklength = defpack.length();
            }
        }

        if (Main.args.getProperty("xpriv") != null)
            acclevel = 2;
    }


    void Close ()
    {
        if (os != null)
        {
            os.flush();
            os.close();
            os = null;
        }
    }


    boolean Write (API api, String f)
    {
        String fname;
        if (f == null || (fname = f.trim()).length() == 0)
        {
            System.err.println("Missing destination file name");
            return false;
        }

//        try {
            os = new SigLogAdapter();
//            os = new PrintWriter(new UFileWriter(fname));
            Write(api);
            return true;
//        }
//         catch (IOException x)
//         {
//             System.err.println("Failed to open destination file "+fname);
//             System.err.println(x);
//             return false;
//         }
    }


    boolean Write (API api, PrintStream s)
    {
        os = new SigLogAdapter();
	//        os = new PrintWriter(s);
        Write(api);
        return true;
    }


    void WriteAll (API api, SigLogIntf pw)
    //    void WriteAll (API api, PrintWriter pw)
    {
        os = pw;
        allflag = true;
        Write(api);
        allflag = false;
    }


    void Write (API api)
    {
        os.println(magic);
        OutProps(api.props);

        indent = "";
        Out(api.GetXProg().packs);

        //OutInfo(xprog);
        os.println("\n//end of Java signature file");
    }


    void OutProps (Props props)
    {
        for (int i = 0; i < props.Size(); i++)
            os.println("//"+props.Key(i)+": "+props.Value(i));

        os.println("");
    }


    void OutInfo (XProg xprog)
    {
        os.println();
        os.println("/*");

        os.println();
        os.println("class types:");
        os.println();
        for (Enumeration e = xprog.typesclass.elements(); e.hasMoreElements();)
        {
            os.print("\t");
            os.println((XTypeClass)e.nextElement());
        }
          
        os.println();
        os.println("array types:");
        os.println();
        for (Enumeration e = xprog.typesarray.elements(); e.hasMoreElements();)
        {
            os.print("\t");
            os.println((XTypeArray)e.nextElement());
        }

        os.println();
        os.println("*/");
    }


    void OutExtern (XProg xprog)
    {       
        OutExternWalk w = new OutExternWalk();
        w.Walk(xprog, null);

        int n = w.externs.size();
        if (n != 0)
        {
            String[] tmp = new String[n];
            w.externs.toArray(tmp);
            Arrays.sort(tmp);

            os.println();
            os.println("/*");
            os.println("use");
            for (int i = 0; i < n; i++)
            {
                os.print("\t");
                if (tmp[i] != null)
                    os.print((String)tmp[i]);
                else
                    os.print("???");
                os.println(i != n-1 ? ',' : ';');
            }
            os.println("*/");
        }
    }


    static 
    class OutExternWalk extends XProgWalk
    {
        Vector externs = new Vector();

        void Walk (XClass x)
        {
            if (!x.defined)
                externs.add(x.FullName());
            else
                super.Walk(x);
        }
    }


    void Out (XPack xpack)
    {
        int n = 0;
        for (Enumeration e = xpack.classes.elements(); e.hasMoreElements();)
            if (((XClass)e.nextElement()).defined)
                n++;

        if (n != 0 || allflag)
        {
            if (xpack.name != null)
            {
                os.println();
                os.print("package ");
                os.print(xpack.FullName());
                os.println(";");
            }

            for (Enumeration e = xpack.classes.elements(); e.hasMoreElements();)
                Out((XClass)e.nextElement());
        }

        for (Enumeration e = xpack.packs.elements(); e.hasMoreElements();)
            Out((XPack)e.nextElement());
    }


    void Out (XClass xclass)
    {
        if (!xclass.defined && !allflag)
            return;
        if (XModifier.Access(xclass.modifier) < acclevel)
            return;

        os.println();
        os.print(indent);

        if (!xclass.defined)
        {
            os.print("/*unknown*/ class ");
            os.print(xclass.name);
        }
        else
        {
            if (xclass.IsInterface())
            {
                print(XModifier.toString(xclass.modifier & ~XModifier.xinterface
                                         /*& ~XModifier.xabstract*/));
                os.print("interface ");
                os.print(xclass);

                if (xclass.implement.size() != 0)
                {
                    os.print(" extends ");
                    OutClassNames(xclass.implement);
                }   
            }
            else
            {
                print(XModifier.toString(xclass.modifier));
                os.print("class ");
                os.print(xclass);

                if (xclass.extend != null)
                    if (!xclass.extend.FullName().equals("java.lang.Object"))
                    {
                        os.print(" extends ");
                        os.print(ShortenClassName(xclass.extend.FullName()));
                    }

                if (xclass.implement.size() != 0)
                {
                    os.print(" implements ");
                    OutClassNames(xclass.implement);
                }   
            }
        }

    //--- Class members

        os.print(" {");

        int first = 0;

        // Construcrors

        for (Enumeration e = xclass.constructors.elements(); e.hasMoreElements();)
        {
            XClassConstructor xcons = (XClassConstructor)e.nextElement();
            if (XModifier.Access(xcons.modifier) < acclevel)
                continue;

            if (first++ == 0) os.println();
            os.print(indent);
            os.print("\t");

            print(XModifier.toString(xcons.modifier));

            //os.print(xcons.toString());
            os.print(xcons.name);
            os.print("(");
            OutClassNames(xcons.args);
            os.print(")");

            //print(" throws ", xcons.xthrows.toString());
            if (xcons.xthrows.size() != 0)
            {
                os.print(" throws ");
                OutClassNames(xcons.xthrows);
            }

            os.println(";");
        }

        // Methods

        for (Enumeration e = xclass.methods.elements(); e.hasMoreElements();)
        {
            XClassMethod xmethod = (XClassMethod)e.nextElement();
            if (XModifier.Access(xmethod.modifier) < acclevel)
                continue;

            if (first++ == 0) os.println();
            os.print(indent);
            os.print("\t");

            int mods = xmethod.modifier;
            //if (xclass.IsInterface())
            //{
            //    mods &= ~XModifier.xabstract;
            //}
            print(XModifier.toString(mods));

            os.print(ShortenClassName(xmethod.type.toString()));
            os.print(" ");
            os.print(xmethod.name);
            os.print("(");
            OutClassNames(xmethod.args);
            os.print(")");

            if (xmethod.xthrows.size() != 0)
            {
                os.print(" throws ");
                OutClassNames(xmethod.xthrows);
            }

            os.println(";");
        }

        // Fields

        for (Enumeration e = xclass.fields.elements(); e.hasMoreElements();)
        {
            XClassField xfield = (XClassField)e.nextElement();
            if (XModifier.Access(xfield.modifier) < acclevel)
                continue;

            if (first++ == 0) os.println();
            os.print(indent);
            os.print("\t");

            int mods = xfield.modifier;
            //if (xclass.IsInterface())
            //{
            //    mods &= ~XModifier.xstatic;
            //    mods &= ~XModifier.xfinal;
            //}
            print(XModifier.toString(mods));

            print(ShortenClassName(xfield.type.toString()));

            os.print(xfield.toString());

            OutValue(xfield);

            os.println(";");
        }

        // Inner classes

        for (Enumeration e = xclass.inners.elements(); e.hasMoreElements();)
        {
            XClass xinner = (XClass)e.nextElement();          
            if (XModifier.Access(xinner.modifier) < acclevel)
                continue;

            if (first++ == 0) os.println();
            indent += "\t";
            Out(xinner);
            indent = indent.substring(0, indent.length()-1);
        }
    
        if (first != 0) os.print(indent);
        os.println("}");
    }


    void OutClassNames (XTypes list)
    {
        for (int i = 0; i < list.size(); i++)
        {
            if (i != 0) os.print(", ");
            os.print(ShortenClassName(((XType)list.elementAt(i)).toString()));
        }   
    }


    void OutClassNames (NamedList list)
    {
        for (int i = 0; i < list.size(); i++)
        {
            if (i != 0) os.print(", ");
            os.print(ShortenClassName(((XClass)list.elementAt(i)).FullName()));
        }   
    }


    String ShortenClassName (String s)
    {
        if (defpack != null && s.startsWith(defpack) && s.indexOf('.', defpacklength) == -1)
            return s.substring(defpacklength);
        else
            return s;
    }


    void OutValue (XClassField xfield)
    {
        if (xfield.value != null)
            os.print("="+Value(xfield.value));
    }


    void print (String s)
    {
        if (s.length() != 0)
        {
            os.print(s);
            os.print(" ");
        }
    }


    void print (String s1, String s2)
    {
        if (s2.length() != 0)
        {
            os.print(s1);
            os.print(s2);
        }
    }


    static
    String Value (Object value)
    {
        String s = null;

        if (value instanceof Character)
            s = "\'" + StuffOut(value.toString()) + "\'";

        else if (value instanceof String)
            s = "\"" + StuffOut(value.toString()) + "\"";

        else if (value instanceof Long)
            s = value.toString() + "L";

        else if (value instanceof Float)
        {
            Float f = (Float)value;
            s = f.toString();
            if (!f.isNaN() && !f.isInfinite())
                s += "f";
        }

        else if (value instanceof Double)
        {
            Double d = (Double)value;
            s = d.toString();
            if (!d.isNaN() && !d.isInfinite())
                s += "d";           
        }

        else // boolean, byte, short, int
            s = value.toString();

        return s;
    }


    static final 
    String spec1 = "\b\t\n\f\r\"\'\\",
           spec2 = "btnfr\"\'\\";

    static
    String StuffOut (String s)
    {
        StringBuffer x = new StringBuffer();

        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);

            int  k = spec1.indexOf(c);
            if (k >= 0)
                x.append('\\').append(spec2.charAt(k));

            else if (c < 0x20)
                x.append(UFileWriter.Esc(c));

            else
                x.append(c);
        }

        return x.toString();
    }
}

