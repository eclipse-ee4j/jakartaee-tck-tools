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


class APIReaderSig extends API
{

    static final String line1v0 = "#Signature file",
                        line1v1 = "#Signature file v1",
                        line1v2 = "#Signature file v2",
                        line1p  = "#PJava Signature file",
                        line2   = "#Version";

    String fname;
    //LineNumberReader in;
    BufferedReader in;
    String fmtid;
    String line;
    StringTokenizer tokens;
    String lex;
    char   chr;
    XClass xclass;
    ArrayList intfs;
    Map inners;
    
       
//  Check if valid sigfile was specified.

    static boolean check (BufferedReader rdr) 
    {
        boolean ok = false;
        String s;
    
        try {
            if ((s = rdr.readLine()) == null)
                return false; 
                
            s = s.trim();
            
            if (!s.equals(line1v1) &&
                !s.equals(line1v2) &&
                !s.equals(line1p)) {
                return false;
            }
            
            if ((s = rdr.readLine()) == null)
                return false; 
            
            if (!s.startsWith(line2)) {
                return false;
            }
            
            ok = true;
        }
        catch (IOException e) {
        }
        
        return ok;
    }
    
    
    APIReaderSig (BufferedReader rdr, String f) 
    {
        in    = rdr;
        fname = f;
        inners = new HashMap();
        setProp("Members", "Inherited");
    }

    
    public void close () 
    {
        close_CLSS();
        tokens = null;
        line   = null;
        
        inners = null;
        fname  = null;
        in     = null;
    }

    
    //  Implementation of API interface
    
    public ClassIterator getClassIterator ()
    {
        return null;
    }
    
    
    //  Implementation of API interface
    //  Random-access (by fully qulified name) method not supported
    
    public XClass getXClass (String fqn)
    {
        throw new UnsupportedOperationException();
    }


    //  Implementation of API interface
    //  Sequental-access method
    
    public XClass getXClass ()
    {
	    try {
            for (;;) {
                read();
                if (xclass == null) 
                    return null;
                    
                if (xclass.home != null)
                    continue;
                    
                return xclass;
            }
                
    	}
	    catch (IOException e) {
	        System.err.println(e.getMessage());
    	}
	    catch (Error e) {
	        System.err.println(e.getMessage());
            //e.printStackTrace();
    	}

        return null;
    }


    //  Implementation of API interface
    //  Sequental-access method
    
    public void rewind ()
    {
        throw new UnsupportedOperationException();
    }
    
    
    
//  Read sigfile.


    boolean isFQMembers ()
    {
        return fmtid != null && (fmtid.equals("v1") || fmtid.equals("v2"));
    }
        

    void read () throws IOException 
    {
        for (xclass = null;;) {

            if (line == null) {
                if ((line = in.readLine()) == null)
                    break;
                line = line.trim();
                if (line.length() == 0) {
                    line = null;
                    continue;
                }
            }
            
            if (line.charAt(0) == '#') {
                if (line.startsWith(line1v0)) {
                    if (line.length() == line1v0.length()) 
                        fmtid = "v0";
                    else
                        fmtid = line.substring(line1v0.length()+1).trim();
                } 
                if (line.equals(line1p)) {
                    fmtid = "v1";
                }
                    
                if (line.startsWith(line2) && line.length() > line2.length()) 
                    setProp("Version", line.substring(line2.length()).trim());
                    
                if (fmtid.equals("v2") && line.equals("#ThrowsRemoved")) 
                    setProp("Throws", "Removed");
                    
                if (fmtid.equals("v2") && line.equals("#ThrowsNormalized")) 
                    setProp("Throws", "Normalized");
                    
                line = null;
                continue;
            }

            if (line.length() < 5)
                read_err();

            tokens = new StringTokenizer(line.substring(5), " ,()<>", true);
            scan(); 

            if (line.startsWith("CLSS ")) {
                if (xclass != null) {
                    break;
                }
                read_CLSS();
            }
            else if (line.startsWith("supr "))
                read_supr();
            else if (line.startsWith("intf "))
                read_intf();
            else if (line.startsWith("cons "))
                read_cons();
            else if (line.startsWith("meth "))
                read_meth();
            else if (line.startsWith("fld  "))
                read_fld();
            else if (line.startsWith("innr "))
                read_innr();
            else
                read_err();

            line = null;
        }

        close_CLSS();
    }
    
    
    void nameXClass (XClass xclass, String fqn)
    {
        xclass.name = Utils.getSimpleClassName(fqn);
        
        int l;
        if ((l = fqn.lastIndexOf('$')) == -1) {
            xclass.packname = Utils.getPackClassName(fqn, xclass.name);
        }
        else {
            String outname = fqn.substring(0, l);
            XClass outclass = (XClass)inners.get(outname);
            if (outclass == null) {
                outclass = newXClass();
                nameXClass(outclass, outname);
                inners.put(outname, outclass);
            }
            
            xclass.link(outclass);
            inners.put(fqn, xclass);
        }
    }


    void read_CLSS () 
    {
        close_CLSS();

        int m = scanModifiers();
        
        //System.out.println("CLSS "+lex);
        
        xclass = (XClass)inners.get(lex);
        if (xclass == null) {
            xclass = newXClass();
            nameXClass(xclass, lex);
        }
        else {
            if (xclass.defined)
                read_err();
        }
        
        xclass.modifiers = m;
        xclass.defined  = true;
        intfs = new ArrayList();
        
        scan();
        if (lex != null)
            read_err();
    }


    void close_CLSS ()
    {
        if (xclass == null)
            return;

        xclass.implement = toArray(intfs);
        intfs  = null;
    }


    void read_supr () 
    {
        if (xclass == null)
            return;

        if (lex == null)
            read_err();

        if (lex.equals("null")) {
            if ((xclass.modifiers & XModifier.xinterface) != 0 ||
                xclass.name.equals("java.lang.Object"))
                xclass.extend = null;
            else
                xclass.extend = "java.lang.Object";
                
            scan();
        }
        
        else {
            String prevname = null;
            while (lex != null) {
            /***
                XClass x = api.findAdd(lex);
                if (x.isInterface()) 
                    read_err();
                
                if (prevname != null) {
                    if (x.extend == null)
                        x.extend = prevname;
                    else if (x.extend.equals(prevname))
                            read_err();
                }
                
            ***/
                xclass.extend = lex;
                prevname = lex;
                scan();
            }
        }
    
        if (lex != null) 
            read_err();
    }


    void read_intf () 
    {
        if (xclass == null)
            return;

        intfs.add(lex);

        scan();
        if (lex != null)
            read_err();
    }


    void read_cons () 
    {
        if (xclass == null)
            return;

        XClassCtor mbr = newXClassCtor();
        mbr.modifiers = scanModifiers();
        scanName(mbr);
        mbr.args = scanArgs();
        mbr.xthrows = scanThrows();
        mbr.link(xclass);

        if (mbr.isInherited() || !mbr.name.equals(xclass.name))
            read_err();

        if (lex != null)
            read_err();
    }


    void read_meth () 
    {
        if (xclass == null)
            return;

        XClassMethod mbr = newXClassMethod();
        mbr.modifiers = scanModifiers();
        mbr.type = scanType();
        scanName(mbr);
        mbr.args = scanArgs();
        mbr.xthrows = scanThrows();
        mbr.link(xclass);

        if (lex != null)
            read_err();
    }


    void read_fld () 
    {
        if (xclass == null)
            return;

        XClassField mbr = newXClassField();
        mbr.modifiers = scanModifiers();
        mbr.type = scanType();
        scanName(mbr);
        mbr.link(xclass);

        if (lex != null)
            read_err();
    }


    void read_innr () 
    {
        if (xclass == null)
            return;

        int m = scanModifiers();
        
        //System.out.println("innr "+lex);

        int l = lex.lastIndexOf('$');
        if (l == -1)
            read_err();
            
        XClass mbr = (XClass)inners.get(lex);
        if (mbr == null) {
            mbr = newXClass();
            nameXClass(mbr, lex);
            mbr.modifiers = m;                
        }
        else {
            if (mbr.modifiers != m)
                read_err();
        }
            
        String outname = lex.substring(0, l);

        if (outname.equals(xclass.getFullName())) {
            mbr.inherited = null;
        }
        else {
            XClass xxx = newXClass();
            xxx.name      = mbr.name;
            xxx.modifiers = mbr.modifiers;                
            
            if ((xxx.inherited = mbr.inherited) == null)
                xxx.inherited = mbr.home.getFullName();
                
            xxx.link(xclass);
        }

        scan();
        
        if (lex != null)
            read_err();
    }


    String scanName (XClassMember mbr)
    {
        String s = lex;           
        int i = lex.lastIndexOf('.');

        if (isFQMembers()) {
            if (i == -1) 
                read_err();
            mbr.name = lex.substring(i+1);
            mbr.inherited = lex.substring(0,i);

            if (xclass.getFullName().equals(mbr.inherited))
                mbr.inherited = null;
        }
        else {
            if (i != -1) 
                read_err();
            mbr.name = lex;
        }

        scan();
        return s;
    }


    String[] scanArgs ()
    {
        ArrayList w = new ArrayList();

        if (chr == '(') {
            scan();

            if (chr != ')') {
                while (lex != null) {
                    w.add(scanType());
                    if (chr != ',')
                        break;
                    scan();
                }
            }

            if (chr != ')')
                read_err();
            scan();
        }

        return toArray(w);
    }


    String[] scanThrows ()
    {
        ArrayList w = new ArrayList();

        if (lex != null && lex.equals("throws")) {
            scan();

            while (lex != null) {
                w.add(scanType());
                if (chr != ',')
                    break;
                scan();
            }
        }

        return toArray(w);
    }


    String scanType ()
    {
        String r = lex;
        if (chr == '[') 
            r = Utils.convertVMType(new StringBuffer(lex));

        scan();
        return r;
    }


    int scanModifiers () 
    {
        int mask = 0, m;

        while (lex != null && (m = XModifier.convert(lex)) != 0) {
            mask |= m;
            scan();
        }

        return mask;
    }


    boolean scan () 
    {
        do {
            if (tokens.hasMoreElements()) {
                lex = (String)tokens.nextElement();
                chr = lex.charAt(0);
            }
            else
                lex = null;
        }
        while (chr == ' ');

        return lex != null;
    }


    void read_err () 
    {
        //throw new Error("Error at line " + in.getLineNumber() + ":\n" + line);
        throw new Error("Error in line " + "\n" + line);
    }


    String[] toArray (ArrayList list) 
    {
        if (list != null && list.size() != 0) 
            return (String[])list.toArray(new String[list.size()]);
        else
            return null;
    }
}
