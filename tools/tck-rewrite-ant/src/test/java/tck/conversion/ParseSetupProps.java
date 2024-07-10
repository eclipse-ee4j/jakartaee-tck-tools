package tck.conversion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;

public class ParseSetupProps {
    static int PREFIX_LEN = "@class.setup_props: ".length();

    public static void main(String[] args) throws IOException {
        HashSet<String> set = new HashSet<>();
        Path setup = Paths.get("/tmp/setup.txt");
        for(String line : Files.readAllLines(setup)) {
            // @class.setup_props:
            int index = line.indexOf('@');
            if(index != -1 && index < line.length() - PREFIX_LEN) {
                String props = line.substring(index+PREFIX_LEN);
                String[] propNames = props.split("; ");
                for(String propName : propNames) {
                    set.add(propName.trim());
                }
            }
        }
        /*
        System.out.printf("String[%d] = {\n", set.size());
        for(String propName : set) {
            System.out.printf("    \"%s\",\n", propName);
        }
        System.out.println("};");
         */
        String[] tsJtePropNames = new String[] {
                "ts_home",
                "sigTestClasspath, Location of JAXWS jar files",
                "logical.hostname.servlet",
                "org.omg.CORBA.ORBClass",
                "whitebox-anno_no_md",
                "whitebox-tx, JNDI name of TS WhiteBox",
                "harness.log.port",
                "whitebox-xa, JNDI name of TS WhiteBox",
                "rauser1",
                "sigTestClasspath, Location of JTA api jar files",
                "whitebox-tx",
                "javamail.protocol",
                "webServerHost",
                "log.file.location",
                "sigTestClasspath",
                "password",
                "platform.mode",
                "whitebox-xa",
                "user",
                "ts_home, The base path of this TCK",
                "sigTestClasspath",
                "javamail.username",
                "sigTestClasspath",
                "authuser",
                "securedWebServicePort",
                "authpassword",
                "whitebox-anno_no_md",
                "whitebox-multianno",
                "sigTestClasspath,",
                "whitebox-mdcomplete",
                "ws_wait",
                "password",
                "db1, the database name with",
                "generateSQL",
                "jms_timeout, in milliseconds - how long to wait on",
                "whitebox-mdcomplete",
                "org.omg.CORBA.ORBClass",
                "rapassword1",
                "jms_timeout",
                "whitebox-mixedmode",
                "rauser1",
                "javamail.server",
                "harness.log.traceflag",
                "webServerPort",
                "ws_wait",
                "whitebox-notx",
                "password,",
                "webServerPort",
                "Driver",
                "authuser",
                "platform.mode",
                "whitebox-permissiondd",
                "securedWebServicePort",
                "log.file.location",
                "java.naming.factory.initial",
                "user",
                "jdbc.db",
                "db.supports.sequence",
                "user",
                "webServerHost",
        };
        Arrays.sort(tsJtePropNames);
        for(String propName : tsJtePropNames) {
            System.out.printf("    \"%s\",\n", propName);
        }
    }
}
