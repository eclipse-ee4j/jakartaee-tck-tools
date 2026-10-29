package com.example.api;

/**
 * Stub service interface used by the live-classpath-test IT.
 */
public interface GreetingService {

    /**
     * Returns a greeting for the given name.
     *
     * @param name the name to greet
     * @return greeting string
     */
    String greet(String name);

    /**
     * Returns the number of greetings issued so far.
     *
     * @return greeting count
     */
    int getCount();
}
