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

import java.io.*;
import java.util.*;

/**
 * A simple class containing some useful file utilities.  No instances
 * of this class can be created (all methods are static).  All methods
 * in this class are synchronized, So if this the only means of file
 * management in an application, all files will be in a consistent state.
 */
public final class FileUtility {

    private static final int MAX_BUFFER_SIZE = 1024;

    /**
     * Do not allow anyone to create an instance of this class.
     */
    private FileUtility() {
    }

    /**
     * Removes the specified directory recursively.  Removes all files
     * and sub-directories contained in the specified directory.
     *
     * @param directory The directory to remove
     * @throws SecurityException If this application does not have
     *         permission to remove the specified location or any of
     *         it's children
     * @throws NullPointerException If the specified directory does
     *         not exist
     */
    public static synchronized void removeDirRecursively(String directory) {
	removeDirRecursively0(directory);
	System.out.println("REMOVED: dir \"" + directory + "\" including sub-dirs.");
    }

    private static synchronized void removeDirRecursively0(String directory)
	throws SecurityException, NullPointerException {
	File dir = null;

	dir = new File(directory);
	String[] filesInDir = dir.list();
	File currentFile = null;
	for (int i = 0; i < filesInDir.length; i++) {
	    currentFile = new File(directory, filesInDir[i]);
	    if (currentFile.isFile()) {
		//		System.out.println("Removing file " + currentFile);
		currentFile.delete();
	    } else if (currentFile.isDirectory()) {
		removeDirRecursively0(currentFile.getPath());
	    } else {
		System.out.println("Error in removeDirRecursively, " +
		    "unknown file type " + currentFile.getPath());
	    }
	}
	//	System.out.println("Removing directory " + dir);
	dir.delete();
    }

    /**
     * Returns the list of filenames held in the specified directory.
     * Sub-directories are not listed.
     *
     * @param dirName The directory to list the files of.
     * @return String[] The list of filenames in the specified directory.
     */
    public static synchronized String[] listDirFiles(String dirName) {
	List filenames = new ArrayList();
	File dir = new File(dirName);
	File[] contents = dir.listFiles();

	for (int i = 0; i < contents.length; i++) {
	    if (contents[i].isFile()) {
		filenames.add(contents[i].getName());
	    }
	}
	String[] result = new String[filenames.size()];
	return ((String[])(filenames.toArray(result)));
    }

    /**
     * Deletes the specified file.
     *
     * @param filename The path and name of the file to delete.
     * @return  boolean True if the file is removed, false otherwise.
     */
    public static synchronized boolean deleteFile(String filename) {
	File file = new File(filename);
	return (file.delete());
    }

    /**
     * Moves the specified source file to the specified destination file.  If the
     * specified destination file exists it is overwritten.
     *
     * @param source The path and name of the file to move.
     * @param destination The path and name of the destination file.
     * @throws FileNotFoundException
     */
    public static synchronized boolean move(String source, String destination)
	throws FileNotFoundException 
    {
	return (move(source, destination, true));
    }

    /**
     * Moves the specified source file to the specified destination file.
     *
     * @param source The path and name of the file to move.
     * @param destination The path and name of the destination file.
     * @param overwriteExisting If false and the specified file exists, an FileNotFoundException
     *        is thrown.  If true the existing file, if it exists, is overwritten.
     * @throws FileNotFoundException
     */
    public static synchronized boolean move(String source, String destination, boolean overwriteExisting)
	throws FileNotFoundException 
    {
	if (!overwriteExisting && fileExists(destination)) {
	    throw new FileNotFoundException("Destination file \"" + destination + "\"exists");
	}
	File sourceFile = new File(source);
	File destinationFile = new File(destination);
	return (sourceFile.renameTo(destinationFile));
    }

    /**
     * Copies the specified source file to the specified destination file.  If the
     * specified destination file exists it is overwritten.
     *
     * @param source The path and name of the file to copy.
     * @param destination The path and name of the destination file.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static synchronized void copy(String source, String destination)
	throws FileNotFoundException, IOException
    {
	copy(source, destination, true);
    }

    /**
     * Copies the specified source file to the specified destination file.
     *
     * @param source The path and name of the file to copy.
     * @param destination The path and name of the destination file.
     * @param overwriteExisting If false and the specified file exists, an FileNotFoundException
     *        is thrown.  If true the existing file, if it exists, is overwritten.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static synchronized void copy(String source, String destination, boolean overwriteExisting)
	throws FileNotFoundException, IOException
    {
	byte[] buffer = new byte[MAX_BUFFER_SIZE];
	int bytesRead = 0;

	if (!overwriteExisting && fileExists(destination)) {
	    throw new FileNotFoundException("Destination file \"" + destination + "\"exists");
	}
	BufferedInputStream sourceStream = 
	    new BufferedInputStream(new FileInputStream(source));
	BufferedOutputStream destinationStream = 
	    new BufferedOutputStream(new FileOutputStream(destination));
	try {
	    while ((bytesRead = sourceStream.read(buffer, 0, buffer.length)) > 0) {
		destinationStream.write(buffer, 0, bytesRead);
	    }
	} finally {
	    sourceStream.close();
	    destinationStream.close();
	}
    }

    public static synchronized void copy(InputStream in, OutputStream out)
	throws FileNotFoundException, IOException
    {
	byte[] buffer = new byte[MAX_BUFFER_SIZE];
	int bytesRead = 0;
	try {
	    while ((bytesRead = in.read(buffer, 0, buffer.length)) > 0) {
		out.write(buffer, 0, bytesRead);
	    }
	} finally {
	    in.close();
	    out.close();
	}
    }

    /**
     * Returns true if the specified file exists and is a file else returns
     * false.
     *
     * @param filename The file to check for existence
     * @return boolean true of the file exists else false
     */
    public static synchronized boolean fileExists(String filename) {
	boolean result = false;

	try {
	    File file = new File(filename);
	    result = file.exists() && file.isFile();
	} catch (NullPointerException npe) {
	    System.err.println("NullPointerException in fileExists");
	    return (false);
	} catch (SecurityException se) {
	    System.err.println("SecurityException in fileExists");
	    return (false);
	}
	return (result);
    }

    /**
     * Creates the specified file.
     *
     * @param filename The file to be created
     * @throws NullPointerException
     * @throws SecurityException
     * @throws IOException
     * @throws Exception
     */
    public static synchronized void createFile(File file)
	throws NullPointerException, SecurityException, IOException {
	boolean result = false;
	
	result = file.createNewFile();
	
	if (!result) {
	    throw(new IOException("Could not create file " + file));
	}
    }

    public static synchronized void createFile(String filename)
	throws NullPointerException, SecurityException, IOException 
    {
	createFile(new File(filename));
    }

    /**
     * Returns true if the specified directory  exists and is a directory
     * else returns false.
     *
     * @param dir The directory to check for existence
     * @return boolean true of the directory exists else false
     */
    public static synchronized boolean dirExists(File dirFile) {
	boolean result = false;

	try {
	    result = dirFile.exists() && dirFile.isDirectory();
	} catch (NullPointerException npe) {
	    System.err.println("NullPointerException in dirExists");
	    return (false);
	} catch (SecurityException se) {
	    System.err.println("SecurityException in dirExists");
	    return (false);
	}
	return (result);
    }

    public static synchronized boolean dirExists(String dir) {
	return dirExists(new File(dir));
    }
    
    /**
     * Creates the specified directory.
     *
     * @param dir The directory to be created
     * @throws NullPointerException
     * @throws SecurityException
     * @throws Exception
     */
    public static synchronized void createDir(File dirFile)
	throws NullPointerException, SecurityException, Exception {
	boolean result = false;
	
	result = dirFile.mkdir();
	if (!result) {
	    throw(new Exception("Could not create directory " + dirFile));
	}
    }
    
    public static synchronized void createDir(String dir)
	throws NullPointerException, SecurityException, Exception 
    {
	createDir(new File(dir));
    }
    

    /**
     * Writes the specified object to the specified disk file.
     *
     * @param location The path and filename to write the object to.
     * @param object The object to write to the disk file.
     * @throws FileNotFoundException
     * @throws SecurityException
     * @throws IOException
     * @throws InvalidClassException
     * @throws NotSerializableException
     */
    public static synchronized void writeObject(String location, Object object)
	throws FileNotFoundException, SecurityException, IOException,
      InvalidClassException, NotSerializableException {
	ObjectOutputStream oos = null;

	try {
	    oos = new ObjectOutputStream(new FileOutputStream(location));
	    oos.writeObject(object);
	} finally {
	    if (oos != null) {
		try {
		    oos.close();
		} catch (IOException ioe) {}
	    }
	}
    }
    
    /**
     * Reads and returns the object contained in the specified disk
     * file.  If no object exists or there is an error reading the object,
     * null is returned.
     *
     * @param location The path and filename of the object file
     * @return Object The object contained in the file or null if there is
     *         an error accessing the file
     */
    public static synchronized Object readObject(String location) {
	ObjectInputStream ois = null;
	Object result = null;

	try {
	    ois = new ObjectInputStream(new FileInputStream(location));
	    result = ois.readObject();
	} catch (Exception e) {
	    System.err.println("Error reading object " + location);
	    result = null;
	} finally {
	    if (ois != null) {
		try {
		    ois.close();
		} catch (IOException ioe) {}
	    }
	}
	return (result);
    }

} // end class FileUtility
