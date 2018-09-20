/*
 * Copyright (c) 2000, 2018 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.cts.util;

/**
 * This exception is thrown when a user specifies a command line flag that
 * does not conform to valid command line flag rules.
 */
public class InvalidFlagException extends Exception {

    /**
     * Constructs an InvalidFlagException with no message.
     */
    public InvalidFlagException() {
	super();
    }

    /**
     * Constructs an InvalidFlagException with the specified message.
     */
    public InvalidFlagException(String message) {
	super(message);
    }
}
