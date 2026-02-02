package io.github.headlesshq.headlessmc.auth;

import net.lenni0451.commons.httpclient.IHttpClientLogger;

public enum NoLogging implements IHttpClientLogger {
    INSTANCE;

    @Override
    public void info(String message) {
        // NOP
    }

    @Override
    public void warn(String message) {
        // NOP
    }

    @Override
    public void error(String message) {
        // NOP
    }

    @Override
    public void error(String message, Throwable cause) {
        // NOP
    }
}
