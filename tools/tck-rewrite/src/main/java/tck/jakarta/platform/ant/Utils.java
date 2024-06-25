package tck.jakarta.platform.ant;

import org.apache.tools.ant.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class Utils {
    public static <T> List<T> asList(final Enumeration<T> e) {
        return Collections.list(e);
    }

    public static List<String> toList(Enumeration<String> iter) {
        ArrayList<String> list = new ArrayList<>();
        while (iter.hasMoreElements()) {list.add(iter.nextElement());}
        return list;
    }

    public static String toString(Task[] tasks) {
        StringBuilder tmp = new StringBuilder();
        tmp.append('[');
        for (Task task : tasks) {
            tmp.append(toString(task));
            tmp.append("; ");
        }
        tmp.append(']');
        return tmp.toString();
    }
    public static String toString(Task task) {
        return String.format("%s, type=%s, attrs: %s", task.getTaskName(), task.getTaskType(),
                task.getRuntimeConfigurableWrapper().getAttributeMap());
    }
    public static String toDotClassList(String classes) {
        return classes.replace(", ", "\n").replace('$', '.').replace('/', '.');
    }
}
