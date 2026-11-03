// verify.groovy — resolve-mojo-test IT
// Verifies that the build succeeded, meaning:
// 1. sigtest-next:resolve ran and resolved the stub-api jar.
// 2. Surefire forwarded the property to the test JVM.
// 3. SignatureAssert used the resolved jar path and found no violations.

assert new File(basedir, "build.log").text.contains("BUILD SUCCESS"), "Expected build to succeed: resolve-mojo-test"

// Also verify the Surefire report shows all tests passed
File reportDir = new File(basedir, "target/surefire-reports")
assert reportDir.exists(), "Surefire reports directory not found: ${reportDir}"

def reports = reportDir.listFiles().findAll { f -> f.name.endsWith(".xml") }
assert reports && reports.size() > 0, "No Surefire XML reports found in ${reportDir}"

reports.each { report ->
    def xml = new groovy.xml.XmlSlurper().parse(report)
    int failures = xml.testcase.failure.size()
    int errors = xml.testcase.error.size()
    assert failures == 0, "Unexpected test failures in ${report.name}: ${failures}"
    assert errors == 0, "Unexpected test errors in ${report.name}: ${errors}"
}

true
