package tck.jakarta.platform.ant;

import org.apache.tools.ant.Location;

/**
 * Record for a task referenced from the package target
 * @param name task name
 * @param location build.xml location
 */
public record TaskInfo(String name, Location location) {
}
