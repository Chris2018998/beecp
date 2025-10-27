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

/**
 * Method execution listener interface factory
 *
 * @author Chris Liao
 */
public interface BeeMethodExecutionListenerFactory {

    /**
     * Creates method execution listener.
     *
     * @return created listener instance
     * @throws Exception when failed to create listener
     */
    BeeMethodExecutionListener create() throws Exception;

}
