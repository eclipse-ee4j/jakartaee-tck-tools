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
#	This script was designed to run all the test areas for CTS 1.4 
#       This script can be used on the following platforms. 
#
#	Solaris
#	Linux
#	Windows  (Must have MKS Tool Kit 7.2 and the script must be run in a ksh term)
#
#	Author: Doug Donahue
#	Last updated: 01/22/04
#
# TODO: 1. Add code to start the RMI/IIOP server before we run RMI/IIOP tests
#          and stop the server after the tests are run.
#       2. Port this script to a custom Ant target.
#

#
# HOST: The host that you are running on
# OS: The Operating System (linux, solaris)
# DB: The Database you are using (pointbase, oracle, sybase, mssql, db2)
# BUILD: The build you are testing
#
HOSTNAME=`hostname`
OS=linux
DB=pointbase
BUILD=cts14_b52_s1as8

#
# Set the Report and Work Directories
#
# NOTE: Make sure that you set the "if.existing.work.report.dirs" to append in the
#       ${TS_HOME}/bin/ts.jte file.
#
REPORT_DIR=/results/JTReport/${BUILD}/${OS}/${DB}
WORK_DIR=/results/JTWork/${BUILD}/${OS}/${DB}

#
# TS_HOME: Set to CTS installation 
# ANT_HOME: Set to Ant installation
# JAVA_HOME: Set to J2SE installation
# J2EE_HOME: Set to RI/PE installation
#
TS_HOME=/ts/j2eetck
JAVA_HOME=/jdk
J2EE_HOME=/usr/sun/SUNWappserver
export TS_HOME JAVA_HOME J2EE_HOME

##set ant home here
#ANT_HOME=
#export ANT_HOME

#
# Add the test directories that you want to test in between the ""
# 
# If running rmiiiop make sure to start the rmiiiop stand alone server.
# If running jaxr make sure the registry server is started.
#
#	Example:
#		TESTS="appclient assembly jdbc jms ejb/ee/bb"
#
#	Valid Test areas:
#		appclient 		assembly 	compat12 
#		compat13 		connector 	ejb/ee/bb 
#		ejb/ee/deploy 		ejb/ee/pm 	ejb/ee/sec	
#		ejb/ee/timer 		ejb/ee/tx 	ejb/ee/webservices
#		integration 		interop 	j2eetools 
#		jacc 			javamail 	jaxp 
#		jaxr 			jaxrpc 		jdbc
#		jms 			jmx 		jsp
#		jta 			rmiiiop 	saaj 
#		samples 		servlet 	signaturetest
#		webservices 		xa
#
#
TESTS="samples"

#----------------------------DO NOT EDIT BELOW THIS LINE---------------------------#

cd ${TS_HOME}/src/com/sun/ts/tests

for TEST_DIR in ${TESTS}
  do
   ${J2EE_HOME}/bin/asadmin stop-domain
   ${J2EE_HOME}/bin/asadmin start-domain
   ${J2EE_HOME}/bin/asadmin version --user admin --password adminadmin
   STATUS=$?

   RESTART=0
   if [ ${STATUS} -ne 0 ]
    then
       while [ ${RESTART} -ne 3 ]
           do
            while [ $? -ne 0 ]
                do
                 ${J2EE_HOME}/bin/asadmin start-domain
        
	         if [ $? -eq 0 ]
                  then
                     RESTART=0
                     return
                  else
                     RESTART=`expr ${RESTART} + 1`
                     echo "Trying to restart appserver ${RESTART}" 
                 fi
            done
       done
   fi

   if [ ${RESTART} -ne 0 ]
    then
       echo "Failed to start Appserver"
       exit 1
   fi


   cd ${TS_HOME}/src/com/sun/ts/tests/${TEST_DIR}

#
# Remap the following EJB test directory names so the EJB results
# are broken up into separate top-level results directories.  This
# is historic since we treat each EJB area as a separate test area
# specifically within the exclude list.
#
	case ${TEST_DIR} in
		"ejb/ee/bb") TEST_DIR=ejb_bb;;
		"ejb/ee/deploy") TEST_DIR=ejb_deploy;;
		"ejb/ee/pm") TEST_DIR=ejb_pm;;
		"ejb/ee/sec") TEST_DIR=ejb_sec;;
		"ejb/ee/timer") TEST_DIR=ejb_timer;;
		"ejb/ee/tx") TEST_DIR=ejb_tx;;
		"ejb/ee/webservices") TEST_DIR=ejb_webservices;;
	esac		

#
# Enable jacc logging if testing jacc
#

   if [ $TEST_DIR = jacc ]
    then
       ${ANT_HOME}/bin/ant enable.jacc
   fi

   ${ANT_HOME}/bin/ant runclient -Dreport.dir="${REPORT_DIR}/${TEST_DIR}" -Dwork.dir="${WORK_DIR}/${TEST_DIR}"

#
# Disable jacc logging when finished testing jacc
#
   if [ $TEST_DIR = jacc ]
    then
       ${ANT_HOME}/bin/ant disable.jacc
   fi

done
