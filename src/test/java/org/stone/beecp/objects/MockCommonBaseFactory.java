/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.objects;

import org.stone.beecp.driver.MockConnectionProperties;

import java.sql.SQLException;

/**
 * A connection factory impl for mock test
 *
 * @author Chris Liao
 */
public class MockCommonBaseFactory extends DatabaseLinkInfo {
    protected SQLException createException1;
    protected RuntimeException createException2;
    protected Error createException3;
    protected int curCreatedCount;//reach value of maxCreationSize,then throws an exception.
    protected int maxCreationSize;//limit size
    protected boolean returnNullOnCreate;
    protected MockConnectionProperties properties;

    public MockCommonBaseFactory() {
        this(new MockConnectionProperties());
    }

    public MockCommonBaseFactory(MockConnectionProperties properties) {
        this.properties = properties;
    }

    protected void throwCreationException() throws SQLException {
        if (createException1 != null) throw createException1;
        if (createException2 != null) throw createException2;
        if (createException3 != null) throw createException3;
    }

    public void setProperties(MockConnectionProperties properties) {
        this.properties = properties;
    }

    public void setCreateException1(SQLException createException1) {
        this.createException1 = createException1;
    }

    public void setCreateException2(RuntimeException createException2) {
        this.createException2 = createException2;
    }

    public void setCreateException3(Error createException3) {
        this.createException3 = createException3;
    }

    public void setReturnNullOnCreate(boolean returnNullOnCreate) {
        this.returnNullOnCreate = returnNullOnCreate;
    }

    public void setMaxCreationSize(int maxCreationSize) {
        this.maxCreationSize = maxCreationSize;
    }

    public void increaseCurCreatedCount() {
        if (maxCreationSize > 0) curCreatedCount++;
    }

    public void checkCurCreatedCount() throws SQLException {
        if (maxCreationSize > 0 && maxCreationSize == curCreatedCount)
            throw new SQLException("the count of created connections has reached max");
    }
}
