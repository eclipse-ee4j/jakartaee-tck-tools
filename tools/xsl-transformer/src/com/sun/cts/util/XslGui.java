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

package com.sun.cts.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Date;
import java.util.Properties;
import java.util.Enumeration;
import java.util.ArrayList;

public class XslGui extends JFrame {

    private static final String PROPS_FILE      = "props.txt";
    private static final String FILE_DELIM      = ";";
    private static final String TEMP_DIR        = File.separator + "tmp" + File.separator;
    private static final int    TEXT_FIELD_COLS = 60;
    private static final String BROWSE_TEXT     = "Browse...";
    
    private File    lastFile   = new File("/");
    private JButton xmlBtn     = new JButton(BROWSE_TEXT);
    private JButton xslBtn     = new JButton(BROWSE_TEXT);
    private JButton outBtn     = new JButton(BROWSE_TEXT);
    private JButton allPropBtn = new JButton("Select All Rows");
    private JButton delPropBtn = new JButton("Delete Property");
    private JButton addPropBtn = new JButton("Add Property");
    private JButton defPropBtn = new JButton("Read Default Properties");
    private JButton applyBtn   = new JButton("Apply XSL");
    private JButton cancelBtn  = new JButton("Dismiss");

    private JTextField xmlField = new JTextField(TEXT_FIELD_COLS);
    private JTextField xslField = new JTextField(TEXT_FIELD_COLS);
    private JTextField outField = new JTextField(TEXT_FIELD_COLS);

    private PropertyPanel propPanel;

    private Properties readProps() {
	Properties props = new Properties();
	try {
	    props.load(getClass().getClassLoader().getResourceAsStream(PROPS_FILE));
	} catch (Exception e) {
	    System.out.println(e);
	    System.out.println("No Props Defined, Application Continuing...");
	}
	return props;
    }

    public XslGui() {
	super("XSL Transformation GUI");
	Container cp = this.getContentPane();
	cp.setLayout(new BorderLayout());
	JLabel xmlL = new JLabel("XML File: ");
	JLabel xslL = new JLabel("XSL File: ");
	JLabel outL = new JLabel("Out File: ");

	JPanel filePanel = new JPanel(new BorderLayout());
	JPanel xmlPnl = new JPanel(new FlowLayout());
	xmlPnl.add(xmlL); xmlPnl.add(xmlField); xmlPnl.add(xmlBtn);
	filePanel.add(xmlPnl, BorderLayout.NORTH);
	JPanel xslPnl = new JPanel(new FlowLayout());
	xslPnl.add(xslL); xslPnl.add(xslField); xslPnl.add(xslBtn);
	filePanel.add(xslPnl, BorderLayout.CENTER);
	JPanel outPnl = new JPanel(new FlowLayout());
	outPnl.add(outL); outPnl.add(outField); outPnl.add(outBtn);
	filePanel.add(outPnl, BorderLayout.SOUTH);

	propPanel = new PropertyPanel(this, readProps());

	JPanel buttonPanel = new JPanel(new FlowLayout());
	buttonPanel.add(allPropBtn);
	buttonPanel.add(addPropBtn);
	buttonPanel.add(delPropBtn);
	buttonPanel.add(defPropBtn);
	buttonPanel.add(applyBtn);
	buttonPanel.add(cancelBtn);

	cp.add(filePanel, BorderLayout.NORTH);
	cp.add(propPanel, BorderLayout.CENTER);
	cp.add(buttonPanel, BorderLayout.SOUTH);
	setEventMgrs();
    }

    public static class NameValuePair {
	private JCheckBox  box;
	private JTextField name;
	private JTextField value;
	public NameValuePair(JCheckBox box, JTextField name, JTextField value) {
	    this.box   = box;
	    this.name  = name;
	    this.value = value;
	}
	public JCheckBox getCheckBox() {
	    return box;
	}
	public JTextField getNameTextField() {
	    return name;
	}
	public JTextField getValueTextField() {
	    return value;
	}
	public String getName() {
	    return name.getText();
	}
	public String getValue() {
	    return value.getText();
	}
	public boolean isSelected() {
	    return box.isSelected();
	}
    }

    public static class PropertyPanel extends JPanel {
	private Properties props = null;
	private File       tempPropFile;
	private JFrame     parent;
	private JPanel     headers;
	private JPanel     valuePairs;
	private ArrayList  valuePairList = new ArrayList();

	private JLabel createHeader(String header) {
	    JLabel headerLabel = new JLabel(header, SwingConstants.CENTER);
	    Font f = new Font("Monospaced", Font.BOLD, 20);
	    headerLabel.setFont(f);
	    return headerLabel;
	}
	public PropertyPanel(JFrame parent, Properties props) {	    
	    super(new BorderLayout());
	    headers = new JPanel(new GridLayout(0,3));
	    valuePairs = new JPanel(new GridLayout(0,3));
	    JScrollPane sp = new JScrollPane(valuePairs);
	    sp.setWheelScrollingEnabled(true);
	    add(headers, BorderLayout.NORTH);
	    add(sp, BorderLayout.CENTER);
	    this.parent = parent;
	    this.props = props;
	    headers.add(createHeader("Select Row"));
	    headers.add(createHeader("Name"));
	    headers.add(createHeader("Value"));
	    readDefaultProperties();
	}
	public void readDefaultProperties() {
	    try {
		props.load(getClass().getClassLoader().getResourceAsStream(PROPS_FILE));
		tempPropFile = File.createTempFile("tempProps", "txt");
	    } catch (Exception e) {
		System.err.println("Can not create temp file");
	    }
	    Enumeration e = props.propertyNames();
	    while (e.hasMoreElements()) {
		String name  = (String)e.nextElement();
		String value = props.getProperty(name, "");
		NameValuePair p = new NameValuePair(new JCheckBox(),
						    new JTextField(name),
						    new JTextField(value));
		valuePairList.add(p);
	    }
	    updateGUI();
	}
	private void updateGUI() {
	    valuePairs.removeAll();
	    int size = valuePairList.size();
	    for (int i = 0; i < size; i++) {
		NameValuePair nvp = (NameValuePair)valuePairList.get(i);
		valuePairs.add(nvp.getCheckBox());
		valuePairs.add(nvp.getNameTextField());
		valuePairs.add(nvp.getValueTextField());
	    }
	    parent.pack();
	    parent.setVisible(true);
	}
	public void delPropertyFromGUI() {
	    ArrayList deleteMe = new ArrayList();
	    int size = valuePairList.size();
	    for (int i = 0; i < size; i++) {
		NameValuePair nvp = (NameValuePair)valuePairList.get(i);
		if (nvp.isSelected()) {
		    deleteMe.add(nvp);
		}
	    }
	    size = deleteMe.size();
	    for (int i = 0; i < size; i++) {
		NameValuePair nvp = (NameValuePair)deleteMe.get(i);
		valuePairList.remove(nvp);
	    }	    
	    updateGUI();
	}
	public void addPropertyToGUI() {
	    valuePairList.add(new NameValuePair(new JCheckBox(),
						new JTextField(),
						new JTextField()));
	    updateGUI();
	}
	private void updateProps() {
	    props.clear();
	    int size = valuePairList.size();
	    for (int i = 0; i < size; i++) {
		NameValuePair nvp = (NameValuePair)valuePairList.get(i);
		props.setProperty(nvp.getName(), nvp.getValue());
	    }
	}
	private void writeProps(File file) {
	    try {
		FileOutputStream out = new FileOutputStream(tempPropFile);
		props.store(out, "XSL-Transformer Parameter Values");
	    } catch (Exception e) {
		System.err.println("Error writing properties file \"" +
				   tempPropFile + "\"");
	    }
	}
	public File getPropFile() {
	    updateProps();
	    writeProps(tempPropFile);	    
	    return tempPropFile;
	}
	public void selectAllRows() {
	    int size = valuePairList.size();
	    for (int i = 0; i < size; i++) {
		NameValuePair nvp = (NameValuePair)valuePairList.get(i);
		nvp.getCheckBox().setSelected(true);
	    }	    
	    updateGUI();
	}
    } // end class PropertyPanel


    private void enableGUI(boolean enable) {
	xmlBtn.setEnabled(enable);
	xslBtn.setEnabled(enable);
	outBtn.setEnabled(enable);
	allPropBtn.setEnabled(enable);
	delPropBtn.setEnabled(enable);
	addPropBtn.setEnabled(enable);
	defPropBtn.setEnabled(enable);
	applyBtn.setEnabled(enable);
	cancelBtn.setEnabled(enable);
    }

    public class RunTransform extends Thread {
	private int counter = 1;
	private Random rand = new Random(new Date().getTime());
	private String[] parseXSLFiles(String str) {
	    return str.split(FILE_DELIM);
	}
	private String getTempFile() {
	    String tempFile = "trans-" + Math.abs(rand.nextInt()) + ".out" + counter++;
	    return TEMP_DIR + tempFile;
	}
        public void run() {
	    enableGUI(false);
            try {
		String   inFile   = xmlField.getText();
		String   outFile  = null;
		String[] xslFiles = parseXSLFiles(xslField.getText());
		for (int i = 0; i < xslFiles.length; i++) {
		    if (i < xslFiles.length - 1) {
			outFile = getTempFile();
		    } else {
			outFile = outField.getText();
		    }
		    System.out.println();
		    System.out.println("Applying Stylesheet");
		    System.out.println("\tXML File   : " + inFile);
		    System.out.println("\tXSL File   : " + xslFiles[i]);
		    System.out.println("\tOutput File: " + outFile);
		    Trans tt = new Trans(inFile,
					 xslFiles[i],
					 outFile,
					 propPanel.getPropFile());
		    tt.transform();
		    inFile = outFile;
		}
            } catch (Exception ex) {
                ErrorMessage message = new ErrorMessage
                    (ex.getClass().getName(), ex.getMessage());
            } finally {
		enableGUI(true);
            }
        }
    } // end inner class RunTransform

    class MyActionListener implements ActionListener {
        private JTextField field;
        public MyActionListener(JTextField field) {
            this.field = field;
        }
        public void actionPerformed(ActionEvent e) {
            JFileChooser files = new JFileChooser(lastFile);
            int state = files.showOpenDialog(null);
            if (state == JFileChooser.APPROVE_OPTION) {
                File selectedFile = files.getSelectedFile();
		if (this.field == xslField) {
		    if (xslField.getText().length() <= 0) {
			field.setText(selectedFile.toString());
		    } else {
			field.setText(xslField.getText() + FILE_DELIM
				      + selectedFile.toString());
		    }
		} else {
		    field.setText(selectedFile.toString());
		}
                lastFile = selectedFile.getParentFile();
            }
        }
    }

    private void setEventMgrs() {
        xmlBtn.addActionListener(new MyActionListener(xmlField));
        xslBtn.addActionListener(new MyActionListener(xslField));
        outBtn.addActionListener(new MyActionListener(outField));
	allPropBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
		    propPanel.selectAllRows();
                }
	    });
	addPropBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
		    propPanel.addPropertyToGUI();
                }
	    });
	delPropBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
		    propPanel.delPropertyFromGUI();
                }
	    });
	defPropBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
		    propPanel.readDefaultProperties();
                }
	    });
        applyBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    RunTransform runT = new RunTransform();
                    runT.start();
                }
            });
        cancelBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
    }

    public void display() {
	this.pack();
	this.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent event) {
		    System.exit(0);
		}
	    });
	this.setVisible(true);
    }

    public static void main(String[] args) {
	try {
	    XslGui gui = new XslGui();
	    gui.display();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
} // end class XslGui
