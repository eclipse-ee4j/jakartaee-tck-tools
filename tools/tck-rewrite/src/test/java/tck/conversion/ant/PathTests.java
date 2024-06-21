package tck.conversion.ant;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Path resolution tests
 */
public class PathTests {
    public static void main(String[] args) throws IOException, URISyntaxException {
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

        target = Paths.get("/home/starksm/tmp/legacytck/LegacyTCKFolderName/jakartaeetck/src/com/sun/ts/tests/ejb30/bb/session/stateless/basic/build.xml");
        Path ejbJar = Paths.get("/home/starksm/tmp/legacytck/LegacyTCKFolderName/jakartaeetck/src/com/sun/ts/tests/ejb30/bb/session/stateless/basic/ejb3_bb_stateless_basic_ejb.xml");
        URL jarURL = new URL("jar:file:/home/starksm/.m2/repository/jakarta/tck/ejb30/11.0.0-SNAPSHOT/ejb30-11.0.0-SNAPSHOT.jar!/com/sun/ts/tests/ejb30/bb/session/stateless/basic/ejb3_bb_stateless_basic_ejb.xml");
        Path ejbJar2 = Paths.get(jarURL.toURI());
        // Find the com path
        int index = 0;
        for(;!ejbJar2.getName(index).toString().equals("com"); index ++) {
        }
        System.out.println("com/sun/.../ejb-jar.xml, "+ejbJar2.subpath(index, ejbJar2.getNameCount()));

        System.out.println("being5, "+target);
        System.out.println("done4, "+resolve);
    }
}
