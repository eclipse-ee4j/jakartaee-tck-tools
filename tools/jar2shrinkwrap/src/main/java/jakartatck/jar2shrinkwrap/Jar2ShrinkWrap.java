package jakartatck.jar2shrinkwrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Jar2ShrinkWrap
 *
 * @author Scott Marlow
 */
public class Jar2ShrinkWrap {

    private static String LegacyTCKFolderPropName = "LegacyTCKFolder";
    private static String defaultFolderName = "legacytck";
    private static String legacyTCKZip = "jakarta-jakartaeetck-10.0.2.zip";
    private static String legacyTCKZipDownload = "https://download.eclipse.org/jakartaee/platform/10/jakarta-jakartaeetck-10.0.2.zip";
    private static String legacyTCKFolder = "jakartaeetck";
    private static String LegacyTCKFolderName = System.getProperty(LegacyTCKFolderPropName,System.getProperty("java.io.tmpdir") + File.separator + defaultFolderName);

    public static JarProcessor fromPackage(String packageName) {
        System.out.println("looking for existing copy of jakarta-jakartaeetck-10.0.2.zip in folder " + LegacyTCKFolderName);
        if (System.getProperty("java.io.tmpdir") == null) {
            System.out.println("java.io.tmpdir needs to point to temp folder, exiting with failure code 3");
            System.exit(3);
        }
        if (LegacyTCKFolderName == null) {
            LegacyTCKFolderName = System.getProperty("java.io.tmpdir") + File.separator + defaultFolderName;
            System.out.println( LegacyTCKFolderPropName + "wasn't specified so will instead use " + LegacyTCKFolderName);
        }
        File legacyTCKFolder = new File(LegacyTCKFolderName);
        legacyTCKFolder.mkdirs();
        System.out.println("looking for existing extracted " + legacyTCKZip + " in folder " + LegacyTCKFolderName );

        File target = new File(legacyTCKFolder,"LegacyTCKFolderName");
        if(target.exists()) {
            System.out.println("reusing existing legacy TCK folder for reading TCK archives" + target.getName());
        } else {
            System.out.println("will download " + legacyTCKZipDownload + " and extract contents into " + target.getName());
            downloadLegacyTCK(target);

            System.out.println("will unzip " + legacyTCKZipDownload + " into " +target.getName());
            unzip(target);
            System.out.println("one time setup is complete");
        }

        System.out.println("Locate the TCK archive that contains the test for package " + packageName);
        if (packageName.startsWith("com.ibm")) {
            System.out.println("ignoring the request for the Batch TCK tests as they were already rewritten and moved to Batch Specification");
        }
        File targetWarFile = locateTargetPackageFolder(target, packageName);
        JarVisit visitor = new JarVisit(targetWarFile);
        return visitor.execute();
    }

    private static File locateTargetPackageFolder(File target, String packageName) {
        File findTCKDistArchive = new File(target,"dist" + package2Name(packageName));
        System.out.println("looking inside of " + findTCKDistArchive.getName() + " for the archive that contains a test client for package " + packageName);
        // TODO: Add support for ear achives which can contain jar + war files
        // TODO: Add support for jar archives
        // Initial support is for war archives
        if (findTCKDistArchive.exists()) {
            File[] possibleMatches = findTCKDistArchive.listFiles();
            File match = null;
            for (File targetWarFile: possibleMatches) {
                if (targetWarFile.getName().endsWith(".war")) {
                    if (match != null) {
                        throw new RuntimeException("found multiple matches for " + packageName + " " + match.getName() + " vs " + targetWarFile.getName());
                    }
                    match = targetWarFile;
                }
            }
            if (match == null) {
                throw new RuntimeException("could not find a match for " + packageName + " in " + target.getName());
            }
            return match;
        } else {
            throw new RuntimeException("could not locate " + packageName + " in " + findTCKDistArchive.getName());
        }
    }

    private static String package2Name(String packageName) {
        return packageName.replace(".", File.pathSeparator);
    }

    private static void unzip(File target) {
    }

    private static void downloadLegacyTCK(File targetFolder) {

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", "wget", "--directory-prefix=" + targetFolder.getName());
        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
            output.append(line + "\n");
            }
            int exitVal = process.waitFor();
            if (exitVal != 0) {
                System.out.println("failure calling wget (for " + targetFolder + ") , Calling exit with code 1.");
                System.exit(1);
            }
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
