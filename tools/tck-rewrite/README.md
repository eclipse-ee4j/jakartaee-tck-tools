The tck-rewrite tools is a set of OpenRewrite based rules that convert the legacy JavaTest based test into tests that are based on Junit 5 and Arquillian.

There are two rules:
- `tck.jakarta.platform.rewrite.AddArquillianDeployMethod` - This looks to add an Arquillian `@Deployment` method. This depends on the jar2shrinkwrap tool for determining the expected contents of a test that is being rewritten.
- `tck.jakarta.platform.rewrite.ConvertJavaTestNameVisitor` - This transforms the JavaTest `@testName` javadoc tagged tests into `@Test` Junit 5 tests.

There is also a `tck.jakarta.platform.rewrite.JavaTestToArquillianShrinkwrap` recipe that combines these rules to cover both rewrite steps.


