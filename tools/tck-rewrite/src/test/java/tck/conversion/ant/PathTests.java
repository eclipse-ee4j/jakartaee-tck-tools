package tck.conversion.ant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path resolution tests
 */
public class PathTests {
    public static void main(String[] args) throws IOException {
        Path sourceRoot = Paths.get("/home/starksm/Dev/Jakarta/rh-platform-tck");
        Path buildXml = Paths.get("/home/starksm/Dev/Jakarta/rh-platform-tck/jws/src/main/java/com/sun/ts/tests/jws/webparam/webparam1/client/build.xml");
        Path target = buildXml.getParent();
        while(!target.equals(sourceRoot)) {
            target = target.getParent();
        }
        String path = "../";
        target = buildXml.getParent();
        while(!Files.isSameFile(sourceRoot, target)) {
            target = buildXml.getParent().resolve(path).normalize();
            path = "../" + path;
        }
        System.out.println("done1");

        path = "../";
        target = buildXml.getParent();
        while(!sourceRoot.equals(target)) {
            target = buildXml.getParent().resolve(path).normalize();
            path = "../" + path;
        }
        System.out.println("done2");

        Path build2Xml = Paths.get("/home/starksm/Dev/Jakarta/rh-platform-tck/jws/src/main/java/com/sun/ts/tests/jws/webparam/webparam1/client/build.xml");
        path = "../../../../../../../../../jws-common/src/main/java/com/sun/ts/tests/jws/common/xml/common.xml";
        target = build2Xml.getParent().resolve(path).normalize();
        boolean resolve = false;
        while(target.startsWith(sourceRoot)) {
            path = "../" + path;
            target = build2Xml.getParent().resolve(path).normalize();
            if(Files.isReadable(target)) {
                resolve = true;
                System.out.printf("Correct import is: %s\n", path);
                System.out.println(target.toFile().getCanonicalFile());
            }
        }
        System.out.println("done3, "+resolve);

        path = "../../../../../../../../../src/com/sun/ts/tests/jws/common/xml/common.xml";
        target = build2Xml.getParent().resolve(path).normalize();
        resolve = false;
        while(target.startsWith(sourceRoot)) {
            path = "../" + path;
            target = build2Xml.getParent().resolve(path).normalize();
            if(Files.isReadable(target)) {
                resolve = true;
                System.out.printf("Correct import is: %s\n", path);
                System.out.println(target.toFile().getCanonicalFile());
            }
        }
        System.out.println("done4, "+resolve);
    }
}
