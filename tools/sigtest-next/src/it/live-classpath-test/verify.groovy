// verify.groovy — live-classpath-test IT
// The build must succeed: the live API exactly matches the generated baseline.
assert new File(basedir, "build.log").text.contains("BUILD SUCCESS"), "Expected build to succeed but it failed"
