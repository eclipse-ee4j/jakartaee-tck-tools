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
# README FILE FOR CTS EXCLUDELIST TESTING TOOL.
#

#
# **** IMPORTANT NOTE For CTS 1.4.1!!!!! ****
#
#    Before you can modify the CTS excludelist or run this tool against the 
# excludelist you must first check it out of source control. 
#

Edit/Layout
-----------

1. Edit the exclude list. Here is an example of the format of an existing 
   exclude list. (CTS 5)

	##################
	# EJB excludes
	##################

	#
	# Bug id: 6253992
	#
	com/sun/ts/tests/ejb/ee/bb/session/stateless/reentranttest/Client.java#test1
    com/sun/ts/tests/ejb/ee/bb/session/stateless/reentranttest/Client.java#test2

	#
	# Bug id: 6223549
	#           
	com/sun/ts/tests/ejb/ee/webservices/allowedmethodstest/bm/Client.java#wsbmAllowedMethodsTest1


  	##################
	# JDBC excludes
	##################    
	
	etc.....
  
2. Here is a template of what the format of the exclude list should look like.

	##############
	# TEST AREA
	##############	
	 -- Blank Line --
	#
	# Bug id: Bug Number
	#
	Test Name
	Test Name
	 -- Blank Line --
	#
	# Bug id: Bug Number
	#
    -- Blank Line --
    -- Blank Line --
	##############
	# NEXT TEST AREA
	##############

3. If all else fails look at what is already in a previously released exclude 
   list. 


Test/Format
-----------

1. Test the exclude list.
   
   Example using CTS6
   ------------------
   
	#setevn ETOOL_HOME /etool
	
    # cd ./etool/bin

	# ant test_list -Dexcludelist=/ts/javaeetck/bin/ts.jtx -Dversion=6

	buildfile: build.xml

	etool.home:

	test_list:
     	[java] Created a backup file called: /ts/javaeetck/bin/ts.jtx.orig
     	[java] Removed all blank spaces at beginning and end of each line from file /ts/javaeetck/bin/ts.jtx
     	[java] 
     	[java] Total PASSED:  6
     	[java] Total FAILED:  0
     	[java]               --------
     	[java] Total RUN:     6

	BUILD SUCCESSFUL
	Total time: 1 second

		

2. Next create the xml version of the exclude list.(optional)
	
	# ant create_xml -Dexcludelist=/ts/javaeetck/bin/ts.jtx -Dversion=6

	Buildfile: build.xml

	etool.home:

	create_xml:
     	[java] *** Applying transformation to "/ts/javaeetck/bin/ts.jtx"
     	[java] *** Creating "/ts/javaeetck/bin/ts.jtx.xml" file



To generate a master list
-------------------------
- Grab a released bundle and unzip it.
- set the harness.executeMode to 2 in ts.jte (if set to 5 same as list.tests)
- cd $TS_HOME/src/com/sun/ts/tests 
- ant runclient | grep "Beginning Test:" | cut -d " " -f 5 > master.list.file

Using list.tests target (faster option by quite a bit)
------------------------------------------------------
- Grab a released bundle and unzip it.
- cd $TS_HOME/src/com/sun/ts/tests
- ant list.tests | grep "Finished Test:" | cut -d . -f 10-11 > master.list.file
