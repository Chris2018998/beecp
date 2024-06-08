/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.base;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

//collect stone log messages
public class StoneLogAppender extends ConsoleAppender<LoggingEvent> {
    private final StringBuilder messageBuf = new StringBuilder();
    private boolean collectInd;

    public void beginCollectStoneLog() {
        this.collectInd = true;
        this.messageBuf.delete(0, messageBuf.length());
    }

    public String endCollectedStoneLog() {
        this.collectInd = false;
        return messageBuf.toString();
    }

    protected void append(LoggingEvent event) {
        if (this.isStarted()) {
            String loggerName = event.getLoggerName();
            if (loggerName != null && loggerName.startsWith("org.stone")) {
                if (collectInd) messageBuf.append(event.getFormattedMessage()).append("\n");
            } else {
                super.subAppend(event);
            }
        }
    }
}
