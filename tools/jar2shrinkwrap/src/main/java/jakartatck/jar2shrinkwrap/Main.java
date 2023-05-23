package jakartatck.jar2shrinkwrap;

import java.io.File;

/**
 * ${NAME}
 *
 * @author Scott Marlow
 */
public class Main {

    public static void main(String[] args) {
        for (String file : args) {
            System.out.println("process input file " + file);
            File fileInputArchive = new File(file);
            JarVisit visitor = new JarVisit(fileInputArchive);
            JarProcessor jarProcessor = visitor.execute();
            jarProcessor.saveOutput(fileInputArchive);
        }
    }

}