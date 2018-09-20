/*
 * Copyright (c) 2002, 2018 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.ts.legacy;

public class QueueHandlerFactory {

    private static final String FACTORY_PROP    = "queue.handler.class";
    private static final String DEFAULT_FACTORY = "com.sun.ts.legacy.DefaultSpecHandler";

    private static final QueueHandlerFactory factory = new QueueHandlerFactory();

    private QueueHandlerFactory() {
    }

    public static QueueHandlerFactory instance() {
	return factory;
    }

    public QueueHandler getHandler() {
	String       handlerType = System.getProperty(FACTORY_PROP, DEFAULT_FACTORY);
	QueueHandler handler     = null;
	Class        clazz       = null;
	try {
	    clazz   = Class.forName(handlerType);
	    handler = (QueueHandler)clazz.newInstance();
	} catch (Exception e) {
	    System.err.println("Error creating specified handler \""
			       + handlerType + "\", returning the default"
			       + " handler type.");
	    try {
		clazz   = Class.forName(DEFAULT_FACTORY);
		handler = (QueueHandler)clazz.newInstance();
	    } catch (Exception ex) {
		System.err.println("Error creating default handler, exiting");
		ex.printStackTrace();
		System.exit(1);
	    }
	}
	System.out.println("$$$ Handler in use: " + handler.getClass().getName());
	return handler;
    }

}
