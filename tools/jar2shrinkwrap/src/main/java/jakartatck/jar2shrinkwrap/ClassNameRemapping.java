package jakartatck.jar2shrinkwrap;

/**
 * Class name remapping utility that automatically can change EE 10 => EE 11 TCK test package names and/or class names.
 *
 * @author Scott Marlow
 */
public interface ClassNameRemapping {
    default String getName(String className) {
        return className;
    }

    default boolean shouldBeIgnored(String className) {
        return false;
    }

    default String getTargetClassName() { return ""; }

    default String getTargetClassNamePackage() { return ""; }
}
