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
   * This is a simple style sheet used to sort assertions lists in XML format.
   * THis can be used to sort spec or API assertion docs.  The docs can contain
   * assertion IDs that are simple integers or more formal IDs like:
   *    JPA:JAVADOC:1234
   *    JPA:SPEC:1234
   *
   * The assertions are sorted by the numeric value.  Note, assertion docs
   * should consistently follow one format and not mix ID formats.
   *
   * 
   * To run the stylesheet on a specification assertion document cd to
   * $CTS_TOOLS_WS/tools/xsl-transformer/scripts
   * execute the run script and pass the xml file, the stylesheet, and
   * an optional output file (if no output file is specified the results
   * are written to standard output).  Example:
   *
   *   run xml_file xsl_stylesheet [output_file]
   *
   * OR
   *
   * You can use the Ant xslt/style task.  See Ant docs for details.
   -->

  <xsl:template match="assertions">
    <xsl:copy>
      <xsl:for-each select="assertion">
        <xsl:sort select="substring-after(id, 'JAVADOC:')" data-type="number"/>
        <xsl:sort select="substring-after(id, 'SPEC:')" data-type="number"/>
        <xsl:sort select="id" data-type="number"/>
        <xsl:apply-templates select="."/>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="sub-assertions">
    <xsl:copy>
      <xsl:for-each select="assertion">
        <xsl:sort select="substring-after(id, 'JAVADOC:')" data-type="number"/>
        <xsl:sort select="substring-after(id, 'SPEC:')" data-type="number"/>
        <xsl:sort select="id" data-type="number"/>
        <xsl:apply-templates select="."/>
      </xsl:for-each>
    </xsl:copy>  
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
