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
import java.util.*;


class APIBuffer extends API implements ClassProcessor
{
    API api;
    List        all;
    Iterator    iter;
    AbstractMap map;
    int outercount, innercount;


    APIBuffer (API a, ClassFilter cf)
    {
        all  = new ArrayList();
        iter = null;
        map  = new HashMap();
        
        api = a;
        
    //  Read and store in the internal buffer all *outer* classes.
    //  Note that inner classes will not be stored.
    
        ClassIterator it = api.getClassIterator();
        if (it != null) {
            it.iterate(this, cf);
        }
        else {
            for (XClass xclass; (xclass = api.getXClass()) != null;) 
                if (cf == null || cf.inPath(xclass.getFullName()))
                    storeClass(xclass);
        }
        
        addProps(api);
    }
    
    
    //  Implementation of API interface
    
    public void close ()
    {
        if (api != null) {
            api.close();
            api = null;
        }
        
        iter = null;
        map  = null;
        all  = null;
    }
    
    
    void sort ()
    {
        Comparator c = new Comparator() {
          public int compare (Object o1, Object o2)
          {
            return (((XClass)o1).getFullName()).compareTo(
                   (((XClass)o2).getFullName()));
          }
        };
    
        Collections.sort(all, c);
        
        for (Iterator it = all.iterator(); it.hasNext();) 
            sortClass((XClass)it.next());
    }
    
    
    void sortClass (XClass xclass)
    {
        if (xclass.ctors != null)
            Collections.sort(xclass.ctors);
            
        if (xclass.methods != null)
            Collections.sort(xclass.methods);
            
        if (xclass.fields != null)
            Collections.sort(xclass.fields);
            
        if (xclass.inners != null) {
            Collections.sort(xclass.inners);
            for (Iterator it = xclass.inners.iterator(); it.hasNext();) 
                    sortClass((XClass)it.next());
        }
    }
    
    
    //  Implementation of the ClassProcessor interface
    
    public void process (String fqn, ClassData cd)
    {
        storeClass(api.getXClass(fqn));
    }
    
    
    void storeClass (XClass xclass)
    {
        if (xclass != null) {
            all.add(xclass);
            mapClass(xclass);
        }
    }
    
    
    void mapClass (XClass xclass)
    {
        map.put(xclass.getFullName(), xclass);
        
        if (xclass.inners != null)
            for (Iterator it = xclass.inners.iterator(); it.hasNext();) 
                mapClass((XClass)it.next());
                
        if (xclass == null)
            ++outercount;
        else
            ++innercount;
    }


    //  Implementation of API interface
    
    public ClassIterator getClassIterator ()
    {
        return new ClassIterator () {
            public void iterate (ClassProcessor cp, ClassFilter cf) 
            {
                for (Iterator it = all.iterator(); it.hasNext();) {
                    String fqn = ((XClass)it.next()).getFullName();
                    if (cf == null || cf.inPath(fqn))
                        cp.process(fqn, null);
                }
            }
        };
    }
  

    //  Implementation of API interface
    //  Random-access (by fully qulified name) method 
    
    public XClass getXClass (String fqn)
    {
        XClass xclass;
        
        if ((xclass = (XClass)map.get(fqn)) == null)  {
            try {
                if ((xclass = api.getXClass(fqn)) != null) 
                    mapClass(xclass);
            }
            catch (UnsupportedOperationException e) {
            }
        }
        
        return xclass;
    }


    //  Implementation of API interface
    //  Sequental-access method
    
    public XClass getXClass ()
    {
        if (iter == null)
            iter = all.iterator();
    
        if (iter.hasNext())
            return (XClass)iter.next();
        else
            return null;
    }
    
    
    //  Implementation of API interface
    
    public void rewind ()
    {
        iter = all.iterator();
    }
}
