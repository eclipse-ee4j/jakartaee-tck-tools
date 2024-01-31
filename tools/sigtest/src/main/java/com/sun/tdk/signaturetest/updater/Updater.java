/*
 * $Id$
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

package com.sun.tdk.signaturetest.updater;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.util.*;
import java.io.*;
import java.lang.reflect.Field;

import com.sun.tdk.signaturetest.model.AnnotationItem;

/**
 * @author Mikhail Ershov
 */
public class Updater extends DefaultHandler {

    private UpdateRecord ur;
    private LinkedList commands;
    private String lastData;
    private PrintWriter log;

    public boolean perform(String updFile, String fromFile, String toFile, PrintWriter log) {
        if (log != null) {
            this.log = log;
        }
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            SAXParser sp = spf.newSAXParser();
            sp.parse(updFile, this);
            return applyUpdate(fromFile, toFile);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void startDocument() throws SAXException {
        commands = new LinkedList();
    }


    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("update")) {
            ur = new UpdateRecord();
            fillUR(ur, attributes);
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (lastData == null) {
            lastData = new String(ch, start, length);
        } else {
            lastData += new String(ch, start, length);
        }
        if (lastData != null) {
            lastData = lastData.trim();
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        Command c = null;
        if (ur != null) {
            if (ur.atype.equalsIgnoreCase("removeclass")) {
                RemoveClass rc = new RemoveClass(log);
                rc.comments = ur.acomments;
                rc.id = ur.aid;
                rc.className = ur.aclassname;
                c = rc;
            } else if (ur.atype.equalsIgnoreCase("removepackage")) {
                RemovePackage rp = new RemovePackage(log);
                rp.comments = ur.acomments;
                rp.id = ur.aid;
                rp.packageName = ur.apackagename;
                c = rp;
            } else if (ur.atype.equalsIgnoreCase("addclass")) {
                AddClass ac = new AddClass(log);
                ac.comments = ur.acomments;
                ac.id = ur.aid;
                ac.className = ur.aclassname;
                ac.body = lastData;
                c = ac;
            } else if (ur.atype.equalsIgnoreCase("removemember")) {
                RemoveMember rm = new RemoveMember(log);
                rm.comments = ur.acomments;
                rm.id = ur.aid;
                rm.className = ur.aclassname;
                rm.memberName = ur.amember;
                c = rm;
            } else if (ur.atype.equalsIgnoreCase("addmember")) {
                AddMember am = new AddMember(log);
                am.comments = ur.acomments;
                am.id = ur.aid;
                am.className = ur.aclassname;
                am.memberName = ur.amember;
                c = am;
            } else if (ur.atype.equalsIgnoreCase("changemember")) {
                ChangeMember cm = new ChangeMember(log);
                cm.comments = ur.acomments;
                cm.id = ur.aid;
                cm.className = ur.aclassname;
                cm.memberName = ur.amember;
                cm.newMemberName = ur.anewmember;
                c = cm;
            }
            if (c == null) {
                throw new IllegalArgumentException("Unknown type \"" + ur.atype + "\" for update");
            }
            c.validate(); // IllegalArgumentException can be thrown
            commands.add(c);
        }
        ur = null;
        lastData = null;
    }

    private void fillUR(UpdateRecord ur, Attributes attributes) {
        Field[] fs = UpdateRecord.class.getDeclaredFields();
        try {
            for (int i = 0; i < fs.length; i++) {
                Field f = fs[i];
                String fName = f.getName();
                if (fName.startsWith("a")) {
                    f.set(ur, attributes.getValue(fName.substring(1)));
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void processCommands(Collection commands, Updater.SigList sl) {
        Iterator it = commands.iterator();
        while (it.hasNext()) {
            ((Command) (it.next())).perform(sl);
        }
    }

    private boolean applyUpdate(String from, String to) {
        try {
            // read src
            SigList sl = readInput(from);

            // transform
            processCommands(commands, sl);
            commands.clear();

            // remove some empty lines
            sl.pack();

            // write result
            writeOut(to, sl);

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void writeOut(String to, SigList sl) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(new FileOutputStream(to));
        Iterator it = sl.iterator();
        while (it.hasNext()) {
            pw.write((String) it.next() + '\n');
        }
        pw.close();
    }

    private SigList readInput(String from) throws IOException {
        LineNumberReader r = new LineNumberReader(new BufferedReader(new FileReader(from)));
        SigList sl = new SigList();
        String s;
        while ((s = r.readLine()) != null) {
            sl.add(s);
        }
        r.close();
        return sl;
    }

    class SigList extends ArrayList {
        private int startPos = -1;

        public boolean findClass(String className) {
            startPos = -1;
            int i = 0;
            while (i < size()) {
                String l = (String) get(i);
                if (l.startsWith("CLSS ") && l.endsWith(" " + className)) {
                    startPos = i;
                    return true;
                }
                i++;
            }
            return false;
        }

        public void removeCurrentClass() {
            if (startPos >= 0) {
                while (!"".equals(((String) get(startPos)).trim())) {
                    remove(startPos);
                }
            }
        }

        public void addText(String body) {
            StringTokenizer st = new StringTokenizer(body, "\n");
            add("");
            while (st.hasMoreTokens()) {
                add(st.nextToken().trim());
            }
            add("");
        }

        public boolean findPackageMember(String packageName) {
            startPos = -1;
            int i = 0;
            final String pSig = " " + packageName + ".";
            while (i < size()) {
                String l = (String) get(i);
                if (l.startsWith("CLSS ")) {
                    int x = l.indexOf('<');
                    int y = l.indexOf(pSig);
                    if (y > 0 && ((y < x) || (x == -1))) {
                        startPos = i;
                        return true;
                    }
                }
                i++;
            }
            return false;
        }

        public boolean removeMember(String memberName) {
            if (startPos >= 0) {
                for (int i = startPos; i < size(); i++) {
                    String l = ((String) get(i)).trim();
                    if (memberName.equals(l)) {
                        remove(i);
                        return true;
                    } else {
                        if ("".equals(l)) {
                            break;
                        }
                    }
                }
            }
            return false;
        }

        public boolean changeMember(String oldMember, String newMember) {
            if (startPos >= 0) {
                for (int i = startPos; i < size(); i++) {
                    String l = ((String) get(i)).trim();
                    if (oldMember.equals(l)) {
                        set(i, newMember);
                        return true;
                    } else {
                        if ("".equals(l)) {
                            break;
                        }
                    }
                }
            }
            return false;
        }

        public void pack() {
            boolean empty = false;
            for (int i = 0; i < size(); i++) {
                String l = (String) get(i);
                if ("".equals(l.trim())) {
                    if (empty) {
                        remove(i--);
                        continue;
                    } else {
                        empty = true;
                    }
                } else {
                    empty = false;
                }
            }
        }

        public boolean addMember(String memberName) {
            if (startPos >= 0) {
                for (int i = startPos + 1; i < size(); i++) {
                    String l = ((String) get(i)).trim();
                    if (!l.startsWith(AnnotationItem.ANNOTATION_PREFIX)) {
                        add(i, memberName);
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // data bean
    private class UpdateRecord {
        String atype;
        String aclassname;
        String aid;
        String acomments;
        String apackagename;
        String amember;
        String anewmember;
    }

}
