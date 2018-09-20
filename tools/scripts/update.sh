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

#/bin/tcsh

if ( $#argv == 1 ) then 
  set BASE_DIR=$1
else
  set BASE_DIR=`pwd`
endif
echo "BASE_DIR is '$BASE_DIR'"
set PWD=`pwd`
echo "PWD is '$PWD'"
echo "cd $BASE_DIR"
echo ""
cd $BASE_DIR
set ROOT=`pwd`

foreach dir ( `ls -d */ | sed 's/\/$//'` )
  echo "cd $ROOT/$dir"
  cd $ROOT/$dir
  set IS_SVN=`ls -a | grep .svn`
  if ("$IS_SVN" == ".svn") then
    svn info | grep URL
    echo "svn update --ignore-externals"
    svn update --ignore-externals
  else
    echo "'$ROOT/$dir' does not appear to be an SVN workspace, skipping..."
  endif
  echo ""
end

echo "cd $PWD"
cd $PWD
