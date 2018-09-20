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

  <xsl:output method="xml" version="1.0" encoding="US-ASCII" indent="yes"/>

  <!--
   *
   * This is a simple style sheet used to extract assertion IDs
   * from XML assertion documents.  The documents can be spec or
   * API assertion documents.  The XML output of this transformation
   * is then used as one of the two inputs into the assertion
   * coverage tool.
   *
   * To run the stylesheet on an assertion document cd to
   * $CTS_TOOLS_WS/tools/xsl-transformer/scripts
   * execute the run script and pass the xml file, the stylesheet, and
   * an optional output file (if no output file is specified the results
   * are written to standard output).  Example:
   *
   *   run xml_file xsl_stylesheet [output_file]
   *
   -->

  <xsl:template match="assertion">
    <xsl:element name="assertion">
      <xsl:element name="id">
	<xsl:value-of select="id"/>
      </xsl:element>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="/">
    <xsl:element name ="assertions">
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:apply-templates/>
  </xsl:template>

</xsl:stylesheet>
