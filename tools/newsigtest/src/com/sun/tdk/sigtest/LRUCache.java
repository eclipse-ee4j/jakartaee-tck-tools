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

import java.util.*;


abstract class LRUCache 
{
    final int maxcache;
    int count, hit, miss;
    
    Map  map;
    Entry first;
    
    //Set replacements;
    //int repcount;
    
    
    LRUCache (int sz)
    {
        maxcache = sz;
        count    = 0;
        map   = new HashMap();
        first = null;
        
        //replacements = new HashSet();
        //repcount = 0;
    }
    
    
    void close ()
    {
        //replacements = null;
        //
        //if (hit != 0 && miss != 0)
        //    System.out.println("Cache hit:" + hit + " miss:" + miss + 
        //                       "  rate:" + hit*100/(hit+miss) + "%");     
        //                       
        //System.out.println("Cache replacements " + repcount);                                  
        
        first = null;
        map   = null;
        
    }
    
    
    abstract Object getMiss (Object key);

    
    Object get (Object key)
    {
        Object val;
        Entry  entry;
        
        if ((entry = (Entry)map.get(key)) == null) {
        //  miss
            if ((val = getMiss(key)) == null)
                return null;
                
            if (++count < maxcache) {
            //  cache expands
                entry = new Entry();
            }
            else {
            //  cache replacements
                entry = first.prev;
                remove(entry);
                map.remove(entry.key);
                
                //replacements.add(entry.key);
                //if (replacements.contains(key))
                //    repcount++;
            }
            
            entry.key = key;
            entry.val = val;
            map.put(key, entry);
            miss++;
        }
        else {
        //  hit
            val = entry.val;
            remove(entry);
            hit++;
        }
        
        add(entry);
        
        return val;
    }


    void add (Entry e)
    {
        if (first == null)
            e.next = e.prev = e;
        else {
            e.next = first;
            e.prev = first.prev;
            first.prev.next = e;
            first.prev      = e;
        }

        first = e;
    }


    void remove (Entry e)
    {
        e.prev.next = e.next;
        e.next.prev = e.prev;

        if (first == e)
            if ((first = e.next) == e)
                first = null;
    }

    
    static class Entry
    {
        Entry next, prev;
        Object key;
        Object val;
    }
    
}


