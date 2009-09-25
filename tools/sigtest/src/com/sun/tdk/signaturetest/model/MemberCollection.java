/*
 * $Id: MemberCollection.java 4516 2008-03-17 18:48:27Z eg216457 $
 *
 * Copyright 1996-2009 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tdk.signaturetest.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Maxim Sokolnikov
 * @author Roman Makarchuk
 */
public class MemberCollection {

    private Set members;

    public MemberCollection() {
        members = new HashSet();
    }

    /**
     * Add the given <code>member</code> to this member collection.
     * If a member with the the same "key" already exists in the collection,
     * the new member will override it.
     *
     * @param member New member to add to the collection.
     */

    public void addMember(MemberDescription member) {
        if (MemberType.CLASS==member.getMemberType())
            throw new IllegalArgumentException("Instances of ClassDescription are not allowed here!");
        members.add(member);
    }
    
    public void updateMember(MemberDescription member) {        
        if (MemberType.CLASS==member.getMemberType())
            throw new IllegalArgumentException("Instances of ClassDescription are not allowed here!");
        members.remove(member);
        members.add(member);
    }


    public Collection getAllMembers() {
        return members;
    }

    public boolean contains(MemberDescription newMember) {
        return members.contains(newMember);
    }

    public Iterator iterator() {
        return members.iterator();
    }

    public void changeMember(MemberDescription oldMember, MemberDescription newMember) {
        if (MemberType.CLASS==newMember.getMemberType())
            throw new IllegalArgumentException("Instances of ClassDescription are not allowed here!");
        if (!contains(oldMember))
            throw new IllegalArgumentException("Member " + oldMember + " not found!");
        members.remove(oldMember);
        members.add(newMember);
    }


    // TODO (Roman Makarchuk) weak performance!
    public MemberDescription find(MemberDescription mr) {
        for (Iterator e = members.iterator(); e.hasNext();) {
            MemberDescription member = (MemberDescription) e.next();
            if (member.equals(mr))
                return member;
        }
        return null;
    }

    public MemberDescription findSimilar(MemberDescription mr) {
        for (Iterator e = members.iterator(); e.hasNext();) {
            MemberDescription member = (MemberDescription) e.next();
            if (member.getType().equals(mr.getType()) && member.getName().equals(mr.getName()))
                return member;
        }
        return null;
    }


    public int getMembersCount(MemberType memberType, String fqname) {
        int count = 0;
        for (Iterator e = members.iterator(); e.hasNext();) {
            MemberDescription member = (MemberDescription) e.next();
            if ((memberType == null || memberType==member.getMemberType()) &&
                    fqname.equals(member.getQualifiedName()))
                count++;
        }
        return count;
    }

}

