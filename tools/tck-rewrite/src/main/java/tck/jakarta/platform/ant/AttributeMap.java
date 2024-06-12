package tck.jakarta.platform.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.RuntimeConfigurable;

import java.util.Hashtable;

/**
 * A wrapper or the {@link RuntimeConfigurable#getAttributeMap()} that adds property reference resolution
 */
public class AttributeMap {
    private Project project;
    private Hashtable<String, Object> attributes;

    /**
     *
     * @param project - the ant project
     * @param attributes - a RuntimeConfigurable#getAttributeMap() value
     */
    public AttributeMap(Project project, Hashtable<String, Object> attributes) {
        this.project = project;
        this.attributes = attributes;
    }

    public Hashtable<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Resolve the value of the given attribute. This uses the {@link PropertyHelper#parseProperties(String)} method
     * for the associated project.
     *
     * @param name attribute name
     * @return possibly null resolved value
     */
    public String getAttribute(String name) {
        Object rawValue = attributes.get(name);
        String value = null;
        if (rawValue != null) {
            Object resolvedValue = PropertyHelper.getPropertyHelper(project).parseProperties(rawValue.toString());
            value = resolvedValue.toString();
        }
        return value;
    }
    /**
     * Resolve the value of the given attribute. This uses the {@link PropertyHelper#parseProperties(String)} method
     * for the associated project.
     *
     * @param name attribute name
     * @return the resolved value or the defaultValue if {@link #getAttribute(String)} returned null
     */
    public String getAttribute(String name, String defaultValue) {
        String value = getAttribute(name);
        if (value == null) {
            Object resolvedValue = PropertyHelper.getPropertyHelper(project).parseProperties(defaultValue);
            value = resolvedValue.toString();
        }
        return value;
    }

    public boolean containsKey(String key) {
        return attributes.containsKey(key);
    }
}
