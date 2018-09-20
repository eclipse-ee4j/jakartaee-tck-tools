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
	<xsl:output method="xml" indent="yes" doctype-system="http://dummy.domain.com/CTS/XMLassertions/dtd/javadoc_assertions.dtd"/>
	
	<xsl:param name="include_constants" select="'false'"/>

	<xsl:template match="/">
		<xsl:apply-templates/>
	</xsl:template>


	<xsl:template match="javadoc">
		<!--
		<xsl:text>&lt;?xml-stylesheet type="text/xsl" href="http://invalid.domain.com/CTS/javadoc_assertions.xsl"?&gt;</xsl:text>
-->
		<xsl:element name="javadoc">
			<xsl:element name="next-available-id">
				<xsl:value-of select="'1'"/>
			</xsl:element>
			<xsl:element name="previous-id">
				<xsl:value-of select="'0'"/>
			</xsl:element>
			<xsl:element name="technology">
				<xsl:value-of select="'__TECHNOLOGY__'"/>
			</xsl:element>
			<xsl:element name="id">
				<xsl:value-of select="'__ID__'"/>
			</xsl:element>
			<xsl:element name="name">
				<xsl:value-of select="'__NAME__'"/>
			</xsl:element>
			<xsl:element name="version">
				<xsl:value-of select="'__VERSION__'"/>
			</xsl:element>
			<xsl:element name="assertions">
				<xsl:for-each select="./package">
					<xsl:variable name="package" select="."/>
					<xsl:for-each select="./classes/class|./interfaces/interface">
						<xsl:variable name="class" select="."/>

						<xsl:if test="$include_constants='true'">
						<xsl:for-each select="./fields/field">
							<xsl:if test="starts-with(@modifiers,'public')">
								<xsl:call-template name="create-default-assert">
									<xsl:with-param name="package" select="$package"/>
									<xsl:with-param name="class" select="@type"/>
									<xsl:with-param name="method" select="."/>
									<xsl:with-param name="method-type" select="'field'"/>
									<xsl:with-param name="containing-class" select="$class/@name"/>
								</xsl:call-template>
							</xsl:if>
						</xsl:for-each>
						</xsl:if>

						<xsl:for-each select="./constructors/constructor">
							<xsl:if test="starts-with(@modifiers,'public')">
								<xsl:call-template name="create-default-assert">
									<xsl:with-param name="package" select="$package"/>
									<xsl:with-param name="class" select="$class"/>
									<xsl:with-param name="method" select="."/>
									<xsl:with-param name="method-type" select="'constructor'"/>
								</xsl:call-template>
							</xsl:if>
						</xsl:for-each>
						<!-- constructor loop -->
						<xsl:for-each select="./methods/method">
							<xsl:if test="starts-with(@modifiers,'public')">
								<xsl:call-template name="create-default-assert">
									<xsl:with-param name="package" select="$package"/>
									<xsl:with-param name="class" select="$class"/>
									<xsl:with-param name="method" select="."/>
									<xsl:with-param name="method-type" select="'method'"/>
								</xsl:call-template>
							</xsl:if>
						</xsl:for-each>
						<!-- method loop  -->
					</xsl:for-each>
					<!-- class loop   -->
				</xsl:for-each>
				<!-- package loop -->
			</xsl:element>
			<!-- assertions element -->
		</xsl:element>
		<!-- javadoc element    -->
	</xsl:template>


	<xsl:template name="add-common">
		<xsl:param name="package"/>
		<xsl:param name="method"/>
                <xsl:variable name="status">
			<xsl:choose>
				<xsl:when test="$method/deprecated">
					<xsl:value-of select="'deprecated'"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="'active'"/>
				</xsl:otherwise>
			</xsl:choose>
                </xsl:variable>
		<xsl:attribute name="required"><xsl:value-of select="'true'"/></xsl:attribute>
		<xsl:attribute name="impl-spec"><xsl:value-of select="'false'"/></xsl:attribute>
		<xsl:attribute name="status"><xsl:value-of select="$status"/></xsl:attribute>
		<xsl:attribute name="testable"><xsl:value-of select="'true'"/></xsl:attribute>
		<xsl:element name="id">
			<xsl:value-of select="'__NUMBER__'"/>
		</xsl:element>
	</xsl:template>


	<xsl:template name="create-default-assert">
		<xsl:param name="package"/>
		<xsl:param name="class"/>
		<xsl:param name="method"/>
		<xsl:param name="method-type"/>
                <xsl:param name="containing-class"/>
                <xsl:if test="not(starts-with($method/@inherited.from,'java.util.')) ">
                        <xsl:if test="not(starts-with($method/@inherited.from,'java.lang.')) ">

		<xsl:element name="assertion">
			<xsl:call-template name="add-common">
				<xsl:with-param name="package" select="$package"/>
				<xsl:with-param name="method" select="$method"/>
			</xsl:call-template>
			<xsl:choose>
				<xsl:when test="starts-with($method-type,'field')">
					<xsl:element name="description">
						<xsl:value-of select="$method/comment"/>
					</xsl:element>
					<xsl:element name="package">
						<xsl:value-of select="$package/@name"/>
					</xsl:element>
					<xsl:element name="class-interface">
						<xsl:value-of select="$containing-class"/>
					</xsl:element>
					<xsl:element name="field">
						<xsl:attribute name="name"><xsl:value-of select="$method/@name"/></xsl:attribute>
						<xsl:attribute name="type"><xsl:value-of select="$class"/></xsl:attribute>
					</xsl:element>
				</xsl:when>
				<xsl:otherwise>
					<xsl:element name="description">
						<xsl:value-of select="$method/comment"/>
					</xsl:element>
					<xsl:element name="package">
						<xsl:value-of select="$package/@name"/>
					</xsl:element>
					<xsl:element name="class-interface">
						<xsl:value-of select="$class/@name"/>
                                        <xsl:if test="$method/@inherited.from != '' ">
                                                <xsl:text>.{</xsl:text>
                                                <xsl:value-of select="$method/@inherited.from"/>
                                                <xsl:text>}</xsl:text>
                                        </xsl:if>

					</xsl:element>
					<xsl:choose>
						<xsl:when test="starts-with($method-type,'constructor')">
							<xsl:element name="method">
                                                                <xsl:attribute name="name"><xsl:value-of select="$class/@name"/></xsl:attribute>
								<xsl:attribute name="return-type"><xsl:value-of select="$class/@name"/></xsl:attribute>
								<xsl:if test="count($method/parameters/parameter) &gt; 0">
									<xsl:element name="parameters">
										<xsl:for-each select="$method/parameters/parameter">
											<xsl:element name="parameter">
												<xsl:value-of select="./@package"/>
												<xsl:if test="./@package">
													<xsl:text>.</xsl:text>
												</xsl:if>
												<xsl:value-of select="./@type"/>
												<xsl:value-of select="./@dimension"/>
											</xsl:element>
										</xsl:for-each>
									</xsl:element>
								</xsl:if>
							</xsl:element>
						</xsl:when>
						<xsl:otherwise>
							<!--- this is for methods -->
							<xsl:element name="method">
								<xsl:attribute name="name"><xsl:value-of select="$method/@name"/></xsl:attribute>
								<xsl:attribute name="return-type"><xsl:value-of select="$method/returns/@package"/><xsl:if test="$method/returns/@package"><xsl:text>.</xsl:text></xsl:if><xsl:value-of select="$method/returns/@type"/><xsl:value-of select="$method/returns/@dimension"/></xsl:attribute>
								<xsl:if test="count($method/parameters/parameter) &gt; 0">
									<xsl:element name="parameters">
										<xsl:for-each select="$method/parameters/parameter">
											<xsl:element name="parameter">
												<xsl:value-of select="./@package"/>
												<xsl:if test="./@package">
													<xsl:text>.</xsl:text>
												</xsl:if>
												<xsl:value-of select="./@type"/>
												<xsl:value-of select="./@dimension"/>
											</xsl:element>
										</xsl:for-each>
									</xsl:element>
								</xsl:if>
							</xsl:element>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:element>
		<xsl:for-each select="$method/throws/throw">
			<xsl:call-template name="create-exception-assert">
				<xsl:with-param name="package" select="$package"/>
				<xsl:with-param name="class" select="$class"/>
				<xsl:with-param name="method" select="$method"/>
				<xsl:with-param name="throw" select="."/>
				<xsl:with-param name="method-type" select="$method-type"/>
			</xsl:call-template>
		</xsl:for-each>
                        </xsl:if>
                </xsl:if>

	</xsl:template>


	<xsl:template name="create-exception-assert">
		<xsl:param name="package"/>
		<xsl:param name="class"/>
		<xsl:param name="method"/>
		<xsl:param name="throw"/>
		<xsl:param name="method-type"/>
		<xsl:element name="assertion">
			<xsl:call-template name="add-common">
				<xsl:with-param name="package" select="$package"/>
				<xsl:with-param name="method" select="$method"/>
			</xsl:call-template>
			<xsl:element name="description">
				<xsl:value-of select="$throw"/>
			</xsl:element>
			<xsl:element name="package">
				<xsl:value-of select="$package/@name"/>
			</xsl:element>
			<xsl:element name="class-interface">
				<xsl:value-of select="$class/@name"/>
                                <xsl:if test="$method/@inherited.from != '' ">
                                        <xsl:text>.{</xsl:text>
                                        <xsl:value-of select="$method/@inherited.from"/>
                                        <xsl:text>}</xsl:text>
                                </xsl:if>
			</xsl:element>
			<xsl:element name="method">
                               <xsl:choose>
                                   <xsl:when test="starts-with($method-type,'constructor')">
    			                <xsl:attribute name="name"><xsl:value-of select="$class/@name"/></xsl:attribute>
                                        <xsl:attribute name="return-type"><xsl:value-of select="$class/@name"/></xsl:attribute>
                                   </xsl:when>
                               <xsl:otherwise>                
				   <xsl:attribute name="name"><xsl:value-of select="$method/@name"/></xsl:attribute>
				   <xsl:attribute name="return-type"><xsl:value-of select="$method/returns/@package"/>
				   <xsl:if test="$method/returns/@package"><xsl:text>.</xsl:text></xsl:if>
				   <xsl:value-of select="$method/returns/@type"/><xsl:value-of select="$method/returns/@dimension"/></xsl:attribute>
			      </xsl:otherwise>
                              </xsl:choose>
				<xsl:if test="count($method/parameters/parameter) &gt; 0">
					<xsl:element name="parameters">
						<xsl:for-each select="$method/parameters/parameter">
							<xsl:element name="parameter">
								<xsl:value-of select="./@package"/>
								<xsl:if test="./@package">
									<xsl:text>.</xsl:text>
								</xsl:if>
								<xsl:value-of select="./@type"/>
								<xsl:value-of select="./@dimension"/>
							</xsl:element>
						</xsl:for-each>
					</xsl:element>
				</xsl:if>
				<xsl:element name="throw">
					<xsl:value-of select="$throw/@name"/>
				</xsl:element>
			</xsl:element>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>
