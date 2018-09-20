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
<xsl:output method="xml" indent="yes"
 doctype-system="http://dummy.domain.com/CTS/XMLassertions/dtd/javadoc_assertions.dtd"/>
	
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="assertions">
    <xsl:copy>
      <xsl:for-each select="assertion">
        <xsl:sort select="package" data-type="text"/>
        <xsl:sort select="class-interface" data-type="text"/>
        <xsl:sort select="field/@name" data-type="text"/>
        <xsl:sort select="method/@name" data-type="text"/>
        <xsl:apply-templates select="."/>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
