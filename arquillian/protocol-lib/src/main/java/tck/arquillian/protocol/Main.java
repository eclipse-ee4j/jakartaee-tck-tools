package tck.arquillian.protocol;

import com.sun.ts.lib.harness.Status;

/**
 * Simple main class for javadoc generation.
 */
public class Main {
    /**
     * Just prints out the protection domain of the {@link Status} class.
     * @param args - unused
     */
    public static void main(String[] args) {
        System.out.printf("Status.pd: %s\n", Status.class.getProtectionDomain());
    }
}
