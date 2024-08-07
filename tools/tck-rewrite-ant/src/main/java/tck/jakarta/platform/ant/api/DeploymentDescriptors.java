package tck.jakarta.platform.ant.api;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

public class DeploymentDescriptors {
    private static TreeMap<String, String> descriptors = new TreeMap<>(new ArchiveSorter());

    public synchronized static void load() throws IOException {
        if (descriptors.isEmpty()) {
            URL resURL = DeploymentDescriptors.class.getResource("/deployment.properties");
            try (InputStreamReader reader = new InputStreamReader(resURL.openStream())) {
                Properties deployments = new Properties();
                deployments.load(reader);
                for(String key : deployments.stringPropertyNames()) {
                    descriptors.put(key, deployments.getProperty(key));
                }
            }
        }
    }

    /**
     * Custom comparator for sorting deployments based on the deployment name base name and then any parts
     * separated by '_'.
     */
    static class ArchiveSorter implements Comparator<String> {
        public int compare(String a, String b) {
            String[] aParts = a.split("_");
            String[] bParts = b.split("_");
            // First compare the first part of the string
            int compare = aParts[0].compareTo(bParts[0]);
            if (compare == 0) {
                // Continue comparing by parts
                for (int i = 1; i < aParts.length; i++) {
                    if (i < bParts.length) {
                        compare = aParts[i].compareTo(bParts[i]);
                        if (compare != 0) {
                            break;
                        }
                    } else {
                        // A longer deployment name is greater
                        compare = 1;
                        break;
                    }
                }
                if(compare == 0 && bParts.length > aParts.length){
                    compare = -1;
                }
            }
            return compare;
        }
    }



    /**
     * Given the base deployment name, return a string of deployments and subdeployments and the list of deployment
     * descriptors they contain, one per line.
     * @param deploymentName - a base deployment name
     * @return possibly empty string of deployment names to descriptors
     */
    public static String getDeploymentDescriptors(String deploymentName) {
        StringBuilder sb = new StringBuilder();

        String lowerKey = descriptors.lowerKey(deploymentName);
        String higherKey = descriptors.higherKey(deploymentName);
        while(higherKey.startsWith(deploymentName)){
            higherKey = descriptors.higherKey(higherKey);
        }

        SortedMap<String, String> map = descriptors.subMap(lowerKey, false, higherKey, false);
        for (String key : map.keySet()) {
            sb.append(key).append(": ").append(map.get(key)).append("\n");
        }

        return sb.toString();
    }
}
