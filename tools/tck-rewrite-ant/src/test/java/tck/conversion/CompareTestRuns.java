package tck.conversion;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * One off program to read in the junit xml from an EE10 module test run and compare it to the output of the EE11
 * module test run. This reads in the single xml result from the glassfish ci and the multiple xml results from the
 * EE11 module test run and compares them.
 */
public class CompareTestRuns {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: CompareTestRuns <ee10-junit-xml> <ee11-junit-xml-dir>");
            System.exit(1);
        }
        String ee10JunitXml = args[0];
        String ee11JunitXmlDir = args[1];
        System.out.println("Comparing " + ee10JunitXml + " to " + ee11JunitXmlDir);
        // EE10
        try {
            List<Testcase> ee10Testcases = parseJunitXml(ee10JunitXml);
            System.out.println("Found " + ee10Testcases.size() + " testcases in " + ee10JunitXml);
            /*
            for (Testcase testcase : ee10Testcases) {
                System.out.println(testcase);
            }
            */
            // Summarize test count by test package
            System.out.println("--- EE10 test count by package:");
            summarizeTestCountByPackage(ee10Testcases);
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        // EE11
        List<Path> testXmlFiles = Files.walk(Paths.get(ee11JunitXmlDir))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".xml"))
                .toList()
                ;
        List<Testcase> ee11Testcases = new ArrayList<>();
        for (Path testXmlFile : testXmlFiles) {
            try {
                List<Testcase> testcases = parseJunitXml(testXmlFile.toString());
                ee11Testcases.addAll(testcases);
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Found " + ee11Testcases.size() + " testcases in " + ee11JunitXmlDir);
        System.out.println("--- EE11 test count by package:");
        summarizeTestCountByPackage(ee11Testcases);
    }

    record Testcase(String name, String classname, int time) {
    }
    static List<Testcase> parseJunitXml(String junitXml) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(Paths.get(junitXml).toFile());
        List<Node> list = document.selectNodes("//testcase");
        return list.stream()
                .map(CompareTestRuns::parseTestcase)
                .toList();
    }
    static Testcase parseTestcase(Node node) {
        String name = node.valueOf("@name");
        String classname = node.valueOf("@classname");
        String time = node.valueOf("@time");
        int seconds = Float.valueOf(time).intValue();
        return new Testcase(name, classname, seconds);
    }
    static void summarizeTestCountByPackage(List<Testcase> ee10Testcases) {
        HashMap<String, Integer> testCountByPackage = new HashMap<>();
        for (Testcase testcase : ee10Testcases) {
            String packageName = testcase.classname().substring(0, testcase.classname().lastIndexOf('.'));
            testCountByPackage.put(packageName, testCountByPackage.getOrDefault(packageName, 0) + 1);
        }
        for(String packageName : testCountByPackage.keySet()) {
            System.out.println(packageName + ": " + testCountByPackage.get(packageName));
        }
    }
}
