/*
 * Copyright 1998-1999 Sun Microsystems, Inc.  All Rights Reserved.
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

package org.netbeans.apitest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;


/** This test has following tested mode:<dd>
 *  1. In the default mode, APIChangesTest checks the binary compatibility of
 *  API changes between major releases (dot releases). The constraints of binary
 *  compatibility for the second API attribute are found in the Java Language
 *  Specification, chapter 13.<dd>
 *  2. In the maintenance mode, APIChangesTest checks the absence of
 *  API changes between maintenance releases (dot-dot releases).
 *  APIChangeTest reports most differences in the public and protected API
 *  between the master API signature file and the Java implementation
 *  under test as errors.  The following declaration orderings are not
 *  important and should not be required:
 *       <dd>Class declarations<dd>
 *       <dd>Class interface implements declarations
 *       <dd>Class member declarations
 *       <dd>Constructor and method declared throwables<br>
 *  All other differences in the public and protected API between
 *  maintenance releases are errors.<dd>
 *  3. In the setup mode APIChangesTest creates API master signature file for
 *  tracked implementation.<p>
 *  If test is run in the verification mode, result can be printed in the two
 *  formats:<dd>
 *  1. Plain format.(The errors are not grouped or sorted. The each
 *  founded error is reported immediately. This format is cpecified by options
 *  -FormatPlain by arqument.<dd>
 *  2. Default format. Differences should be grouped by type, and
 *     alphabetized by class then name. The groups are different in the
 *     default and maintenance mode. <br>In the default mode, differences are
 *     reported in the following sequence:
 *       <dd>Missing Classes,
 *       <dd>Missing Superclasses or Superinterfaces,
 *       <dd>Missing Fields,
 *       <dd>Missing Constructors,
 *       <dd>Missing Methods,
 *       <dd>Incompatible changes of the class modifiers,
 *       <dd>Incompatible changes of the fields,
 *       <dd>Incompatible changes of the constructors,
 *       <dd>Incompatible changes of the Methods,
 *       <dd>LinkageError thrown during tracking of the definition.
 *     <br>In the maintenance mode,  differences are reported in the following
 *     sequence:
 *       <dd>Missing Classes,
 *       <dd>Missing Class definitions,
 *       <dd>Missing Superclasses or Superinterfaces,
 *       <dd>Missing Fields,
 *       <dd>Missing Constructors,
 *       <dd>Missing Methods,
 *       <dd>Added Classes,
 *       <dd>Added Class definitions,
 *       <dd>Added Superclasses or Superinterfaces,
 *       <dd>Added Fields,
 *       <dd>Added Constructors,
 *       <dd>Added Methods,<br>
 *     The messages are reported in the following format:<dd>
 *     &lt; name of the class which declares definition &gt; : &lt; definition&gt; <br>
 *     If the errors is missing or added class then ": &lt;definition&gt;" should be ignored.
 *     <br>In the default mode, definition declares member in the based implementation.
 *     <br>In the maintenance mode, missing definition declares member in the
 *     based implementation, and added definition declares member in the
 *     tested implementation.<p>
 *  Usage: java javasoft.sqe.tests.api.SignatureTest.APIChangesTest &lt;options&gt;<p>
 *  Where &lt;options&gt; includes:<p>

 *  Options for setup mode

 *   <dd> -setup 	 - run in setup mode
 *   <dd> -FileName &lt;n&gt;   - output signature file name 
 *   <dd> -Package &lt;p&gt; - package which are added to signature file,
 *      all "java.*" by default.  (several options can be specified)
 *   <dd> -Exclude &lt;package or class name&gt; - package or class which is not needed
 *        to be tracked.(several options can be specified)
 *   <dd> -UseReflect - use java.lang.Class.getDeclaredMethod() to find
 *      nested classes (works since jdk1.2b4).  In the default mode nested
 *      class name conventions are used as workaround for finding nested
 *      classes.
 *   <dd> -AllPublic - add unaccessible nested classes to signature file
 *      (i.e. which are public or protected but are members of default or
 *      private access class).
 *   <dd> -Claspath &lt;path&gt;- specify the path, which includes tracked
 *      classes. Only classes from &lt;path&gt; will be added to the signature
 *      file.
 *   <dd> -Verbose - print ignored class names.<p>
 * 
 * Options for checking mode
 * 
 *   <dd>-maintenance - track absence of any API changes. This options
 *        doesn't track throwable clause for jdk1.1 because of JDK bug#4022219.
 *   <dd>-FileName &lt;n&gt;   - signature file name (short, without directory)
 *   <dd>-Package &lt;p&gt;	 - package which are needed to be tracked
 *        (several options can be specified)  
 *   <dd>-Exclude &lt;package or class name&gt; - package or class which is not needed
 *        to be tracked.(several options can be specified)
 *   <dd>-UseReflect 	 - use java.lang.Class.getDeclaredMethod() to
 *        find nested classes (works since jdk1.2b4). 
 *        In the default mode nested class name conventions are
 *        used as workaround for finding nested classes.
 *   <dd>  -AllPublic      - track unaccessible nested classes (i.e. which are public or 
 *        protected but are members of default or private access class). 
 *   <dd> -FormatPlain - do not sort messages in report
 *   <dd> -Claspath &lt;path&gt;- specify the path, which includes tracked
 *      classes. Only classes from &lt;path&gt; will be used for tracking
 *      adding classes.
 *  <p>Examples:<br>
 *  Creation of the signature file:<br>
 *      <dd>java javasoft.sqe.tests.api.APIChangesTest-FileName
 *      apiMasterFile.jdk1.1.1 -setup<br>
 *  Tracking of the jdk:<br>
 *      <dd>java javasoft.sqe.tests.api.APIChangesTest -FileName
 *      apiMasterFile.jdk1.1.1 <br>
 **/

final class Main {
    /** print errors and warnings. **/
    PrintWriter log;
    /**Reads class names and classes, which are available by current
       Java VM.**/
    private ClassesFromClasspath classIterator;
    /**formats member definition before checking.**/
    private PrimitiveConstantsChecker converter;
    /**prints error message in the specified format.**/
    private ErrorFormatter errorWriter;
    /**contains founded nested classes.**/
    private Hashtable<String,TableOfClass> nestedClasses;
    /**includes packages which are required to be tracked.**/
    private Pattern packages[];
    /** name of the packages or classes which should must be excluded
        from tracking.**/
    private String excludedElements[];
    /**classes which can not be linked(LinkageError is thrown)**/
    private Vector<String> nonLinkedClasses = new Vector<String>();
    /**contains tracked class names.**/
    private Vector<String> trackedClassNames = new Vector<String>();
    /**specify if the unaccessible nested public classes are required to be
     * tracked.**/
    private boolean isAllPublicTracked = false;
    /**specify if the warning are reported.**/
    private boolean isIgnorableReported = false;
    /**specify if the current mode is maintenance mode.**/
    private boolean isMaintenanceMode = false;
    /** in this mode people can add new methods into interfaces,
     * which is binary compatible for linkage, but not execution
     */
    private boolean extensibleInterfaces;
    /**specify if the reflection is used for founding nested classes.**/
    private boolean isReflectUsed = false;
    /**number of the founded classes in the scanned PATH.**/
    private int allClassesSize = 0;
    /**number of the scanned classes.**/
    private int scanedClassesSize = 0;
    /**number of the founded errors.**/
    private int errors;
    /**loads SignatureClasses.**/
    private ClassFinder loader;
    /** version of the provided file */
    private String version = System.getProperty("java.version");
    /** contains detail options. This options are used for workaround
        of implementation **/
    protected Properties details = new Properties();
    
    public static void main(String[] args) {
        Status s = run(args);
        if (s.getType() != 0) {
            System.err.println(s.getReason());
            System.exit(s.getType());
        }
    }
    
    static Status run(String[] args) {
	Main t = new Main();
        PrintWriter log = new PrintWriter(new OutputStreamWriter(System.err),
                                          true);
        PrintWriter ref = new PrintWriter(new OutputStreamWriter(System.out),
                                          true);
	return t.run(args, log, ref);
    }

    /**runs test.**/
    private Status run(String[] args, PrintWriter log, PrintWriter ref) {
	this.log = log;
	// ref ignored
	boolean setup = false;
	boolean isOrdering = true;

	String fileName = null;
	URL sigFile;
	Vector<Pattern> tempPackages = new Vector<Pattern>();
	Vector<String> tempExcludedElements = new Vector<String>();
        String classpath = null;

	for (int i = 0; i < args.length; ++i) {
	    if (args[i].equals("-FormatPlain")) {
		isOrdering = false;
	    } else if (args[i].equals("-FileName") &&
		       (args.length > i + 1)) {
		fileName = args[++i];
	    } else if (args[i].equals("-Version") &&
		       (args.length > i + 1)) {
		version = args[++i];
	    } else if (args[i].equals("-Package") &&
		       (args.length > i + 1)) {
                String pkg = args[++i];
		tempPackages.addElement(Pattern.compile(pkg + "\\..*"));
	    } else if (args[i].equals("-PackageWithoutSubpackages") &&
		       (args.length > i + 1)) {
                String pkg = args[++i];
		tempPackages.addElement(Pattern.compile(pkg + "\\.[^\\.]*"));
	    } else if (args[i].equals("-Exclude") &&
		       (args.length > i + 1)) {
		tempExcludedElements.addElement(args[++i]);
	    } else if (args[i].equals("-AllPublic")) {
                isAllPublicTracked = true;
	    } else if (args[i].equals("-setup")) {
                setup = true;
	    } else if (args[i].equals("-maintenance")) {
                isMaintenanceMode = true;
	    } else if (args[i].equals("-extensibleinterfaces")) {
                extensibleInterfaces = true;
            } else if (args[i].equals("-UseReflect")) {
                isReflectUsed = true;
	    } else if (args[i].equals("-Classpath") &&
                       (args.length > i + 1)) {
                classpath = args[++i];
            } else if (args[i].equals("-Verbose")) {
                isIgnorableReported = true;
            } else {
                return Status.failed("Unknown option: " + args[i]);
            }
	}
	// check arguments
	if (fileName == null) {
	    return Status.failed("Need to specify --FileName");
	}

	//Construct array of the tested packages
	if (tempPackages.isEmpty()) {
            return Status.failed("Specify some packages to test");
	} else {
	    packages = tempPackages.toArray(new Pattern[0]);
	}
        //Construct array of the excluded elements
	if (tempExcludedElements.isEmpty()) {
	    excludedElements = new String[0];
        } else {
	    excludedElements = new String[tempExcludedElements.size()];
	    for (int i = 0; i < tempExcludedElements.size(); i++) {
                excludedElements[i] = tempExcludedElements.elementAt(i);
            }
	}
        if (!isOrdering) {
            if (isMaintenanceMode) {
                errorWriter = new ErrorFormatter(log);
            } else {
                errorWriter = new ErrorFormatter(log, new String[]{
                    "Required class not found in implementation: ", 
                    // Missing nested Classes or class definitions
                    "Definition required but not found in ", 
                    // Missing Superclasses or Superinterfaces
                    "Definition required but not found in ", 
                    "Definition required but not found in ", // Missing Fields
                    // Missing Constructors
                    "Definition required but not found in ", 
                    "Definition required but not found in ", // Missing Methods
                    "Incompatible change is found in ", 
                    // Added nested Classes or class definitions
                    "Incompatible change is found in ", 
                    // Added Superclasses or Superinterfaces
                    "Incompatible change is found in ", 
                    "Incompatible change is found in ", // Added Fields
                    "Incompatible change is found in ", // Added Constructors
                    "Incompatible change is found in ", // Added Methods
                    // LinkageError
                    "LinkageError does not allow to track definition in "
                });
            }
        } else {
            if (isMaintenanceMode) {
                errorWriter = new SortedErrorFormatter(log);
            } else {
                errorWriter = new APISortedErrorFormatter(log);
            }
        }

        if (classpath == null) {
            return Status.failed("Specify --Classpath");
        } else {
            classIterator = new ClassesFromClasspath(classpath, isIgnorableReported);
        }

	if (setup) {
            return setup(fileName);
        } else {
            return verify(fileName);
        }
    }

    /**runs test in the setup mode. creates API master signature file.
     * @param outFileName name of the created API master signature file.**/
    Status setup(String outFileName) {
        ClassCollection nestedErrors = new ClassCollection();
        boolean isThrowsTracked = classIterator.isThrowsTracked();
        converter = new PrimitiveConstantsChecker(true, isThrowsTracked);
        Vector<String> packageClasses = new Vector<String>();
	try {
	    PrintWriter out = new PrintWriter(new FileOutputStream(outFileName));
            out.println("#API master signature file");
            out.println("#Version " + version);
            if (!isThrowsTracked) {
                out.println("#Throws clause not tracked.");
            }
            loader = new ClassFinder(converter, details, classIterator.getClassLoader());
	    String name;
            Vector<String> duplicateClasses = new Vector<String>();
            InputStream classStream;
            // create table of the nested classes.
	    while ((name = classIterator.nextClassName()) != null) {
                allClassesSize++;
		    try {
                        if (name.indexOf('$') >= 0) {
                            if (isAccessible(TableOfClass.addNestedClass(name,
                                                                         loader))
                                && isPackageMember(name)
                            ) {
                                packageClasses.addElement(name);
                            }
                        } else {
                            if (isPackageMember(name) &&
                                isAccessible(loader.loadClass(name))) {
                            packageClasses.addElement(name);
                        }
                        }
		    } catch (ClassNotFoundException ex) {
                        nestedErrors.addUniqueElement(name, "Class not found: " +
                                                      name);
		    } catch (LinkageError ex1) {
                        nestedErrors.addUniqueElement(name, "Class not linked: " + 
                                                      name + " throw  " + ex1);
		    }
            }
            // adds classes which are member of classes from tracked package
            // and sorts class names
            ClassSorter temp = new ClassSorter(packageClasses, isReflectUsed, loader);
            packageClasses = temp.getSortedClasses(nestedErrors);
            classIterator.clear();
            // create table of the primitive constants
	    while ((name = classIterator.nextClassName()) != null) {
                try {
                    if (!packageClasses.contains(name)) {
                        ignore(name);
                    }
                    if (!temp.isAccessible(name)) {
                        continue;
                    }
                    //Class c = Class.forName(name);
                    if (duplicateClasses.contains(name)) {
                        setupProblem("The class " + name + " is found twice.");
                    } else {
                        duplicateClasses.addElement(name);
                        classStream = classIterator.getCurrentClass();
                        converter.checkPrimitiveConstants(name, classStream); 
                    }
                } catch (IOException t) {
                    setupProblem("The primitive constans of the class " + 
                                 name + " can not be tracked throw " + t);
                } catch (ClassFormatError er) {
                    setupProblem("The primitive constans of the class " + 
                                 name + " can not be tracked throw " +  er);
                //} catch (ClassNotFoundException e) {
                } catch (LinkageError er2) {
                }
            }
            duplicateClasses = null;
            classIterator.clear();
            // scan class and writes definition to the signature file
            for (int i = 0; i < packageClasses.size(); i++) {
                name = packageClasses.elementAt(i);
                try {
             //       Class c = Class.forName(name);
                    scanClass(out, loader.loadClass(name));
                } catch (ClassNotFoundException ex) {
                    nestedErrors.addUniqueElement(name, "Class not found: " + name);
                } catch (LinkageError ex1) {
                    nestedErrors.addUniqueElement(name, "Class not linked: " + name +
                                                  " throw " + ex1);
                }
            }
            // prints errors 
            for (Enumeration e = nestedErrors.keys(); e.hasMoreElements();) {
                String tempName = (String)e.nextElement();
                int tempPos = tempName.lastIndexOf('$') + 1;
                if (temp.isAccessible(tempName.substring(0, tempPos)) ||
                    temp.isAccessible(tempName)) {
                    Vector h = nestedErrors.get(tempName);
                    for (int i = 0; i < h.size(); i++) {
                        setupProblem((String) h.elementAt(i));
                    }
                }
            }
            out.close();
        } catch (IOException e) {
	    log.println("problem creating definitions file");
	    log.println(e);
	    return Status.failed("problem creating definitions file");
	}
        errors = errors + classIterator.printErrors(log);
            
        /*
        try {
            String classpath = ClassesFromClasspath.getClasspath();
            log.println("In the CLASSPATH : " + classpath);
        } catch (Throwable t) {
        }
        */
        
        log.println("\n  Found classes   : " + allClassesSize);
        log.println("  Scanned classes : " + scanedClassesSize);
	if (errors == 0) {
            return Status.passed("");
        } else {
            return Status.failed(errors + " errors");
        }
            
    }

    /**scan class in the setup mode
     * @param out prints in the API signature file.
     * @param c scanned class.**/
    private void scanClass(PrintWriter out, SignatureClass c) 
	throws ClassNotFoundException {
        int m = c.getModifiers();
        if (Modifier.isPublic(m) || Modifier.isProtected(m)) {
            scanedClassesSize++;
            TableOfClass cl = new TableOfClass(c, isReflectUsed);
            cl.createMembers();
            cl.writeDefinitions(out);
        }
    }

    /**runs test in the default or maintenance mode
     * @param sigFileURL API signature file.**/
    private Status verify(String sigFileURL) {
	nestedClasses = new Hashtable<String, TableOfClass>();
	trackedClassNames = new Vector<String>();
        ClassSignatureReader in = null;
	try {
	    String line;
	    in = new ClassSignatureReader(sigFileURL);
            boolean isThrowsTracked = classIterator.isThrowsTracked() &&
                                      in.isThrowsTracked;
            in.isThrowsTracked = isThrowsTracked;
            // That the fields is primitive constant is not tracked by test
            if (isMaintenanceMode) {
                String tempModifiers[][] = {
                    {SignatureConstants.FIELD, SignatureConstants.PRIMITIVE_CONSTANT}
                };
                converter = new PrimitiveConstantsChecker(true, isThrowsTracked,
                                                          tempModifiers);
            } else {
                converter = new PrimitiveConstantsChecker(true, isThrowsTracked);
            }

            loader = new ClassFinder(converter, details, classIterator.getClassLoader());
        
            in.setDefinitionConverter(converter);
	    TableOfClass currentClass;
	    while ((currentClass = in.nextAPIClass()) != null) {
		String name = currentClass.getName();
		if (name.indexOf('$') < 0) {
                    verifyClass(currentClass);
                } else {
                    //includes nested classes to the table.
                    nestedClasses.put(name, currentClass);
                }
	    }

            // track inaccessible nested classes
            if (isAllPublicTracked) {
                for (Enumeration<String> e = nestedClasses.keys(); e.hasMoreElements();) {
                    String name = e.nextElement();
                    if (!trackedClassNames.contains(name)) {
                        verifyClass(nestedClasses.get(name));
                    }
                }
            }
                
	} catch (IOException e) {
	    log.println("problem with definitions file");
	    log.println(e);
	    return Status.failed("problem with definitions file");
	} catch (SecurityException ex) {
	    log.println("The security constraints does not allow" +
			"to read from file.");
	    log.println(ex);
	    return Status.failed("The security constraints does " +
				 "not allow to read from file.");
	}

        //check that new classes are not added to the tracked packages.
	try {
	    String name;
	    while (isMaintenanceMode &&
                   ((name = classIterator.nextClassName()) != null))  {
		int pos = name.lastIndexOf('$');
                try {
                    SignatureClass c = loader.loadClass(name);
                    if (isAccessible(c) && isPackageMember(name) &&
                        !trackedClassNames.contains(c.getName()) &&
                        !nonLinkedClasses.contains(c.getName())
                    ) {
                        errorWriter.addError("Added", c.getName(), null, null);
                    }
                } catch (ClassNotFoundException ex) {
                } catch (LinkageError ex1) {
                }
	    }
	} catch (SecurityException ex) {
	    log.println("The security constraints does not allow to inspect" + 
			" CLASSPATH.");
	}

        log.println("APIChangeTest Report\n");
        if (in != null) {
            log.println("Base version:   " + in.javaVersion);
        }
        String javaVersion = version;
        log.println("Tested version: " + javaVersion);
        log.println("");
        errors = errorWriter.printErrors();
        
        if (errors == 0) {
            return Status.passed("");
        } else {
	    return Status.failed(errors + " errors");
	}
    }

    /** verify class in the default mode
     *  @param required verified class in the based implementation.**/
    private void verifyClass(TableOfClass required) {
        String name = required.getName();
        if (isPackageMember(name)) {
            try {
                SignatureClass c = loader.loadClass(name);
                TableOfClass cl = new TableOfClass(c, isReflectUsed);
                if (isMaintenanceMode) {
                    verifyMaintenanceClass(required, cl);
                } else {
                    verifyClass(required, cl);
                }
            } catch (ClassNotFoundException ex) {
                errorWriter.addError("Missing", name, null, null);
            } catch (LinkageError er) {
                errorWriter.addError("LinkageError", name,
                                      name + " throw " + er, null);
                nonLinkedClasses.addElement(name);
            }
        }
    }

    /** verify class in the maintenance mode
     *  @param required class specification in the based implementation.
     *  @param found class specification in the tested implementation.**/
    private void verifyMaintenanceClass(TableOfClass required,
                                        TableOfClass found)
        throws ClassNotFoundException  {
        // track class modifiers
	if (trackedClassNames.contains(found.getClassName())) {
            return; // this class is tracked
        }// this class is tracked

        trackedClassNames.addElement(found.getClassName());
        found.createMembers();
        String modReq = required.classDef;
        modReq = modReq.substring(0, modReq.lastIndexOf(' '));
        String modFou = found.classDef;
        modFou = modFou.substring(0, modFou.lastIndexOf(' '));
        if (!modReq.equals(modFou)) {
            errorWriter.addError("Missing", required.getName(), 
                                 required.classDef, null);
            errorWriter.addError("Added", found.getName(), found.classDef, null);
        }
	// track members declared in the signature file.
	for (Enumeration eReq = required.keys(); eReq.hasMoreElements();) {
	    String name = (String)eReq.nextElement();
	    Vector mReq = required.get(name);
	    Vector mFou = found.get(name);
	    if (mFou == null) {
		mFou = new Vector();
            }
	    if (name.startsWith(SignatureConstants.INNER)) {
		trackNestedClass(found, mReq, mFou);
	    } else {
                trackMember(found.getName(), mReq, mFou,false);
            }
	}
	// track members which are added in the current implementation.
	for (Enumeration eFou = found.keys(); eFou.hasMoreElements();) {
	    String name = (String)eFou.nextElement();
	    Vector mReq = required.get(name);
	    Vector mFou = found.get(name);
	    if ((mReq == null) && (!name.startsWith(SignatureConstants.INNER) || isReflectUsed)) {
		mReq = new Vector();
		trackMember(found.getName(), mReq, mFou,false);
	    } 
	}
    }

    /** track Vector of the members with the same signature in the maintenance
     *  mode.
     *  @param name name of the enclosing class.
     *  @param required definition of the based implementation.
     *  @param found definition of the tested implementation.
     *  @param onlyAbstract warn only if an abstract member is added
     */
    private void trackMember(String name, Vector required, Vector found, boolean onlyAbstract) {
	Vector req = (Vector)required.clone();
 	Vector fou = (Vector)found.clone(); 
        
	Vector retVal = new Vector();

	for (int i = 0; i < req.size(); i++) {
	    if (i < 0) {
                break;
            }
	    int pos = fou.indexOf(req.elementAt(i));
	    if (pos >= 0) {
		req.removeElementAt(i--);
		fou.removeElementAt(pos);
	    }
	}

	for (int i = 0; i < req.size(); i++) {
            errorWriter.addError("Missing", name, (String) req.elementAt(i), null);
        } 
	for (int i = 0; i < fou.size(); i++) {
            String m = (String)fou.elementAt(i);
            if (onlyAbstract && !m.contains(" abstract ")) {
                continue;
            }
            errorWriter.addError("Added", name, m, null);
        }
    }

    /** track Vector of the nested classes with the same local name in the maintenance
     *  mode.
     *  @param trackedClass enclosing class in the tested implementation.
     *  @param required definition of the based implementation.
     *  @param found definition of the tested implementation.**/
    private void trackNestedClass(TableOfClass trackedClass, Vector required,
                                  Vector found) {
        if (required.isEmpty()) {
            trackMember(trackedClass.getName(), required, found,false);
            return;
        }
        String localName = (String)required.elementAt(0);
        localName = localName.substring(localName.lastIndexOf(' ') + 1);
        localName = localName.substring(localName.lastIndexOf('.') + 1);
        localName = localName.substring(localName.lastIndexOf('$') + 1);
        Vector foundClasses = trackedClass.getNestedClassDefinitions(localName);
        Vector<String> reqClasses = new Vector<String>();
        for (int i = 0; i < required.size(); i++) {
            String temp = (String)required.elementAt(i);
            reqClasses.addElement(temp.substring(0, temp.lastIndexOf(' ') + 1) +
                                  localName);
        }
        for (int i = 0; i < required.size(); i++) {
            String name = (String)required.elementAt(i);
            name = name.substring(name.lastIndexOf(' ') + 1);
            TableOfClass req = nestedClasses.get(name);
            try {
                SignatureClass c = loader.loadClass(name);
                TableOfClass cl = new TableOfClass(c, isReflectUsed);
                verifyMaintenanceClass(req, cl);
            } catch (ClassNotFoundException ex) {
                errorWriter.addError("Missing", name, null, null);
            } catch (LinkageError er) {
                errorWriter.addError("LinkageError", name, 
                                     name + " throw " + er, null);
                nonLinkedClasses.addElement(name);
            }
        }
        trackMember(trackedClass.getName(), required, foundClasses,false);
    }


    /** verify class in the default mode
     *  @param required verified class in the based implementation.
     *  @param found verified class in the tested implementation.**/
    private void verifyClass(TableOfClass required, TableOfClass found) 
        throws ClassNotFoundException {
	// adds class name to the table of the tracked classes.
	if (trackedClassNames.contains(found.getClassName())) {
            return; // this class is tracked
        } else {
            trackedClassNames.addElement(found.getClassName());
        }
        found.createMembers();
        // track class definition.
        trackClassDefinition(required.getName(), required.classDef, 
                             found.classDef);
        // The protected member of the final classes are not tracked, because
        // they are inaccessible in the old implementation.
        boolean isProtectedTracked = (required.classDef.indexOf(" final ") < 0);
	// track members declared in the API master signature file excluded
        // primitive constants.
	for (Enumeration eReq = required.keys(); eReq.hasMoreElements();) {
	    String name = (String)eReq.nextElement();
	    Vector<?> requiredMembers = required.get(name);
	    Vector<?> existingMembers = found.get(name);
	    if (existingMembers == null) {
		existingMembers = new Vector();
                existingMembers.add(null);
            }
	    if (name.startsWith(SignatureConstants.INNER)) {
                if (isAllPublicTracked) {
                    continue;
                }
                for (int i = 0; i < requiredMembers.size(); i++) {
                    String def = (String)requiredMembers.elementAt(i);
                    def = def.substring(def.lastIndexOf(' ') + 1);
                    try {
                        SignatureClass c = loader.loadClass(def);
                        TableOfClass reqNest;
                        reqNest = nestedClasses.get(def);
                        if ((reqNest.classDef.indexOf(" public ") >= 0) ||
                            isProtectedTracked) {
                            TableOfClass cl = new TableOfClass(c, isReflectUsed);
                            verifyClass(reqNest, cl);
                        }
                    } catch (ClassNotFoundException ex) {
                        errorWriter.addError("Missing", required.getName(),
                                             def, null);
                    } catch (LinkageError er) {
                        errorWriter.addError("LinkageError", required.getName(),
                                             def, null);
                    } 
                }
            } else if (name.startsWith(SignatureConstants.SUPER)) {
                if ((requiredMembers != null) && !requiredMembers.isEmpty()) {
                    SignatureClass c = found.getClassObject();
                    if (c != null) {
                        c = c.getSuperclass();
                    }
                    String superName = (String)requiredMembers.elementAt(0);
                    superName = superName.substring(superName.lastIndexOf(' ') + 1);
                    // TBD: Next two lines look like an error to me
                    for (; ((c != null) && !superName.equals(c.getName()));
                         c = c.getSuperclass());
                    if ((c == null) && !superName.equals("null")) {
                        errorWriter.addError("Missing", required.getName(), (String) requiredMembers.elementAt(0), null);
                    }
                }                    
            } else {
                BIG: for (int i = 0; i < requiredMembers.size(); i++) {
                    String tempReq = (String)requiredMembers.elementAt(i);
                    String clName = required.getName();
                    
                    /*
                    if (
                        (tempReq.indexOf(" abstract ") >= 0) ||
                        (tempReq.indexOf(" protected ") >= 0) &&
                        !isProtectedTracked ||
                        converter.isPrimitiveConstant(tempReq)
                    ) {
                        continue;
                    }
                    */

                    ErrorMessage error = null;
                    for (Object objMember : existingMembers) {
                        String tempFou = (String)objMember;

                        if ((tempFou == null) &&
                            (tempReq.startsWith(SignatureConstants.METHOD) ||
                             tempReq.startsWith(SignatureConstants.FIELD)  ||
                             name.startsWith(SignatureConstants.INTERFACE) ||
                             tempReq.startsWith(SignatureConstants.CONSTRUCTOR))) {
                            errorWriter.addError("Missing", clName, tempReq, null);
                            continue BIG;
                        }
                        if (tempReq.startsWith(SignatureConstants.METHOD)) {
                            if (tempReq.indexOf(" static ") < 0) {
                                error = trackMethodDefinition(
                                    clName, required.classDef, tempReq, tempFou
                                );
                            } else {
                                error = trackMethodDefinition(
                                    clName, required.classDef, tempReq
                                );
                            }
                        } else if (tempReq.startsWith(SignatureConstants.FIELD)) {
                            trackFieldDefinition(clName, tempReq);
                        } else if (tempReq.startsWith(SignatureConstants.CONSTRUCTOR)) {
                            trackConstructorDefinition(clName, tempReq, tempFou);
                        }
                        
                        if (error == null) {
                            // ok, no error, we found the member which passes
                            // the test
                            continue BIG;
                        }
                    }
                    assert error != null;
                    errorWriter.addError(error);
                }
            }
        } 
        trackedClassNames.addElement(found.getClassName());
        found.createMembers();
        String modReq = required.classDef;
        modReq = modReq.substring(0, modReq.lastIndexOf(' '));
        String modFou = found.classDef;
        modFou = modFou.substring(0, modFou.lastIndexOf(' '));
        if (!isMaintenanceMode) {
            if (!modFou.endsWith(" final")) {
                modReq = modReq.replaceAll(" final", "");
            }
        }
        if (!modReq.equals(modFou)) {
            errorWriter.addError("Missing", required.getName(), 
                                 required.classDef, null);
            errorWriter.addError("Added", found.getName(), found.classDef, null);
        }
    
        if (extensibleInterfaces && required.isInterface()) {
            return;
        }
        
	// track abstract members which are added in the current implementation.
	for (Enumeration eFou = found.keys(); eFou.hasMoreElements();) {
	    String name = (String)eFou.nextElement();
	    Vector mReq = required.get(name);
	    Vector mFou = found.get(name);
            
	    if (mReq == null) {
		mReq = new Vector();
		trackMember(found.getName(), mReq, mFou, true);
	    } 
	}
    }

    /**founds field in the tested implementation and check it in the default mode
     * @param name name of the enclosing class.
     * @param def field definition in the based implementation.**/
    private void trackFieldDefinition(String name, String def) {
        if (converter.isPrimitiveConstant(def)) {
            return;
        }
        MemberDefinition required = new MemberDefinition(name, def);
        String className = required.getDeclaringClass();
        String fieldName = required.getShortSignature();
        try {
            Class c = Class.forName(className, false, classIterator.getClassLoader());
            Field field = null;
            for (Class current = c; (current != null) && (field == null);
                 current = current.getSuperclass()) {
                try {
                    field = current.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                }
            }
            if (field != null) {
                MemberEntry entry = new MemberEntry(field, converter);
                trackFieldDefinition(name, def, entry.getEntry());
                return;
            }
        } catch (ClassNotFoundException e) {
        } catch (LinkageError er) {
            errorWriter.addError("LinkageError", name, "Can't link " + def +
                                 " throw " + er, null);
            return;
        }
        errorWriter.addError("Missing", name, def, null);
    }

    /**founds method in the tested implementation and check it in the default mode
     * @param clName name of the enclosing class.
     * @param clDef class definition in the based implementation.
     * @param definition method definition in the based implementation.**/
    private ErrorMessage trackMethodDefinition(String clName, String clDef,
                                       String definition) {
        MemberDefinition def = new MemberDefinition("", definition);
        String name = def.getShortSignature();
        Method meth = null;
        try {
            Class c = Class.forName(def.getDeclaringClass(), false, classIterator.getClassLoader());
            String methodName = name.substring(0, name.indexOf("("));
            for (Class current = c; (current != null);
                 current = current.getSuperclass()) {
                Method [] methods = current.getDeclaredMethods();
                for (int i = 0; i < methods.length; i++) {
                    String temp = new MemberEntry(methods[i],
                                                  converter).getKey();
                    temp = temp.substring(SignatureConstants.METHOD.length());
                    if (methodName.equals(methods[i].getName()) &&
                        name.equals(temp)) {
                        MemberEntry t = new MemberEntry(methods[i], converter);
                        return trackMethodDefinition(clName, clDef, definition,
                                              t.getEntry());
                    }
                }
            }
        } catch (ClassNotFoundException e) {
        } catch (LinkageError er) {
            errorWriter.addError("LinkageError", clName, definition + 
                                 " throw " + er, null);
        }
        errorWriter.addError("Missing", clName, definition, null);
        return null;
    }

    /** check class definition in the default mode.
     *  @param name name of the enclosing class for nested classes or class
     *  name for regular classes.
     *  @param name before class definition in the based implementation.
     *  @param name after class definition in the tested implementation.**/
    private void trackClassDefinition(String name, String before, String after) {
	MemberDefinition beforeDef = new MemberDefinition(name, before);
	MemberDefinition afterDef = new MemberDefinition(name, after);
        String modifs[][] = {
            {null, "final"},
            {null, "abstract"},
            {"interface", "interface"}
        };
        
        if (before.indexOf(" interface ") >= 0) {
            String tempModifs[][] = {
                {"interface", "interface"}
            };
            modifs = tempModifs;
        }
        // track modifiers.
	trackModifiers(name, modifs, beforeDef, afterDef);
    }

    /** check constructor definition in the default mode.
     *  @param name name of the enclosing class for nested classes or class
     *  name for regular classes.
     *  @param name before constructor definition in the based implementation.
     *  @param name after constructor definition in the tested implementation.**/
    private void trackConstructorDefinition(String name, String before,
                                            String after) {
	MemberDefinition beforeDef = new MemberDefinition(name, before);
	MemberDefinition afterDef = new MemberDefinition(name, after);
        // track modifiers.
	trackModifiers(name, new String[0][0], beforeDef, afterDef);
    }

    /** check field definition in the default mode.
     *  @param name name of the enclosing class for nested classes or class
     *  name for regular classes.
     *  @param name before field definition in the based implementation.
     *  @param name after field definition in the tested implementation.**/
    private void trackFieldDefinition(String name, String before,
                                      String after) {
	MemberDefinition beforeDef = new MemberDefinition(name, before);
	MemberDefinition afterDef = new MemberDefinition(name, after);
        String modifs[][] = {
            {null, "final"},
            {"static", "static"},
            {"volatile", "volatile"},
        };
        // track modifiers.
	trackModifiers(name, modifs, beforeDef, afterDef);
        // track return type
	if (!beforeDef.getType().equals(afterDef.getType())) {
	    errorWriter.addError("Change type.", name, before,
                                 "of the " + beforeDef.getType() + " type.");
	}
    }

    /** check method definition in the default mode.
     *  @param name name of the enclosing class for nested classes or class
     *  name for regular classes.
     *  @param name before method definition in the based implementation.
     *  @param name after method definition in the tested implementation.
     *  @return error message or null, if everythig is ok
     **/
    private ErrorMessage trackMethodDefinition(String name, String clDef,
                                       String before, String after) {
	MemberDefinition beforeDef = new MemberDefinition(name, before);
	MemberDefinition afterDef = new MemberDefinition(name, after);
        String modifs[][];

        if (clDef.indexOf(" final ") < 0) {
            String temp[][] = {
                {null, "final"}, 
                {"static", "static"},
                {null, "abstract"}
            };
            modifs = temp;
        } else {
            String temp[][] = {
                {"static", "static"},
                {null, "abstract"}
            };
            modifs = temp;
        }
        // track modifiers.
	trackModifiers(name, modifs, beforeDef, afterDef);
        // track return type
	if (!beforeDef.getType().equals(afterDef.getType())) {
            return errorWriter.createError("Change type.", name, before ,
                                  " return value of " + afterDef.getType());
	}  
        return null;
    }
         

    /** determinates if the class is member of the tested packages.
     *  @param name name of the class.**/
    private boolean isPackageMember(String name) {
	for (int j = 0; j < excludedElements.length; ++j) {
            if (name.startsWith(excludedElements[j] + ".") || name.equals(excludedElements[j])) {
                return false;
            }
        }
	for (int i = 0; i < packages.length; i++) {
            if (packages[i].matcher(name).matches()) {
                return true;
            }
        }
	return false;
    }

    /** prints error in the setup mode.**/
    private void setupProblem(String msg) {
	log.println(msg);
	errors++;
    }
    
    /** determinate if the class is accessible for current mode.
     *  @param name name of the class.**/
    boolean isAccessible(SignatureClass c) {
        int m;
        if (isAllPublicTracked) {
            m = c.getModifiers();
            return (Modifier.isPublic(m) || Modifier.isProtected(m));
        }
        if (isReflectUsed) {
            for (SignatureClass cl = c; cl != null; cl = cl.getDeclaringClass()) {
                m = cl.getModifiers();
                if (!Modifier.isPublic(m) && !Modifier.isProtected(m)) {
                    return false;
                }
            }
        } else {
            for (SignatureClass cl = c; cl != null;) {
                m = cl.getModifiers();
                if (!Modifier.isPublic(m) && !Modifier.isProtected(m)) {
                    return false;
                }
                int pos = cl.getName().lastIndexOf('$');
                try {
                    if (pos > 0) {
                        String nextName = cl.getName().substring(0, pos);
                        cl = loader.loadClass(nextName);
                    } else {
                        cl = null;
                    }
                } catch (ClassNotFoundException e) {
                    cl = null;
                } catch (LinkageError er) {
                    errorWriter.addError("LinkageError", c.getName(),
                                         "Can't track accessibility: " +
                                         er + " thrown.", null);
                    return false;
                }
            }
        }
        return true;
    }

    /** tracks modifiers.
     *  @param name name of the enclosing class,
     *  @param modifiers modifiers which required to be checked.
     *  @param beforeDef definition in the based implementation.
     *  @param afterDef definition in the tested implementation.**/
    private void trackModifiers(String name, String[][] modifiers, MemberDefinition beforeDef,
                                MemberDefinition afterDef) {
        int nBefore = beforeDef.getAccesModifier();
        int nAfter = afterDef.getAccesModifier();
        if (nAfter < nBefore) {
            if (nBefore == MemberDefinition.PUBLIC) {
                errorWriter.addError("is not public", name, beforeDef.stringDefinition, "is not public in the new implementation");
            }
            else if (nBefore == MemberDefinition.PROTECTED) {
                errorWriter.addError("is not protected", name, beforeDef.stringDefinition, "is not protected in the new implementation");
            }
            else {
                errorWriter.addError("less accessible", name, beforeDef.stringDefinition, "less accessible in the new implementation");
            }
        }

        for (int i = 0; i < modifiers.length; i++) {
            String before = modifiers[i][0];
            String after = modifiers[i][1];
            if (before != null) {
                if (beforeDef.definitions.contains(before) &&
                    !afterDef.definitions.contains(before)) {
                    errorWriter.addError("is not " + before, name, beforeDef.stringDefinition, "is not " + before + " in the new implementation");
                }
            }
            if (after != null) {
                if (!beforeDef.definitions.contains(after) &&
                    afterDef.definitions.contains(after)) {
                    errorWriter.addError("is " + after, name, beforeDef.stringDefinition, "is " + after + " in the new implementation");
                }
            }
        }
    }

    /** prints warning in the setup mode.**/
    private void ignore(String message) {
        if (isIgnorableReported)
            log.println("Ignoring " + message);
    }
}
    
