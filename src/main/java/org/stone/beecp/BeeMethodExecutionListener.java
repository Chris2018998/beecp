/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beecp;

import java.sql.SQLException;
import java.util.List;

/**
 * Method execution listener interface.
 *
 * @author Chris Liao
 */
public interface BeeMethodExecutionListener {

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param log to be handled
     * @throws SQLException when failure during onMethodStart call
     */
    void onMethodStart(BeeMethodExecutionLog log) throws SQLException;

    /**
     * Plugin method: Handles a log of method call.
     *
     * @param log to be handled
     * @throws SQLException when failure during onMethodEnd call
     */
    void onMethodEnd(BeeMethodExecutionLog log) throws SQLException;

    /**
     * Handle a list of long-running logs
     *
     * @param logList to be handled
     */
    List<Boolean> onLongRunningDetected(List<BeeMethodExecutionLog> logList);

}
