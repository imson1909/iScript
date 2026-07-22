package com.iscript.iscript.gui.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptLog {
    private static final int MAX_LINES = 100;
    private final List<LogEntry> entries = new ArrayList<>();

    public enum Level {
        INFO(0xFFAAAAAA),
        WARN(0xFFFFAA44),
        ERROR(0xFFFF4444),
        DEBUG(0xFF44AAFF);

        public final int color;
        Level(int color) { this.color = color; }
    }

    public record LogEntry(String message, Level level, long timestamp, String sourceFile, int sourceLine) {}

    private static final ScriptLog INSTANCE = new ScriptLog();

    public static ScriptLog get() {
        return INSTANCE;
    }

    public synchronized void add(String message, Level level, String sourceFile, int sourceLine) {
        entries.add(new LogEntry(message, level, System.currentTimeMillis(), sourceFile, sourceLine));
        if (entries.size() > MAX_LINES) {
            entries.remove(0);
        }
    }

    public synchronized void add(String message, Level level) {
        add(message, level, null, -1);
    }

    public synchronized void info(String message) {
        add(message, Level.INFO, null, -1);
    }

    public synchronized void warn(String message) {
        add(message, Level.WARN, null, -1);
    }

    public synchronized void error(String message) {
        add(message, Level.ERROR, null, -1);
    }

    public synchronized void error(String message, String sourceFile, int sourceLine) {
        add(message, Level.ERROR, sourceFile, sourceLine);
    }

    public synchronized void debug(String message) {
        add(message, Level.DEBUG, null, -1);
    }

    public synchronized List<LogEntry> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }
}