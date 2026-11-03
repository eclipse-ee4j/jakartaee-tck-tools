package com.example.resolveapi;

/**
 * Stub interface shipped in the resolve-stub-api jar.
 * Resolved by sigtest-next:resolve and scanned via the file path
 * without being on the test classpath.
 */
public interface ResolveService {

    /**
     * Performs a resolution operation.
     *
     * @param input the input string
     * @return resolved output
     */
    String resolve(String input);
}
