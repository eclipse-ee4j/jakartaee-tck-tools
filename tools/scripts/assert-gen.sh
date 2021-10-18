#! /bin/sh -f
#
# Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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
# This script creates an XML asertion document (this is an API or Javadoc
# assertion document NOT a spec assertion doc).  This document is created
# by using various tools, the script serializes the application of the
# various tools so users may simply execute this script instead of running
# each tool individually.  The script uses two Java applications and two
# XSL style-sheets to produce the final output document.  All tools and
# style-sheets are located in the CTS tools workspace.
#
# To use the script the user must supply a directory path that contains the
# source code of the technology area they wish to generate assertions for.
# They must also supply an output directory where the final result file and
# various intermediate files will be written.  Currently, both of these paths
# need to be absolute paths (as oppossed to relative paths).  Users must also
# supply a list of packages that they wish to process.  All specified file
# names and paths should be absolute.  Usage statement:
#     usage: assert-gen source_directory output_directory package_1 package_2 ...
#
# Example Usage:
#     Say we have a set of Java source code organized within directories that
#     mimic the package declarations within the source files.  Also assume
#     all package directories live under the /tmp/code/ directory.  Say we
#     want to produce API assertions for the java.util and java.io packages.
#     To do this we would have to ensure that the Java source files for the
#     java.io package are in the directory; /tmp/code/java/io/.  We must also
#     ensure that the java.util source files are in the directory;
#     /tmp/code/java/util/.  To produce the XML assertions from the source code
#     and write the results to the /tmp/code/xml-asserts/ directory, run the
#     following command:
#         assert-gen /tmp/code /tmp/code/xml-asserts java.io java.util
#
# The following tools are applied by the script in the specified order.
# 
# First, the script runs the XML doclet tool on the specified source code
# (specified by the source_directory parameter and the package names).
# The output of this tool is an XML file that represents the Javadoc information
# extracted from the source files.  The XML file is written to the specified
# output directory (specified by the output_directory parameter).
#
# The output of the XML doclet tool will then be transformed to an XML assertion
# file.  This is an XML file that contains API assertions extracted from the XML
# representation of the Javadoc (produced by the doclet).  An XSL style-sheet
# will be applied to the XML Javadoc document using a simple Java tool (the
# name of the stylesheet is javadoc2assertions.xsl).  The output
# of this transformation will be an XML assertion document without numbered
# assertions.  To number the assertions a simple Java application is used to
# number each assertion on the XML asserion document.  The numbered assertions
# document is then transformed into HTML using an XSL stylesheet (the name of
# the stylesheet is javadoc_assertions.xsl) and the same java tool used to
# apply the first transformation.
#
# The following figure represents the entire process:
#                                                                    <----------
#                                                                   | Numbering ^
#                                                                   v   Tool    |
# ---------------             ------------------                  -----------------
# - Java Source - XML Doclet  - XML version of - XSL style-sheet  - XML Assertion -
# -    Code     - ----------> -     Javadocs   - ---------------> -   Document    -
# ---------------             ------------------                  -----------------
#                                                                         |
#                                                              XSL style- |
#                                                                sheet    V
#                                                                ------------------
#                                                                - HTML Assertion -
#                                                                -    Document    -
#                                                                ------------------
#
# Common JAX-RPC usage:
#   assert-gen JAXRPC "JAX-RPC 1.0" "Java API for XML-based RPC" "0.7" \
#              /net/awee/files/cvs/jaxrpc-api/src \
#              /home/ryano/cts-tools2/tools/scripts/output \
#              javax.xml.rpc javax.xml.rpc.encoding javax.xml.rpc.handler \
#              javax.xml.rpc.handler.soap javax.xml.rpc.holders \
#              javax.xml.rpc.namespace javax.xml.rpc.server javax.xml.rpc.soap
#
# NOTE: Currently, if a file exists in the specified output directory it WILL BE
# OVERWRITTEN without warning.
#
#

USAGE="usage: ${0} technology id name version source_dir output_dir package_name_1 [package_name_2 ...]"

#####
# Must specify a source directory, an output directory and at least one package
#####
if [ $# -lt 7 ]; then
    echo $USAGE
    exit 1
fi

#####
# Setup the input arguments
#####
TECH=${1}
ID=${2}
NAME=${3}
VERSION=${4}
SOURCE_DIR=${5}
OUTPUT_DIR=${6}
OUTPUT_FILE="xml-doclet.out"
NUM_PACKAGES=`expr $# - 6`
echo "NUM_PACKAGES = $NUM_PACKAGES"


#####
# Check that the source and destination directories exist
#####
if [ ! -d "${SOURCE_DIR}" ]; then
  echo "\n\n***Error, directory \"${SOURCE_DIR}\" does not exist\n\n"
  exit 1
fi
if [ ! -d "${OUTPUT_DIR}" ]; then
  echo "\n\n***Error, directory \"${OUTPUT_DIR}\" does not exist\n\n"
  exit 1
fi


#####
# The following chunk of script simply takes each package name and puts them
# into a comma separated list (with no spaces).  This constructed string is
# passed to the ant build script that runs the XML doclet.  If users find it
# annoying to type each package name when invoking the script they can comment
# out this code and simply define the PACKAGE_ARG variable.  The PACKAGE_ARG
# variable should be a comma-separated list of package names without any spaces.
# Example: PACKAGE_ARG="java.io,java.util"
#####
LOOP_COUNTER=6
while [ "$LOOP_COUNTER" -gt 0 ]; do
  shift
  LOOP_COUNTER=`expr $LOOP_COUNTER - 1`
done

ARGS=0
PACKAGE_ARG=""
COMMA_COUNT=`expr $NUM_PACKAGES - 1`
while [ "$1" != "" ]; do
  PACKAGE_ARG="$PACKAGE_ARG${1}" 
  if [ $ARGS -lt $COMMA_COUNT ]; then
    PACKAGE_ARG="$PACKAGE_ARG,"
    ARGS=`expr $ARGS + 1`
  fi
  shift
done
echo "\nPACKAGE_ARG=$PACKAGE_ARG\n"

#####
# Run the XML doclet tool and output the results to the specified output directory.
# The OUTPUT_FILE variable contains the file name.
#####
cd ../xml-doclet
CMD="ant -Dsrc-path=$SOURCE_DIR -Doutput-file=${OUTPUT_DIR}/${OUTPUT_FILE} -Dpackages-for-docs=${PACKAGE_ARG}"
echo "\n\n$CMD\n\n"
$CMD

#####
# Check for the output of the XML doclet, if it does not exist there must have
# been an error.  So this would be a good time to bail out.
#####
if [ ! -s "${OUTPUT_DIR}/${OUTPUT_FILE}" ]; then
  echo "\n\n***** Error, running the XML doclet, exiting...\n\n*****"
  exit 1
fi

#####
# Convert all occurrences of the "&amp;" to a "&", and replace all instances of "&nbsp" with an empty
# space.
#####
INPUT_FILE="${OUTPUT_DIR}/${OUTPUT_FILE}"
OUTPUT_FILE="${OUTPUT_DIR}/${OUTPUT_FILE}.converted"
echo "removing existing converted file"
rm -rf ${OUTPUT_FILE}*
echo "\n\nsed -e 's/\&amp;/\&/g' ${INPUT_FILE} > ${OUTPUT_FILE}_one\n\n"
#sed -e 's/\&amp;/\&/g' ${INPUT_FILE} > ${OUTPUT_FILE}_one
sed -e 's/\&amp;/ /g' ${INPUT_FILE} > ${OUTPUT_FILE}_one

echo "\n\nsed -e 's/\&nbsp;/ /g' ${OUTPUT_FILE}_one > ${OUTPUT_FILE}\n\n"
sed -e 's/\&nbsp;/ /g' ${OUTPUT_FILE}_one > ${OUTPUT_FILE}

#####
# Check for the output of the conversion, if it does not exist there must have
# been an error.  So this would be a good time to bail out.
#####
if [ ! -s "${OUTPUT_FILE}" ]; then
  echo "\n\n***** Error, running the conversion, exiting...\n\n*****"
  exit 1
fi

#####
# Transform the XML doclet file using the javadoc to XML assertion XSL style-sheet.
#####
echo "---------------------------------------------------------"
echo cd ../xsl-transformer/scripts 
cd ../xsl-transformer/scripts
XML_ASSERT_NO_NUMS_FILE="assertions-no-nums.xml"
XSL_FILE="../../../docs/xsl/assertions/javadoc2assertions.xsl"
INPUT_FILE="${OUTPUT_FILE}"
OUTPUT_FILE="${OUTPUT_DIR}/${XML_ASSERT_NO_NUMS_FILE}"
CMD="./run.sh ${INPUT_FILE} ${XSL_FILE} ${OUTPUT_FILE}"
echo "\n\n$CMD\n\n"
$CMD

#####
# Check for the output of the transformation, if it does not exist there must have
# been an error.  So this would be a good time to bail out.
#####
if [ ! -s "${OUTPUT_FILE}" ]; then
  echo "\n\n***** Error, transforming doclet output, exiting...\n\n*****"
  exit 1
fi


#####
# Add sed command to substitute out required elements added by the
# javadoc2assertions.xsl stylesheet above
#####
TEMP_FILE=/tmp/assert-gen.tmp
sed "s/__TECHNOLOGY__/${TECH}/" ${OUTPUT_FILE} | \
sed "s/__ID__/${ID}/" | \
sed "s/__NAME__/${NAME}/" | \
sed "s/__VERSION__/${VERSION}/" > ${TEMP_FILE}
mv -f ${TEMP_FILE} ${OUTPUT_FILE}


#####
# javadoc sorting transformation here
#####
XML_ASSERT_SORTED_FILE="assertions-sorted-no-nums.xml"
XSL_FILE="../../../docs/xsl/assertions/sort-javadoc-assertions.xsl"
INPUT_FILE="${OUTPUT_FILE}"
OUTPUT_FILE="${OUTPUT_DIR}/${XML_ASSERT_SORTED_FILE}"
CMD="./run ${INPUT_FILE} ${XSL_FILE} ${OUTPUT_FILE}"
echo "\n\n$CMD\n\n"
$CMD

#####
# Check for sorted output file
#####
if [ ! -s "${OUTPUT_FILE}" ]; then
  echo "\n\n***** Error, sorting the javadoc assertion file, exiting...\n\n*****"
  exit 1
fi


#####
# Number the assertions in the XML assertion document.
#####
cd ../../number-assert
XML_ASSERT_NUMS_FILE="assertions-nums.xml"
INPUT_FILE="${OUTPUT_FILE}"
OUTPUT_FILE="${OUTPUT_DIR}/${XML_ASSERT_NUMS_FILE}"
CMD="ant -Dinput_file=${INPUT_FILE} -Doutput_file=${OUTPUT_FILE}"
echo "\n\n$CMD\n\n"
$CMD

#####
# Check for the output of the numbering tool, if it does not exist there must have
# been an error.  So this would be a good time to bail out.
#####
if [ ! -s "${OUTPUT_FILE}" ]; then
  echo "\n\n***** Error, numbering the XML assertion document, exiting...\n\n*****"
  exit 1
fi

#####
# Transform the numbered XML assertion document to HTML
#####
cd ../xsl-transformer/scripts
HTML_ASSERT_FILE="assertions.html"
XSL_FILE="../../../docs/xsl/assertions/javadoc_assertions.xsl"
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

#####
# leave em where they started
#####
cd ../scripts

echo "\n\n***** Success, the assertion file \"${OUTPUT_FILE}\" has been created. *****\n\n"
