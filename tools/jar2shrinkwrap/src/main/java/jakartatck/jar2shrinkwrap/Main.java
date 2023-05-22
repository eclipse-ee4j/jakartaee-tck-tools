package jakartatck.jar2shrinkwrap;

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
            JarVisit visitor = new JarVisit(file, targetFolder);
            visitor.execute();

        }
    }

}