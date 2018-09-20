/*
 * Copyright (c) 2000, 2018 Oracle and/or its affiliates. All Rights Reserved.
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

package com.sun.cts.common;

import java.util.*;

/**
 * The CommandLineArgProc class can be used to process command line arguments.
 * The user creates an instance of this class specifying a set of all possible
 * flags and the actual command line arguments.  This class simply parse the
 * command line arguments looking for valid flags.  If the command line argument
 * following a flag is not a flag this class assumes it is a value associated
 * with previously parsed flag.  The flag and the value are stored in a name
 * value pair data structure.  If a flag is found preceding another flag or is
 * the last command line argument, this class assumes it is flag that has no
 * associated value and simply stores the flag with a null value.  If any flag
 * is found that is not a vlaid flag, that flag is placed in a invlid argument
 * list.  The user can query this list to see if invalid command line arguments
 * were sent to the parsing application.
 *
 * Once the parsing is complete the user can use convenience methods to retrieve
 * the command line flags and their associated values, if they exist.
 *
 * An instance of this class should be instantiated by each user wishing to
 * utilize it's functionality.  In other words this class is not thread safe.
 */
public class CommandLineArgProc {

    private static final String FLAG_DELIMITER = "-";    // all flags start with this string

    private Map      flagValues         = new HashMap();   // holds the flag-value pairs
    private String[] possibleFlags;                        // all known possible flags
    private String[] commandLineArgs;                      // actual command line args
    private List     invalidArgs        = new ArrayList(); // list of args that were invalid
    private String   badFlag;                              // set when user passes in bad flag
    private boolean  ignoreInvalidFlags = false;

    /**
     * Default constructor.
     */
    public CommandLineArgProc() {
    }

    /**
     * Returns an instance of the CommandLineArgProc class with the specified
     * flags and command line arguments.
     *
     * @param possibleFlags The list of flags that the parsing application supports.
     *        Any flag listed in this array may be passed to the parsing application
     *        but not necessarilly.
     * @param commandLineArgs The command line arguments passed to the parsing
     *        application during application invocation.
     */
    public CommandLineArgProc(String[] possibleFlags, String[] commandLineArgs) throws InvalidFlagException {
	if (!(isFlag(possibleFlags))) {
	    throw new InvalidFlagException("bad flag = " + badFlag);
	}
	this.possibleFlags = possibleFlags;
	this.commandLineArgs = commandLineArgs;
    }

    public CommandLineArgProc(String[] possibleFlags,
			      String[] commandLineArgs,
			      boolean ignoreInvalidFlags) 
    {
	this.possibleFlags      = possibleFlags;
	this.commandLineArgs    = commandLineArgs;
	this.ignoreInvalidFlags = ignoreInvalidFlags;
    }
    
    /**
     * Set the list of command line arguments that should be parsed.
     *
     * @param commandLineArgs The list of command line arguments to parse.
     */
    public void setArgs(String[] commandLineArgs) {
	this.commandLineArgs = commandLineArgs;
    }

    /**
     * Returns the command line arguments to be parsed.
     *
     * @return String[] The command line arguments to be parsed.
     */
    public String[] getArgs() {
	return (commandLineArgs);
    }

    /**
     * Sets the list of possible flags.  These are all the valid flags that
     * the application knows about.  If any flag is found in the command line
     * arguments that is not in this list it is considered an invalid flag.
     *
     * @param possibleFlags All valid flags for the parsing application.
     */
    public void setPossibleFlags(String[] possibleFlags) throws InvalidFlagException {
	if (ignoreInvalidFlags || isFlag(possibleFlags)) {
	    this.possibleFlags = possibleFlags;
	} else {
	    throw new InvalidFlagException("bad flag = " + badFlag);
	}
    }

    /**
     * Returns the list of all valid flags for the parsing application.
     *
     * @return String[] All valid flags for the parsing application.
     */
    public String[] getPossibleFlags() {
	return (possibleFlags);
    }

    /**
     * Returns true if the specified flag was found in the list of specified
     * command line arguments and was a valid flag.
     *
     * @param flag The flag to search the list of valid flags for.
     * @return boolean True if the specified flag is found and is valid, else
     *         false.
     */
    public boolean flagPresent(String flag) {
	boolean result = false;

	try {
	    result = (flagValues.containsKey(flag));
	} catch (Exception e) {
	    result = false;
	}
	return (result);
    }
    
    /**
     * Rteuns the associated valus of the specified flag if applicable.  If the
     * specified flag is valid and contains an associated value the vlaue is
     * returned to the user.  If the flag is valid with no associated value
     * or the flag is invalid or not present a null value is returned.
     *
     * @param flag The flag to retrieve the value for.
     * @return String The associated value provided one exists and the flag is
     *         valid.
     */
    public String getFlagValue(String flag) {
	String result = null;
	
	try {
	    return ((String)(flagValues.get(flag)));
	} catch (Exception e) {
	    result = null;
	}
	return (result);
    }

    /**
     * Returns true if the command line processor found any invalid flags.
     *
     * @return boolean True if invalid flags were found else false.
     */
    public boolean hasErrors() {
	if (this.ignoreInvalidFlags) {
	    return (false);
	}
	return (invalidArgs.size() > 0);
    }
    
    /**
     * Returns the list of invalid flags found during command line processing.
     * 
     * @return List List of invalid flags.
     */
    public List getErrantFlags() {
	return (invalidArgs);
    } 

    /**
     * Returns true is the specified string is a flag.  Simply looks at the
     * first character of the specified flag and determines if it is the
     * defined flag character.
     *
     * @param flag The possible flag.
     * @return boolean True if the specified flag is a valid flag.  Meaning the
     *         the first character of the flag matches the flag delimiter.  IF
     *         not false is returned.
     */
    private boolean isFlag(String flag) {
// 	if (ignoreInvalidFlags) {
// 	    return (true);
// 	}
	if (flag == null) {
	    return (false);
	}
	return (flag.startsWith(FLAG_DELIMITER));
    }

    /**
     * Returns true is all the specified strings are flags.  Simply looks at the
     * first character of each specified flag and determines if it is the
     * defined flag character.
     *
     * @param flags The possible flags.
     * @return boolean True if the specified flags are all valid flags.  Meaning
     *         the the first character of each flag matches the flag delimiter.
     *         If any flag is invalid false is returned.
     */
    private boolean isFlag(String[] flags) {
	boolean result = true;

	if (flags == null) {
	    result = false;
	} else {
	    for (int i = 0; i < flags.length; i++) {
		if (!(isFlag(flags[i]))) {
		    result = false;
		    badFlag = flags[i]; // set bad flag so it can be returned to the user
		    break;
		}
	    }
	}
	return (result);
    }

    /**
     * Returns the flag that caused the InvalidFlagException to be thrown.
     * The badFlag variable is only set to a valid value when an invalid
     * flag is detected in the constructor or the setPossibleFlags methods.
     *
     * @return String The invalid flag that caused the InvalidFlagException
     *         to be thrown.
     */
    public String getExceptionFlag() {
	return (badFlag);
    }
    
    /**
     * Returns true is ther specified flag is considered valid.  A valid flag is
     * a flag that is listed within the possible flags list.
     *
     * @param flag The flag to check for validity.
     * @return boolean True if the specified flag is in the list of possible
     *         flag values else false.
     */
    private boolean isValidFlag(String flag) {
	if (ignoreInvalidFlags) {
	    return (true);
	}

	boolean result = false;
	if (flag == null) {
	    return (result);
	}

	for (int i = 0; i < possibleFlags.length; i++) {
	    if (flag.equalsIgnoreCase(possibleFlags[i])) {
		result = true;
		break;
	    }
	}
	return (result);
    }

    /**
     * Returns the command line argument at the specified index provided it is
     * not a flag.  If it is a flag or the specified index is out of range
     * this method simply returns null.
     *
     * @param valueIndex The possible index of a command line flag's associated
     *        value.
     * @return String The value found at the specified index or null if the
     *         command line argument at the index was a flag or the specified
     *         index was beyond the command line argument length.
     */
    private String getFlagValue(int valueIndex) {
	if (valueIndex < commandLineArgs.length) {
	    String possibleValue = commandLineArgs[valueIndex];
	    if (!isFlag(possibleValue)) {
		return (possibleValue);
	    }
	}
	return (null);
    }

    /**
     * Removes the specified flag and its associated value (if it exists)
     * from the list of flags and data held in this CommandLineArgProc.
     *
     * @param String The flag to remove from the list of flags.
     */
    public void removeFlag(String flagName) {
	if (flagValues.containsKey(flagName))  {
	    flagValues.remove(flagName);
	}
    }

    /**
     * Returns any remaining flags held in the flag to value mapping
     * structure.
     *
     * @param return String[] remaining flags held in the flag to
     *        value mapping structure.
     */
    public String[] getRemainingFlags() {
	if (flagValues.size() <= 0) { return (null); }
	String   flag;
	String   value;
	List     flags = new ArrayList();
	Iterator iter  = flagValues.keySet().iterator();
	while (iter.hasNext()) {
	    flag  = (String)iter.next();
	    value = (String)flagValues.get(flag);
	    flags.add(flag);
	    if (value != null) {
		flags.add(value);
	    }
	}
	String[] result = new String[flags.size()];
	return ((String[])(flags.toArray(result)));
    }

    /**
     * Processes the specified command line arguments and places them into
     * and easy access data structure.
     */
    public void processArgs() {
	String currentArg = null;
	String currentArgValue = null;

	if (commandLineArgs == null || possibleFlags == null) {
	    return;
	}

	for (int i = 0; i < commandLineArgs.length; i++) {
	    currentArg = commandLineArgs[i];
	    if (isFlag(currentArg) && isValidFlag(currentArg)) {
		int possibleValueIndex = i + 1;
		currentArgValue = getFlagValue(possibleValueIndex);
		if (currentArgValue != null) {
		    i = possibleValueIndex;
		}
		flagValues.put(currentArg, currentArgValue);
	    } else {
		if (ignoreInvalidFlags) {
		    flagValues.put(currentArg, null);
		} else {
		    invalidArgs.add(currentArg);
		}
	    }
	}
    }

    /**
     * A simple unit test driver to verify functionality.  This is not an all
     * encompassing test just a simple sanity check.
     */
    public static void main(String[] args) {
	try {
	    String[] possibleFlags = {"-flag1", "-flag2", "-flag3", "-flag4", "-flag5"};
	    String[] testArgs = {"-flag", "-flag4", "-flag1", "1", "-flag2", "2", "-flag3", "-flag6", "junk"};
	    CommandLineArgProc clp = new CommandLineArgProc(possibleFlags, testArgs);
	    clp.processArgs();
	    
	    if (clp.hasErrors()) {
		System.out.println("Errors found on command line, Errant flags are:");
		List errors = clp.getErrantFlags();
		for (int i = 0; i < errors.size(); i++) {
		    System.out.println("\t" + (String)(errors.get(i)));
		}
	    } else {
		System.out.println("No Parse Errors");
	    }
	    if (clp.flagPresent("-flag1")) {
		System.out.println("Flag 1 is present value = " + clp.getFlagValue("-flag1"));
	    } else {
		System.out.println("Flag 1 NOT present");
	    }
	    if (clp.flagPresent("-flag2")) {
		System.out.println("Flag 2 is present value = " + clp.getFlagValue("-flag2"));
	    } else {
		System.out.println("Flag 2 NOT present");
	    }
	    if (clp.flagPresent("-flag3")) {
		System.out.println("Flag 3 is present value = " + clp.getFlagValue("-flag3"));
	    } else {
		System.out.println("Flag 3 NOT present");
	    }
	    if (clp.flagPresent("-flag4")) {
		System.out.println("Flag 4 is present value = " + clp.getFlagValue("-flag4"));
	    } else {
		System.out.println("Flag 4 NOT present");
	    }
	    if (clp.flagPresent("-flag5")) {
		System.out.println("Flag 5 is present value = " + clp.getFlagValue("-flag5"));
	    } else {
		System.out.println("Flag 5 NOT present");
	    }
	} catch (Exception e) {
	    System.out.println("Unexpected Exception " + e);
	}
    }
    
} // end class CommandLineArgProc
