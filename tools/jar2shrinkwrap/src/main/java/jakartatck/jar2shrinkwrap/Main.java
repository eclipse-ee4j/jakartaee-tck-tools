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
            if(file.startsWith("com.sun.ts.tests")) {
                System.out.println("looking for TCK test package " + file);
                JarProcessor jarProcessor =  Jar2ShrinkWrap.fromPackage(file);
                System.out.println("jarProcessor classes " + jarProcessor.getClasses());
                System.out.println("jarProcessor libraries " + jarProcessor.getLibraries());
                System.out.println("jarProcessor metainf " + jarProcessor.getMetainf());
                System.out.println("jarProcessor webinf " + jarProcessor.getWebinf());
                System.out.println("jarProcessor other files " + jarProcessor.getOtherFiles());
                System.out.println("done with TCK test package " + file);
                System.out.println("");
            }
            File fileInputArchive = new File(file);
            JarVisit visitor = new JarVisit(fileInputArchive);
            JarProcessor jarProcessor = visitor.execute();
            jarProcessor.saveOutput(fileInputArchive);
        }
    }

}