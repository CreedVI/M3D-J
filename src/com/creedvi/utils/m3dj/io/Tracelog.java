package com.creedvi.utils.m3dj.io;

public class Tracelog {

    public static class LogLevel {
        public final static int
            LEVEL_INFO = 0,
            LEVEL_WARNING = 1,
            LEVEL_ERROR = 2,
            LEVEL_DEBUG = 3;
    }

    public enum LogType {
        LOG_INFO,
        LOG_WARNING,
        LOG_ERROR,
        LOG_DEBUG
    }

    private int logLevel;

    public Tracelog(int logLevel) {
        this.logLevel = logLevel;
    }

    public void out(LogType logType, String message) {
        if (logType.ordinal() <= this.logLevel) {
            System.out.println("M3D-J :: " + logType + " :: " + message);
        }
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public int getLogLevel() {
        return logLevel;
    }
}
