package tck.conversion.log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestLogRegex {
    public static void main(String[] args) {
        final String regex = "^\\[(\\w+)](\\d+:\\d+:\\d+.\\d+)\\s\\((\\w+)\\)\\s.*$";
        final String string = "[INFO]21:37:04:317 (DeploymentMethodInfoBuilder) Parsing(/home/starksm/Dev/Jakarta/wildflytck/jakartaeetck/src/com/sun/ts/tests/jms/core/bytesMsgTopic/build.xml)\n";

        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(string);

        while (matcher.find()) {
            System.out.println("Full match: " + matcher.group(0));

            for (int i = 1; i <= matcher.groupCount(); i++) {
                System.out.println("Group " + i + ": " + matcher.group(i));
            }
        }
    }
}
