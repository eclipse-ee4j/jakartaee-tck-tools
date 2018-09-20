#!/usr/bin/ksh -x
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
# OVERVIEW:  
# ---------
#
# This is an example of a shell script that shows how to record all 
# new sigfiles for the entries in sig-test_se7.map. 
#
# This should not be distributed to customers.
#
# This SHOULD be modified to match the variable sigTestClasspath (in ts.jte)
# as well as to match the correct jdk version (via sig-test.map file).
# This makes use of the TCK utilities when recording.
#
# -Dsig.source=<sigTestClasspath var from ts.jte>
#              Note that the sig.source will need to refer to *all* classes which 
#              are under test as well as classes referred to or used by the 
#              classes under test.  (By "under test" we mean classes you are 
#              recording signatures of.)
# -Dmap.file= point at desired sigtest map file in TS_HOME/bin
# -Drecorder.type=sigtest (this will ALWAYS be "sigtest")
#
#
# NOTE:  This makes use of CTS specific build files and classes so is NOT 
#        intended to be used outside of a TCK environment.
#


#
# IMPORTANT!!!!   change AS_HOME
#
TS_HOME=/export/home/files2/projects/svn-spider/trunk
export TS_HOME

AS_HOME=/vi/glassfish-4.0-b34-04_19_2012/glassfish
export AS_HOME

##set ant home here
#ANT_HOME=
#export ANT_HOME

GFLIB=${AS_HOME}/modules
export GFLIB

FELIX_CLASSES=${AS_HOME}/osgi/felix/bin/felix.jar:${GFLIB}/org.apache.felix.configadmin.jar

CLASSES=${GFLIB}/endorsed/javax.annotation.jar:${GFLIB}/endorsed/jaxb-api-osgi.jar:${GFLIB}/endorsed/webservices-api-osgi.jar:${GFLIB}/javax.enterprise.deploy.jar:${GFLIB}/bean-validator.jar:${GFLIB}/weld-osgi-bundle.jar:${GFLIB}/jersey-core.jar:${GFLIB}/jaxr-api-osgi.jar:${GFLIB}/jaxrpc-api-osgi.jar:${GFLIB}/endorsed/javax.annotation.jar:${GFLIB}/jaxrpc-api-osgi.jar:${GFLIB}/javax.ejb.jar:${GFLIB}/javax.el-api.jar:${GFLIB}/javax.el.jar:${GFLIB}/javax.enterprise.deploy.jar:${GFLIB}/javax.faces.jar:${GFLIB}/javax.inject.jar:${GFLIB}/javax.jms.jar:${GFLIB}/javax.mail.jar:${GFLIB}/javax.management.j2ee.jar:${GFLIB}/javax.persistence.jar:${GFLIB}/javax.resource.jar:${GFLIB}/javax.security.auth.message.jar:${GFLIB}/javax.security.jacc.jar:${GFLIB}/javax.servlet-api.jar:${GFLIB}/javax.servlet.jsp-api.jar:${GFLIB}/javax.servlet.jsp.jar:${GFLIB}/javax.servlet.jsp.jstl-api.jar:${GFLIB}/javax.servlet.jsp.jstl.jar:${GFLIB}/javax.transaction.jar:${FELIX_CLASSES}:${JAVA_HOME}/jre/lib/rt.jar:${JAVA_HOME}/jre/lib/jce.jar:${AS_HOME}/lib/javaee.jar


#
#  EXAMPLE USAGE
#

${ANT_HOME}/bin/ant -file record-build.xml \
-Dsig.source=${CLASSES} \
-Dmap.file=/export/home/files2/projects/svn-spider/trunk/install/j2ee/bin/sig-test_se7.map \
-Drecorder.type=sigtest record.sig.batch  |tee output.txt

