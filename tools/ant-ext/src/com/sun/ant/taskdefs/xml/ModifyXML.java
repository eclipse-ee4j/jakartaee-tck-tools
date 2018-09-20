/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.ant.taskdefs.xml;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.Namespace;
import org.jdom.filter.ContentFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;
import org.jdom.xpath.XPath;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * This ant task allows users to add and remove  XML fragments to and
 * from an XML document.  Thetask also allows users to modify the existing
 * content of an XML document.
 * 
 * The task takes the specified infile and extracts the node set
 * that matches the specified xpathexpr.  The text values of the nodes
 * in the returned node set are replaced with the value specified in
 * value attribute.  The resulting XML file is written to the file
 * specified in by the outfile attribute.  The schemapaths element
 * allows user to specify a comma or space separated list of directories
 * where schemas can be located.  This set of directories is used to
 * setup an entity resolver for the XML parser.  The overwriteoutfile
 * attribute is used to determine if the output file should be
 * overwritten if it already exists.  This attribute value defaults to
 * true, set it to false if you don't want to overwrite the existing
 * output file.
 *
 * Users must specify one of the following attributes or elements
 * (and only one).  If the user specifies two or more attributes an
 * error is thrown.
 *
 *   If the user specifies a value attribute the task changes the values
 *   of text nodes, element nodes and attribute nodes that are found
 *   with the specified XPath expression.
 *
 *   If the user specifies a true value for the deletenodes.  The nodeset
 *   returned by the specified XPath expression is removed from the XML
 *   document.
 *
 *   If the user specifies a nested xmlfragment element.  The xml fragment
 *   is added to the nodeset returned by the specified XPath expression.
 *
 *
 * Typical usage for this ant task
 *
 *    <modifyxml infile="some_input_file"
 *               outfile="some_output_file"
 *               xpathexpr="valid_xpath_expression"
 *               ( [value="value_to_write_to_xpathexpr_result"] ||
 *                 [deletenodes="{true|false}"] defaults to false ||
 *                 [nested xmlfragment element {see below for more info}]
 *               )
 *               [validate="{true|false}"] defaults to false
 *               [schemapaths="comma_or_space_separated_list_of_dirs_to_look_for_schemas"]
 *               [defaultnsprefix="default_name_space_prefix"] defaults to 'j'
 *               [overwriteoutfile="{true|false}"] defaults to true
 *
 * Nested Elements:
 *
 *  Users can specify an xml fragment to insert into the document being modified.
 *  The XML fragment is inserted into the document being modified as a child of the
 *  element (or elements) selected by the XPath statement.
 *     ex. <xmlfragment>
 *           <![CDATA[
 *             <some-new-element xmlns="http://sun.com" attr1="attr1">
 *               <some-nested-element attr2="attrib2">random text</some-nested-element>
 *             </some-new-element>
 *           ]]>
 *         </xmlfragment>
 *    
 *  Users can specify zero or many namespace elements.  These elements map namespace
 *  prefixes to namespace URIs.  This is necessary for the XPath statement.  This
 *  tells the XPath expression which prefixes are mapped to which namespapce URIs.
 *     ex. <namespace prefix="j2ee" uri="http://java.sun.com/xml/ns/j2ee"/>
 *
 * NOTES:
 *
 * Why do we have a defaultnsprefix attribute and nested namespace elements?  This
 * is necessary since XPath has no notion of default namespaces.  If an XPath
 * expression contains elements that are not preceded by a namespace prefix,
 * XPath assumes the elements are in no namespace, not the default namespace.
 *
 * Rules for specifying namespaces:
 *   If the user specifies one or more namespace elements, these namespaces are
 *   added to the namespaces that the XPath processor is aware of.  If the user
 *   does not specify any namespace elements, the namespace of the root element
 *   is determined and this namespace is mapped to the prefix specified by the
 *   defaultnsprefix attribute.  If this attribute is missing the prefix "j" is
 *   mapped to the namespace.  Note, if the root element is not in a namespace,
 *   no namespace is added to the XPath namespace list.
 *
 *   When adding an XML fragment to a document, the element of the
 *   fragment will have a namespace set if no namespace is specified.  If the
 *   user specifies a set of namespace elements they may set one or many of the
 *   namespace elements to be the default namespace for the document being modified.
 *   This ant task will assume the last namespace element specified with the
 *   docdefault attribute set to true is the default namespace of the doc being
 *   modfied.  So when an XML fragment is added and it has no namespace
 *   declaration, the ant task adds the fragment elements to the default namespace
 *   specified by the namespace elements.  If the user did not supply any
 *   namespace elements the fragment elements are added to the doc's root
 *   elements namespace.
 */
public class ModifyXML extends Task implements TaskDataIntf {

    private File              infile;
    private File              outfile;
    private String            xpathexpr;
    private String            value;
    private boolean           validate;
    private String[]          schemapaths;
    private String            defaultnsprefix  = "j";
    private boolean           overwriteoutfile = true;
    private List              namespaces       = new ArrayList();
    private NameSpace         docdefaultnamespace;
    private XMLFragment       fragment;
    private boolean           deletenodes;
    private String            property;
    private Document          xmldoc;
    private NodeProcessorIntf processor;


    public Project getProject() {
	return project;
    }

    public void setInfile(File infile) {
	this.infile = infile;
    }

    public void setOutfile(File outfile) {
	this.outfile = outfile;
    }
    public String getOutfile() {
	return this.outfile.getPath();
    }

    public void setXPathexpr(String xpathexpr) {
	this.xpathexpr = xpathexpr;
    }

    public void setOverwriteoutfile(boolean overwriteoutfile) {
	this.overwriteoutfile = overwriteoutfile;
    }

    public void setValue(String value) {
	this.value = value;
    }
    public String getValue() {
	return this.value;
    }

    public void setProperty(String property) {
	this.property = property;
    }
    public String getProperty() {
	return property;
    }

    public void setValidate(boolean validate) {
	this.validate = validate;
    }

    public void setSchemapaths(String schemapaths) {
	String delimiters = " \t\n\r\f,";
	StringTokenizer tokens = new StringTokenizer(schemapaths, delimiters);
	this.schemapaths = new String[tokens.countTokens()];
	for (int i = 0; tokens.hasMoreTokens(); i++) {
	    this.schemapaths[i] = project.translatePath(tokens.nextToken().trim());
	}
    }

    public void setDefaultnsprefix(String defaultnsprefix) {
	this.defaultnsprefix = defaultnsprefix;
    }

    public void addConfiguredNamespace(NameSpace namespace) {
	namespace.init();
	namespaces.add(namespace);
	// if this namespace is marked as the document default
	// we save it as such
	if (namespace.isDocdefault()) {
	    docdefaultnamespace = namespace;
	}
    }

    public void addConfiguredXmlfragment(XMLFragment fragment) {
	fragment.init();
	this.fragment = fragment;
    }
    public XMLFragment getFragment() {
	return fragment;
    }

    public void setDeletenodes(boolean deletenodes) {
	this.deletenodes = deletenodes;
    }

    public boolean getDeletenodes() {
	return deletenodes;
    }

    private void printNamespaces() {
	int numNS = namespaces.size();
	NameSpace ns = null;
	log("User specified namespaces:", project.MSG_VERBOSE);
	for (int i = 0; i < numNS; i++) {
	    ns = (NameSpace)namespaces.get(i);
	    log("  namespace " + i + "   : " + ns, project.MSG_VERBOSE);
	}
    }

    private void dumpState() {
	printNamespaces();
	log("infile          : \"" + infile + "\"", project.MSG_VERBOSE);
	log("outfile         : \"" + outfile + "\"", project.MSG_VERBOSE);
	log("xpathexpr       : \"" + xpathexpr + "\"", project.MSG_VERBOSE);
	log("value           : \"" + value + "\"", project.MSG_VERBOSE);
	log("deletenodes     : \"" + deletenodes + "\"", project.MSG_VERBOSE);
	log("property        : \"" + property + "\"", project.MSG_VERBOSE);
	if (fragment != null) {
	    log("xmlfragment     : \"" + fragment.getContainingElementName() + "\"", project.MSG_VERBOSE);
	} else {
	    log("xmlfragment     : \"null\"", project.MSG_VERBOSE);
	}
	log("defaultnsprefix : \"" + defaultnsprefix + "\"", project.MSG_VERBOSE);
	log("overwriteoutfile: \"" + overwriteoutfile + "\"", project.MSG_VERBOSE);
	log("validate XML    : \"" + validate + "\"", project.MSG_VERBOSE);
	if (this.schemapaths != null) {
	    log("schemapaths     : \"" + Arrays.asList(this.schemapaths) + "\"", project.MSG_VERBOSE);
	}
    }

    public void execute() throws BuildException {
	dumpState();
	checkPreConditions();
	parseInputDoc();
	modifyInputDoc();
	if (processor.getMode() != BaseProcessor.READ) {
	    writeOutputDoc();
	}
    }

    private void checkPreConditions() throws BuildException {
	if (infile == null) {
	    throw new BuildException
		("Error: you must specify a valid file for attribute infile");
	}
	if (!infile.isFile()) {
	    throw new BuildException("Error: infile is not a valid file.");
	}
	if (property == null) {
	    if (outfile == null) {
		throw new BuildException
		    ("Error: you must specify a file for attribute outfile");
	    }
	    if (outfile.isFile() && !overwriteoutfile) {
		throw new BuildException("Error: outfile already exists, to overwrite"
					 + " specify overwriteoutfile=\"false\" or"
					 + " omit the overwriteoutfile attribute.");
	    }
	}
	if (xpathexpr == null || xpathexpr.length() == 0) {
	    throw new BuildException("Error: xpathexpr must contain a valid XPath expression.");
	}
	if (schemapaths != null) {
	    boolean foundError = false;
	    for (int i = 0; i < schemapaths.length; i++) {
		File currentDir = new File(schemapaths[i]);
		if (!currentDir.isDirectory()) {
		    log("Error in schema path: \"" + schemapaths[i] +
			"\" is not a valid directory", project.MSG_ERR);
		    foundError = true;
		}
	    }
	    if (foundError) {
		throw new BuildException("Error: one or more schema paths are invalid directories.");
	    }
	}
	this.processor = BaseProcessor.getProcessor(this);
    }

    private void parseInputDoc() throws BuildException {
	try {
	    SAXBuilder builder = new SAXBuilder(validate);
	    // parser will still try to find the referenced schema or DTD,
	    // so if the user specified schemapaths, setup the entity resolver
	    if (schemapaths != null) {
		MyResolver resolver = new MyResolver(schemapaths);
		builder.setEntityResolver(resolver);
	    }
	    this.xmldoc = builder.build(this.infile);
	} catch (Exception e) {
	    throw new BuildException(e);
	}
    }

    private void addDefautNamespace(XPath xpath) {
	// get the namespace of the root element.  we're assuming this
	// namespace is the default target namespace
	String nsURI = xmldoc.getRootElement().getNamespaceURI();
	String nsPrefix = xmldoc.getRootElement().getNamespacePrefix();
	log("Root Element NameSpace URI   : \"" + nsURI + "\"", project.MSG_VERBOSE);
	log("Root Element NameSpace Prefix: \"" + nsPrefix + "\"", project.MSG_VERBOSE);
	if (nsURI.length() > 0) {
	    if (nsPrefix.length() > 0) {
		xpath.addNamespace(nsPrefix, nsURI);
		log("Added NameSpace              : \"[" + nsPrefix + " -> " +
		    nsURI + "]\"", project.MSG_VERBOSE);
	    } else {
		xpath.addNamespace(defaultnsprefix, nsURI);
		log("Added NameSpace              : \"[" + defaultnsprefix +
		    " -> " + nsURI + "]\"", project.MSG_VERBOSE);
	    }
	    
	}
    }

    private void addUserSpecifiedNamespaces(XPath xpath) {
	int numNS = namespaces.size();
	NameSpace ns = null;
	for (int i = 0; i < numNS; i++) {
	    ns = (NameSpace)namespaces.get(i);
	    xpath.addNamespace(ns.getPrefix(), ns.getUri());
	}
    }

    public Namespace getDefaultNamespace() {
	Namespace ns = null;
	if (docdefaultnamespace != null) {
	    ns = org.jdom.Namespace.getNamespace(docdefaultnamespace.getUri());
	} else {
	    ns = xmldoc.getRootElement().getNamespace();
	}
	return ns;
    }
    
    private void modifyInputDoc () throws BuildException {
	try {
 	    XPath xpath = XPath.newInstance(xpathexpr);
	    if (namespaces.size() == 0) {
		addDefautNamespace(xpath);
	    } else {
		addUserSpecifiedNamespaces(xpath);
	    }
 	    List elements = xpath.selectNodes(xmldoc);
	    int numElements = (elements == null) ? 0 : elements.size();
	    processor.process(elements, this);
	    if (numElements == 0) {
		log("XPath expression \"" + xpathexpr + "\" returned no nodes.",
		    project.MSG_WARN);
	    } else {
		log("XPath expression \"" + xpathexpr + "\" " + processor.getModeStr() +
		    " " + numElements + " node(s)", project.MSG_WARN);
	    }
	    log(processor.getMessage(), project.MSG_WARN);
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new BuildException(e);
	}
    }

    private void writeOutputDoc() throws BuildException {
	Writer writer = null;
	try {
	    XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
	    writer = new FileWriter(this.outfile);
	    out.output(this.xmldoc, writer);
	} catch (Exception e) {
	    throw new BuildException(e);
	} finally {
	    try { writer.close(); } catch (Throwable t) {}
	}
    }

    class MyResolver implements EntityResolver {
	private String[] paths;
	public MyResolver(String[] paths) {
	    this.paths = paths;
	}
 	public InputSource resolveEntity (String publicId, String systemId) {
	    String result = null;
 	    String schemaName = systemId.substring(systemId.lastIndexOf("/") + 1);
	    for (int i = 0; i < paths.length; i++) {
		String schemaLocation = paths[i] + File.separator + schemaName;
		File possibleSchema = new File(schemaLocation);
		if (possibleSchema.isFile()) {
		    result = schemaLocation;
		    break;
		}
	    }
	    if (result == null) {
		throw new BuildException("Error could not resolve schema \"" + schemaName + "\"");
	    }
 	    return new InputSource(result);
 	}
    } // end class MyResolver

} // end class ModifyXML
