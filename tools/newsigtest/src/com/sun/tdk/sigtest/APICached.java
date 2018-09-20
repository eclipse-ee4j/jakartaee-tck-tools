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


class APICached extends API
{

    class MyCache extends LRUCache
    {
        MyCache ()
        {
            super(128);
        }
    
        Object getMiss (Object key) 
        {
            return api.getXClass((String)key);
        }
    }
    
    
    API api;
    LRUCache cache;


    APICached (API a)
    {
        api   = a;
        addProps(api);
        cache = new MyCache();
    }
    
    
    //  Implementation of API interface
    
    public void close ()
    {
        cache.close();
        cache = null;
        api.close();
        api   = null;
    }


    //  Implementation of API interface
    //  Random-access (by fully qulified name) method 
    //
    public XClass getXClass (String fqn)
    {
        return (XClass)cache.get(fqn);
    }


    //  Implementation of API interface
    //  Sequental-access method
    //
    public XClass getXClass ()
    {
        return api.getXClass();
    }
    
    
    //  Implementation of API interface
    //  Sequental-access method
    //
    public void rewind ()
    {
        api.rewind();
    }
    
    
    //  Implementation of API interface
    //
    public ClassIterator getClassIterator ()
    {
        return api.getClassIterator();
    }
    
}
