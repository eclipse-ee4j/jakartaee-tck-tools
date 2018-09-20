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

#
# This script numbers a spec XML assertion document and transform it into an
# HTML document.  The stylesheet applied is called spec_assertion.xsl.
#
# The following figure represents the entire process:
#          -----------------
#          - XML Assertion -
#          -   Document    -
#          - could be hand -
#          -    edited     -
#          -----------------
#                  |
#       XSL style- | number-spec.xsl
#         sheet    V
#         ----------------------
#         - Numbered Assertion -
#         -      Document      -
#         ----------------------
#                  |
#       XSL style- | spec_assertion.xsl
#         sheet    V
#         ------------------
#         - HTML Assertion -
#         -    Document    -
#         ------------------
#
#
# NOTE: Currently, if a file exists in the specified output directory it WILL BE
# OVERWRITTEN without warning.
#

USAGE="usage: ${0} xml_assertion_file output_dir"

#####
# Must specify a source directory, an output directory and at least one package
#####
if [ $# -ne 2 ]; then
    echo $USAGE
    exit 1
fi

#####
# Number the assertions in the XML spec assertion document.
#####
cd ../xsl-transformer/scripts
XML_ASSERT_NUMS_FILE="assertions-nums.xml"
INPUT_FILE=${1}
OUTPUT_DIR=${2}
OUTPUT_FILE="${OUTPUT_DIR}/${XML_ASSERT_NUMS_FILE}"
XSL_FILE="../../../docs/xsl/assertions/number-spec.xsl"
CMD="./run ${INPUT_FILE} ${XSL_FILE} ${OUTPUT_FILE}"
echo "\n\n$CMD\n\n"
$CMD

#####
# Check for the output of the numbering transformation, if it does not exist
# there must have been an error.  So this would be a good time to bail out.
#####
if [ ! -s "${OUTPUT_FILE}" ]; then
  echo "\n\n***** Error, numbering the XML assertion document, exiting...\n\n*****"
  exit 1
fi

#####
# Transform the numbered XML assertion document to HTML
#####
HTML_ASSERT_FILE="assertions.html"
XSL_FILE="../../../docs/xsl/assertions/spec_assertions.xsl"
INPUT_FILE="${OUTPUT_FILE}"
OUTPUT_FILE="${OUTPUT_DIR}/${HTML_ASSERT_FILE}"
CMD="./run ${INPUT_FILE} ${XSL_FILE} ${OUTPUT_FILE}"
echo "\n\n$CMD\n\n"
$CMD

#####
# Check for the output of the transformation to HTML, if it does not exist
# there must have been an error.  So this would be a good time to bail out.
#####
if [ ! -s "${OUTPUT_FILE}" ]; then
  echo "\n\n***** Error, transforming the XML to HTML, exiting...\n\n*****"
  exit 1
fi

# leave em where they started
cd ../../scripts
