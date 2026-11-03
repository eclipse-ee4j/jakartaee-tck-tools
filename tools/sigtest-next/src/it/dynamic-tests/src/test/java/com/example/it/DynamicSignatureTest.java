package com.example.it;

import jakarta.tck.sigtest.junit.SignatureAssert;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test: verifies that {@link SignatureAssert#forResource(String)}{@code .dynamicTests()}
 * produces per-class {@link DynamicTest} entries that appear as distinct test cases in Surefire reports.
 *
 * <p>The verify.groovy script checks the Surefire XML to confirm that:
 * <ul>
 *   <li>Individual test case entries exist for each API class ({@code AlphaService}, {@code BetaService}).</li>
 *   <li>An {@code [added classes]} test case is present.</li>
 *   <li>All dynamic tests pass (no failures).</li>
 * </ul>
 */
class DynamicSignatureTest {

    private static final String BASELINE = "/dynapi.sig.json";

    /**
     * Produces one {@link DynamicTest} per baseline class plus {@code [added classes]}.
     * All tests must pass because the live API exactly matches the generated baseline.
     */
    @TestFactory
    Stream<DynamicTest> apiSignaturesDynamic() {
        return SignatureAssert.forResource(BASELINE).dynamicTests();
    }

    /**
     * Sanity-check that the stream contains at least the expected number of tests and
     * includes entries for both API classes.
     */
    @TestFactory
    Stream<DynamicTest> verifyDynamicTestNames() {
        List<DynamicTest> tests = SignatureAssert.forResource(BASELINE)
                .dynamicTests()
                .collect(Collectors.toList());

        // At least: AlphaService, BetaService, [added classes]
        assertTrue(tests.size() >= 3, "Expected at least 3 dynamic tests, got: " + tests.size());

        List<String> names = tests.stream()
                .map(DynamicTest::getDisplayName)
                .collect(Collectors.toList());

        assertTrue(names.contains("com.example.dynapi.AlphaService"),
                "Expected dynamic test for AlphaService, got: " + names);
        assertTrue(names.contains("com.example.dynapi.BetaService"),
                "Expected dynamic test for BetaService, got: " + names);
        assertTrue(names.contains("[added classes]"),
                "Expected [added classes] dynamic test, got: " + names);

        // Return the same tests so Surefire records them individually
        return tests.stream();
    }
}
