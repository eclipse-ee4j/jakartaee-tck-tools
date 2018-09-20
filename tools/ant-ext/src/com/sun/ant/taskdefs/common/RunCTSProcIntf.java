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

package com.sun.ant.taskdefs.common;

import java.util.Properties;
import org.apache.tools.ant.Project;

public interface RunCTSProcIntf {

    /**
     * Post and Pre processor interface.  This interface should be implemented by all
     * processors that will be plugged into the CTS Ant task that runs the CTS tests.
     * The idea here is to add two hook points into the Ant task to allow users to
     * run pre and post processing code to configure and perhaps clean up after a
     * certain test area is run.  These processing classes are specified using the
     * preprocs and postprocs attributes of the CTS ant task.  These props are comma
     * delimited lists of classes that implement the RunCTSProcIntf.  Please note
     * these classes must have a no-arg constructor since instances are created using
     * the reflection API.
     *
     * A preprocessor and post processor example can be seen at RunCTSPreProcessor
     * and RunCTSPostProcessor, respectively.
     *
     * @param currentTestDir The current test are being executed
     * @param props A set of props that contain specific data needed by the processor
     * @param project The current ant project
     * @return boolean Returns true if you want to continue executing tests areas.  Returns
     *                 false if you want the ant task to stop processing this test area.
     * @throws Exception If any unexpected error is encountered running the processor.
     *                   It is better to catch all exceptions and use the boolean return
     *                   value to indicate that the current test directory should no
     *                   longer be processed since an error occurred.
     */
    public boolean execute(String currentTestDir, Project project, Properties props) throws Exception;

}
