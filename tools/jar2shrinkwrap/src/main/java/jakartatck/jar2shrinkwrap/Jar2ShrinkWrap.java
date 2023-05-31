package jakartatck.jar2shrinkwrap;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Jar2ShrinkWrap
 *
 * @author Scott Marlow
 */
public class Jar2ShrinkWrap {

    private static final String LegacyTCKFolderPropName = "LegacyTCKFolder";
    private static final String defaultFolderName = "legacytck";
    private static final String legacyTCKZip = "jakarta-jakartaeetck-10.0.2.zip";
    private static final URL tckurl;
    private static File legacyTckRoot;

    static {
        try {
            tckurl = new URL("https://download.eclipse.org/jakartaee/platform/10/jakarta-jakartaeetck-10.0.2.zip");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String legacyTCKZipDownload = "https://download.eclipse.org/jakartaee/platform/10/jakarta-jakartaeetck-10.0.2.zip";
    private static final String unzippedLegacyTCK = "jakartaeetck";
    private static String LegacyTCKFolderName = System.getProperty(LegacyTCKFolderPropName, System.getProperty("java.io.tmpdir") + File.separator + defaultFolderName);

    /**
     * Look for a previously downloaded TCK bundle, or download it,  and return the root dir containing the unzipped bundle.
     * @return root directory containing the unzipped TCK bundle
     */
    public static File maybeDownloadTck() {
        if(legacyTckRoot != null) {
            return legacyTckRoot;
        }

        System.out.println("looking for existing copy of jakarta-jakartaeetck-10.0.2.zip in folder " + LegacyTCKFolderName);
        if (System.getProperty("java.io.tmpdir") == null) {
            System.out.println("java.io.tmpdir needs to point to temp folder, exiting with failure code 3");
            System.exit(3);
        }
        if (LegacyTCKFolderName == null) {
            LegacyTCKFolderName = System.getProperty("java.io.tmpdir") + File.separator + defaultFolderName;
            System.out.println(LegacyTCKFolderPropName + "wasn't specified so will instead use " + LegacyTCKFolderName);
        }
        File legacyTCKFolder = new File(LegacyTCKFolderName);
        legacyTCKFolder.mkdirs();
        System.out.println("looking for existing extracted " + legacyTCKZip + " in folder " + LegacyTCKFolderName);

        File target = new File(legacyTCKFolder, "LegacyTCKFolderName");
        target.mkdirs();
        File targetTCKZipFile = new File(target, legacyTCKZip);
        if (targetTCKZipFile.exists()) {
            System.out.println("already downloaded " + targetTCKZipFile.getName());
        } else {
            System.out.println("will download " + legacyTCKZipDownload + " and extract contents into " + target.getName());
            downloadUsingStream(tckurl, target);

            System.out.println("will unzip " + legacyTCKZipDownload + " into " + target.getName());
            unzip(target);
            System.out.println("one time setup is complete");
        }
        legacyTckRoot = target;
        return target;
    }
    public static JarProcessor fromPackage(String packageName) {
        // Locate or download the legacy TCK
        File target = maybeDownloadTck();
        System.out.println("Locate the TCK archive that contains the test for package " + packageName);
        if (packageName.startsWith("com.ibm")) {
            System.out.println("ignoring the request for the Batch TCK tests as they were already rewritten and moved to Batch Specification");
        }
        target = new File(target, unzippedLegacyTCK);
        File targetWarFile = locateTargetPackageFolder(target, packageName);
        JarVisit visitor = new JarVisit(targetWarFile);
        return visitor.execute();
    }

    /**
     * Get the candidate pkg names of the tests in the legacy TCK bundle
     * @param rootPkgName - optional root pkg path to filter against, com/sun/ts/tests/servlet
     * @return A possibly empty set of pkg names
     * @throws IOException - on failure to traverse the bundle contents
     */
    public static Set<String> getTestPkgNames(String rootPkgName) throws IOException {
        if(rootPkgName == null) {
            rootPkgName = "";
        }
        // Locate or download the legacy TCK
        File target = maybeDownloadTck();
        Path tckRoot = target.toPath().resolve(unzippedLegacyTCK).resolve("dist");
        System.out.println("Searing in: "+tckRoot.toAbsolutePath());
        TestPkgVisitor visitor = new TestPkgVisitor(tckRoot, rootPkgName);
        Files.walkFileTree(tckRoot, visitor);
        return visitor.getTestPkgs();
    }
    private static String getLegacyTCKFolder() {
        String name = null;
        return name;
    }
    private static File locateTargetPackageFolder(File target, String packageName) {
        File findTCKDistArchive = new File(target, "dist" + File.separator + package2Name(packageName));
        System.out.println("locateTargetPackageFolder will look inside of " + target.getName() + " for findTCKDistArchive = " + findTCKDistArchive.getName());
        System.out.println("looking inside of " + findTCKDistArchive.getName() + " for the archive that contains a test client for package " + packageName);
        // TODO: Add support for ear achives which can contain jar + war files
        // TODO: Add support for jar archives
        // Initial support is for war archives
        if (findTCKDistArchive.exists()) {
            File[] possibleMatches = findTCKDistArchive.listFiles();
            File match = null;
            for (File targetWarFile : possibleMatches) {
                if (targetWarFile.getName().endsWith(".war")) {
                    if (match != null) {
                        throw new IllegalStateException("found multiple matches for " + packageName + " " + match.getName() + " vs " + targetWarFile.getName());
                    }
                    match = targetWarFile;
                }
            }
            return match;
        } else {
            throw new RuntimeException("could not locate " + packageName + " in " + findTCKDistArchive.getName());
        }
    }

    private static String package2Name(String packageName) {
        return packageName.replace(".", File.separator);
    }

    private static void unzip(File fileFolder) {
        File file = new File(fileFolder, legacyTCKZip);
        byte[] buffer = new byte[10240];
        System.out.println("Unzipping " + file.getName());
        try {
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(fileFolder, zipEntry.getName());
                String destDirPath = fileFolder.getCanonicalPath();
                String destFilePath = newFile.getCanonicalPath();
                if (!destFilePath.startsWith(destDirPath + File.separator)) {
                    throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
                }
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Could not create folder " + newFile.getName());
                    }
                } else {
                    if (!newFile.getParentFile().isDirectory() && !newFile.getParentFile().mkdirs()) {
                        throw new IOException("Could not create parent folder for " + newFile.getName());
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(newFile);
                    int len;
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, len);
                    }
                    fileOutputStream.close();
                }
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            zipInputStream.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Finished Unzipping " + file.getName());
    }

    private static void downloadUsingStream(URL url, File fileFolder) {
        try {
            File file = new File(fileFolder, legacyTCKZip);
            BufferedInputStream bis = new BufferedInputStream(url.openStream());
            FileOutputStream fis = new FileOutputStream(file);
            byte[] buffer = new byte[10240];
            int count = 0;
            int loop = 0;
            System.out.println("downloading from " + url + " to " + fileFolder.getName());
            while ((count = bis.read(buffer, 0, 10240)) != -1) {
                fis.write(buffer, 0, count);
                if (loop++ == 1024 ) {
                    System.out.print(".");
                    loop = 0;
                }
            }
            fis.close();
            bis.close();
            System.out.println("finished download of " + url);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
