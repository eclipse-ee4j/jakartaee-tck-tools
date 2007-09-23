/*
 * Copyright 1998-1999 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package org.netbeans.apitest;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/** This class represents table which can store Vector of entries
 *  for each String  key **/
final class ClassCollection {
    private Hashtable definitions;

    /** creates empty table **/
    public ClassCollection() {
        this.definitions = new Hashtable();
    }

    /** Adds new value to the Vector which mapped by key 
     *  @param entry this MemberEntry includes key and entry 
     *  which will be included **/
    public void addElement(MemberEntry entry) {
        addElement(entry.getKey(), entry.getEntry());
    }
    
    /** Adds new value to the Vector which mapped by key 
     *  @param key the key 
     *  @param def entry which will be included **/
    public void addElement(String key, Object def) {
        Vector h = (Vector)definitions.get(key);
        if (h == null) {
            h = new Vector();
            this.definitions.put(key, h);
        }
        h.addElement(def);
    }

    /** Adds unique new value to the Vector which mapped by key 
     *  If the entry is contained, than new empty will not be added  
     *  @param key the key 
     *  @param def entry which will be included **/
    public void addUniqueElement(String key, Object def) {
        Vector h = (Vector)definitions.get(key);
        if (h == null){
            h = new Vector();
            this.definitions.put(key, h);
        }
        if (!h.contains(def))
            h.addElement(def);
    }

    /** Returns enumeration of the keys **/
    public Enumeration keys() {
        return definitions.keys();
    }

    /** Returns Vectors of the entries which mapped for given key 
     *  @param key the key **/
    public Vector get(String key) {
        return (Vector)definitions.get(key);
    }

    /** put entry for given key. All entries which mapped by this key
     *  are removed 
     *  @param key the key 
     *  @param def the new entry which will be included instead all 
     *  previous values **/
    public void put(String key, Object def) {
        Vector h = new Vector();
        h.addElement(def);
        this.definitions.put(key, h);
    }

    /** put the Vector of the entries for given key. All entries which 
     *  mapped by this key are removed 
     *  @param key the key 
     *  @param def the new entry which will be included instead all 
     *  previous values **/
    public void putVector(String key, Vector member) {
        definitions.put(key, member);
    }

    /** removes entries for given key 
     *  @param key the key **/
    public void remove(String key) {
        definitions.remove(key);
    }

    /** removes entries for all keys **/
    public void clear() {
        definitions.clear();
    }
}
