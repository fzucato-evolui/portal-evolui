package br.com.evolui.portalevolui.web.rest.dto.aws;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupRestoreRDSStatusDTO {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private LocalDateTime timeStamp;
    private String message;
    private Level logLevel;
    @JsonIgnore
    private String threadName;
    @JsonIgnore
    private Class className;
    private boolean finished;
    private boolean heartbeat;

    public BackupRestoreRDSStatusDTO() {
        this.timeStamp = LocalDateTime.now();
    }
    public BackupRestoreRDSStatusDTO(String message, Level logLevel, Class className, boolean log) {
        this.timeStamp = LocalDateTime.now();
        this.message = message;
        this.logLevel = logLevel;
        this.threadName = Thread.currentThread().getName();
        this.className = className;
        if (log) {
            LoggerFactory.getLogger(className).atLevel(logLevel).log(message);
        }
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getFormattedMessage() {
        String formatTimeStamp = this.timeStamp.format(formatter);
        return String.format("%s [%s] %s %s -- %s", formatTimeStamp, this.threadName, this.logLevel, this.className, message);
    }


    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public Class getClassName() {
        return className;
    }

    public void setClassName(Class className) {
        this.className = className;
    }

    public boolean isHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(boolean heartbeat) {
        this.heartbeat = heartbeat;
    }
}
