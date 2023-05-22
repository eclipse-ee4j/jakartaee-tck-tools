/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package jakartatck.jar2shrinkwrap;

/**
 * ${NAME}
 *
 * @author Scott Marlow
 */
public class Main {
    private static final String TARGET_FOLDER="targetFolder";
    public static void main(String[] args) {
        final String targetFolder = System.getProperty(TARGET_FOLDER);
        if(targetFolder == null || targetFolder.length() == 0) {
            System.err.println("define the output folder via -D" + TARGET_FOLDER + "=OUTPUT FOLDER NAME");
        }
        System.out.println("targetFolder is " + targetFolder);
        for(String file:args) {
            System.out.println("process file " + file);
            JarVisit visitor = new JarVisit(file, targetFolder);
            visitor.execute();

        }
    }

}