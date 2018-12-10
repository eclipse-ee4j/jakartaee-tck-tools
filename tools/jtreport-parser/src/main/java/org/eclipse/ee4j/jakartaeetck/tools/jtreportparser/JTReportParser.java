/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.eclipse.ee4j.jakartaeetck.tools.jtreportparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author anajosep
 */
public class JTReportParser {

    //eg:
    //start=Wed Jan 18 09\:46\:08 UTC 2017
    //end=Wed Jan 18 09\:46\:08 UTC 2017
    private static final String DATE_PATTERN = "(\\w{3}\\s\\w{3}\\s\\d{2}\\s\\d{2}\\\\:\\d{2}\\\\:\\d{2}.*\\d{4}$)";
    private static final Pattern START_TIME_PATTERN = Pattern.compile("start=" + DATE_PATTERN);
    private static final Pattern END_TIME_PATTERN = Pattern.compile("end=" + DATE_PATTERN);
    private static final int MAX_RETRY_COUNT = 1;

    private static long getDuration(File sysoutFile, TestSuite suite) throws FileNotFoundException {
        long duration = 0L;
        if (!sysoutFile.exists()) {
            return duration;
        }
        String startTime = null;
        String endTime = null;
        try (Scanner in = new Scanner(sysoutFile)) {
            while (in.hasNext()) {
                String line = in.nextLine();
                Matcher m = START_TIME_PATTERN.matcher(line);
                if (m.find()) {
                    startTime = m.group(1).replace("\\", "");
                }
                m = END_TIME_PATTERN.matcher(line);
                if (m.find()) {
                    endTime = m.group(1).replace("\\", "");
                }
                if (startTime != null && endTime != null) {
                    break;
                }
            }
            if (startTime == null || endTime == null) {
                return duration;
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy");
            LocalDateTime startDateTime = LocalDateTime.parse(startTime, formatter);
            LocalDateTime endDateTime = LocalDateTime.parse(endTime, formatter);
            suite.setStartDateTime(startDateTime);
            suite.setEndDateTime(endDateTime);
            duration = Duration.between(startDateTime, endDateTime).getSeconds();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(System.err);
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            throw ex;
        }
        return duration;
    }

    private static String getLastLine(String sysoutFilePath) throws IOException {
        String lastLine;
        try (Stream<String> stream = Files.lines(Paths.get(sysoutFilePath))) {
            lastLine = stream.reduce((a, b) -> b).orElse("");
        } catch (NoSuchFileException ex) {
            lastLine = "";
        }
        return lastLine;
    }

    public static File getHtmlDir(String reportDirPath, String componentName)
            throws FileNotFoundException {
        File reportDir = new File(reportDirPath, componentName);
        if (!reportDir.exists()) {
            throw new FileNotFoundException("The report base dir " + reportDir + " does not exist");
        }
        File htmlDir = new File(reportDir, "html");
        if (!htmlDir.exists()) {
            if (new File(reportDir, "report.html").exists()) {
                return reportDir;
            }
            throw new FileNotFoundException("The html dir " + htmlDir + " does not exist");
        }
        return htmlDir;
    }

    public static File getIndexedHtmlDir(String reportDirPath, String componentName, int index)
            throws FileNotFoundException {
        File reportDir = new File(reportDirPath, componentName);
        if (!reportDir.exists()) {
            throw new FileNotFoundException("The report base dir " + reportDir + " does not exist");
        }
        File htmlDir = new File(reportDir, "html~" + index + "~");
        if (!htmlDir.exists()) {
            if (new File(reportDir, "report.html").exists()) {
                return reportDir;
            }
            throw new FileNotFoundException("The html dir " + htmlDir + " does not exist");
        }
        return htmlDir;
    }

    public static Set<TestCase> parseKnowFailures(final String knownFailuresDirPath, final String component) throws IOException {
        Set<TestCase> testCases = new HashSet<>();
        if (knownFailuresDirPath == null) {
            return testCases;
        }
        File knownFailuresDir = new File(knownFailuresDirPath);
        String parentComponentName = null;
        String flattenedComponentName = null;
        String componentFileName;
        if (component.contains("/")) {
            parentComponentName = component.split("/")[0];
            flattenedComponentName = component.replaceAll("/", "_");
        }

        if (!knownFailuresDir.exists()) {
            System.out.println("[warning] known failure directory "
                    + "does not exist." + knownFailuresDir);
            return testCases;
        } else if (!knownFailuresDir.isDirectory()) {
            System.out.println("[warning] known failure directory "
                    + "is not a directory. " + knownFailuresDir);
            return testCases;
        } else if (!knownFailuresDir.canRead()) {
            System.out.println("[warning] known failure directory "
                    + "is not readable. " + knownFailuresDir);
            return testCases;
        } else if (!new File(knownFailuresDir, component + ".txt").exists()) {
            if (new File(knownFailuresDir, parentComponentName + ".txt").exists()) {
                componentFileName = parentComponentName;
            } else if (new File(knownFailuresDir, flattenedComponentName + ".txt").exists()) {
                componentFileName = flattenedComponentName;
            } else {
                System.out.println("[warning] known failure file does not exist in directory \'"
                        + knownFailuresDir + "\' for component \'" + component + "\'");
                return testCases;
            }
        } else {
            componentFileName = component;
        }

        try (Stream<String> stream
                = Files.lines(Paths.get(knownFailuresDirPath, componentFileName + ".txt"))) {
            testCases
                    = stream.map(JTReportParser::createKnownFailureTestCase).
                            filter(x -> x != null).
                            collect(Collectors.toSet());
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            throw ex;
        }
        return testCases;
    }

    public static TestCase createKnownFailureTestCase(String text) {
        if (!text.startsWith("#") && text.contains("#")) {
            String testName = text.split("#")[1];
            String className = text.split("#")[0].replace(".java", "").replaceAll("/", ".");
            System.out.println("Adding known failure [class: " + className
                    + ", method:" + testName + "]");
            return new TestCase(testName, className, 0L, TestStatus.FAILED, "");
        } else {
            System.out.println("[Warning] Ignoring commented/empty line \'"
                    + text + "\' in known failures file ");
            return null;
        }
    }

    public static Set<TestCase> parseTestCases(String reportDirPath,
            String component, TestSuite suite, TestStatus testType,
            Set<TestCase> knownFailures) throws IOException {
        Set<TestCase> testCases = new HashSet<>();
        for (int i = 1; i <=MAX_RETRY_COUNT; i++) {
            try {
                String htmlDir = getIndexedHtmlDir(reportDirPath, component, i).getAbsolutePath();
                Set<TestCase> aTestCaseSet = parseTestCasesPerHtmlDir(htmlDir, component, suite, testType, knownFailures);
                aTestCaseSet.forEach((TestCase aTestCase) -> {
                    if (testCases.contains(aTestCase)){
                        System.out.println("[INFO] Removing test case already present" + aTestCase.getName());
                        testCases.remove(aTestCase);
                    }
                    testCases.add(aTestCase);
                });
                System.out.println("[INFO] Size of the Test Case set after parsing html dir with index " + 
                        i + ":" + testCases.size());
            } catch (FileNotFoundException ex) {
                System.out.println("[INFO] Indexed html backup dir not found.");
                break;
            }
        }
        System.out.println("[INFO] Size of the Test Case set after parsing all backup html dirs" + testCases.size());
        Set<TestCase> aTestCaseSet = parseTestCasesPerHtmlDir(getHtmlDir(reportDirPath, component).getAbsolutePath(), component,
                suite, testType, knownFailures);
        aTestCaseSet.forEach((TestCase aTestCase) -> {
                    if (testCases.contains(aTestCase)){
                        System.out.println("[INFO] Removing test case already present" + aTestCase.getName());
                        testCases.remove(aTestCase);
                    }
                    testCases.add(aTestCase);
                });
        System.out.println("[INFO] Size of the Test Case set after parsing all html dirs" + testCases.size());
        return testCases;
    }

    public static Set<TestCase> parseTestCasesPerHtmlDir(String htmlDirPath,
            String component, TestSuite suite, TestStatus testType,
            Set<TestCase> knownFailures) throws IOException {
        Set<TestCase> testCases = new HashSet<>();
        File testCaseHtmlFile
                = new File(htmlDirPath, testType.getHtmlFileName());
        if (!testCaseHtmlFile.exists()) {
            return testCases;
        }

        Document doc = Jsoup.parse(testCaseHtmlFile, StandardCharsets.UTF_8.name());
        Elements links = doc.select("a[href]");
        long totalDuration = 0L;
        int knownTestFailureCount = 0;
        int newTestFailureCount = 0;
        for (int i = 0; i < links.size(); i++) {
            String sysoutFilePath = links.get(i).attr("href");
            String text = links.get(i).text();
            String testName;
            String className = "";
            if (text.contains("#")) {
                testName = text.split("#")[1];
                className = text.split("#")[0].replace(".java", "").replaceAll("/", ".");
            } else {
                testName = text;
            }
            File sysoutFile = new File(sysoutFilePath);
            long duration = getDuration(sysoutFile, suite);
            totalDuration += duration;
            TestCase test = new TestCase(testName, className, duration, testType, sysoutFilePath);
            String errorMsg = getLastLine(sysoutFilePath);
            test.setErrorMsg(errorMsg);

            if (TestStatus.FAILED == testType) {
                if (!knownFailures.contains(test)) {
                    testCases.add(test);
                    System.out.println("Found a new failure:" + test);
                    newTestFailureCount++;
                } else {
                    test = new TestCase(testName, className, duration, TestStatus.EXCLUDED, sysoutFilePath);
                    testCases.add(test);
                    System.out.println("Found a known failure:" + test
                            + ". So marking it as excluded in report");
                    knownTestFailureCount++;
                }
            } else {
                testCases.add(test);
            }
        }
        switch (testType) {
            case PASSED:
                suite.setPassedTestsDuration(totalDuration);
                suite.setPassedTestsCount(testCases.size());
                break;
            case FAILED:
                suite.setFailedTestsDuration(totalDuration);
                suite.setFailedTestsCount(newTestFailureCount);
                suite.setExcludedTestsCount(knownTestFailureCount);
                break;
            case ERROR:
                suite.setErrorTestsDuration(totalDuration);
                suite.setErrorTestsCount(testCases.size());
                break;
            case EXCLUDED:
                break;
            default:
                throw new IllegalArgumentException("Invalid test case type:" + testType);
        }
        return testCases;
    }

    public static Set<TestCase> parseExcludedTests(String reportDirPath, String component, TestSuite suite) throws IOException {
        Set<TestCase> testCases = new HashSet<>();
        File excludedTestHtml = new File(getHtmlDir(reportDirPath, component),
                TestStatus.EXCLUDED.getHtmlFileName());
        if (!excludedTestHtml.exists()) {
            return testCases;
        }
        Document doc = Jsoup.parse(excludedTestHtml, StandardCharsets.UTF_8.name());
        Elements allTables = doc.select("table");
        if (!allTables.isEmpty()) {
            Element excludedTestsTable = allTables.get(0);
            Elements rows = excludedTestsTable.select("tr");
            for (int i = 1; i < rows.size(); i++) { //first row is the col names so skip it.
                Element row = rows.get(i);
                Elements cols = row.select("td");
                String text = cols.get(0).text();
                boolean isTestForCurrentComponent;
                if ("jbatch".equals(component) && text.startsWith("com/ibm/" + component + "/")) {
                    isTestForCurrentComponent = true;
                } else {
                    isTestForCurrentComponent = text.startsWith("com/sun/ts/tests/" + component + "/");
                }
                if (!isTestForCurrentComponent) {
                    continue;
                }
                String[] textArray = new String[]{};
                if (text.contains("#")) {
                    textArray = text.split("#");
                }
                if (textArray.length < 2) {
                    System.out.println("[Warning] the test string does not "
                            + "contain class name and method name");
                } else {
                    String testName = textArray[1];
                    String className = textArray[0].replace(".java", "").replaceAll("/", ".");
                    TestCase test = new TestCase(testName, className, 0L, TestStatus.EXCLUDED, "");
                    testCases.add(test);
                }
            }
        } else {
            System.out.println("[Warning]: There are no excluded tests to be processed.");
        }
        suite.setExcludedTestsCount(testCases.size());
        return testCases;
    }

    public static Properties parseProperties(String reportDirPath, String component) throws IOException {
        Properties properties = new Properties();
        Document doc = Jsoup.parse(new File(getHtmlDir(reportDirPath, component), "env.html"), "utf-8");
        Elements allTables = doc.select("table");
        if (!allTables.isEmpty()) {
            Element envTable = allTables.get(0);
            Elements rows = envTable.select("tr");
            for (int i = 1; i < rows.size(); i++) { //first row is the col names so skip it.
                Element row = rows.get(i);
                Elements cols = row.select("td");
                String value = cols.get(1).text();
                value = value.replaceAll("\"", "&#34;");
                value = value.replaceAll("&", "&#38;");
                value = value.replaceAll("'", "&#39;");
                value = value.replaceAll("<", "&lt;");
                value = value.replaceAll(">", "&gt;");
                properties.setProperty(cols.get(0).text(), value);
            }
        } else {
            System.out.println("[Warning]: There are no environment properties to be processed.");
        }
        return properties;
    }

    public static void dumpPropertiesXML(Properties props, PrintWriter writer) {
        writer.println("<properties>");
        props.stringPropertyNames().stream().forEach((property) -> {
            writer.println("<property name=\"" + property + "\" value=\"" + props.getProperty(property) + "\" />");
        });
        writer.println("</properties>");
    }

    public static void main(String[] args) throws Exception {
        File argsFile;
        String baseJTReportDirPath = null;
        String junitReportDirPath = null;
        String knownFailuresDirPath = null;
        String reportAggregatorDirPath = null;
        if (args.length == 3) {
            argsFile = new File(args[0]);
            if (!argsFile.exists()) {
                throw new IllegalArgumentException("The args file \'" + args[0]
                        + "\' does not exist");
            }
            knownFailuresDirPath = System.getProperty("cts.knownFailures.dir");
            reportAggregatorDirPath = System.getProperty("cts.report.aggregator.dir");
            baseJTReportDirPath = args[1];
            junitReportDirPath = args[2];
            if (!new File(baseJTReportDirPath).exists()) {
                throw new IllegalArgumentException("The JT report dir \'" + baseJTReportDirPath + "\' does not exist");
            }
            if (!new File(junitReportDirPath).exists()) {
                throw new IllegalArgumentException("The junit report dir \'" + junitReportDirPath + "\' does not exist");
            }
        } else {
            System.out.println("Specify the required arguments: argsFile baseReportDir junitReportDir");
            return;
        }

        try (Scanner in = new Scanner(argsFile)) {
            while (in.hasNextLine()) {
                String line = in.nextLine();
                System.out.println("Read line: " + line);
                String[] splitLine = line.split("\\s+");
                if (splitLine.length != 3) {
                    System.out.println("[WARN] Could not get the required arguments."
                            + "processed line does not meet the requirements");
                    continue;
                }
                String id = splitLine[0];
                String component = splitLine[1];
                String hostname = splitLine[2];
                try {
                    if (!getHtmlDir(baseJTReportDirPath, component).exists()) {
                        System.out.println("[WARN] Skipping JUnit report generation "
                                + "for component \'" + component + "\'");
                        continue;
                    }
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace(System.err);
                    continue;
                }
                File junitReportXml = new File(junitReportDirPath,
                        component.replaceAll("/", "-") + "-junit-report.xml");
                System.out.println("Creating report file:" + junitReportXml);
                PrintWriter junitReportWriter = new PrintWriter(junitReportXml);
                System.out.println("Creating test suite with id=" + id + ", component="
                        + component + ", hostname=" + hostname);
                TestSuite suite = new TestSuite(id, component, hostname);
                try {
                    Set<TestCase> knownFailures = new HashSet<>();
                    if (knownFailuresDirPath != null) {
                        knownFailures = parseKnowFailures(knownFailuresDirPath, component);
                    }
                    Set<TestCase> passedTests
                            = parseTestCases(baseJTReportDirPath, component, suite,
                                    TestStatus.PASSED, knownFailures);
                    Set<TestCase> failedTests
                            = parseTestCases(baseJTReportDirPath, component, suite,
                                    TestStatus.FAILED, knownFailures);
                    Set<TestCase> errorTests
                            = parseTestCases(baseJTReportDirPath, component, suite,
                                    TestStatus.ERROR, knownFailures);
                    Set<TestCase> excludedTests
                            = parseExcludedTests(baseJTReportDirPath, component, suite);
                    Properties props = parseProperties(baseJTReportDirPath, component);
                    junitReportWriter.println(suite.getXMLStartElement());
                    dumpPropertiesXML(props, junitReportWriter);
                    Set<TestCase> newFailures = new HashSet<>();
                    Set<TestCase> excludedFailures = new HashSet<>();
                    failedTests.stream().forEach((testCase) -> {
                        if (TestStatus.EXCLUDED == testCase.getStatus()) {
                            excludedFailures.add(testCase);
                        } else {
                            newFailures.add(testCase);
                        }
                    });
                    newFailures.stream().sorted(
                            Comparator.comparing(TestCase::getName))
                            .forEach(x -> x.toXML(junitReportWriter));
                    excludedFailures.stream().sorted(
                            Comparator.comparing(TestCase::getName))
                            .forEach(x -> x.toXML(junitReportWriter));
                    errorTests.stream().sorted(
                            Comparator.comparing(TestCase::getName))
                            .forEach(x -> x.toXML(junitReportWriter));
                    passedTests.stream().sorted(
                            Comparator.comparing(TestCase::getName))
                            .forEach(x -> x.toXML(junitReportWriter));
                    excludedTests.stream().sorted(
                            Comparator.comparing(TestCase::getName))
                            .forEach(x -> x.toXML(junitReportWriter));
                    junitReportWriter.println(suite.getXMLEndElement());
                    System.out.println("Successfully created JUnit report for component \'"
                            + component + "\'");
                    if (reportAggregatorDirPath != null) {
                        appendToReportAggregator(reportAggregatorDirPath, component, suite);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                } finally {
                    junitReportWriter.flush();
                    junitReportWriter.close();
                }
            }
        }
    }

    private static void appendToReportAggregator(String reportAggregatorDirPath,
            String component, TestSuite suite) throws IOException {
        File aggregatorFile = new File(reportAggregatorDirPath, component);
        if (!aggregatorFile.exists()) {
            aggregatorFile.createNewFile();
        }
        FileWriter writer = new FileWriter(aggregatorFile, true);
        String newLine = System.getProperty("line.separator");
        writer.append(newLine);
        writer.append(suite.asText());
        writer.append(newLine);
    }
}
