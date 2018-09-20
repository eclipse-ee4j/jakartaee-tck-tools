#! /bin/sh -f
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

#####
#
# This shell script applies the 3 transformations used to sort
# specification assertion documents.
#
# USAGE:
#   sort-spec-assertions absolute_path_to_spec_doc absolute_path_to_output_file
#
# In order to sort spec assertions it was necessary to apply some
# intermediate transformations to achieve the end goal.  This was necessary
# due to the fact that the spec assertion string is a variable level
# number ID separated by some delimiter.  The following section IDs are
# valid examples:
#   1.2.3
#   1
#   1.2.6.78.2
# 
# In order to sort these values in numeric order using XSLT it seems to
# be necessary to make the section IDs all have the same number of levels.
# This greatly simplifies the sorting code.  Currently the stylesheets
# support up to 9 levels of nesting for a section ID.
#
# The first stylesheet that gets applied to the user's specified spec
# assertion document makes every section ID the same length by appending
# zero levels.  For example, if the section ID is 1.21 and we want all
# section IDs to have 5 levels the 1.21 section ID would be 1.21.0.0.0
# after the first stylesheet is appilied.  The first stylesheet is named
# normalize.xsl and is located in the $TOOLS_WS/docs/xsl/assertions
# directory.  The output of the first transformation is stored in a temp
# file.  The second transformation is then applied to the temp file.  This
# transformation actually sorts the spec assertions by chapter and section.
# The name of this stylesheet is sort.xsl and it lives in
# $TOOLS_WS/docs/xsl/assertions as well.  The output of this transformation
# is also stored in a temp file.  The third and final transformation is
# then applied to the temp file (output of second transformation).  This
# style sheet removes the trailing zero levels on the section IDs.  This
# stylesheet is named denormalize.xsl and it lives in the
# $TOOLS_WS/docs/xsl/assertions directory as well.  The ouput of this
# stylesheet is written to the user specified output file.  Users must use
# absolute paths to their input spec assertion document and output file.
#
#####


if [ $# -ne 2 ]; then
  echo "usage ${0} assertion_file sorted_output_file"
  exit 1
fi

ASSERTION_FILE=$1
OUTPUT_FILE=$2
CURRENT_DIR=`pwd`
TRANS_CMD_DIR="../xsl-transformer/scripts"
TRANS_CMD="./run"
XSL_DIR="../../../docs/xsl/assertions"
XSL_NORM=${XSL_DIR}/normalize.xsl
XSL_SORT=${XSL_DIR}/sort.xsl
XSL_DENORM=${XSL_DIR}/denormalize.xsl
TEMP="/tmp"
TEMP_FILE1="normal-out.xml"
TEMP_FILE2="sort-out.xml"

cd "${TRANS_CMD_DIR}"

if [ ! -s "${ASSERTION_FILE}" ]; then
  echo "\n\n**** Error no such assertion file \"${ASSERTION_FILE}\" ****\n\n"
  exit 1
fi

if [ -s "${OUTPUT_FILE}" ]; then
  echo "\n\n**** Error output file \"${OUTPUT_FILE}\" already exists ****\n\n"
  exit 1
fi


${TRANS_CMD} ${ASSERTION_FILE} ${XSL_NORM} ${TEMP}/${TEMP_FILE1}
${TRANS_CMD} ${TEMP}/${TEMP_FILE1} ${XSL_SORT} ${TEMP}/${TEMP_FILE2}
${TRANS_CMD} ${TEMP}/${TEMP_FILE2} ${XSL_DENORM} ${OUTPUT_FILE}

cd "${CURRENT_DIR}"

echo "\n\n**** ${ASSERTION_FILE} sorted, results written to ${OUTPUT_FILE} ****\n\n"
