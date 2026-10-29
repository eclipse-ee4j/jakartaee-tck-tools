package com.example.api;

/**
 * Live API for the violations IT.
 * Intentionally diverges from the baseline stored in src/test/resources/:
 * - {@code execute()} has been removed → MISSING_METHOD
 * - {@code getCount()} return type changed from {@code int} to {@code long} → METHOD_RETURN_TYPE_CHANGED
 * - {@code describe()} has been added → ADDED_METHOD
 */
public interface ViolatingService {

    /**
     * Returns the number of operations performed (return type changed from int to long).
     *
     * @return operation count
     */
    long getCount();

    /**
     * Returns a description (method added — not in baseline).
     *
     * @return description
     */
    String describe();

    // execute() intentionally removed
}
