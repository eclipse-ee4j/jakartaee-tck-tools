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

/*
 *      This class contains the version information of the apiCheck. 
 */



package javasoft.sqe.apiCheck;


class Version
{
    static final String 
        BUILD     = "O",
        VERSION   = "1.0",
        MILESTONE = "alpha",
        SERIAL    = "36";


    static
    String Ident ()
    {

/*
        if (Main.args.getProperty("version") != null)
        {
            Package pkg = Package.getPackage("javasoft.sqe.apiCheck");           
            if (pkg != null)
                return pkg.getImplementationVersion();
        }
*/

        return VERSION + MILESTONE + BUILD + SERIAL;
    }
}
