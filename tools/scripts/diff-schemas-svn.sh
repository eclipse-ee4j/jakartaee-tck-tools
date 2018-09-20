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

update () {
  SOURCE_FILE=$1
  TARGET_FILE=$2
  echo "Copying $SOURCE_FILE  to  $TARGET_FILE"
  \cp $SOURCE_FILE $TARGET_FILE
}

USAGE="usage: ${0} ts.home java.ee.home"

if [ $# -ne 2 ]; then
    echo $USAGE
    exit 1
fi

TS_HOME=${1}
JAVAEE_HOME=${2}

cd $JAVAEE_HOME/lib/schemas


echo "****** datatypes.dtd"
VAL=`diff datatypes.dtd $TS_HOME/lib/dtds`
if [ -n "${VAL}" ]
then
  update $JAVAEE_HOME/lib/schemas/datatypes.dtd $TS_HOME/lib/dtds/datatypes.dtd
fi


echo "****** XMLSchema.dtd"
VAL=`diff XMLSchema.dtd $TS_HOME/lib/dtds`
if [ -n "${VAL}" ]
then
  update $JAVAEE_HOME/lib/schemas/XMLSchema.dtd $TS_HOME/lib/dtds/XMLSchema.dtd
fi


for file in `ls *.xsd`
do
  echo "****** $file"
  VAL=`diff $file $TS_HOME/lib/schemas`
  if [ -n "${VAL}" ]
  then
    update $JAVAEE_HOME/lib/schemas/${file} $TS_HOME/lib/schemas/${file}
  fi  
done


cd $JAVAEE_HOME/lib/dtds

for file in `ls *.dtd`
do
  echo "****** $file"
  VAL=`diff $file $TS_HOME/lib/dtds`
  if [ -n "${VAL}" ]
  then
    update $JAVAEE_HOME/lib/dtds/${file} $TS_HOME/lib/dtds/${file}
  fi  
done

