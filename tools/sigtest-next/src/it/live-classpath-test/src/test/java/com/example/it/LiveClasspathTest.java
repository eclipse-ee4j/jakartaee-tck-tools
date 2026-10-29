package com.example.it;

import jakarta.tck.sigtest.junit.SignatureAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;

/**
 * Integration test: verifies that {@link SignatureAssert} works correctly when the live API
 * is on the test classpath and the baseline was generated into {@code test-classes}.
 *
 * <p>The baseline is written by the {@code sigtest-next:generate} goal during
 * {@code process-classes} into {@code ${project.build.testOutputDirectory}} so it is
 * available as a classpath resource at test time.
 */
class LiveClasspathTest {

    private static final String BASELINE = "/greeting-api.sig.json";

    /**
     * Single-assertion mode: all violations in one assertion error.
     */
    @Test
    void apiMatchesBaseline() {
        SignatureAssert.assertCompatible(BASELINE);
    }

    /**
     * Dynamic-test mode: one test per baseline class.
     */
    @TestFactory
    Stream<DynamicTest> apiSignaturesDynamic() {
        return SignatureAssert.forResource(BASELINE).dynamicTests();
    }
}
