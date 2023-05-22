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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * JarVisit
 *
 * @author Scott Marlow
 */
public class JarVisit {

    private final String targetFolder;
    private final String archiveFile;

    public JarVisit(String archiveFile, String targetFolder) {
        this.archiveFile = archiveFile;
        this.targetFolder = targetFolder;
    }

    public void execute() {
        File fileTargetFolder = new File(targetFolder);
        if(!fileTargetFolder.exists()) {
            fileTargetFolder.mkdirs();
        }
        JarProcessor jarProcessor;
        if (archiveFile.endsWith(".war"))
            jarProcessor = new WarFileProcessor(archiveFile, fileTargetFolder);
        else
            throw new IllegalStateException("unsupported file type extension: " + archiveFile);

        System.out.println("output will be in " + fileTargetFolder.getName());
        final byte[] buffer = new byte[100*1024];
        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(archiveFile));
            ZipEntry entry = zipInputStream.getNextEntry();
            while(entry != null) {
                jarProcessor.process(entry);
                entry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
            jarProcessor.saveOutput();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
