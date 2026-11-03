package com.example.it;

import jakarta.tck.sigtest.junit.SignatureAssert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Negative integration test: the live API intentionally diverges from the baseline,
 * so {@link SignatureAssert#assertCompatible(String)} must throw {@link AssertionError}.
 *
 * <p>Expected violations from the live API vs the baseline:
 * <ul>
 *   <li>{@code MISSING_METHOD} — {@code execute()} removed from live API</li>
 *   <li>{@code METHOD_RETURN_TYPE_CHANGED} — {@code getCount()} changed from {@code int} to {@code long}</li>
 *   <li>{@code ADDED_METHOD} — {@code describe()} added to live API</li>
 * </ul>
 *
 * <p>The verify.groovy script checks that this test fails with the expected message,
 * and that the overall Maven build still exits 0 (because Surefire is configured with
 * {@code testFailureIgnore=true}).
 */
class ViolationsTest {

    private static final String BASELINE = "/live-classpath-violations.sig.json";

    @Test
    void apiViolationsAreDetected() {
        AssertionError error = assertThrows(
                AssertionError.class,
                () -> SignatureAssert.assertCompatible(BASELINE),
                "Expected AssertionError for intentional API violations"
        );

        String message = error.getMessage();

        // Verify violation summary is present
        assertTrue(message.contains("violation"), "Message should mention 'violation': " + message);

        // Verify the specific violations are reported
        assertTrue(message.contains("MISSING_METHOD"),
                "Expected MISSING_METHOD violation (execute() removed): " + message);
        assertTrue(message.contains("METHOD_RETURN_TYPE_CHANGED"),
                "Expected METHOD_RETURN_TYPE_CHANGED violation (getCount int->long): " + message);
        assertTrue(message.contains("ADDED_METHOD"),
                "Expected ADDED_METHOD violation (describe() added): " + message);
    }
}
