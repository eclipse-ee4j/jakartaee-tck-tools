package com.example.it;

import jakarta.tck.sigtest.junit.SignatureAssert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Integration test: verifies the sigtest-next:resolve end-to-end flow.
 *
 * <p>The {@code sigtest-next:resolve} mojo runs during {@code process-test-resources} and
 * sets the project property {@code sigtest-next.resolve.api} to the absolute path of the
 * resolved stub-api jar. Surefire forwards this via {@code systemPropertyVariables} so that
 * {@link SignatureAssert} picks it up and scans the jar by file path instead of scanning
 * the test classpath.
 *
 * <p>Because the stub-api jar is intentionally NOT on the test classpath, this test also
 * verifies that the non-loaded-jar path works correctly (ASM scans the file, no classloading).
 */
class ResolveMojoTest {

    private static final String BASELINE = "/resolve-stub-api.sig.json";
    private static final String RESOLVE_PROPERTY = "sigtest-next.resolve.api";

    @Test
    void resolvePropertyIsSet() {
        String value = System.getProperty(RESOLVE_PROPERTY);
        assertNotNull(value, "System property '" + RESOLVE_PROPERTY + "' must be set by sigtest-next:resolve");
        assertFalse(value.startsWith("${"),
                "Property value must not be a Maven placeholder — was it forwarded by Surefire? value=" + value);
    }

    @Test
    void apiMatchesBaseline_viaResolvedJar() {
        // This will use the sigtest-next.resolve.api system property path (priority 2),
        // scanning the resolved jar by file path without loading the classes.
        assertDoesNotThrow(
                () -> SignatureAssert.assertCompatible(BASELINE),
                "API signature check must pass for the resolved stub-api jar"
        );
    }
}
