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
  <xsl:output method="html" indent="yes"/>

  <xsl:template match="/">
    <html>
      <head>
        <title>
          <xsl:value-of select="webpage/@title"/>
        </title>
      </head>
      <body>
      	<h1><div align="Center">
      	  <xsl:value-of select="webpage/@title"/>
      	</div></h1><hr/>
        <xsl:apply-templates/>
      </body>
    </html>
  </xsl:template>
  
  <xsl:template match="introduction">
    <p>
       <xsl:apply-templates/>
    </p>
    <hr/><b/>
  </xsl:template> 

  <xsl:template match="sections">
    <xsl:apply-templates/>
  </xsl:template> 

  <xsl:template match="section">
    <h2><u><xsl:value-of select="@title"/></u></h2>
    <xsl:apply-templates/>
    <br/>
  </xsl:template> 

  <xsl:template match="sub-section">
    <p>
      <xsl:apply-templates/>
    </p>
  </xsl:template> 

  <xsl:template match="footer">
    <hr/>
    <xsl:apply-templates/>
  </xsl:template> 
  
  <xsl:template match="list">
    <h2><u><xsl:value-of select="@title"/></u></h2>    
    <xsl:choose>
      <xsl:when test="./@numbered='TRUE'">
        <ol>
          <xsl:apply-templates/>
        </ol>
      </xsl:when>
      <xsl:otherwise>
        <ul>
          <xsl:apply-templates/>        
        </ul>
      </xsl:otherwise>
    </xsl:choose>
    <br/>
  </xsl:template>
  
  <xsl:template match="list-element">
    <li>
      <xsl:apply-templates/>
    </li>
  </xsl:template>
  
  <xsl:template match="image">
    <h2><u><xsl:value-of select="@title"/></u></h2>
    <xsl:element name="div">
      <xsl:attribute name="align">
        <xsl:choose>
          <xsl:when test="./@position='CENTER'">
            <xsl:text>center</xsl:text>
          </xsl:when>
          <xsl:when test="./@position='RIGHT'">
            <xsl:text>right</xsl:text>
          </xsl:when>
        </xsl:choose>
      </xsl:attribute>  
      <xsl:element name="img">
        <xsl:attribute name="src">
          <xsl:value-of select="./@url"/>
        </xsl:attribute>
      </xsl:element>
    </xsl:element>
    <br/>
  </xsl:template>
  
  <xsl:template match="table">
    <h2><u><xsl:value-of select="@title"/></u></h2>
    <table cellpadding="2" cellspacing="2" border="1" width="100%">
      <tbody>
        <xsl:call-template name="col-headers">
          <xsl:with-param name="table" select="."/>
          <xsl:with-param name="row-labels" select="./@row-labels"/>
        </xsl:call-template>
        <xsl:call-template name="table-rows">
          <xsl:with-param name="table" select="."/>
          <xsl:with-param name="row-labels" select="./@row-labels"/>
        </xsl:call-template>
      </tbody>
    </table><br/>
  </xsl:template>
    
  <xsl:template name="table-rows">
  <xsl:param name="table"/>
  <xsl:param name="row-labels"/>
    <xsl:for-each select="$table/row-data">
      <tr>
        <xsl:if test="$row-labels='TRUE'">
          <td valign="top"><xsl:value-of select="./@label"/><br/>
          </td>
        </xsl:if>
        <xsl:for-each select="./row-element">
          <td valign="top"><xsl:value-of select="."/><br/>
          </td>
        </xsl:for-each>
      </tr>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="col-headers">
  <xsl:param name="table"/>
  <xsl:param name="row-labels"/>
    <tr>
    <xsl:if test="$row-labels='TRUE'">
      <td valign="top"><br/>
      </td>
    </xsl:if> 
    <xsl:for-each select="$table/col-label">
      <td valign="top"><xsl:value-of select="."/><br/>
      </td>
    </xsl:for-each>
    </tr>
  </xsl:template>  
    
  <xsl:template match="link">
    <xsl:element name="a">
      <xsl:attribute name="href">
        <xsl:value-of select="./@url"/>
      </xsl:attribute>
      <xsl:value-of select="."/>
    </xsl:element>
  </xsl:template> 

  <xsl:template match="mail">
    <xsl:element name="a">
      <xsl:attribute name="href">
        <!-- <xsl:text>mailto:</xsl:text> -->
        <xsl:value-of select="./@url"/>
      </xsl:attribute>
      <xsl:value-of select="."/>
    </xsl:element>
  </xsl:template> 
   
</xsl:stylesheet>
