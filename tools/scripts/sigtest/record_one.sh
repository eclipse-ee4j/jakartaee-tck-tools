#!/usr/bin/ksh
#
# Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v. 2.0, which is available at
# http://www.eclipse.org/legal/epl-2.0.
#
# This Source Code may also be made available under the following Secondary
# Licenses when the conditions for such availability set forth in the
# Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
# version 2 with the GNU Classpath Exception, which is available at
# https://www.gnu.org/software/classpath/license.html.
#
# SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
#

# This script shows how to use the sigtest utility to record a signature file
# by itself.  This script assumes it is being run from within the
#  TS_HOME/src/com/sun/ts/tests/signaturetest directory.
#
#  Must set the following below:
#     AS_HOME   - should point to GF v4.0
#     JAVA_HOME - should point to java se 7 (should work with se 6)
#     TS_HOME   - should be set 
#
# NOTE:  although this references TS_HOME and AS_HOME, this is intended to 
#        show how to record a sigfile WITHOUT any TCK dependencies.
#

AS_HOME=/vi/glassfish-4.0-b34-04_19_2012/glassfish
JAVA_HOME=/export/home/files1/j2ee/jdk7/jdk1.7.0

$JAVA_HOME/bin/java -cp "$TS_HOME/lib/sigtestdev.jar" \
          com.sun.tdk.signaturetest.Setup \
          -static \
          -classpath "${AS_HOME}/modules/bean-validator.jar:$JAVA_HOME/jre/lib/rt.jar" \
          -filename signature-repository/javax.validation.sig_1.1_se7 \
          -package javax.validation

echo "if successful, new sigfile recorded at:  signature-repository/javax.validation.sig_1.1_se7"


