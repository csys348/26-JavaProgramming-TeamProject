package oiot.system;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StatusLogger {
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter LINE_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logDir;

    public StatusLogger(Path logDir) {
        this.logDir = logDir;
    }

    public Path saveSnapshot(String statusText) {
        try {
            Files.createDirectories(logDir);
            Path logFile = logDir.resolve("snapshot_" + LocalDateTime.now().format(FILE_TS) + ".log");
            String body = "[" + LocalDateTime.now().format(LINE_TS) + "] System Snapshot\n"
                    + statusText
                    + System.lineSeparator();
            Files.writeString(logFile, body, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            return logFile;
        } catch (IOException e) {
            throw new IllegalStateException("로그 저장 실패: " + e.getMessage(), e);
        }
    }
}
