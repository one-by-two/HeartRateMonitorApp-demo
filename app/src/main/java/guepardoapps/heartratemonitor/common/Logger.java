package guepardoapps.heartratemonitor.common;

import java.io.Serializable;

import android.support.annotation.NonNull;
import android.util.Log;

import guepardoapps.heartratemonitor.common.constants.Enables;

public class Logger implements Serializable {
    private static final long serialVersionUID = -6278387904140268519L;

    private String _tag;
    private boolean _debuggingEnabled;

    public Logger(@NonNull String tag, boolean debuggingEnabled) {
        _tag = tag;
        _debuggingEnabled = debuggingEnabled;
        Debug("Created logger for " + _tag + "! Is Enabled: " + String.valueOf(_debuggingEnabled));
    }

    public Logger(@NonNull String tag) {
        this(tag, Enables.LOGGING);
    }

    public void SetEnable(boolean debuggingEnabled) {
        _debuggingEnabled = debuggingEnabled;
    }

    public void Verbose(@NonNull String message) {
        if (_debuggingEnabled) {
            if (message.length() > 0) {
                Log.v(_tag, message);
            }
        }
    }

    public void Debug(@NonNull String message) {
        if (_debuggingEnabled) {
            if (message.length() > 0) {
                Log.d(_tag, message);
            }
        }
    }

    public void Info(@NonNull String message) {
        if (_debuggingEnabled) {
            if (message.length() > 0) {
                Log.i(_tag, message);
            }
        }
    }

    public void Warn(@NonNull String message) {
        if (_debuggingEnabled) {
            if (message.length() > 0) {
                Log.w(_tag, message);
            }
        }
    }

    public void Error(@NonNull String message) {
        if (_debuggingEnabled) {
            if (message.length() > 0) {
                Log.e(_tag, message);
            }
        }
    }

    public void LogTest(
            @NonNull String message,
            boolean success) {
        if (_debuggingEnabled) {
            if (success) {
                if (message.length() > 0) {
                    Log.d(_tag, "TestResult:");
                    Log.i(_tag, message);
                }
            } else {
                if (message.length() > 0) {
                    Log.d(_tag, "TestResult:");
                    Log.e(_tag, message);
                }
            }
        }
    }
}
