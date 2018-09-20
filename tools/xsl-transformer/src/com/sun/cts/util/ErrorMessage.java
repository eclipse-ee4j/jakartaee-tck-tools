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

public class ErrorMessage extends JFrame {

    public ErrorMessage(String title, String message) {
	super();
	final JDialog dialog = new JDialog(this, title, true);
	Container cp = dialog.getContentPane();
	cp.setLayout(new BorderLayout());
	JTextField text = new JTextField(message);
        text.setHorizontalAlignment(JTextField.CENTER);
	JButton ok = new JButton("Dismiss");
	ok.addMouseListener(new MouseAdapter() {
		public void mouseReleased(MouseEvent e) {
		    dialog.setVisible(false);
		    dialog.dispose();
		}
	    });
	cp.add(ok, BorderLayout.SOUTH);
	cp.add(text, BorderLayout.CENTER);
	dialog.pack();
	dialog.setVisible(true);
    }
}
