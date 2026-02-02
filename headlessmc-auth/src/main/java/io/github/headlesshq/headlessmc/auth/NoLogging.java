package io.github.headlesshq.headlessmc.auth;

import net.lenni0451.commons.httpclient.logging.IHttpClientLogger;

public enum NoLogging implements IHttpClientLogger {
    INSTANCE;

    @Override
    public void info(String s) {
        // NOP
    }

    @Override
    public void warn(String s) {
        // NOP
    }

    @Override
    public void error(String s) {
        // NOP
    }

    @Override
    public void error(String s, Throwable t) {
        // NOP
    }
}
