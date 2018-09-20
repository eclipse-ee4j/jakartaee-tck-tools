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

package com.sun.ts.assertion.coverage;

import java.io.FileNotFoundException;
import java.io.File;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import javax.xml.transform.sax.SAXSource;

class MyResolver implements EntityResolver {

	private File localDTDPath;

	MyResolver(String localPath) throws FileNotFoundException {
		this.localDTDPath = new File(localPath);
		if (!localDTDPath.isDirectory()) {
			throw new FileNotFoundException(localPath);
		}
	}

	public InputSource resolveEntity(String publicId, String systemId) {
		InputSource result = null;
		String fileName = systemId.substring(systemId.lastIndexOf("/") + 1);
		if (fileName.endsWith(".dtd")) {
			String path = localDTDPath.getPath() + File.separator + fileName;
			result = new InputSource(path);
		} else {
			result = new InputSource(fileName);
		}
		System.err.println("Entity Resolver returning \""
				+ result.getSystemId() + "\"");
		return result;
	}
}
