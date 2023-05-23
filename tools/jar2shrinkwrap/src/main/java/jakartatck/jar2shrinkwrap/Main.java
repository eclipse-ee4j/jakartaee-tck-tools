package jakartatck.jar2shrinkwrap;

import java.io.File;

/**
 * ${NAME}
 *
 * @author Scott Marlow
 */
public class Main {
    private static final String TARGET_FOLDER = "targetFolder";

    public static void main(String[] args) {
        final String targetFolder = System.getProperty(TARGET_FOLDER);
        if (targetFolder == null || targetFolder.length() == 0) {
            System.err.println("define the output folder via -D" + TARGET_FOLDER + "=OUTPUT FOLDER NAME");
        }
        System.out.println("targetFolder is " + targetFolder);
        for (String file : args) {
            System.out.println("process file " + file);
            JarVisit visitor = new JarVisit(new File(file));
            JarProcessor jarProcessor = visitor.execute();
            jarProcessor.saveOutput(new File(targetFolder));
        }
    }

}