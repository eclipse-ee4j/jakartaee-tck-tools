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
 *      This module compares to sets of classes (base and test), represented by
 *      XProg structures.
 */


package javasoft.sqe.apiCheck;

import java.util.HashSet;
import java.util.Vector;



interface Equalizer
{
    boolean Ident   (Object b, Object t);

    boolean Visible (Object x);
}



class Pairs
{
    XClass base,
           test;

    Equalizer eq;

    Vector basedcl,    
           testdcl,
           deleted, 
           added;

    boolean inherited;
    Vector baseinh,
           testinh;


    Pairs (XClass b, XClass t)
    {
        base = b;
        test = t;

        basedcl = new Vector();  
        testdcl = new Vector();
        deleted = new Vector();
        added   = new Vector();

        inherited = false;
        baseinh = new Vector();  
        testinh = new Vector();
    }


    void Clear()
    {
        basedcl.clear();
        testdcl.clear();
        deleted.clear();
        added.clear();

        inherited = false;
        baseinh.clear();
        testinh.clear();
    }


    void Init (Vector b, Vector t, Equalizer e)
    {
        Clear();

        eq = e;

        added.addAll(t);

        for (int i = 0; i < b.size(); i++)
        {
            Object x = b.elementAt(i);
            for (int k = 0; k < added.size(); k++)
            {
                Object y = added.elementAt(k);
                if (eq.Ident(x, y))
                {
                    if (eq.Visible(x) || eq.Visible(y))
                    {
                        basedcl.addElement(x);
                        testdcl.addElement(y);
                    }
                    x = null;
                    added.removeElementAt(k);
                    break;
                }
            }
            if (x != null && eq.Visible(x))
                deleted.addElement(x);
        }

        for (int i = added.size(); --i >= 0;)
        {
            Object x = added.elementAt(i);
            if (!eq.Visible(x))
                added.removeElementAt(i);
        }
    }


//  Methods inheritance

    boolean IsBaseInherited (XClassMethod x)
    {
        if (!inherited)
        {
            GetMethods(baseinh, base);
            GetMethods(testinh, test);
            inherited = true;
        }

        for (int i = baseinh.size(); --i >= 0;)
            if (eq.Ident(baseinh.elementAt(i), x))
                return true;      

        return false;
    }


    boolean IsTestInherited (XClassMethod x)
    {
        if (!inherited)
        {
            GetMethods(baseinh, base);
            GetMethods(testinh, test);
            inherited = true;
        }

        for (int i = testinh.size(); --i >= 0;)
            if (eq.Ident(testinh.elementAt(i), x))
                return true;      

        return false;
    }


    void GetMethods (Vector v, XClass x)
    {
        if (x.extend != null)
            ExtendMethods(v, x.extend);

        for (int i = 0; i < x.implement.size(); i++)
            ImplementMethods(v, (XClass)x.implement.elementAt(i));

        if (x.extend != null)
            ImplementMethods(v, x.extend);
    }


    void ExtendMethods (Vector v, XClass x)
    {
        for (int i = 0; i < x.methods.size(); i++)
            InheritItem(v, (XClassMethod)x.methods.elementAt(i));

        if (x.extend != null)
            ExtendMethods(v, x.extend);
    }


    void ImplementMethods (Vector v, XClass x)
    {
        for (int i = 0; i < x.methods.size(); i++)
            InheritItem(v, (XClassMethod)x.methods.elementAt(i));

        for (int i = 0; i < x.implement.size(); i++)
            ImplementMethods(v, (XClass)x.implement.elementAt(i));

        if (x.extend != null)
            ImplementMethods(v, x.extend);
    }


//  Fields inheritance

    boolean IsBaseInherited (XClassField x)
    {
        if (!inherited)
        {
            GetFields(baseinh, base);
            GetFields(testinh, test);
            inherited = true;
        }

        for (int i = baseinh.size(); --i >= 0;)
            if (eq.Ident(baseinh.elementAt(i), x))
                return true;      

        return false;
    }


    boolean IsTestInherited (XClassField x)
    {
        if (!inherited)
        {
            GetFields(baseinh, base);
            GetFields(testinh, test);
            inherited = true;
        }

        for (int i = testinh.size(); --i >= 0;)
            if (eq.Ident(testinh.elementAt(i), x))
                return true;      

        return false;
    }


    void GetFields (Vector v, XClass x)
    {
        if (x.extend != null)
            ExtendFields(v, x.extend);

        for (int i = 0; i < x.implement.size(); i++)
            ImplementFields(v, (XClass)x.implement.elementAt(i));

        if (x.extend != null)
            ImplementFields(v, x.extend);
    }


    void ExtendFields (Vector v, XClass x)
    {
        for (int i = 0; i < x.fields.size(); i++)
            InheritItem(v, (XClassField)x.fields.elementAt(i));

        if (x.extend != null)
            ExtendFields(v, x.extend);
    }


    void ImplementFields (Vector v, XClass x)
    {
        for (int i = 0; i < x.fields.size(); i++)
            InheritItem(v, (XClassField)x.fields.elementAt(i));

        for (int i = 0; i < x.implement.size(); i++)
            ImplementFields(v, (XClass)x.implement.elementAt(i));

        if (x.extend != null)
            ImplementFields(v, x.extend);
    }


//  Classes inheritance

    boolean IsBaseInherited (XClass x)
    {
        if (!inherited)
        {
            GetInners(baseinh, base);
            GetInners(testinh, test);
            inherited = true;
        }

        for (int i = baseinh.size(); --i >= 0;)
            if (eq.Ident(baseinh.elementAt(i), x))
                return true;      

        return false;
    }


    boolean IsTestInherited (XClass x)
    {
        if (!inherited)
        {
            GetInners(baseinh, base);
            GetInners(testinh, test);
            inherited = true;
        }

        for (int i = testinh.size(); --i >= 0;)
            if (eq.Ident(testinh.elementAt(i), x))
                return true;      

        return false;
    }


    void GetInners (Vector v, XClass x)
    {
        if (x.extend != null)
            InheritInners(v, x.extend, x);
    }


    void InheritInners (Vector v, XClass x, XClass h)
    {
        for (int i = 0; i < x.inners.size(); i++)
            InheritItem(v, (XClass)x.inners.elementAt(i));

        if (x.extend != null)
            InheritInners(v, x.extend, h);
    }


//  v - vector of inherited members so far
//  x - would be inherited member
//
    void InheritItem (Vector v, XClassMember x)
    {
        if ((x.modifier & XModifier.xprivate) != 0)
            return;

        for (int i = 0; i < v.size(); i++)
            if (eq.Ident(v.elementAt(i), x))
                return;

        v.addElement(x);
    }
}



class XProgComp
{
    PackageSet  packs;
    int         access;

    static final 
    int xvis = XModifier.xpublic | XModifier.xprotected;


    class EqName implements Equalizer
    {
        public
        boolean Ident (Object b, Object t)
        {
            return ((XNode)b).name.equals(((XNode)t).name);
        }


        public
        boolean Visible (Object o)
        {
            return true;
        }
    }


    class EqMember implements Equalizer
    {
        public
        boolean Ident (Object b, Object t)
        {
            return ((XNode)b).name.equals(((XNode)t).name);
        }


        public
        boolean Visible (Object o)
        {
            XClassMember x = (XClassMember)o;

            if (x.home == null)
            {
                if (!((XClass)x).defined)
                    return false;

                switch (access) // for classes (not inner classes)
                {
                    case 1:
                    case 2:
                        return (x.modifier & XModifier.xpublic) != 0;

                   default:
                        return true;
                }
            }
            else
                switch (access) // for class members
                {
                    case 1:  // public
                        return (x.modifier & XModifier.xpublic) != 0;

                    case 2:  // public + protected
                        return (x.modifier & xvis) != 0;

                    case 3:  // public + protected + package
                        return (x.modifier & XModifier.xprivate) == 0;

                   default:  // *all*
                        return true;
                }
        }
    }


    class EqConstructor extends EqMember
    {
        public
        boolean Ident (Object b, Object t)
        {
            return ((XClassConstructor)b).args.equals(((XClassConstructor)t).args);
        }
    }


    class EqMethod extends EqMember
    {
        public
        boolean Ident (Object b, Object t)
        {
            return ((XNode)b).name.equals(((XNode)t).name) &&
                   ((XClassMethod)b).args.equals(((XClassMethod)t).args);
        }
    }


    EqName        eqname   = new EqName();
    EqMember      eqmember = new EqMember();
    EqConstructor eqctor   = new EqConstructor();
    EqMethod      eqmethod = new EqMethod();

    final int stackdepth = 64;
    int       stackidx;
    Pairs[]   stack = new Pairs[stackdepth];
    Pairs     tos;

    HashSet /*XClass*/ basevisited = new HashSet(),
                       testvisited = new HashSet();


	
    void Compare (PackageSet ps, XProg base, XProg test, String acc)
    {
        packs  = ps;

    //  select class members to be compared
        if (acc.equals("public"))
            access = 1;
        else if (acc.equals("protected"))
            access = 2;
        else if (acc.equals("package"))
            access = 3;
        else if (acc.equals("private"))
            access = 4;
        else
        {
            System.err.println("access selector must be public, protected, package or private");
            access = 2;
        }
	
        stackidx = -1;
        tos = null;

        Compare(base.packs, test.packs);

        packs = null;
    }


//  Package


    void Compare (XPack base, XPack test)
    {
        if (!packs.OnPath(base.FullName()) && !packs.OnPath(test.FullName()))
            return;

		Pairs memb = new Pairs(null, null);
        int i;

	//	Compare classes

        if (packs.InPath(base.FullName()) || packs.InPath(test.FullName()))
        {
            memb.Init(base.classes, test.classes, eqmember);
		
            for (i = 0; i < memb.basedcl.size(); i++)
            {
                XClass b = (XClass)memb.basedcl.elementAt(i);
                XClass t = (XClass)memb.testdcl.elementAt(i);
                if (!b.defined)
                    Added(b);
                else if (!t.defined)
                    Deleted(t);
                else
                    XCompare(b, t);
            }
		
            for (i = 0; i < memb.deleted.size(); i++)
            {
                XClass x = (XClass)memb.deleted.elementAt(i);
                Normalize(x);
                Deleted(x);
            }
			
            for (i = 0; i < memb.added.size(); i++)
            {
                XClass x = (XClass)memb.added.elementAt(i);
                Normalize(x);
                Added(x);
            }
        }
		
	//	Compare subpackages
			
		memb.Init(base.packs, test.packs, eqname);
		
		for (i = 0; i < memb.basedcl.size(); i++)
			Compare((XPack)memb.basedcl.elementAt(i), (XPack)memb.testdcl.elementAt(i));
		
		for (i = 0; i < memb.deleted.size(); i++)
			Deleted((XPack)memb.deleted.elementAt(i));
			
		for (i = 0; i < memb.added.size(); i++)
			Added((XPack)memb.added.elementAt(i));
    }


    void Deleted (XPack xpack)
    {
        for (int i = 0; i < xpack.classes.size(); i++)
        {
            XClass x = (XClass)xpack.classes.elementAt(i);
            if (eqmember.Visible(x))
            {
                Normalize(x);
                Deleted(x);
            }
        }

        for (int i = 0; i < xpack.packs.size(); i++)
            Deleted((XPack)xpack.packs.elementAt(i));
    }


    void Added (XPack xpack)
    {
        for (int i = 0; i < xpack.classes.size(); i++)
        {
            XClass x = (XClass)xpack.classes.elementAt(i);
            if (eqmember.Visible(x))
            {
                Normalize(x);
                Added(x);
            }
        }

        for (int i = 0; i < xpack.packs.size(); i++)
            Added((XPack)xpack.packs.elementAt(i));
    }


//  Class


    void XCompare (XClass base, XClass test)
    {
        final boolean bv = basevisited.contains(base),
                      tv = testvisited.contains(test);

        if (bv && tv)
            return;

        if (bv || tv)
            Fatal.Stop("visited: "+base.FullName()+" or "+test.FullName());

        basevisited.add(base);
        testvisited.add(test);

        Normalize(base);
        Normalize(test);
        Compare(base, test);
    }



    void Compare (XClass b, XClass t)
    {
        // to be overriden
        // call CompareMembers(XClass,XClass)
    }


    void CompareMembers (XClass base, XClass test)
    {
    //  Enter new stack frame

        if (++stackidx == stackdepth)
            Fatal.Stop("XProgComp stack overflow");

//        if ((tos = stack[stackidx]) == null)
            stack[stackidx] = tos = new Pairs(base, test);

/*
        layer.vis = ((base.modifier & xvis) != 0 &&
                     (test.modifier & xvis) != 0);

        if (stackidx > 0)
            layer.vis &= stack[stackidx-1].vis;
*/

	//	Compare constructors
		
        int i;

		tos.Init(base.constructors, test.constructors, eqctor);

		for (i = 0; i < tos.basedcl.size(); i++)
			Compare((XClassConstructor)tos.basedcl.elementAt(i), 
                    (XClassConstructor)tos.testdcl.elementAt(i));
		
		for (i = 0; i < tos.deleted.size(); i++)
            Deleted((XClassConstructor)tos.deleted.elementAt(i));
			
		for (i = 0; i < tos.added.size(); i++)
            Added((XClassConstructor)tos.added.elementAt(i));
		
	//	Compare methods
			
		tos.Init(base.methods, test.methods, eqmethod);
		
		for (i = 0; i < tos.basedcl.size(); i++)
			Compare((XClassMethod)tos.basedcl.elementAt(i), 
                    (XClassMethod)tos.testdcl.elementAt(i));
		
		XClassMethod method = null;
		for (i = 0; i < tos.deleted.size(); i++) {
		    /*
		     * If the method is flagged as deleted, this means the method is
		     * in the signature file but does not appear in the test class.
		     * This is an incompatible change unless the deleted method is in fact
		     * inherited from a base class.
		     */
		    method = (XClassMethod)tos.deleted.elementAt(i);
		    boolean methodInherited = false;
		    System.err.println("### Method may be deleted \"" + method.home.FullName() + "." + method.name + "()\"");
		    try {
			methodInherited = InheritedUtil.instance().methodIsInherited(method);
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		    if (!methodInherited) {
			Deleted(method);
		    }
		}
			
		for (i = 0; i < tos.added.size(); i++) {
		    /*
		     * If the method is flagged as added, this means the method is not
		     * in the signature file but appears in the test class.  This is
		     * an incompatible change unless the additional method is in fact
		     * inherited from a base class and overridden in this subclass under
		     * test.
		     */
		    method = (XClassMethod)tos.added.elementAt(i);
		    boolean methodOverridden = false;
		    System.err.println("### Method may be added \"" + method.home.FullName() + "." + method.name + "()\"");
		    try {
			methodOverridden = InheritedUtil.instance().methodIsOverridden(method);
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		    if (!methodOverridden) {
			Added(method);
		    }
		}
		
	//	Compare fields
			
		tos.Init(base.fields, test.fields, eqmember);
			
		for (i = 0; i < tos.basedcl.size(); i++)
            Compare((XClassField)tos.basedcl.elementAt(i), 
                    (XClassField)tos.testdcl.elementAt(i));
		
		XClassField field = null;
		for (i = 0; i < tos.deleted.size(); i++) {
		    /*
		     * If the field is flagged as deleted, this means the field is
		     * in the signature file but does not appear in the test class.
		     * This is an incompatible change unless the deleted field is in fact
		     * inherited from a base class.
		     */
		    field = (XClassField)tos.deleted.elementAt(i);
		    boolean fieldInherited = false;
		    System.err.println("### Field may be deleted \"" + field.home.FullName() + "." + field.name + "\"");
		    try {
			fieldInherited = InheritedUtil.instance().fieldIsInherited(field);
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		    if (!fieldInherited) {
			Deleted(field);
		    }
		}
		
		for (i = 0; i < tos.added.size(); i++) {
		    /*
		     * If the field is flagged as added, this means the field is not
		     * in the signature file but appears in the test class.  This is
		     * an incompatible change unless the additional field is in fact
		     * inherited from a base class and overridden in this subclass under
		     * test.
		     */
		    field = (XClassField)tos.added.elementAt(i);
		    boolean fieldOverridden = false;
		    System.err.println("### Field may be added \"" + field.home.FullName() + "." + field.name + "\"");
		    try {
			fieldOverridden = InheritedUtil.instance().fieldIsOverridden(field);
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		    if (!fieldOverridden) {   
			Added(field);
		    }
		}
		
	//	Compare inner classes
			
		tos.Init(base.inners, test.inners, eqmember);
		
		for (i = 0; i < tos.basedcl.size(); i++)
            XCompare((XClass)tos.basedcl.elementAt(i), 
                     (XClass)tos.testdcl.elementAt(i));
		
		for (i = 0; i < tos.deleted.size(); i++)
            Deleted((XClass)tos.deleted.elementAt(i));
			
		for (i = 0; i < tos.added.size(); i++)
            Added((XClass)tos.added.elementAt(i));

    //  Exit stack frame

        stack[stackidx] = null;
        tos.Clear();

        tos = --stackidx < 0 ? null : stack[stackidx];
    }


    void Deleted (XClass x)
    {
        // to be overriden
    }
	

    void Added (XClass x)
    {
        // to be overriden
    }
	

//  Constructor
	
	
    void Compare (XClassConstructor base, XClassConstructor test)
    {
        // to be overriden
    }


    void Deleted (XClassConstructor x)
    {
        // to be overriden
    }


    void Added (XClassConstructor x)
    {
        // to be overriden
    }


//  Method
	
	
    void Compare (XClassMethod base, XClassMethod test)
    {
        // to be overriden
    }
	
	
    void Deleted (XClassMethod x)
    {
        // to be overriden
    }
	
	
    void Added (XClassMethod x)
    {
        // to be overriden
    }


//  Field
	
	
    void Compare (XClassField base, XClassField test)
    {
        // to be overriden
    }


    void Deleted (XClassField x)
    {
        // to be overriden
    }
	
	
    void Added (XClassField x)
    {
        // to be overriden
    }



    void Normalize (XClass xclass)
    {
        if (xclass.IsInterface())
        {
            // Every interface is impicitly abstract
            xclass.modifier |= XModifier.xabstract;

            // Every method declaration in the body of an interface is implicitly public & abstract
            for (int i = 0; i < xclass.methods.size(); i++)
                ((XClassMethod)xclass.methods.elementAt(i)).modifier |= 
                    (XModifier.xpublic | XModifier.xabstract);

            // Every field declaration in the body of an interface is implicitly public, 
            // static and final
            for (int i = 0; i < xclass.fields.size(); i++)
                ((XClassField)xclass.fields.elementAt(i)).modifier |= 
                    (XModifier.xpublic | XModifier.xstatic | XModifier.xfinal);
        }
    }


/*
//  Visibility of current (top stack) class pair (base & test)

    boolean Visible ()
    {
        return stackidx == -1 || layer.vis;
    }
*/
}
