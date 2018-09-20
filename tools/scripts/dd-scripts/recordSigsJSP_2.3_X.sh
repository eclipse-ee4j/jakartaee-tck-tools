#! /bin/sh
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

#
# TestSuite Home 
#
TS_HOME=/files/Dougd/workspace/jsp

#
# Ant Home
#
##set ant home here
#ANT_HOME=
#export ANT_HOME

#
# Glassfish Home
#
GF_HOME=/files/Dougd/4.0/glassfish3

#
# Deliverable we recording
#
deliverabledir=jsp
export deliverabledir

#
# Library root
#
LIB_DIR=${GF_HOME}/glassfish/modules

#
# Technology Specific Jars needed for the recording.
#
RECORD_JARS=${LIB_DIR}/javax.servlet-api.jar:/files/Dougd/scripts/Jars/javax.servlet.jsp-api-2.3.1-SNAPSHOT.jar:/files/Dougd/scripts/Jars/javax.servlet.jsp-2.3.1-SNAPSHOT.jar:${LIB_DIR}/javax.servlet.jsp.jstl-api.jar:/files/Dougd/scripts/Jars/javax.el-3.0-b04-SNAPSHOT.jar

#
# Java SE6 & SE7 RunTime Jars
#
SE6_RUNTIME=/files2/util/jdk1.6.0_24
SE7_RUNTIME=/files2/util/jdk1.7.0

#### Start Recording Steps ####

cd ${TS_HOME}/src/com/sun/ts/tests/signaturetest/signature-repository

for SE_VERSION in ${SE6_RUNTIME} ${SE7_RUNTIME}
  do
    echo "Start recording for ${deliverabledir} with ${SE_VERSION}"

    JAVA_HOME=${SE_VERSION}
    export JAVA_HOME

    case ${JAVA_HOME} in
     ${SE6_RUNTIME})
        MAP_VER=se6;;
     ${SE7_RUNTIME})
        MAP_VER=se7;;
    esac

    ${ANT_HOME}/bin/ant -find record-build.xml -Dsig.source=${RECORD_JARS}:${SE_VERSION}/jre/lib/rt.jar -Dmap.file=$TS_HOME/install/${deliverabledir}/bin/sig-test_${MAP_VER}.map -Drecorder.type=sigtest record.sig.batch

    echo "Finished recording for ${deliverabledir} with ${SE_VERSION}"
    echo " "
done

#### Finished Recording Steps ####
