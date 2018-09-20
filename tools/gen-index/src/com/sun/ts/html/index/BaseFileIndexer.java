/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ts.html.index;

import java.io.*;
import java.util.*;
import org.apache.ecs.*;
import org.apache.ecs.html.*;
import org.apache.ecs.xhtml.*;
import com.sun.cts.common.DateUtil;

public class BaseFileIndexer {

    public BaseFileIndexer() {
    }

    protected void writeFile(File indexFile, Document doc) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(indexFile);
            fw.write(doc.toString());
        } catch (IOException ioe) {
            System.err.println("Error writing file \"" + indexFile + "\"");
        } finally {
            try { fw.close(); } catch (IOException e) {}
        }
    }

    protected void addRelativePath(java.util.Map map, String path, String tech) {
	if (map.containsKey(tech)) {
	    List l = (List)(map.get(tech));
	    l.add(path);
	} else {
	    List l = new ArrayList();
	    l.add(path);
	    map.put(tech, l);
	}
    }

    protected String formatTech(String tech) {
	String techName = null;
	String cleanedVersion = null;
	int index = tech.indexOf("_");
	if (index != -1) {
	    techName = tech.substring(0, index);
	    String version = tech.substring(index + 1);
	    cleanedVersion = version.replace('_','.');
	} else { // default if expected TECH_vid1_vid2_vid3 format not used
	    techName = tech;
	    cleanedVersion = "0.0.0";
	}
	return techName.toUpperCase() + " " + cleanedVersion;
    }
 
    protected String getAssertionDesc(String url) {
	if (url.indexOf("api") != -1) {
	    return "API Assertion List";
	} else if (url.indexOf("spec") != -1) {
	    return "SPEC Assertion List";
	} else {
	    return url;
	}
    }

   protected void timeStamp(Document doc) {
	doc.appendBody(new BR());
	String date = "Page last updated on " + DateUtil.instance().getFullDate();
	doc.appendBody(new Font().setSize("+0").addElement(date));
    }

}
