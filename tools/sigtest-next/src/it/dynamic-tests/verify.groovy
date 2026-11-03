// verify.groovy — dynamic-tests IT
// Verifies that the build succeeded and that Surefire XML reports contain per-class
// DynamicTest entries for AlphaService, BetaService, and [added classes].

assert new File(basedir, "build.log").text.contains("BUILD SUCCESS"), "Expected build to succeed: dynamic-tests"

File reportDir = new File(basedir, "target/surefire-reports")
assert reportDir.exists(), "Surefire reports directory not found: ${reportDir}"

def reports = reportDir.listFiles().findAll { f -> f.name.endsWith(".xml") }
assert reports && reports.size() > 0, "No Surefire XML reports found in ${reportDir}"

// Collect all test case display names across all report files
def allTestNames = [] as List<String>
reports.each { report ->
    def xml = new groovy.xml.XmlSlurper().parse(report)

    // Verify no failures or errors
    int failures = xml.testcase.failure.size()
    int errors = xml.testcase.error.size()
    assert failures == 0, "Unexpected test failures in ${report.name}: ${failures}"
    assert errors == 0, "Unexpected test errors in ${report.name}: ${errors}"

    xml.testcase.each { tc ->
        allTestNames << tc.@name.text()
    }
}

// Confirm per-class dynamic tests appear in Surefire output
assert allTestNames.any { it.contains("AlphaService") },
        "Expected a test case containing 'AlphaService' in Surefire reports. Found: ${allTestNames}"
assert allTestNames.any { it.contains("BetaService") },
        "Expected a test case containing 'BetaService' in Surefire reports. Found: ${allTestNames}"
assert allTestNames.any { it.contains("[added classes]") },
        "Expected a test case containing '[added classes]' in Surefire reports. Found: ${allTestNames}"
