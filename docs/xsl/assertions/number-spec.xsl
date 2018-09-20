<?xml version="1.0"?>
<!--

    Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License v. 2.0, which is available at
    http://www.eclipse.org/legal/epl-2.0.

    This Source Code may also be made available under the following Secondary
    Licenses when the conditions for such availability set forth in the
    Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
    version 2 with the GNU Classpath Exception, which is available at
    https://www.gnu.org/software/classpath/license.html.

    SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0

-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
   <xsl:output method="xml" version="1.0" encoding="US-ASCII" indent="yes" />

  <!--
   *
   * This is a simple style sheet used to number spec assertions.  The
   * user should leave the id elements empty when hand editing a spec
   * assertion document.  They can then transform their spec document
   * using this stylesheet.  Nested assertions are also handled by this
   * stylesheet.
   *   Example:
   *     If the user has a spec document for the technology area JAXRPC,
   *     top level asertion id elements will be filled in with
   *     "<id>JAXRPC:SPEC:[number]</id>"  where number is the number
   *     of this assertion in reference to all other top level assertions.
   *     If users have nested assertions the IDs will contain a nested id
   *     element similar to:
   *     "<id>JAXRPC:SPEC:[number_parent].[number]</id>" where number_parent
   *     is the number of the parent assertion.  This numbering scheme
   *     will be applied to all levels of sub-assertions.  For instance, if
   *     assertion 1 has a sub-assertion that also has a sub-assertion.
   *     The asigned ID will be:
   *     "<id>JAXRPC:SPEC:1.1.1</id>"
   *
   * To run the stylesheet on a specification assertion document cd to
   * $CTS_TOOLS_WS/tools/xsl-transformer/scripts
   * execute the run script and pass the xml file, the stylesheet, and
   * an optional output file (if no output file is specified the results
   * are written to standard output).  Example:
   *
   *   run xml_file xsl_stylesheet [output_file]
   *
   -->


  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <!--
   * Matches the /spec/id element so it is not numbered like the assertion
   * ids.  The /spec/id element is just passed on through to the output doc.
  -->  
  <xsl:template match="/spec/id">
    <xsl:element name="id">
      <xsl:value-of select="."/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="id">
    <xsl:element name="id">
      <xsl:value-of select="/spec/technology"/>
      <xsl:text>:SPEC:</xsl:text>
      <xsl:number level="multiple" count="assertion"/>
    </xsl:element>
  </xsl:template>

</xsl:stylesheet>
