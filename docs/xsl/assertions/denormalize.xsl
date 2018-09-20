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

  <xsl:output method="xml" version="1.0" indent="yes"/>

  <!--
    *
    * This stylesheet removes zero level padding from section IDs.
    *
    * This style sheet is actually part of a three part transformation
    * that is executed by the $TOOLS_WS/tools/scripts/sort-spec-assertions
    * script.
    *
    * In order to sort spec assertions it was necessary to apply some
    * intermediate transformations to achieve the end goal.  This was necessary
    * due to the fact that the spec assertion string is a variable level
    * number ID separated by some delimiter.  The following section IDs are
    * valid examples:
    *   1.2.3
    *   1
    *   1.2.6.78.2
    * 
    * In order to sort these values in numeric order using XSLT it seems to
    * be necessary to make the section IDs all have the same number of levels.
    * This greatly simplifies the sorting code.  Currently the stylesheets
    * support up to 9 levels of nesting for a section ID.
    *
    * The first stylesheet that gets applied to the user's specified spec
    * assertion document makes every section ID the same length by appending
    * zero levels.  For example, if the section ID is 1.21 and we want all
    * section IDs to have 5 levels the 1.21 section ID would be 1.21.0.0.0
    * after the first stylesheet is appilied.  The first stylesheet is named
    * normalize.xsl and is located in the $TOOLS_WS/docs/xsl/assertions
    * directory.  The output of the first transformation is stored in a temp
    * file.  The second transformation is then applied to the temp file.  This
    * transformation actually sorts the spec assertions by chapter and section.
    * The name of this stylesheet is sort.xsl and it lives in
    * $TOOLS_WS/docs/xsl/assertions as well.  The output of this transformation
    * is also stored in a temp file.  The third and final transformation is
    * then applied to the temp file (output of second transformation).  This
    * style sheet removes the trailing zero levels on the section IDs.  This
    * stylesheet is named denormalize.xsl and it lives in the
    * $TOOLS_WS/docs/xsl/assertions directory as well.  The ouput of this
    * stylesheet is written to the user specified output file.  Users must use
    * absolute paths to their input spec assertion document and output file.
    *
    * To minimize the users burden a simple shell script was created to run all
    * three transformations.  The user supplies the name of the spec assertion
    * document and the name of the output file.  The name of the shell script is
    * sort-spec-assertions and it lives in the $CTS_TOOLS/tools/scripts.
    * 
    -->

  <xsl:template match="/|@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  

  <xsl:template match="location">
    <xsl:element name="location">
      <xsl:variable name="result">
        <xsl:call-template name="denormalize-section">
          <xsl:with-param name="sect-str" select="./@section"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:attribute name="chapter">
        <xsl:value-of select="./@chapter"/>
      </xsl:attribute>
      <xsl:attribute name="section">
        <xsl:value-of select="$result"/>
      </xsl:attribute>
    </xsl:element>
  </xsl:template>


  <xsl:template name="denormalize-section">
    <xsl:param name="sect-str"/>
    <xsl:param name="pattern" select="'.0'"/>
    <xsl:param name="delim" select="'.'"/>
    
    <xsl:variable name="end-index" select="string-length($sect-str) - 1"/>
    <xsl:variable name="end-str" select="substring($sect-str, $end-index)"/>
    <xsl:variable name="start-str" select="substring($sect-str, 0, $end-index)"/>

    <xsl:choose>
      <xsl:when test="($end-str = $pattern)">
        <xsl:call-template name="denormalize-section">
          <xsl:with-param name="sect-str" select="$start-str"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$sect-str"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


</xsl:stylesheet>
