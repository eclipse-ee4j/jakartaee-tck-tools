<?xml version = "1.0" encoding = "UTF-8"?>
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

<xsl:stylesheet xmlns:xsl = "http://www.w3.org/1999/XSL/Transform" version = "1.0">
  
  <!--
    ** This style sheet adds the specified keyword to the keyword list of all
    ** spec assertions within the spec assertion document being transformed,
    ** provided the keyword is not already in the keyword list.  Since the
    ** stylesheet checks to make sure the keyword is not already in the list,
    ** users can run this transformation multiple times on the same spec assertion
    ** document without adding duplicate keywords to the keyword list (the
    ** stylesheet is idempotent).
    **
    ** This stylesheet works on any XML document that is considered valid against
    ** the following DTD:
    **   http://invalid.domain.com/CTS/XMLassertions/dtd/spec_assertions.dtd
    **
    ** The stylesheet contains a user setable parameter called keyword-to-add.
    ** This is the keyword added to all the assertion keyword lists.  Users can
    ** set this parameter in a number of ways.  If they are using the CTS XSL
    ** transformation tool they can modify the keyword-to-add  property in the
    ** props.txt file.  See the README for the xsl-transformer tool in the CTS
    ** tools workspace.  Many command line utilities that invoke transformation 
    ** engines allow users to pass parameters on the command line.  See the
    ** documentation for the transformer you are using.  If all else fails hand
    ** edit the keyword-to-add parameter in this file.
    -->

  <xsl:param name="keyword-to-add" select="'application-server-role'"/>

  <xsl:output method="xml"
    indent="yes"
    doctype-system="http://dummy.domain.com/CTS/XMLassertions/dtd/spec_assertions.dtd"/>
  
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template name="find-role">
    <xsl:param name="keyword-list"/>
    <xsl:for-each select="$keyword-list">
      <xsl:choose>
        <xsl:when test=".=$keyword-to-add">
          <xsl:text>some non empty string</xsl:text>
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template match="assertion">
    <xsl:element name="assertion">
      <xsl:apply-templates select="id"/>
      <xsl:apply-templates select="description"/>
      <xsl:choose>
        <xsl:when test="./keywords">
          <xsl:element name="keywords">
            <xsl:apply-templates select="./keywords/keyword"/>
            <xsl:variable name="found-keyword">
              <xsl:call-template name="find-role">
                <xsl:with-param name="keyword-list" select="./keywords/keyword"/>
              </xsl:call-template>
            </xsl:variable>
            <xsl:choose>
              <xsl:when test="not(string-length($found-keyword))">
                <xsl:element name="keyword">
                  <xsl:value-of select="$keyword-to-add"/>
                </xsl:element>                              
              </xsl:when>
            </xsl:choose>
          </xsl:element>
        </xsl:when>
        <xsl:otherwise>
          <xsl:element name="keywords">
            <xsl:element name="keyword">
              <xsl:value-of select="$keyword-to-add"/>
            </xsl:element>
          </xsl:element>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="location"/>
      <xsl:apply-templates select="comment"/>
      <xsl:apply-templates select="depends"/>
      <xsl:apply-templates select="sub-assertions"/>
    </xsl:element>
  </xsl:template>
  
</xsl:stylesheet>
