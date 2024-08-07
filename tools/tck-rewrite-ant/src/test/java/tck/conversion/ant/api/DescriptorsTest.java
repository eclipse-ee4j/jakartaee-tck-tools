package tck.conversion.ant.api;

import org.junit.jupiter.api.Test;
import tck.jakarta.platform.ant.api.DeploymentDescriptors;

import java.io.IOException;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

public class DescriptorsTest {
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
    @Test
    public void testTreeMap() {
        ArchiveSorter sorter = new ArchiveSorter();
        System.out.printf("sorter.compare(xa_event, xa_event): %d\n", sorter.compare("xa_event", "xa_event"));
        System.out.printf("sorter.compare(xa_event, xa_event_jsp_vehicle): %d\n", sorter.compare("xa_event", "xa_event_jsp_vehicle"));
        System.out.printf("sorter.compare(xa_event_jsp_vehicle, xa_event): %d\n", sorter.compare("xa_event_jsp_vehicle", "xa_event"));
        System.out.printf("sorter.compare(xa_event_jsp_vehicle, xa_event_jsp_vehicle_web): %d\n", sorter.compare("xa_event_jsp_vehicle", "xa_event_jsp_vehicle_web"));
        System.out.printf("sorter.compare(xa_event_jsp_vehicle_web, xa_event_jsp_vehicle): %d\n", sorter.compare("xa_event_jsp_vehicle_web", "xa_event_jsp_vehicle"));

        TreeMap<String, String> descriptors = new TreeMap<>(sorter);
        //descriptors.put("xa_event", "xa_event");
        descriptors.put("xa_connection_servlet_vehicle", "");
        descriptors.put("xa_event_ejb_vehicle", "");
        descriptors.put("xa_event_ejb_vehicle_client", "META-INF/application-client.xml,jar.sun-application-client.xml");
        descriptors.put("xa_event_ejb_vehicle_ejb", "META-INF/ejb-jar.xml,jar.sun-ejb-jar.xml");
        descriptors.put("xa_event_jsp_vehicle", "");
        descriptors.put("xa_event_jsp_vehicle_web", "WEB-INF/web.xml,war.sun-web.xml");
        descriptors.put("xa_event_servlet_vehicle", "");
        descriptors.put("xa_event_servlet_vehicle_web", "WEB-INF/web.xml,war.sun-web.xml");
        descriptors.put("xa_lifecycle_ejb_vehicle", "");

        System.out.printf("floorKey(xa_event): %s\n", descriptors.floorKey("xa_event"));
        System.out.printf("floorEntry(xa_event): %s\n", descriptors.floorEntry("xa_event"));
        System.out.printf("ceilingKey(xa_event): %s\n", descriptors.ceilingKey("xa_event"));
        System.out.printf("ceilingEntry(xa_event): %s\n", descriptors.ceilingEntry("xa_event"));
        System.out.printf("lowerKey(xa_event): %s\n", descriptors.lowerKey("xa_event"));
        System.out.printf("higherKey(xa_event): %s\n", descriptors.higherKey("xa_event"));
        String higherKey = descriptors.higherKey("xa_event");
        while(higherKey.startsWith("xa_event")){
            higherKey = descriptors.higherKey(higherKey);
        }
        System.out.printf("Non-xa_event higherKey(xa_event): %s\n", higherKey);

        System.out.println("\ndescendingKeySet:");
        descriptors.descendingKeySet().forEach(System.out::println);
        System.out.println("--end\n");

        SortedMap<String, String> xa_event = descriptors.subMap(descriptors.lowerKey("xa_event"), descriptors.higherKey("xa_event"));
        System.out.println("xa_event.map:");
        xa_event.forEach((k, v) -> System.out.println(k + ": " + v));

        SortedMap<String, String> xa_event2 = descriptors.subMap(descriptors.lowerKey("xa_event"), false, higherKey, false);
        System.out.println("\nxa_event2.map:");
        xa_event2.forEach((k, v) -> System.out.println(k + ": " + v));
    }
    @Test
    public void test_connector_xa_event() throws IOException {
        DeploymentDescriptors.load();
        String descriptors = DeploymentDescriptors.getDeploymentDescriptors("xa_event");
        System.out.println(descriptors);
    }

    @Test
    public void test_ejb30_assembly_initorder_appclientejb() throws IOException {
        DeploymentDescriptors.load();
        String descriptors = DeploymentDescriptors.getDeploymentDescriptors("ejb3_assembly_initorder_appclientejb");
        System.out.println(descriptors);
    }
}
