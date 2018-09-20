#!/bin/sh
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

echo "JAVA_HOME = $JAVA_HOME"
if [ -z "$JAVA_HOME" ]; then
  echo "Set JAVA_HOME to an SE 1.4 or higher version of java"
  exit
fi

JAVA=${JAVA_HOME}/bin/java

VERSION=`${JAVA} -version 2>&1 | grep 1.4.`
VERSION2=`${JAVA} -version 2>&1 | grep 1.5.`
VERSION3=`${JAVA} -version 2>&1 | grep 1.6.`
VERSION4=`${JAVA} -version 2>&1 | grep 1.7.`
VERSION8=`${JAVA} -version 2>&1 | grep 1.8.`
VERSION9=`${JAVA} -version 2>&1 | grep 1.9.`
#echo "VERSION=${VERSION}"
#echo "VERSION2=${VERSION2}"

if [ -z "${VERSION}" -a -z "${VERSION2}" -a -z "${VERSION3}" -a -z "${VERSION4}" -a -z "${VERSION8}" -a -z "${VERSION9}" ]; then
  echo "Please set JAVA_HOME to an SE 1.4 or higher version of java"
  exit
fi

${JAVA} -classpath ../classes com.sun.cts.util.Trans $@
