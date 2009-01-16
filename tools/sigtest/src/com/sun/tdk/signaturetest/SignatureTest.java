/*
 * $Id: SignatureTest.java 4549 2008-03-24 08:03:34Z me155718 $
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

import com.sun.tdk.signaturetest.classpath.ClasspathImpl;
import com.sun.tdk.signaturetest.core.*;
import com.sun.tdk.signaturetest.model.*;
import com.sun.tdk.signaturetest.plugin.PluginAPI;
import com.sun.tdk.signaturetest.plugin.Transformer;
import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.sigfile.MultipleFileReader;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.OptionInfo;
import com.sun.tdk.signaturetest.errors.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Level;

/**
 * <b>SignatureTest</b> is the main class of signature test. <p>
 * <p/>
 * The main purpose of signature test is to ensure that programs written in Java
 * using the class libraries of one Java implementation of a specific API version
 * (ie. 1.1, 1.2, etc.) can rely on having the same and only the same Java APIs
 * available in other implementations of the same API version.  This is a more
 * stringent requirement than simple binary compatibility as defined in the Java
 * Language Specification, chapter 13.  It is in essence a two-way binary
 * compatibility requirement.  Therefore, third party implementations of the Java
 * API library must retain binary compatibility with the JavaSoft API. Also, the
 * JavaSoft API implementation must retain binary compatibility with the Java API
 * library under test.<p>
 * <p/>
 * <b>SignatureTest</b> implements the standard JavaTest 3.0
 * <code>com.sun.javatest.Test</code> interface and uses the standard
 * <code>main()</code> method implementation. <b>SignatureTest</b> allows
 * to check only specified by command line the package or packages. <p>
 * <p/>
 * SignatureTest tracks the following aspects of binary compatibility:
 * <ul>
 * <li>Fully qualified name of class or interface
 * <li>Class modifiers abstract and final
 * <li>Superclasses and superinterfaces
 * <li>Public and protected class members
 * </ul>
 * <p/>
 * <b>SignatureTest</b> tracks all of the super classes and all of the super
 * interfaces of each public class and public interface within required
 * packages. <p>
 * <p/>
 * <b>SignatureTest</b> tracks all of the public and protected class members
 * for each public class and interface.<p>
 * <p/>
 * For each constructor or method tracked, <b>SignatureTest</b> tracks all
 * modifiers except native and synchronized.  It also tracks other
 * attributes for constructors and methods: method name, argument types
 * and order, return type, and the declared throwables.<p>
 * <p/>
 * For each field tracked, <b>SignatureTest</b> tracks all modifiers except
 * transient.  It also tracks these other attributes for fields: data
 * type, and field name. <p>
 * <p/>
 * Usage: <code>java com.sun.tdk.signaturetest.SignatureTest</code> &lt;options&gt;<p>
 * <p/>
 * where &lt;options&gt; includes:
 * <p/>
 * <dl>
 * <dt><code><b>-TestURL</b></code> &lt;URL&gt;
 * <dd> URL of signature file.
 * <p/>
 * <dt><code><b>-FileName</b></code> &lt;n&gt;
 * <dd> Path name of signature file name.
 * <p/>
 * <dt><code><b>-Package</b></code> &lt;package&gt;
 * <dd> Name of the package to be tested.
 * It is implied, that all subpackages the specified package should also be tested.
 * Such option should be included for each package (but subpackages), which is
 * required to be tested.
 * <p/>
 * <dt><code><b>-PackageWithoutSubpackages</b></code> &lt;package&gt;
 * <dd> Name of the package, which is to be traced itself excluding its subpackages.
 * Such option should be included for each package required to be traced
 * excluding subpackages.
 * <p/>
 * <dt><code><b>-Exclude</b></code> &lt;package_or_class_name&gt;
 * <dd> Name of the package or class, which is not required to be traced,
 * despite of it is implied by <code>-Package</code> or by
 * <code>-PackageWithoutSubpackages</code> options.
 * If the specified parameter names a package, all its subpackages are implied
 * to be also excluded.
 * Such option should be included for each package (but subpackages) or class,
 * which is not required to be traced.
 * <p/>
 * <dt><code><b>-FormatPlain</b></code>
 * <dd> Do not reorder errors report.
 * <p/>
 * <dt><code><b>-AllPublic</b></code>
 * <dd> Trace public nested classes, which are member of classes having default scope.
 * <p/>
 * <dt><code><b>-Classpath</b></code> &lt;path&gt;
 * <dd> Path to packages being tested. If there are several directories and/or zip-files
 * containing the required packages, all of them should be specified here.
 * Use <code>java.io.File.pathSeparator</code> to separate directory and/or
 * zip-file names in the specified path.
 * Only classes from &lt;path&gt; will be used for tracking adding classes.
 * <p/>
 * <dt><code><b>-static</b></code>
 * <dd> Run signature test in static mode.
 * <p/>
 * <dt><code><b>-Version</b></code> &lt;version&gt;
 * <dd> Specify API version. If this parameter is not specified, API version is assumed to
 * be that reported by <code>getProperty("java.version")</code>.
 * <p/>
 * <dt><code><b>-CheckValue</b><code>
 * <dd> Check values of primitive constant. This option can be used in static mode only.
 * <p/>
 * <dt><code><b>-Verbose</b><code>
 * <dd> Enable error diagnostic for inherited class members.
 * </dl>
 *
 * @author Jonathan Gibbons
 * @author Maxim Sokolnikov
 * @author Serguei Ivashin
 * @author Mikhail Ershov
 * @version 05/03/22
 */
public class SignatureTest extends SigTest {

    // Test specific options
    public static final String CHECKVALUE_OPTION = "-CheckValue";
    public static final String NOCHECKVALUE_OPTION = "-NoCheckValue";
    public static final String MODE_OPTION = "-Mode";
    public static final String ENABLESUPERSET_OPTION = "-EnableSuperSet";
    public static final String FILES_OPTION = "-Files";
    public static final String NOMERGE_OPTION = "-NoMerge";
    public static final String WRITE_OPTION = "-Write";

    private String logName = null;
    private String outFormat = null;


    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(SignatureTest.class);

    /**
     * Log-file is not the System.err
     */
    private boolean logFile = false;

    /**
     * When signature test checks API, this table collects names of that
     * classes present in both signature file and in the API being tested.
     */
    private Set trackedClassNames;

    /**
     * Enable diagnostics for inherited class members.
     */
    private boolean isVerbose = false;

    /**
     * Enable constant checking whenever possible, starting with sigtest 1.2.1
     */
    private Boolean isValueTracked = null;
    private boolean isOneWayConstantChecking = false;

    private String writeFileName = null;

    /**
     * Check mode selected.
     */
    private String mode = null;

    public static final String BINARY_MODE = "bin";
    private static final String SOURCE_MODE = "src";

    private static final String FORMAT_PLAIN = "plain";
    private static final String FORMAT_HUMAN = "human";
    private static final String FORMAT_BACKWARD = "backward";


    private boolean isSupersettingEnabled = false;

    private boolean isThrowsRemoved = false;

    private ClassHierarchy signatureClassesHierarchy;

    private Erasurator erasurator = new Erasurator();

    protected Exclude exclude;
    private int readMode = MultipleFileReader.MERGE_MODE;

    /**
     * Run the test using command-line; return status via numeric exit code.
     *
     * @see #run(String[],PrintWriter,PrintWriter)
     */
    public static void main(String[] args) {
        SignatureTest t = SignatureTest.getInstance();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    protected static SignatureTest getInstance() {
        return new SignatureTest();
    }

    /**
     * This is the gate to run the test with the JavaTest application.
     *
     * @param log This log-file is used for error messages.
     * @param ref This reference-file is ignored here.
     * @see #main(String[])
     */
    public void run(String[] args, PrintWriter log, PrintWriter ref) {

        long startTime = System.currentTimeMillis();

        SigTest.log = log;
        mode = null;
        try {
            ClassLoader cl = SignatureTest.class.getClassLoader();
            exclude = (Exclude) cl.loadClass(System.getProperty("exclude.plugin")).newInstance();
        } catch (Exception e) {
            exclude = new DefaultExcludeList();
        }

        //ref ignored

        if (parseParameters(args)) {
            check();
            if (logFile)
                SigTest.log.println(toString());
        } else
            usage();

        if (classpath != null)
            classpath.close();

        long runTime = System.currentTimeMillis() - startTime;
//        SigTest.log.println("Execution time: " + ((double) runTime) / 1000 + " second(s)");

        // don't close logfile if it was readSignatureFile in test harness
        if (logFile) {
            SigTest.log.close();
            System.out.println(i18n.getString("SignatureTest.mesg.see_log", logName));
        }
    }

    /**
     * Parse options specific for <b>SignatureTest</b>, and pass other
     * options to <b>SigTest</b> parameters parser.
     *
     * @param args Same as <code>args[]</code> passes to <code>main()</code>.
     */
    private boolean parseParameters(String[] args) {


        CommandLineParser parser = new CommandLineParser(this, "-");

        // Print help text only and exit.
        if (args == null || args.length == 0 || (args.length == 1
                && (parser.isOptionSpecified(args[0], HELP_OPTION) || parser.isOptionSpecified(args[0], QUESTIONMARK)))) {
            return false;
        }

        args = exclude.parseParameters(args);


        final String optionsDecoder = "decodeOptions";

        parser.addOption(PACKAGE_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);

        // required only in static mode!
        parser.addOption(CLASSPATH_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(FILENAME_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(FILES_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(TESTURL_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(WITHOUTSUBPACKAGES_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
        parser.addOption(EXCLUDE_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
        parser.addOption(VERSION_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(APIVERSION_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(OUT_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(STATIC_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(CLASSCACHESIZE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(FORMATPLAIN_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(FORMATHUMAN_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(FORMATHUMAN_ALT_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(BACKWARD_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(BACKWARD_ALT_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(DEBUG_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(XNOTIGER_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(XVERBOSE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(CHECKVALUE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(NOCHECKVALUE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(ENABLESUPERSET_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(MODE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(ALLPUBLIC_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(VERBOSE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(HELP_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(QUESTIONMARK, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(PLUGIN_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(NOMERGE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(WRITE_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(ERRORALL_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        try {
            parser.processArgs(args);
        } catch (CommandLineParserException e) {
            log.println(e.getMessage());
            return failed(e.getMessage());
        }

        if (packages.isEmpty() && purePackages.isEmpty())
            packages.addPackage("");

        if (parser.isOptionSpecified(FILENAME_OPTION)) {
            readMode = MultipleFileReader.CLASSPATH_MODE;


        }
        // ==================

        if (parser.isOptionSpecified(STATIC_OPTION) && !parser.isOptionSpecified(CLASSPATH_OPTION)) {
            return error(i18n.getString("SignatureTest.error.static.missing_option", CLASSPATH_OPTION));
        }

        if (!parser.isOptionSpecified(FILENAME_OPTION) && !parser.isOptionSpecified(FILES_OPTION)) {
            String invargs[] = {FILENAME_OPTION, FILES_OPTION};
            return error(i18n.getString("SignatureTest.error.options.filename_options", invargs));
        }

        if (parser.isOptionSpecified(FILENAME_OPTION) && parser.isOptionSpecified(FILES_OPTION)) {
            String invargs[] = {FILENAME_OPTION, FILES_OPTION};
            return error(i18n.getString("Setup.error.options.cant_be_used_together", invargs));
        }

        logFile = false;
        if (logName != null) {

            try {
                log = new PrintWriter(new FileWriter(logName), true);
                logFile = true;
            } catch (IOException x) {
                if (SigTest.debug)
                    x.printStackTrace();
                return error(i18n.getString("SignatureTest.error.out.invfile", OUT_OPTION));
            }
        }

        // create ClasspathImpl for founding of the added classes
        try {
            classpath = new ClasspathImpl(classpathStr);
        } catch (SecurityException e) {
            if (SigTest.debug)
                e.printStackTrace();
            log.println(i18n.getString("SignatureTest.error.sec.newclasses"));
        }

        if (isStatic && classpath.isEmpty())
            return error(i18n.getString("SignatureTest.error.classpath.unspec"));

        return passed();
    }


    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {

        if (optionName.equalsIgnoreCase(FILENAME_OPTION)) {
            sigFileName = args[0];
        } else if (optionName.equalsIgnoreCase(FILES_OPTION)) {
            sigFileNameList = args[0];
        } else if (optionName.equalsIgnoreCase(FORMATPLAIN_OPTION)) {
            outFormat = FORMAT_PLAIN;
        } else if (optionName.equalsIgnoreCase(FORMATHUMAN_ALT_OPTION)) {
             outFormat = FORMAT_HUMAN;
        } else if (optionName.equalsIgnoreCase(FORMATHUMAN_OPTION)) {
             outFormat = FORMAT_HUMAN;
        } else if (optionName.equalsIgnoreCase(BACKWARD_OPTION)) {
             outFormat = FORMAT_BACKWARD;
        } else if (optionName.equalsIgnoreCase(BACKWARD_ALT_OPTION)) {
             outFormat = FORMAT_BACKWARD;
        } else if (optionName.equalsIgnoreCase(VERBOSE_OPTION)) {
            isVerbose = true;

        } else if (optionName.equalsIgnoreCase(CHECKVALUE_OPTION)) {
            // default is true as of 1.2.1
            isValueTracked = Boolean.TRUE;

        } else if (optionName.equalsIgnoreCase(NOCHECKVALUE_OPTION)) {
            isValueTracked = Boolean.FALSE;

        } else if (optionName.equalsIgnoreCase(WRITE_OPTION)) {
            writeFileName = args[0];

        } else if (optionName.equalsIgnoreCase(MODE_OPTION)) {

            if (!SOURCE_MODE.equalsIgnoreCase(args[0]) && !BINARY_MODE.equalsIgnoreCase(args[0]))
                throw new CommandLineParserException(i18n.getString("SignatureTest.error.arg.invalid", MODE_OPTION));

            mode = args[0];

        } else if (optionName.equalsIgnoreCase(OUT_OPTION)) {
            logName = args[0];

        } else if (optionName.equalsIgnoreCase(ENABLESUPERSET_OPTION)) {
            isSupersettingEnabled = true;
        } else if (optionName.equalsIgnoreCase(NOMERGE_OPTION)) {
            readMode = MultipleFileReader.CLASSPATH_MODE;
        } else {
            super.decodeCommonOptions(optionName, args);
        }
    }


    /**
     * Prints help text.
     */
    protected void usage() {

        String nl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();
        sb.append(i18n.getString("SignatureTest.usage.version", Version.Number));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.start"));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.static", STATIC_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.mode", MODE_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.backward", new Object[]{BACKWARD_OPTION, BACKWARD_ALT_OPTION}));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.classpath", CLASSPATH_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.filename", FILENAME_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.or"));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.files", new Object[]{FILES_OPTION, java.io.File.pathSeparator}));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.package", PACKAGE_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.human", new Object[]{FORMATHUMAN_OPTION, FORMATHUMAN_ALT_OPTION}));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.out", OUT_OPTION));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.testurl", TESTURL_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.packagewithoutsubpackages", WITHOUTSUBPACKAGES_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.exclude", EXCLUDE_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.nomerge", NOMERGE_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.apiversion", APIVERSION_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.checkvalue", CHECKVALUE_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.formatplain", FORMATPLAIN_OPTION));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.classcachesize", new Object[]{CLASSCACHESIZE_OPTION, new Integer(DefaultCacheSize)}));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.verbose", VERBOSE_OPTION));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.debug", DEBUG_OPTION));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.help", HELP_OPTION));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("SignatureTest.usage.end"));

        System.err.println(sb.toString());
    }


    public boolean useErasurator() {
        return !isTigerFeaturesTracked || BINARY_MODE.equals(mode);
    }


    /**
     * Do run signature test provided its arguments are successfully parsed.
     *
     * @see #parseParameters(String[])
     */
    private boolean check() {

        if (pluginClass != null) {
            pluginClass.init(this);
        }

        String msg;
        //  Open the specified sigfile and read standard headers.

        // ME - TODO: rewrite this in the future
        if (readMode == MultipleFileReader.MERGE_MODE && sigFileNameList != null && sigFileNameList.indexOf(File.pathSeparator) >= 0) {
            try {
                if (writeFileName == null) {
                    File tmpF = File.createTempFile("sigtest", "sig");
                    writeFileName = tmpF.getAbsolutePath();
                    tmpF.deleteOnExit();
                }
                Merge m = Merge.getInstance();
                String[] args = new String[]{"-Files", sigFileNameList, "-Write", writeFileName};
                if (BINARY_MODE.equals(mode)) {
                    args = new String[]{"-Files", sigFileNameList, "-Write", writeFileName, "-Binary"};
                }
                m.testURL = this.testURL;
                m.run(args, log, null);
                if (!m.isPassed()) {
                    error(m.getReason());
                    return false;
                }
                readMode = MultipleFileReader.CLASSPATH_MODE;
                sigFileName = writeFileName;
                sigFileNameList = null;
                testURL = "";

            } catch (IOException ex) {
                ex.printStackTrace(log);
                msg = i18n.getString("SignatureTest.error.tmpsigfile");
                return error(msg);
            }
        } else readMode = MultipleFileReader.CLASSPATH_MODE;

        MultipleFileReader in = new MultipleFileReader(log, readMode);
        String linesep = System.getProperty("line.separator");
        boolean result;

        if (sigFileNameList != null)
            result = in.readSignatureFiles(testURL, sigFileNameList);
        else
            result = in.readSignatureFile(testURL, sigFileName);

        if (!result) {
            if (in != null)
                in.close();
            msg = i18n.getString("SignatureTest.error.sigfile.invalid", sigFileNameList==null ? sigFileName : sigFileNameList);
            log.println(msg);
            return error(msg);

        }


        if (isValueTracked == null) {
            isValueTracked = isStatic ? Boolean.TRUE : Boolean.FALSE;
        }

        if (mode == null) {
            mode = SOURCE_MODE;
        }
        MemberType.setMode(BINARY_MODE.equals(mode));

        isOneWayConstantChecking = isValueTracked.booleanValue() && BINARY_MODE.equals(mode) || !isStatic;

        msg = "";

        if (SOURCE_MODE.equals(mode)) {
            isThrowsRemoved = false;
        }

        if (BINARY_MODE.equals(mode)) {
            isThrowsRemoved = true;
        }
        MemberType.setMode(BINARY_MODE.equals(mode));

        if (isValueTracked.booleanValue() && !in.hasFeature(FeaturesHolder.ConstInfo)) {
            String errmsg = i18n.getString("SignatureTest.mesg.sigfile.noconst");
            log.println(errmsg);
            return failed(errmsg);
        }

        //  If sigfile doesn't contain constant values, constant checking
        //  is impossible
        if (!in.hasFeature(FeaturesHolder.ConstInfo)) {
            isValueTracked = Boolean.FALSE;
        }

        SigTest.isConstantValuesTracked = isValueTracked.booleanValue();
        FieldDescr.isConstantValuesTracked = SigTest.isConstantValuesTracked;

        if (msg.length() != 0) {
            in.close();
            log.println(msg);
            return error(msg);
        }
        log.println(i18n.getString("SignatureTest.mesg.sigtest.report"));
        log.println(i18n.getString("SignatureTest.mesg.sigtest.basevers", in.getApiVersion()));
        log.println(i18n.getString("SignatureTest.mesg.sigtest.testvers", apiVersion));

        if (!isThrowsRemoved)
            log.println(i18n.getString("SignatureTest.mesg.sigtest.checkmode.norm", mode));
        else
            log.println(i18n.getString("SignatureTest.mesg.sigtest.checkmode.removed", mode));


        if (isValueTracked.booleanValue())
            log.println(i18n.getString("SignatureTest.mesg.sigtest.constcheck", i18n.getString("SignatureTest.mesg.sigtest.constcheck.on")));
        else
            log.println(i18n.getString("SignatureTest.mesg.sigtest.constcheck", i18n.getString("SignatureTest.mesg.sigtest.constcheck.off")));

        if (!isTigerFeaturesTracked)
            log.println(i18n.getString("SignatureTest.mesg.sigtest.tigercheck"));

        log.println();

        classpath.printErrors(log);


        trackedClassNames = new HashSet();

        ClassDescriptionLoader loader = getClassDescrLoader();

        classHierarchy = new ClassHierarchyImpl(loader, trackMode);

        builder = new MemberCollectionBuilder(this);

        signatureClassesHierarchy = new ClassHierarchyImpl(in, trackMode);


        // creates ErrorFormatter.
        if ((outFormat != null) && FORMAT_PLAIN.equals(outFormat))
            errorManager = new ErrorFormatter(log);
        else
            if ((outFormat != null) && FORMAT_HUMAN.equals(outFormat))
                errorManager = new HumanErrorFormatter(log, isVerbose,
                        reportWarningAsError ? Level.WARNING : Level.SEVERE );
        else
            if ((outFormat != null) && FORMAT_BACKWARD.equals(outFormat))
                errorManager = new BCProcessor(log, isVerbose, BINARY_MODE.equals(mode),
                        classHierarchy, signatureClassesHierarchy,
                        reportWarningAsError ? Level.WARNING : Level.SEVERE );
        else
            errorManager = new SortedErrorFormatter(log, isVerbose);


        boolean buildMembers = in.hasFeature(FeaturesHolder.BuildMembers);
        MemberCollectionBuilder b = null;
        if (buildMembers) {
            b = new MemberCollectionBuilder(this);
        }

        //  Reading the sigfile: main loop.

        msg = null;

        Erasurator erasurator = new Erasurator();

        try {

            // check that set of classes is transitively closed
            ClassSet closedSet = new ClassSet(signatureClassesHierarchy, true);

            in.rewind();
            ClassDescription currentClass;
            while ((currentClass = in.nextClass()) != null) {
                closedSet.addClass(currentClass.getQualifiedName());
            }

            Set missingClasses = closedSet.getMissingClasses();
            if (!missingClasses.isEmpty()) {

                log.print(i18n.getString("SignatureTest.error.required_classes_missing"));
                int count = 0;
                for (Iterator it = missingClasses.iterator(); it.hasNext();) {
                    if (count != 0)
                        log.print(", ");
                    log.print(it.next());
                    ++count;
                }
                log.println();

                msg = i18n.getString("SignatureTest.error.non_transitively_closed_set");
                return error(msg);
            }


            in.rewind();

            while ((currentClass = in.nextClass()) != null) {
                if (Xverbose) {
                    log.println(i18n.getString("SignatureTest.mesg.verbose.check", currentClass.getQualifiedName()));
                }

                if (buildMembers) {
                    try {
                        b.createMembers(currentClass, true, false, true);
                    } catch (ClassNotFoundException e) {
                        if (SigTest.debug)
                            e.printStackTrace();
                    }
                }

                if (useErasurator())
                    currentClass = erasurator.erasure(currentClass);

                Transformer t = PluginAPI.BEFORE_TEST.getTransformer();
                if (t != null) {
                    try {
                        t.transform(currentClass);
                    } catch (ClassNotFoundException e) {
                        if (SigTest.debug)
                            e.printStackTrace();
                    }
                }

                if (currentClass.isPackageInfo() && isTigerFeaturesTracked) {
                    verifyPackageInfo(currentClass);
                } else {
                    verifyClass(currentClass);
                }
                // save memory
                currentClass.setMembers(null);
            }

        }
        catch (OutOfMemoryError e) {
            msg = i18n.getString("SignatureTest.error.sigfile.oome");
        }
        catch (StackOverflowError e) {
            msg = i18n.getString("SignatureTest.error.sigfile.soe");
        }
        catch (VirtualMachineError e) {
            msg = i18n.getString("SignatureTest.error.sigfile.vme", e.getMessage());
        }
        catch (IOException e) {
            if (SigTest.debug)
                e.printStackTrace();
            msg = i18n.getString("SignatureTest.error.sigfile.prob") + linesep + e;
        }
        catch (SecurityException e) {
            if (SigTest.debug)
                e.printStackTrace();
            msg = i18n.getString("SignatureTest.error.sigfile.sec") + linesep + e;
        }
        catch (Error e) {
            if (SigTest.debug)
                e.printStackTrace();
            msg = i18n.getString("SignatureTest.error.unknownerror") + e;
        }

        if (msg != null) {
            in.close();
            log.println(msg);
            return error(msg);
        }


        //  Finished - the sigfile closed.

        if (!isSupersettingEnabled)
            checkAddedClasses();

        if (isTigerFeaturesTracked)
            checkAddedPackages();


        int auxErrorCount = 0;
        errorManager.printErrors();
        if (reportWarningAsError) {
            auxErrorCount = errorMessages.size();
            printErrors();
        }
        log.println("");

        String repmsg = exclude.report();
        if (isVerbose) System.out.println(repmsg);

        int errors = errorManager.getNumErrors() + auxErrorCount;
        in.close();
        if (errors == 0)
            return passed();
        else
            return failed(i18n.getString("SignatureTest.mesg.failed",
                    Integer.toString(errors)));

    }

    /**
     * Check if packages being tested do not contain any extra class,
     * which is not described in the <code>signatureFile</code>.
     * For each extra class detected, error message is appended to
     * the <code>log</code>.
     *
     * @see #log
     */
    private void checkAddedClasses() {
        //check that new classes are not added to the tracked packages.

        if (classpath == null)
            return;

        try {
            String name;
            while (classpath.hasNext()) {
                name = classpath.nextClassName();
                // Check that class isn't tracked and this class is
                // accessible in the current tested mode
                checkAddedClass(name);
            }
        } catch (SecurityException ex) {
            if (SigTest.debug)
                ex.printStackTrace();
            log.println(i18n.getString("SignatureTest.mesg.classpath.sec"));
            log.println(ex);
        }
    }

    private void checkAddedClass(String name) {
        if (!trackedClassNames.contains(name) && isPackageMember(name)) {
            try {
                ClassDescription c = classHierarchy.load(name);
                if (c.isPackageInfo()) {
                    if (isTigerFeaturesTracked)
                        checkAnnotations(null, c);
                } else {
                    if (classHierarchy.isAccessible(c)) {
                        exclude.check(c, c);
                        errorManager.addError(MessageType.getAddedMessageType(c.getMemberType()), c.getQualifiedName(), c.getMemberType(), null, c);
                    }
                }
            } catch (ClassNotFoundException ex) {
                if (SigTest.debug)
                    ex.printStackTrace();
            } catch (LinkageError ex1) {
                if (SigTest.debug)
                    ex1.printStackTrace();
            } catch (ExcludeException e) {
                if (isVerbose)
                    SigTest.log.println(i18n.getString("SignatureTest.mesg.verbose.checkAddedClass", new Object[]{name, e.getMessage()}));
            }

        }
    }

    private void checkAddedPackages() {
        List wrk = new ArrayList();

        for (Iterator it = trackedClassNames.iterator(); it.hasNext();) {
            String pkg = ClassDescription.getPackageName((String) it.next());
            if (!wrk.contains(pkg))
                wrk.add(pkg);
        }

        Collections.sort(wrk);

        for (Iterator it = wrk.iterator(); it.hasNext();) {
            String fqn = ClassDescription.getPackageInfo((String) it.next());

            if (!trackedClassNames.contains(fqn)) {
                try {
                    ClassDescription c = classHierarchy.load(fqn);
                    checkAnnotations(null, c);
                }
                catch (Throwable e) {
                    // ignore because .package-info may not exist!
                }
            }
        }

    }

    /**
     * Check if the <code>required</code> class described in signature file
     * also presents (and is public or protected) in the API being tested.
     * If this method fails to findByName that class in the API being tested,
     * it appends corresponding message to the errors <code>log</code>.
     *
     * @return <code>Status.failed("...")</code> if security exception
     *         occurred; or <code>Status.passed("")</code> otherwise.
     * @see #log
     */
    private boolean verifyClass(ClassDescription required) {
        // checks that package from tested API

        String name = required.getQualifiedName();

        if (!isPackageMember(name))
            return passed();

        try {
            exclude.check(required, required);
            ClassDescription found = classHierarchy.load(name);

            if (classHierarchy.isAccessible(found)) {

                builder.createMembers(found, true, true, false);

                Transformer t = PluginAPI.BEFORE_TEST.getTransformer();
                if (t != null)
                    t.transform(found);

                if (isThrowsRemoved) {
                    required.removeThrows();
                    found.removeThrows();
                } else {
                    normalizer.normThrows(found, true);
                }

                if (useErasurator()) {
                    found = erasurator.erasure(found);
                } else if (FORMAT_BACKWARD.equals(outFormat)) {
                    if (!hasClassParameter(required) && hasClassParameter(found)) {
                        found = erasurator.erasure(found);
                        required = erasurator.erasure(required); 
                    }
                }

                verifyClass(required, found);

            } else
                errorManager.addError(MessageType.MISS_CLASSES, name, MemberType.CLASS, null, required);
        }

        catch (ClassNotFoundException ex) {
            if (SigTest.debug)
                ex.printStackTrace();
            errorManager.addError(MessageType.MISS_CLASSES, name, MemberType.CLASS, null, required);
        } catch (LinkageError er) {

            if (SigTest.debug) {
                er.printStackTrace();
            }

            errorManager.addError(MessageType.ERROR_LINKERR, name, MemberType.CLASS,
                    i18n.getString("SignatureTest.mesg.linkerr.thrown", er),
                    i18n.getString("SignatureTest.mesg.linkerr.notlink", name), required);

            trackedClassNames.add(name);
        }
        catch (ExcludeException e) {
            trackedClassNames.add(name);
            if (isVerbose)
                SigTest.log.println(i18n.getString("SignatureTest.mesg.verbose.verifyClass", new Object[]{name, e.getMessage()}));
        }
        return passed();
    }

    private boolean hasClassParameter(ClassDescription cl) {
        String tp = cl.getTypeParameters();
        boolean result = (tp != null) && (!"".equals(tp));
        // check all the members also
        if (!result) {
            for (Iterator e = cl.getMembersIterator(); e.hasNext();) {
                MemberDescription mr = (MemberDescription) e.next();
                String tpM = mr.getTypeParameters();
                if ((tpM != null) && (!"".equals(tpM))) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }


    private void verifyPackageInfo(ClassDescription required) {

        assert (isTigerFeaturesTracked);

        // checks that package from tested API
        String name = required.getQualifiedName();
        if (!isPackageMember(name))
            return;

        trackedClassNames.add(name);
        ClassDescription found = null;
        try {
            found = classHierarchy.load(name);
            //loader.createMembers(found);
        }
        catch (Exception e) {
            //  just ignore it ...
        }

        checkAnnotations(required, found);
    }

    private void excluded(ClassDescription testedClass, MemberDescription md) throws ExcludeException {
        if (md != null) {
            if (md.isField() || md.isMethod() || md.isConstructor() || md.isInner()) {
                exclude.check(testedClass, md);
            }
        }
    }


    /**
     * Compare descriptions of the <code>required</code> and <code>found</code> classes.
     * It is assumed, that description for the <code>required</code> class is read
     * from signature file, and the <code>found</code> description belongs to that
     * API being tested. If the descriptions compared are not equal to each other
     * (class names differ, or there are different sets of public members in them),
     * this method appends corresponding error messages to the <code>log</code>-file.
     * Note, that equality of class or member names may do not imply that they have
     * the same <code>static</code> and <code>protected</code> attributes, and
     * <code>throws</code> clause, if the chosen <code>converter</code> enables
     * weaker equivalence.
     *
     * @see #log
     */
    private void verifyClass(ClassDescription required, ClassDescription found) {

        // adds class name to the table of the tracked classes.
        trackedClassNames.add(found.getQualifiedName());

        if (errorManager instanceof SortedErrorFormatter)
            ((SortedErrorFormatter) errorManager).Tested(found);

        // track class modifiers
        checkClassDescription(required, found);

        // track members declared in the signature file.
        for (Iterator e = required.getMembersIterator(); e.hasNext();) {
            MemberDescription requiredMember = (MemberDescription) e.next();
            try {
                excluded(required, requiredMember);
                trackMember(required, found, requiredMember, found.findMember(requiredMember));
            } catch (ExcludeException e1) {
                if (isVerbose)
                    SigTest.log.println(i18n.getString("SignatureTest.mesg.verbose.verifyMember",
                            new Object[]{required.getQualifiedName(),
                                    requiredMember.toString(),
                                    e1.getMessage()}));
            }
        }

        // track members which are added in the current implementation.
        if (!isSupersettingEnabled) {
            for (Iterator e = found.getMembersIterator(); e.hasNext();) {
                MemberDescription foundMember = (MemberDescription) e.next();
                if (!required.containsMember(foundMember)) {
                    try {
                        excluded(found, foundMember);
                        trackMember(required, found, null, foundMember);
                    } catch (ExcludeException e1) {
                        if (isVerbose)
                            SigTest.log.println(i18n.getString("SignatureTest.mesg.verbose.verifyMember2",
                                    new Object[]{found.getQualifiedName(),
                                            foundMember.toString(),
                                            e1.getMessage()}));
                    }
                }
            }
        }
    }

    /**
     * Compare names of the <code>required</code> and <code>found</code> classes.
     * It is assumed, that description for the <code>required</code> class is read
     * from signature file, and the <code>found</code> description belongs to that
     * API being tested. If the descriptions compared are not equal to each other,
     * this method appends corresponding error messages to the <code>log</code>-file.
     * Note, that equality of descriptions may do not imply that they have the same
     * <code>static</code> and <code>protected</code> attributes, if the chosen
     * <code>converter</code> enables weaker equivalence.
     *
     * @see #log
     */
    private void checkClassDescription(ClassDescription required, ClassDescription found) {

        checkAnnotations(required, found);

        if (!required.isCompatible(found)) {
            errorManager.addError(MessageType.MISS_CLASSES,
                    required.getQualifiedName(), MemberType.CLASS, required.toString(), required);
            errorManager.addError(MessageType.ADD_CLASSES,
                    found.getQualifiedName(), MemberType.CLASS, found.toString(), found);
        }
    }

    private MemberDescription transformMember(ClassDescription parent, MemberDescription member) {
        MemberDescription clonedMember = member;

        if (parent.hasModifier(Modifier.FINAL) &&
                member.isMethod() &&
                member.getDeclaringClassName().equals(parent.getQualifiedName())) {

            MethodDescr md = (MethodDescr) member;
            // below is a fix for issue 21
            try {
                if (!member.hasModifier(Modifier.FINAL)) {
                    if (!classHierarchy.isMethodOverriden(md)) {
                        clonedMember = (MemberDescription) member.clone();
                        clonedMember.addModifier(Modifier.FINAL);
                    }
                } else {
                    if (classHierarchy.isMethodOverriden(md)) {
                        clonedMember = (MemberDescription) member.clone();
                        clonedMember.removeModifier(Modifier.FINAL);
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            // end of fix
        }

        if (BINARY_MODE.equals(mode) && member.isMethod() && member.hasModifier(Modifier.STATIC) && member.hasModifier(Modifier.FINAL)) {
            clonedMember = (MemberDescription) member.clone();
            clonedMember.removeModifier(Modifier.FINAL);
        }


        return clonedMember;
    }


    /**
     * Compare the <code>required</code> and <code>found</code> sets of class members
     * having the same signature <code>name</code>. It is assumed, that the
     * <code>required</code> description was read from signature file, and
     * the <code>found</code> description belongs to the API being tested. If these
     * two member descriptions are not equal to each other, this method appends
     * corresponding error messages to the <code>log</code>-file.
     *
     * @param parentReq ClassDesription for contained class from required set
     * @param parentFou ClassDesription for contained class from found set
     * @param required  the required field
     * @param found     the field (or lack thereof) which is present
     * @see #log
     */
    private void trackMember(ClassDescription parentReq, ClassDescription parentFou, MemberDescription required, MemberDescription found) {
        // note: this method is also used to print out an error message
        //       when the implementation being tested has extra fields.
        //       the third parameter is null in this case
        String name = parentReq.getQualifiedName();

        if (required != null) {
            required = transformMember(parentReq, required);
        }

        if (found != null) {
            found = transformMember(parentFou, found);
        }

        if (required != null && found != null) {

            checkAnnotations(required, found);

            // element matching is basically equality of the signature.
            // the signature can be changed depending on the particular
            // levels of enforcement being used (e.g. include constant values
            // or not)

            if (required.isCompatible(found))
                return;                  // OK

            // one way constant checking if constant values don't match
            if (isOneWayConstantChecking && required.isField() ) {

                assert found.isField();

                String constantValue = ((FieldDescr) required).getConstantValue();
                if (constantValue == null && ((FieldDescr) found).getConstantValue() != null &&
                        ((FieldDescr) required).isCompatible(found, true))
                    return;     // OK
            }
        }

        if (required != null)
            errorManager.addError(MessageType.getMissingMessageType(required.getMemberType()), name, required.getMemberType(), required.toString(), required);

        if (!isSupersettingEnabled && found != null)
            errorManager.addError(MessageType.getAddedMessageType(found.getMemberType()), name, found.getMemberType(), found.toString(), found);
    }


    protected AnnotationItem[] removeUndocumentedAnnotations(AnnotationItem[] annotations, ClassHierarchy h) {

        if (annotations == null)
            return AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY;


        int len = annotations.length;

        AnnotationItem[] tempStorage = new AnnotationItem[len];

        if (len == 0)
            return annotations;

        int count = 0;

        for (int i = 0; i < len; ++i) {
            boolean documented = true;

            try {
                documented = h.isDocumentedAnnotation(annotations[i].getName());
            } catch (ClassNotFoundException e) {
                // suppress
            }

            if (documented) {
                tempStorage[count++] = annotations[i];
            }

        }

        if (count == len)
            return annotations;   // nothing to do

        AnnotationItem[] documentedAnnotations = AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY;

        if (count != 0) {
            documentedAnnotations = new AnnotationItem[count];
            System.arraycopy(tempStorage, 0, documentedAnnotations, 0, count);
        }

        return documentedAnnotations;
    }

    private void checkAnnotations(MemberDescription base, MemberDescription test) {

        if (!isTigerFeaturesTracked)
            return;

        AnnotationItem[] baseAnnotList = base == null ? AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY :
                removeUndocumentedAnnotations(base.getAnnoList(), signatureClassesHierarchy);

        AnnotationItem[] testAnnotList = test == null ? AnnotationItem.EMPTY_ANNOTATIONITEM_ARRAY :
                removeUndocumentedAnnotations(test.getAnnoList(), classHierarchy);

        if (baseAnnotList.length == 0 && testAnnotList.length == 0)
            return;

        // NOTE: getAnnoList() always returns sorted annotations array!

        int bl = baseAnnotList.length;
        int tl = testAnnotList.length;
        int bPos = 0;
        int tPos = 0;

        while ((bPos < bl) && (tPos < tl)) {
            int comp = baseAnnotList[bPos].compareTo(testAnnotList[tPos]);
            if (comp < 0) {
                reportError(base, baseAnnotList[bPos].toString(), false);
                bPos++;
            } else {
                if (comp > 0) {
                    reportError(test, testAnnotList[tPos].toString(), true);
                    tPos++;
                } else {
                    tPos++;
                    bPos++;
                }
            }
        }
        while (bPos < bl) {
            reportError(base, baseAnnotList[bPos].toString(), false);
            bPos++;
        }
        while (tPos < tl) {
            reportError(test, testAnnotList[tPos].toString(), true);
            tPos++;
        }
    }

    private void reportError(MemberDescription fid, String anno, boolean added) {
        if (fid != null)
            errorManager.addError(added ? MessageType.ADD_ANNO : MessageType.MISS_ANNO, fid.getQualifiedName(), fid.getMemberType(), anno, fid);
    }

    /**
     * This class is used to store excluded signatures.
     */
    static class DefaultExcludeList implements Exclude {

        public DefaultExcludeList() {

        }


        /* (non-Javadoc)
        * @see com.sun.tdk.signaturetest.core.Exclude#check(java.lang.String)
        */
        public void check(ClassDescription testedClassName, MemberDescription signature) throws ExcludeException {
        }

        /* (non-Javadoc)
        * @see com.sun.tdk.signaturetest.core.Exclude#parseParameters(java.util.Vector)
        */
        public String[] parseParameters(String[] args) {
            return args;
        }


        /* (non-Javadoc)
        * @see com.sun.tdk.signaturetest.core.Exclude#report()
        */
        public String report() {
            return null;
        }

    }
}
