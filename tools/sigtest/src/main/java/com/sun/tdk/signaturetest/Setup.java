/*
 * $Id: Setup.java 4504 2008-03-13 16:12:22Z sg215604 $
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
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.MemberDescription;
import com.sun.tdk.signaturetest.model.MemberType;
import com.sun.tdk.signaturetest.sigfile.FeaturesHolder;
import com.sun.tdk.signaturetest.sigfile.FileManager;
import com.sun.tdk.signaturetest.sigfile.Writer;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;
import com.sun.tdk.signaturetest.util.OptionInfo;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * This class creates signature file. The classes in the
 * signature file are grouped by type, and alphabetized by class name.<br>
 * The following signature files could be created:
 * <p/>
 * Usage: java com.sun.tdk.signaturetest.setup.Setup &lt;options&gt;<p>
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
 * <dt><code><b>-Package</b></code> &lt;package name&gt;
 * <dd> Package which are needed to be tracked (several options can be specified).
 * <p/>
 * <dt><code><b>-PackageWithoutSubpackages</b></code> &lt;package&gt;
 * <dd> Name of the package, which is to be tracked itself excluding its subpackages.
 * Such option should be included for each package required to be tracked
 * excluding subpackages.
 * <p/>
 * <dt><code><b>-Exclude</b></code> &lt;package or class name&gt;
 * <dd> package or class which is not needed
 * to be tracked.(several options can be specified)
 * <p/>
 * <dt><code><b>-static</b></code>
 * <dd> Track in the static mode. In this mode test uses class
 * file parsing instead of the reflection for. The path specified by
 * -Classpath options is required in this mode.
 * <p/>
 * <dt><code><b>-CheckValue</b></code>
 * <dd> Writes values of the primitive constants in signature file.
 * This options could be used in the static mode only.
 * <p/>
 * <dt><code><b>-AllPublic</b></code>
 * <dd> track unaccessible nested classes
 * (I.e. which are public or protected but are members of default
 * or private access class).
 * <p/>
 * <dt><code><b>-Classpath</b></code> &lt;path&gt;
 * <dd> specify the path, which includes tracked classes.
 * <p/>
 * <dt><code><b>-Version</b></code> &lt;version&gt;
 * <dd> Specify API version. If this parameter is not specified, API version is assumed to
 * be that reported by <code>getProperty("java.version")</code>.
 * <p/>
 * <dt><code><b>-Verbose</b></code>
 * <dd> Print names of ignored classes.
 * </dl>
 *
 * @author Maxim Sokolnikov
 * @author Serguei Ivashin
 * @author Mikhail Ershov
 */
public class Setup extends SigTest {

    // Setup specific options
    public static final String CLOSEDFILE_OPTION = "-ClosedFile";
    public static final String NONCLOSEDFILE_OPTION = "-NonClosedFile";
    public static final String CHECKVALUE_OPTION = "-CheckValue";
    public static final String XGENCONSTS_OPTION = "-XgenConsts";


    // -KeepFile option keeps signature file even if some error occured during setup
    // needs for compatibility between 2.0 and 2.1
    public static final String KEEP_SIGFILE_OPTION = "-KeepFile";


    // This option is used only for debugging purposes. It's not recommended
    // to use it to create signature files for production!
    public static final String XREFLECTION_OPTION = "-Xreflection";

    /**
     * contains signature file.
     */
    protected URL signatureFile;
    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(Setup.class);

    protected boolean isClosedFile = true;
    private Boolean explicitlyGenConsts = null;
    private boolean keepSigFile = false;

    /**
     * specifies that ignored class names will be reported.
     */
    protected boolean isIgnorableReported;

    /**
     * runs test in from command line.
     */
    public static void main(String[] args) {
        Setup t = new Setup();
        t.run(args, new PrintWriter(System.err, true), null);
        t.exit();
    }

    /**
     * runs test with the given arguments.
     */
    public void run(String[] args, PrintWriter pw, PrintWriter ref) {

//        assert( pw != null );
        setLog(pw);

        outerClassesNumber = 0;
        innerClassesNumber = 0;
        includedClassesNumber = 0;
        excludedClassesNumber = 0;

        MemberType.setMode(false);

        if (parseParameters(args)) {
            afterParseParameters();
            create(signatureFile);
            getLog().flush();
        } else
            if (args.length > 0 && args[0].equalsIgnoreCase(VERSION_OPTION))  {
                pw.println(Version.getVersionInfo());
            } else {
                usage();
            }
    }

    /**
     * parses parameters and initialize fields as specified by arguments
     *
     * @param args contains arguments required to be parsed.
     */
    protected boolean parseParameters(String[] args) {

        CommandLineParser parser = new CommandLineParser(this, "-");

        // Print help text only and exit.
        if (args == null || args.length == 0 ||
                (args.length == 1 && (parser.isOptionSpecified(args[0], HELP_OPTION) 
                || parser.isOptionSpecified(args[0], QUESTIONMARK)
                || parser.isOptionSpecified(args[0], VERSION_OPTION) ))) {
            return false;
        }

        final String optionsDecoder = "decodeOptions";

        parser.addOption(PACKAGE_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
        parser.addOption(CLASSPATH_OPTION, OptionInfo.requiredOption(1), optionsDecoder);
        parser.addOption(USE_BOOT_CP, OptionInfo.optionVariableParams(0, 1), optionsDecoder);
        parser.addOption(FILENAME_OPTION, OptionInfo.requiredOption(1), optionsDecoder);

        parser.addOption(TESTURL_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(WITHOUTSUBPACKAGES_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
        parser.addOption(EXCLUDE_OPTION, OptionInfo.optionVariableParams(1, OptionInfo.UNLIMITED), optionsDecoder);
        parser.addOption(APIVERSION_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(STATIC_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(CLASSCACHESIZE_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(DEBUG_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(XNOTIGER_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(XVERBOSE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(VERBOSE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);


        parser.addOption(CLOSEDFILE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(NONCLOSEDFILE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(KEEP_SIGFILE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(CHECKVALUE_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(XGENCONSTS_OPTION, OptionInfo.option(1), optionsDecoder);
        parser.addOption(XREFLECTION_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(ALLPUBLIC_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(HELP_OPTION, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(QUESTIONMARK, OptionInfo.optionalFlag(), optionsDecoder);
        parser.addOption(VERSION_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        parser.addOption(PLUGIN_OPTION, OptionInfo.option(1), optionsDecoder);

        parser.addOption(ERRORALL_OPTION, OptionInfo.optionalFlag(), optionsDecoder);

        try {
            parser.processArgs(args);
        }
        catch (CommandLineParserException e) {
            getLog().println(e.getMessage());
            return failed(e.getMessage());
        }

        // since 2.1 - static mode by default
        isStatic = true;

        //if (!parser.isOptionSpecified(XREFLECTION_OPTION) && !parser.isOptionSpecified(STATIC_OPTION))
        //    return error(i18n.getString("Setup.error.mode.notspecified", new Object[]{STATIC_OPTION, XREFLECTION_OPTION}));

        // TODO plugin may set own loader. so these options have no sense in this case
        if (parser.isOptionSpecified(XREFLECTION_OPTION) && !parser.isOptionSpecified(STATIC_OPTION))
            isStatic = false;

        if (parser.isOptionSpecified(NONCLOSEDFILE_OPTION) && parser.isOptionSpecified(CLOSEDFILE_OPTION))
            return error(i18n.getString("Setup.error.mode.contradict", new Object[]{NONCLOSEDFILE_OPTION, CLOSEDFILE_OPTION}));

        // create arguments
        if (packages.isEmpty() && purePackages.isEmpty())
            packages.addPackage("");


        if (sigFileName == null)
            return error(i18n.getString("Setup.error.filename.missing"));

        if (parser.isOptionSpecified(TESTURL_OPTION)) {
            if (new File(sigFileName).isAbsolute())
                return error(i18n.getString("Setup.error.testurl.absolutepath", new Object[]{TESTURL_OPTION, sigFileName}));
        }

        try {
            signatureFile = FileManager.getURL(testURL, sigFileName);
        }
        catch (MalformedURLException e) {
            if (SigTest.debug)
                e.printStackTrace();
            System.err.println(e);
            return error(i18n.getString("Setup.error.url.invalid"));
        }

        if (classpathStr == null)
            return error(i18n.getString("Setup.error.arg.unspecified", CLASSPATH_OPTION));

        return passed();
    }


    public void decodeOptions(String optionName, String[] args) throws CommandLineParserException {
        if (optionName.equalsIgnoreCase(CLOSEDFILE_OPTION)) {
            isClosedFile = true;
        } else if (optionName.equalsIgnoreCase(NONCLOSEDFILE_OPTION)) {
            isClosedFile = false;
        } else if (optionName.equalsIgnoreCase(KEEP_SIGFILE_OPTION)) {
            keepSigFile = true;
        } else if (optionName.equalsIgnoreCase(CHECKVALUE_OPTION)) {
            // do nothing, just for back. comp.
        } else if (optionName.equalsIgnoreCase(XGENCONSTS_OPTION)) {
            String v = args[0];
            if ("on".equalsIgnoreCase(v)) {
                explicitlyGenConsts = Boolean.TRUE;
            } else if ("off".equalsIgnoreCase(v)) {
                explicitlyGenConsts = Boolean.FALSE;
            } else
                throw new CommandLineParserException(i18n.getString("Setup.error.arg.invalidval", XGENCONSTS_OPTION));
        } else if (optionName.equalsIgnoreCase(VERBOSE_OPTION)) {
            isIgnorableReported = true;
        } else if (optionName.equalsIgnoreCase(XREFLECTION_OPTION)) {
            isStatic = false;
        } else {
            super.decodeCommonOptions(optionName, args);
        }
    }


    private void afterParseParameters() {
        SigTest.isConstantValuesTracked = isStatic;
        if (explicitlyGenConsts != null)
            SigTest.isConstantValuesTracked = explicitlyGenConsts.booleanValue();
    }


    /**
     * Prints help text.
     */
    protected void usage() {
        String nl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();

        sb.append(getComponentName() + " - " + i18n.getString("Setup.usage.version", Version.Number));
        sb.append(nl).append(i18n.getString("Setup.usage.start"));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("Setup.usage.classpath", CLASSPATH_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.package", PACKAGE_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.filename", FILENAME_OPTION));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));

        sb.append(nl).append(i18n.getString("Setup.usage.testurl", TESTURL_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.packagewithoutsubpackages", WITHOUTSUBPACKAGES_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.exclude", EXCLUDE_OPTION));
        // sb.append(nl).append(i18n.getString("Setup.usage.static", STATIC_OPTION));
        // sb.append(nl).append(i18n.getString("Setup.usage.closedfile", Setup.CLOSEDFILE_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.nonclosedfile", NONCLOSEDFILE_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.apiversion", APIVERSION_OPTION));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("Setup.usage.verbose", VERBOSE_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.debug", DEBUG_OPTION));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("Setup.helpusage.version", VERSION_OPTION));
        sb.append(nl).append(i18n.getString("Setup.usage.help", HELP_OPTION));
        sb.append(nl).append(i18n.getString("Sigtest.usage.delimiter"));
        sb.append(nl).append(i18n.getString("Setup.usage.end"));
        System.err.println(sb.toString());
    }

    protected String getComponentName() {
        return "Setup";
    }

    private int outerClassesNumber = 0,
            innerClassesNumber = 0,
            includedClassesNumber = 0,
            excludedClassesNumber = 0;


    /**
     * creates signature file.
     */
    private boolean create(URL sigFile) {
        initErrors();

        if (pluginClass != null) {
            pluginClass.init(this);
        }

        // create list of all classes available

        HashSet allClasses = new HashSet();

        getLog().println(i18n.getString("Setup.log.classpath", classpathStr));

        try {
            classpath = new ClasspathImpl(release, classpathStr);
        } catch (SecurityException e) {
            if (SigTest.debug)
                e.printStackTrace();
            getLog().println(i18n.getString("Setup.log.invalid.security.classpath"));
            getLog().println(e);
            return error(i18n.getString("Setup.log.invalid.security.classpath"));
        }

        classpath.printErrors(getLog());

        String name;
        while (classpath.hasNext()) {
            name = classpath.nextClassName();
            if (!allClasses.add(name))
                getLog().println(i18n.getString("Setup.log.duplicate.class", name));
        }

        classpath.setListToBegin();

        ClassDescriptionLoader testableLoader = getClassDescrLoader();
        testableHierarchy = new ClassHierarchyImpl(testableLoader, trackMode);
        testableMCBuilder = new MemberCollectionBuilder(this);

        // adds classes which are member of classes from tracked package
        // and sorts class names
        getLog().println(i18n.getString("Setup.log.constantchecking",
                (SigTest.isConstantValuesTracked ? i18n.getString("Setup.msg.ConstantValuesTracked.on")
                        : i18n.getString("Setup.msg.ConstantValuesTracked.off"))));
        getLog().println(i18n.getString("Setup.log.message.numclasses", Integer.toString(allClasses.size())));


        List sortedClasses;
        Collection packageClasses = getPackageClasses(allClasses);


        if (isClosedFile) {
            ClassSet closedSetOfClasses = new ClassSet(testableHierarchy, true);

            // add all classes including non-accessible
            for (Iterator i = packageClasses.iterator(); i.hasNext();) {
                name = (String) i.next();
                closedSetOfClasses.addClass(name);
            }
            // remove not accessible classes

            Set invisibleClasses = new HashSet();
            Set classes = closedSetOfClasses.getClasses();
            for (Iterator i = classes.iterator(); i.hasNext();) {

                name = (String) i.next();
                ClassDescription c = load(name);

                if (!testableHierarchy.isAccessible(c))
                    invisibleClasses.add(name);
            }

            for (Iterator i = invisibleClasses.iterator(); i.hasNext();) {
                closedSetOfClasses.removeClass((String) i.next());
            }

            sortedClasses = sortClasses(closedSetOfClasses.getClasses());
        } else {
            sortedClasses = sortClasses(packageClasses);
        }

        SortedSet excludedClasses = new TreeSet();


        try {
            //write header to the signature file
            Writer writer = getFileManager().getDefaultFormat().getWriter();
            writer.init(new PrintWriter(new OutputStreamWriter(new FileOutputStream(sigFile.getFile()), "UTF8")));

            writer.setApiVersion(apiVersion);
            if (SigTest.isConstantValuesTracked)
                writer.addFeature(FeaturesHolder.ConstInfo);

            if (isTigerFeaturesTracked)
                writer.addFeature(FeaturesHolder.TigerInfo);

            writer.writeHeader();

            Erasurator erasurator = new Erasurator();
            // scan class and writes definition to the signature file

            // 1st analyze all the classes
            for (Iterator i = sortedClasses.iterator(); i.hasNext();) {
                name = (String) i.next();
                ClassDescription c = load(name);

                if (!testableHierarchy.isAccessible(c))
                    continue;

                // do not write excluded classes
                if (excludedPackages.checkName(name)) {
                    excludedClasses.add(name);
                    continue;
                }

                // TODO - ersh - change this!!!
                if (name.indexOf('$') < 0)
                    outerClassesNumber++;
                else
                    innerClassesNumber++;

                try {
                    testableMCBuilder.createMembers(c, addInherited(), true, false);
                    normalizer.normThrows(c, true);
                    removeUndocumentedAnnotations(c, testableHierarchy);
                } catch (ClassNotFoundException e) {
                    if (SigTest.debug) {
                        e.printStackTrace();
                    }
                    setupProblem(i18n.getString("Setup.error.message.classnotfound", e.getMessage()));
                }

                if (useErasurator())
                    c = erasurator.erasure(c);

                writer.write(c);
            }

            writer.close();
        }
        catch (IOException e) {
            if (SigTest.debug)
                e.printStackTrace();
            getLog().println(i18n.getString("Setup.error.message.cantcreatesigfile"));
            getLog().println(e);
            return error(i18n.getString("Setup.error.message.cantcreatesigfile"));
        }

        printErrors();

        // prints report

        getLog().println(i18n.getString("Setup.report.message.selectedbypackageclasses",
                Integer.toString(includedClassesNumber + excludedClassesNumber)));

        if (!excludedPackages.isEmpty())
            getLog().println(i18n.getString("Setup.report.message.excludedbypackageclasses",
                    Integer.toString(excludedClassesNumber)));

        // print warnings
        if (isClosedFile && excludedClasses.size() != 0) {

            boolean printHeader = true;

            for (Iterator it = excludedClasses.iterator(); it.hasNext();) {

                String clsName = (String) it.next();

                String[] subClasses = testableHierarchy.getDirectSubclasses(clsName);

                if (subClasses.length > 0) {

                    int count = 0;
                    for (int idx = 0; idx < subClasses.length; ++idx) {
                        if (!excludedClasses.contains(subClasses[idx])) {

                            if (count != 0)
                                getLog().print(", ");
                            else {
                                if (printHeader) {
                                    getLog().println(i18n.getString("Setup.log.message.exclude_warning_header"));
                                    printHeader = false;
                                }
                                getLog().println(i18n.getString("Setup.log.message.exclude_warning", clsName));
                            }

                            getLog().print(subClasses[idx]);
                            ++count;
                        }
                    }
                    getLog().println();
                }
            }
        }


        getLog().print(i18n.getString("Setup.report.message.outerclasses", Integer.toString(outerClassesNumber)));
        if (innerClassesNumber != 0)
            getLog().println(i18n.getString("Setup.report.message.innerclasses", Integer.toString(innerClassesNumber)));
        else
            getLog().println();

        if (errors == 0)
            return passed(outerClassesNumber == 0 ? i18n.getString("Setup.report.message.emptysigfile") : "");

        if (!keepSigFile) {
            new File(sigFile.getFile()).delete();
        }
        return failed(i18n.getString("Setup.report.message.numerrors", Integer.toString(errors)));
    }

    private void removeUndocumentedAnnotations(ClassDescription c, ClassHierarchy classHierarchy) {
        c.setAnnoList(removeUndocumentedAnnotations(c.getAnnoList(), classHierarchy));
        for (Iterator e = c.getMembersIterator(); e.hasNext();) {
            MemberDescription mr = (MemberDescription) e.next();
            mr.setAnnoList(removeUndocumentedAnnotations(mr.getAnnoList(), classHierarchy));
        }
    }

    /**
     * initialize table of the nested classes and returns Vector of the names
     * required to be tracked.
     */
    private Collection getPackageClasses(Collection classes) {
        HashSet packageClasses = new HashSet();
        int nonTigerCount = 0;

        // create table of the nested packageClasses.
        for (Iterator i = classes.iterator(); i.hasNext();) {
            String name = (String) i.next();

            if (isPackageMember(name)) {
                includedClassesNumber++;
                try {
                    ClassDescription c = testableHierarchy.load(name);
                    if (testableHierarchy.isAccessible(c)) {
                        packageClasses.add(name);
                        if (!c.isTiger()) {
                            nonTigerCount++;
                            if (Xverbose && isTigerFeaturesTracked)
                                getLog().println(i18n.getString("Setup.report.message.nontigerclass", name));
                        }
                    } else
                        ignore(i18n.getString("Setup.report.ignore.protect", name));
                }
                catch (ClassNotFoundException ex) {
                    if (SigTest.debug)
                        ex.printStackTrace();
                    setupProblem(i18n.getString("Setup.error.message.classnotfound", name));
                }
                catch (LinkageError ex1) {
                    if (SigTest.debug)
                        ex1.printStackTrace();
                    setupProblem(i18n.getString("Setup.error.message.classnotlinked", ex1.getMessage()));
                }
            } else {
                if (!excludedPackages.isEmpty() && excludedPackages.checkName(name))
                    excludedClassesNumber++;
                ignore(i18n.getString("Setup.report.ignore.notreqpackage", name));
            }
        }

        return packageClasses;
    }


    /**
     * returns list of the sorted classes.
     *
     * @param classes MemberCollection which stores occurred errors. *
     */

    private List sortClasses(Collection classes) {
        ArrayList retVal = new ArrayList();
        retVal.addAll(classes);
        Collections.sort(retVal);
        return retVal;
    }

    /**
     * ignore class with given message.
     *
     * @param message given message.
     */
    protected void ignore(String message) {
        if (isIgnorableReported)
            getLog().println(message);
    }


}


