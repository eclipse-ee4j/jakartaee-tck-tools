package tck.conversion.log;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class TestLogFormatter extends Formatter {
    static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    static ZoneId localZone = ZoneId.systemDefault();

    @Override
    public String format(LogRecord record) {
        LocalDateTime time = LocalDateTime.ofInstant(record.getInstant(), localZone);
        String loggerName = record.getLoggerName();
        int lastDot = loggerName.lastIndexOf('.');
        String className = loggerName.substring(lastDot + 1);
        return String.format("[%s]%s (%s) %s", record.getLevel(), timeFormatter.format(time), className, record.getMessage());
    }
}
