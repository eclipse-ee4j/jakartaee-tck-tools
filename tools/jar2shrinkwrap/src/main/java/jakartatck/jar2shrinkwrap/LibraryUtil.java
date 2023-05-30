package jakartatck.jar2shrinkwrap;

import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility stub class used in the generated code to map from a test class to the lsit of
 * JavaArchives the test class uses in the @Deployment method.
 */
public class LibraryUtil {
    private static ConcurrentHashMap<Class<?>, List<JavaArchive>> testClassJars = new ConcurrentHashMap<>();
    public static List<JavaArchive> getJars(Class<?> testClass) {
        return testClassJars.get(testClass);
    }

    public static List<JavaArchive> addJar(Class<?> testClass, JavaArchive jar) {
        return addJars(testClass, Collections.singletonList(jar));
    }

    public static List<JavaArchive> addJars(Class<?> testClass, List<JavaArchive> jars) {
        List<JavaArchive> prevJars = Collections.emptyList();
        synchronized (testClass) {
            if(!testClassJars.contains(testClass)) {

            } else {
                prevJars = testClassJars.get(testClass);
                ArrayList<JavaArchive> newJars = new ArrayList<>(prevJars);
                newJars.addAll(jars);
                testClassJars.put(testClass, newJars);
            }
        }
        return prevJars;
    }
}

