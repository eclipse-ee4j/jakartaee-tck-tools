/*
 * $Id$
 *
 * Copyright 1996-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tdk.signaturetest;


import com.sun.tdk.signaturetest.core.ClassHierarchy;
import com.sun.tdk.signaturetest.core.ClassHierarchyImpl;
import com.sun.tdk.signaturetest.core.Log;
import com.sun.tdk.signaturetest.core.MemberCollectionBuilder;
import com.sun.tdk.signaturetest.loaders.VirtualClassDescriptionLoader;
import com.sun.tdk.signaturetest.merge.JSR68Merger;
import com.sun.tdk.signaturetest.merge.MergedSigFile;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.sigfile.FileManager;
import com.sun.tdk.signaturetest.sigfile.MultipleFileReader;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.OptionInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;


public class Merge extends SigTest implements Log {

    // Command line options
    private static final String FILES_OPTION = "-Files";
    private static final String WRITE_OPTION = "-Write";
    private static final String BINARY_OPTION = "-Binary";
    private static final String HELP_OPTION = "-Help";

    public static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Merge.class);
    private boolean binary = false;
    private String resultedFile;
    private String[] signatureFiles;


    /**
     * Run the test using command-line; return status via numeric exit code.
     *
     * @see #run(String[],PrintWriter,PrintWriter)
     */
    public static void main(String[] args) {
        Merge m = Merge.getInstance();
        m.run(args, new PrintWriter(System.err, true), null);
        m.exit();
    }

    protected static Merge getInstance() {
        return new Merge();
    }

    /**
     * This is the gate to run the test with the JavaTest application.
     *
     * @param pw  This log-file is used for error messages.
     * @param ref This reference-file is ignored here.
     * @see #main(String[])
     */
    public void run(String[] args, PrintWriter pw, PrintWriter ref) {

        log = pw;

        if (parseParameters(args)) {
            perform();
            log.flush();
        } else
            usage();

    }


    private boolean parseParameters(String[] args) {

        CommandLineParser parser = new CommandLineParser(this, "-");
        initErrors();

        // Print help text only and exit.
        if (args == null || args.length == 0 ||
                (args.length == 1 && (parser.isOptionSpecified(args[0], HELP_OPTION) || parser.isOptionSpecified(args[0], QUESTIONMARK))))
        {
            return false;
        }

        final String optionsDecoder = "decodeOptions";

        parser.addOption(FILES_OPTION, OptionInfo.requiredOption(1), optionsDecoder);
        parser.addOption(WRITE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(BINARY_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(HELP_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(TESTURL_OPTION, OptionInfo.option(1), optionsDecoder);


        try {
            parser.processArgs(args);
            if (resultedFile != null)
                checkValidWriteFile();
        }
        catch (CommandLineParserException e) {
            log.println(e.getMessage());
            return failed(e.getMessage());
        }


        return passed();
    }


    private void checkValidWriteFile() throws CommandLineParserException {
        File canonicalFile = null;
        try {
            canonicalFile = (new File(resultedFile)).getCanonicalFile();
        } catch (IOException e) {
            throw new CommandLineParserException(i18n.getString("Merge.could.not.resolve.file", resultedFile));
        }
        
        for (int i = 0; i < signatureFiles.length; i++) {
            try {
                File sigFile = (new File(signatureFiles[i])).getCanonicalFile();
                if (canonicalFile.equals(sigFile)) {
                    throw new CommandLineParserException(i18n.getString("Merge.notunique.writefile"));
                }
            } catch (IOException ex) {
                throw new CommandLineParserException(i18n.getString("Merge.could.not.resolve.file", signatureFiles[i]));
            }
        }
        
        try {
            FileOutputStream f = new FileOutputStream(resultedFile);
            f.close();
        } catch (IOException e) {
            throw new CommandLineParserException(i18n.getString("Merge.could.not.create.write.file"));
        }
    }


    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        if (optionName.equalsIgnoreCase(FILES_OPTION)) {
            StringTokenizer st = new StringTokenizer(args[0], File.pathSeparator);
            ArrayList list = new ArrayList();
            while (st.hasMoreElements()) {
                list.add(st.nextToken());
            }
            signatureFiles = (String[]) list.toArray(new String[0]);
        } else if (optionName.equalsIgnoreCase(WRITE_OPTION)) {
            resultedFile = args[0];
        } else if (optionName.equalsIgnoreCase(BINARY_OPTION)) {
            binary = true;
        } else if (optionName.equalsIgnoreCase(TESTURL_OPTION)) {
            testURL = args[0];
        }
    }


    void perform() {

        String msg;
        MergedSigFile[] files = new MergedSigFile[signatureFiles.length];

        for (int i = 0; i < signatureFiles.length; i++) {
            PrintWriter log = new PrintWriter(System.out);
            MultipleFileReader in = new MultipleFileReader(log, MultipleFileReader.CLASSPATH_MODE);
            String sigFiles = signatureFiles[i];
            if (!in.readSignatureFiles(testURL, sigFiles)) {
                msg = i18n.getString("SignatureTest.error.sigfile.invalid", sigFiles);
                in.close();
                error(msg);
            }
            files[i] = new MergedSigFile(in, this);
            MemberCollectionBuilder builder = new MemberCollectionBuilder(new SilentLog());

            Iterator it = files[i].getClassSet().values().iterator();
            while (it.hasNext()) {
                ClassDescription c = (ClassDescription) it.next();
                c.setHierarchy(files[i].getClassHierarchy());
                try {
                    if (in.hasFeature(FeaturesHolder.BuildMembers))
                        builder.createMembers(c, true, true, false);
                    normalizer.normThrows(c, true);
                } catch (ClassNotFoundException e) {
                    //storeError(i18n.getString("Setup.error.message.classnotfound", e.getMessage()));
                }
            }
        }

        JSR68Merger merger = new JSR68Merger(this, this);
        VirtualClassDescriptionLoader result = merger.merge(files,
                binary ? JSR68Merger.BINARY_MODE : JSR68Merger.SOURCE_MODE);

        if (!isPassed()) {
            printErrors();
            return;
        }

        ClassHierarchy ch = new ClassHierarchyImpl(result, ClassHierarchy.ALL_PUBLIC);
        for (Iterator i = result.getClassIterator(); i.hasNext();) {
            ClassDescription c = (ClassDescription) i.next();
            c.setHierarchy(ch);
        }
        MemberCollectionBuilder builder = new MemberCollectionBuilder(new SilentLog());
        for (Iterator i = result.getClassIterator(); i.hasNext();) {
            ClassDescription c = (ClassDescription) i.next();
            try {
                builder.createMembers(c, false, true, false);
                normalizer.normThrows(c, true);
            } catch (ClassNotFoundException e) {
                storeError(i18n.getString("Merge.warning.message.classnotfound", e.getMessage()));
            }
        }

        try {
            //write header to the signature file
            Writer writer = FileManager.getDefaultFormat().getWriter();
            writer.setApiVersion("");
            if (resultedFile != null) {
                writer.init(new PrintWriter(new OutputStreamWriter(new FileOutputStream(resultedFile), "UTF8")));
            } else {
                writer.init(new PrintWriter(System.out));
            }
            writer.setAllFeatures(merger);
            writer.writeHeader();

            // scan class and writes definition to the signature file

            // 1st analyze all the classes
            for (Iterator i = result.getClassIterator(); i.hasNext();) {
                ClassDescription c = (ClassDescription) i.next();
                writer.write(c);
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            error(e.getMessage());
        }
        printErrors();
    }


    /**
     * Prints help text.
     */
    protected void usage() {
        String nl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();

        sb.append(i18n.getString("Setup.usage.start"));
        sb.append(nl).append(i18n.getString("Merge.usage.files", FILES_OPTION));
        sb.append(nl).append(i18n.getString("Merge.usage.write", WRITE_OPTION));
        sb.append(nl).append(i18n.getString("Merge.usage.binary", BINARY_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.help", HELP_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.end"));
        System.err.println(sb.toString());
    }

    class SilentLog implements Log {
        public void storeError(String s) {
        }
        public void storeWarning(String s) {
        }
    }

}

