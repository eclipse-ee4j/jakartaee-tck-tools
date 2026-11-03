// verify.groovy — live-classpath-violations IT
// The Maven build should succeed (testFailureIgnore=true in pom.xml).
// Surefire XML report for ViolationsTest must show a test failure with the expected message.

import groovy.xml.XmlSlurper

assert new File(basedir, "build.log").text.contains("BUILD SUCCESS"), "Build should succeed even with test failures (testFailureIgnore=true)"

// Locate the Surefire XML report for ViolationsTest
File reportDir = new File(basedir, "target/surefire-reports")
assert reportDir.exists(), "Surefire reports directory not found: ${reportDir}"

def reports = reportDir.listFiles().findAll { f -> f.name.contains("ViolationsTest") && f.name.endsWith(".xml") }
assert reports && reports.size() > 0, "No Surefire XML report found for ViolationsTest in ${reportDir}"

def xml = new XmlSlurper().parse(reports[0])

// There must be at least one test failure
def failures = xml.testcase.failure
assert failures.size() > 0, "Expected at least one test failure in ViolationsTest"

String failureText = failures[0].text()
assert failureText.contains("MISSING_METHOD"),
        "Expected MISSING_METHOD in failure message: ${failureText}"
assert failureText.contains("METHOD_RETURN_TYPE_CHANGED"),
        "Expected METHOD_RETURN_TYPE_CHANGED in failure message: ${failureText}"
assert failureText.contains("ADDED_METHOD"),
        "Expected ADDED_METHOD in failure message: ${failureText}"
