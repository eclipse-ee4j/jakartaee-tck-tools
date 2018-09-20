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

<xsl:stylesheet version = "1.0" xmlns:xsl = "http://www.w3.org/1999/XSL/Transform" xmlns:xsi = "http
://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation = "http://www.w3.org/1999/XSL/Transform xs
lt.xsd">
    <xsl:param name="testarea"/>
    <xsl:output method = "html"/>
    <xsl:template match = "/">
        <html>
            <head>
                <title>
                    <xsl:value-of select = "excludelist/@title"/>
                </title>
            </head>
            <br/>
            <body>
                <h1>
                    <center>
                        <u>
                            <xsl:value-of select = "excludelist/@title"/
>
                            </u>
                        </center>
                    </h1>
                    <xsl:apply-templates/>
                </body>
            </html>
        </xsl:template>
        <xsl:template match = "excluded">
                <!-- <xsl:if test = "@technology ='JACC'"> -->
            <xsl:if test = "@technology = $testarea">
                <font size = "+3">
                    <bold>
                        <u>
                            <xsl:value-of select = "@technology"/>
                        </u>
                    </bold>
                </font>
                <br/>
                <br/>
                <xsl:apply-templates/>
            </xsl:if>
        </xsl:template>
        <xsl:template match = "bug">
            <font size = "+2">
                <bold>
                    <xsl:text>Bug id: </xsl:text>
                </bold>
            </font>
            <xsl:apply-templates/>
        </xsl:template>
        <xsl:template match = "bid">
            <font size = "+2">
                <bold>
                    <a href = "{@url}">
                        <xsl:value-of select = "@id"/>
                    </a>
                </bold>
            </font>
            <xsl:apply-templates/>
        </xsl:template>
        <xsl:template match = "test">
            <font size = "+1">
                <li>
                    <xsl:value-of select = "."/>
                </li>
            </font>
            <br/>
        </xsl:template>
    </xsl:stylesheet>
