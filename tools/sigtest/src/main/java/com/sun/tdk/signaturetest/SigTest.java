/*
 * $Id: SigTest.java 4549 2008-03-24 08:03:34Z me155718 $
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

import com.sun.tdk.signaturetest.classpath.Classpath;
import com.sun.tdk.signaturetest.classpath.ClasspathImpl;
import com.sun.tdk.signaturetest.classpath.Release;
import com.sun.tdk.signaturetest.core.*;
import com.sun.tdk.signaturetest.errors.ErrorFormatter;
import com.sun.tdk.signaturetest.model.AnnotationItem;
import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.plugin.*;
import com.sun.tdk.signaturetest.sigfile.FileManager;
import com.sun.tdk.signaturetest.sigfile.Format;
import com.sun.tdk.signaturetest.util.CommandLineParser;
import com.sun.tdk.signaturetest.util.CommandLineParserException;
import com.sun.tdk.signaturetest.util.I18NResourceBundle;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents core part of the signature tests.
 * It provides tools for parsing core parameters and defining core
 * attributes of the classes such as accessibility and appurtenance to
 * the required packages.
 * <p/>
 * <p>This class parses the following options core for signature tests:
 * <dl>
 * <dt><code>-Package</code> &lt;package&gt;
 * <dt><code>-PackageWithoutSubpackages</code> &lt;package&gt;
 * <dt><code>-Exclude</code> &lt;package_or_class_name&gt;
 * <dt><code>-Classpath</code> &lt;path&gt;
 * <dt><code>-APIversion</code> &lt;version&gt;
 * <dt><code>-static</code>
 * <dt><code>-ClassCacheSize</code> &lt;number&gt;
 * <dt><code>-AllPublic</code>
 * </dl>
 *
 * @author Maxim Sokolnikov
 * @author Serguei Ivashin
 * @author Mikhail Ershov
 * @version 05/04/06
 */
public abstract class SigTest extends Result implements PluginAPI, Log {

    // Command line options
    public static final String ALLPUBLIC_OPTION = "-AllPublic";
    public static final String CLASSPATH_OPTION = "-Classpath";
    public static final String USE_BOOT_CP = "-BootCp";
    public static final String PACKAGE_OPTION = "-Package";
    public static final String WITHOUTSUBPACKAGES_OPTION = "-PackageWithoutSubpackages";
    public static final String EXCLUDE_OPTION = "-Exclude";
    public static final String STATIC_OPTION = "-Static";
    public static final String APIVERSION_OPTION = "-ApiVersion";
    public static final String VERSION_OPTION = "-Version";
    public static final String DEBUG_OPTION = "-Debug";
    public static final String HELP_OPTION = "-Help";
    public static final String QUESTIONMARK = "-?";
    public static final String CLASSCACHESIZE_OPTION = "-ClassCacheSize";
    public static final String VERBOSE_OPTION = "-Verbose";
    public static final String XVERBOSE_OPTION = "-Xverbose";
    public static final String XNOTIGER_OPTION = "-XnoTiger";
    public static final String OUT_OPTION = "-Out";
    public static final String FORMATPLAIN_OPTION = "-FormatPlain";
    public static final String FORMATHUMAN_OPTION = "-FormatHuman";
    public static final String FORMATHUMAN_ALT_OPTION = "-H";
    public static final String BACKWARD_OPTION = "-Backward";
    public static final String BACKWARD_ALT_OPTION = "-B";
    public static final String EXTENSIBLE_INTERFACES_OPTION = "-ExtensibleInterfaces";
    public static final String FILENAME_OPTION = "-FileName";
    public static final String TESTURL_OPTION = "-TestURL";
    public static final String PLUGIN_OPTION = "-Plugin";
    public static final String ERRORALL_OPTION = "-ErrorAll";


    private static I18NResourceBundle i18n = I18NResourceBundle.getBundleForClass(SigTest.class);

    protected String testURL = "";

    protected String sigFileNameList = null;  // value of -Files option
    protected String sigFileName = null;   // value of -FileName option

    private FileManager fm = new FileManager();


    /**
     * Either equals to <code>ALL_PUBLIC</code>, or not.
     */
    protected int trackMode;

    /**
     * List of names of packages to be checked along with subpackages.
     */
    protected PackageGroup packages = new PackageGroup(true);

    /**
     * List of names of packages to be checked excluding subpackages.
     */
    protected PackageGroup purePackages = new PackageGroup(false);

    /**
     * List of names of packages to be ignored along with subpackages.
     */
    protected PackageGroup excludedPackages = new PackageGroup(true);

    /**
     * List of directories and/or zip-files containing the packages to be checked.
     *
     * @see java.io.File#pathSeparator
     */
    protected String classpathStr = null;

    /** search also boot classpath when a class is not found on classpath? */
    protected Release release;

    /**
     * Collector for error messages, or <code>null</code> if log is not required.
     */
    protected static ErrorFormatter errorManager;

    /**
     * Version of the product being tested.
     */
    protected String apiVersion = "";

    /**
     * Either static or reflections-based class descriptions finder.
     *
     * @see #isStatic
     */

    protected MemberCollectionBuilder testableMCBuilder;
    protected ThrowsNormalizer normalizer = new ThrowsNormalizer();

    protected boolean isStatic = false;

    public static boolean isConstantValuesTracked = true;


    public final static int DefaultCacheSize = 1024;
    /**
     * <b>BinaryClassDescrLoader</b> may cache up to <code>cacheSize</code>
     * classes being loaded.
     */
    protected int cacheSize = DefaultCacheSize;

    /**
     * prints error messages.
     */
    private PrintWriter log;

    //  Debug mode (printing stack trace)
    public static boolean debug = false;

    /**
     * Descriptions for all classes found at the specified classpath.
     */
    protected ClasspathImpl classpath;


    public static boolean isTigerFeaturesTracked = false;

    protected Plugin pluginClass = null;


    static {
        // Turn isTigerFeaturesTracked on if SigTest is running on Java version >= 5.0
        String specVersion;
        try {
            specVersion = System.getProperty("java.specification.version");
            if ("1.5".compareTo(specVersion) <= 0)
                isTigerFeaturesTracked = true;
        }
        catch (SecurityException e) {
            // suppress the exception
        }
    }

    static boolean Xverbose = false;

    protected ClassHierarchy testableHierarchy;

    protected Set errorMessages = new HashSet();

    private ClassDescriptionLoader loader;
    protected boolean reportWarningAsError = false;

    public void initErrors() {
        errorMessages.clear();
    }

    public void storeError(String s, Logger utilLogger) {
        if (utilLogger != null && utilLogger.isLoggable(Level.SEVERE)) {
            utilLogger.severe(s);
        }
        errorMessages.add(s);
    }

    public void storeWarning(String s, Logger utilLogger) {
        if (reportWarningAsError) {
            storeError(s, utilLogger);
            return;
        }
        if (utilLogger != null && utilLogger.isLoggable(Level.WARNING)) {
            utilLogger.warning(s);
        }
        log.println(i18n.getString("SigTest.warning", s));
    }


    public void printErrors() {
        // prints errors
        for (Iterator it = errorMessages.iterator(); it.hasNext();)
            setupProblem((String) it.next());

        initErrors();
    }


    /**
     * number of the errors.
     */
    protected int errors;

    /**
     * prints error.
     */
    protected void setupProblem(String msg) {
        log.println(msg);
        errors++;
    }

    protected void setLog(PrintWriter w) {
        log = w;
    }

    public PrintWriter getLog() {
        return log;
    }

    protected void decodeCommonOptions(String optionName, String[] args) throws CommandLineParserException {

        if (optionName.equalsIgnoreCase(TESTURL_OPTION)) {
            testURL = args[0];
        } else if (optionName.equalsIgnoreCase(FILENAME_OPTION)) {
            sigFileName = args[0];
        } else if (optionName.equalsIgnoreCase(PACKAGE_OPTION)) {
            packages.addPackages(CommandLineParser.parseListOption(args));
        } else if (optionName.equalsIgnoreCase(WITHOUTSUBPACKAGES_OPTION)) {
            purePackages.addPackages(CommandLineParser.parseListOption(args));
        } else if (optionName.equalsIgnoreCase(EXCLUDE_OPTION)) {
            excludedPackages.addPackages(CommandLineParser.parseListOption(args));
        } else if (optionName.equalsIgnoreCase(CLASSPATH_OPTION)) {
            classpathStr = args[0];
        } else if (optionName.equalsIgnoreCase(USE_BOOT_CP)) {
            release = Release.BOOT_CLASS_PATH;
        } else if (optionName.equalsIgnoreCase(APIVERSION_OPTION)) {
            apiVersion = args[0];
        } else if (optionName.equalsIgnoreCase(STATIC_OPTION)) {
            isStatic = true;
        } else if (optionName.equalsIgnoreCase(CLASSCACHESIZE_OPTION)) {
            cacheSize = 0;
            try {
                cacheSize = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                if (debug)
                    ex.printStackTrace();
                cacheSize = 0;
            }
            if (cacheSize <= 0)
                throw new CommandLineParserException(i18n.getString("SigTest.error.arg.invalid", optionName));

        } else if (optionName.equalsIgnoreCase(ALLPUBLIC_OPTION)) {
            trackMode = ClassHierarchy.ALL_PUBLIC;
        } else if (optionName.equalsIgnoreCase(DEBUG_OPTION)) {
            debug = true;
        } else if (optionName.equalsIgnoreCase(ERRORALL_OPTION)) {
            reportWarningAsError = true;
        } else if (optionName.equalsIgnoreCase(XNOTIGER_OPTION)) {
            isTigerFeaturesTracked = false;
        } else if (optionName.equalsIgnoreCase(XVERBOSE_OPTION)) {
            Xverbose = true;
        } else if (optionName.equalsIgnoreCase(PLUGIN_OPTION)) {
            pluginClass = loadPlugin(args[0]);
            if (pluginClass==null) {
                throw new CommandLineParserException(i18n.getString("SigTest.error.cant_load.plugin", args[0]));
            }

        } else if (optionName.equalsIgnoreCase(HELP_OPTION) || optionName.equalsIgnoreCase(QUESTIONMARK)) {
            usage();
        } else if (optionName.equalsIgnoreCase(VERSION_OPTION)) {
             System.err.println(Version.getVersionInfo());
        }
    }

    /**
     * @return number of errors found during Setup or Test run if any
     */
    public int getNumErrors() {
        return errorManager.getNumErrors();
    }

    /**
     * @return number of warnings found during Setup or Test run if any
     */
    public int getNumWarnings() {
        return errorManager.getNumWarnings();
    }


    /**
     * Check if the given class <code>name</code> belongs to some of
     * the packages marked to be tested.
     *
     * @see #packages
     * @see #purePackages
     * @see #excludedPackages
     */
    protected boolean isPackageMember(String name) {
        return !excludedPackages.checkName(name) &&
                (packages.checkName(name) || purePackages.checkName(name));
    }

    public void setClassDescrLoader(ClassDescriptionLoader loader) {
        this.loader = loader;
    }

    //  Load either static BinaryClassDescrLoader or reflection-based
    //  class description loaders
    //
    protected ClassDescriptionLoader getClassDescrLoader() {
        //if loader was set directly
        if (loader != null) {
            return loader;
        }

        if (isStatic) {
            //  static mode

            loader = getLoader("com.sun.tdk.signaturetest.loaders.BinaryClassDescrLoader", new Class[]{Classpath.class, Integer.class},
                    new Object[]{classpath, new Integer(cacheSize)}, getLog());

            if (loader == null)
                throw new LinkageError(i18n.getString("SigTest.error.mgr.linkerr.loadstatic"));
        } else {
            //  reflection mode

            if (isTigerFeaturesTracked) {

                loader = getLoader("com.sun.tdk.signaturetest.loaders.TigerRefgClassDescrLoader", new Class[]{}, new Object[]{}, getLog());
                if (loader != null)
                    return loader;

                isTigerFeaturesTracked = false; // sorry ...
            }

            loader = getLoader("com.sun.tdk.signaturetest.loaders.ReflClassDescrLoader", new Class[]{}, new Object[]{}, getLog());

            if (loader == null)
                throw new LinkageError(i18n.getString("SigTest.error.mgr.linkerr.loadreflect"));
        }

        return loader;
    }

    protected ClassDescription load(String name) {
        try {
            return testableHierarchy.load(name);
        }
        catch (ClassNotFoundException e) {
            if (SigTest.debug)
                e.printStackTrace();
            storeError(i18n.getString("SigTest.error.class.missing", name), null);
        }
        catch (LinkageError e) {
            if (SigTest.debug)
                e.printStackTrace();
            storeError(i18n.getString("SigTest.error.class.notlinked", e.getMessage()), null);
        }
        return null;
    }


    private static ClassDescriptionLoader getLoader(String name, Class[] pars, Object[] args, PrintWriter log) {

//        assert pars.length == args.length;

        try {
            Constructor ctor = Class.forName(name).getConstructor(pars);
            ClassDescriptionLoader cl = (ClassDescriptionLoader) ctor.newInstance(args);
            try {
                Method setLog = cl.getClass().getDeclaredMethod("setLog", new Class[] {PrintWriter.class});
                setLog.invoke(cl, new Object[] {log});
            } catch (NoSuchMethodException e) {

            }
            return cl;
        }
        catch (Throwable t) {
            if (debug)
                t.printStackTrace();
        }

        return null;
    }

    public boolean useErasurator() {
        return !isTigerFeaturesTracked;
    }

    protected abstract void usage();

    protected abstract String getComponentName();

    protected Plugin loadPlugin(String pluginClassName) {
        try {
            Constructor ctor = Class.forName(pluginClassName).getConstructor(new Class[0]);
            return (Plugin) ctor.newInstance(new Object[0]);
        }
        catch (Throwable t) {
            if (debug)
                t.printStackTrace();
        }
        return null;
    }

    public Filter getFilter(InjectionPoint injectionPoint) {
        return injectionPoint.getFilter();
    }

    public void setFilter(InjectionPoint injectionPoint, Filter filter) {
        injectionPoint.setFilter(filter);
    }

    public Transformer getTransformer(InjectionPoint injectionPoint) {
        return injectionPoint.getTransformer();
    }

    public void setTransformer(InjectionPoint injectionPoint, Transformer transformer) {
        injectionPoint.setTransformer(transformer);
    }

    protected boolean addInherited() {
        return true;
    }

    // TODO implement the method
    public Context getContext() {
        throw new UnsupportedOperationException("This method is not implemented");
    }

    public void addFormat(Format format, boolean useByDefault) {
        getFileManager().addFormat(format, useByDefault);
    }

    public void setFormat(Format format) {
        getFileManager().setFormat(format);
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

    protected FileManager getFileManager() {
        return fm;
    }

}

