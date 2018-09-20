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

package com.sun.tdk.sigtest.api;

import java.util.*;


public abstract class API
{

//  Gets class iterator (optional)

    public ClassIterator getClassIterator ()    {return null;}

//  Gets class by its fully qulified name (random-access method) (optinal)

    public abstract XClass getXClass (String fqn);
    
//  Gets next class (sequental-access method) (optional)

    public abstract XClass getXClass ();
    public abstract void   rewind ();
    

//  api elements factory
    
    public XClass       newXClass ()            {return new XClass();}
    
    public XClassCtor   newXClassCtor ()        {return new XClassCtor();}

    public XClassMethod newXClassMethod ()      {return new XClassMethod();}
    
    public XClassField  newXClassField ()       {return new XClassField();}
    
    
//  Properties implementation


    HashMap/*String*/ props;


    public void setProp (String key, Object value)
    {
        if (props == null)
            props = new HashMap();
        props.put(key, value);
    }
    
    
    public Object getProp (String key, Object def)
    {
        if (props == null)
            return def;
        else {
            Object x = props.get(key);
            return x == null ? def : x;
        }
    }
    
    
    public Object getProp (String key)
    {
        if (props == null)
            return null;
        else
            return props.get(key);
    }
    
    
    public void addProps (API api)
    {
        if (api.props != null) {
            if (props == null)
                props = new HashMap();
            props.putAll(api.props);
        }
    }
    
    
    public Map getProps ()
    {
        return props;
    }

    
//  Misc    


    public abstract void close ();
    
}
