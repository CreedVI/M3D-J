package com.creedvi.utils.m3dj.io;

public class Tracelog {

    public static class LogLevel {
        public final static int
                LEVEL_ERROR = 0,
                LEVEL_WARNING = 1,
                LEVEL_INFO = 2,
                LEVEL_DEBUG = 3;
    }

    public enum LogType {
        LOG_ERROR,
        LOG_WARNING,
        LOG_INFO,
        LOG_DEBUG
    }

    private int logLevel;
    private boolean mute;

    public Tracelog(int logLevel) {
        this.logLevel = logLevel;
        this.mute = false;
    }

    public void Out(LogType logType, String message) {
        if (!mute) {
            if (logType.ordinal() <= this.logLevel) {
                System.out.println("M3D-J :: " + logType + " :: " + message);
            }
        }
    }

    public void SetLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public int GetLogLevel() {
        return logLevel;
    }

    public void SetMute(boolean mute) {
        this.mute = mute;
    }
}
