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

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.sigfile.FileManager;
import com.sun.tdk.signaturetest.sigfile.Reader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Vector;

/** This is class for reading signature or API signature file **/
final class ClassSignatureFromSigtests extends ClassSignatureReader implements SignatureConstants {
    private Vector definitions;
    private Reader reader;
    private DefinitionFormat converter;

    /** Creates new ClassSignatureReader for given URL
     * @param fileURL given URL which contains signature file **/
    public ClassSignatureFromSigtests(Reader reader, URL url) throws IOException {
        super();
        this.reader = reader;
        reader.readSignatureFile(url);
        /*
        definitions = new Vector();
            if (currentLine.startsWith("#Version"))
                javaVersion = currentLine.substring("#Version ".length());
            if (currentLine.startsWith("#Throws clause not tracked."))
                isThrowsTracked = false;
         */
    }

    @Override
    String getJavaVersion() {
        return reader.getApiVersion();
    }

    /** set the definition converter
     *  @param converter given DefinitionFormat. **/
    @Override
    public void setDefinitionConverter(DefinitionFormat converter) {
        this.converter = converter;
    }

    /** reads definition of the class from signature file and returns
     *  TableOfClass. This TableOfClass contains definitions of the class
     *  members with the short name and can used for SignatureTest only. **/
    @Override
    public TableOfClass nextClass() throws IOException {
        ClassDescription descr = reader.readNextClass();
        if (descr == null) {
            return null;
        }
        TableOfClass retClass = new TableOfClass(descr, converter);	
        /*
        definitions = new Vector();
        while (((currentLine = in.readLine()) != null) && 
	       (!currentLine.startsWith(CLASS))) {
	    definitions.addElement(currentLine);
	}
	retClass.createMembers(definitions.elements());
         */
        return retClass;
    }

    /** reads definition of the class from signature file and returns
     *  TableOfClass for APIChangesTest with definitions of the class members.**/
    @Override
    public TableOfClass nextAPIClass() throws IOException {
        /*
	if ((in == null) || (currentLine == null))
	    return null;
	TableOfClass retClass = new TableOfClass(currentLine, converter);	
	definitions = new Vector();
	while (((currentLine = in.readLine()) != null) && 
	       (!currentLine.startsWith(CLASS))) {
	    definitions.addElement(currentLine);
	}
	retClass.createMembers(definitions.elements());
	return retClass;
         */
        return nextClass();
    }    
}
	    
