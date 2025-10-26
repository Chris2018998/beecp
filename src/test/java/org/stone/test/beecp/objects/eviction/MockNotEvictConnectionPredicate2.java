/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.test.beecp.objects.eviction;

import org.stone.beecp.BeeConnectionPredicate;

import java.sql.SQLException;

/**
 * @author Chris Liao
 */
public class MockNotEvictConnectionPredicate2 implements BeeConnectionPredicate {

    //return desc of eviction,if null or empty,not be evicted
    public String evictionTest(SQLException e) {
        return "";
    }
}
