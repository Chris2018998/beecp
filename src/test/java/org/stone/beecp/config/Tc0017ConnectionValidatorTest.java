/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.beecp.BeeConnectionValidator;
import org.stone.beecp.BeeDataSourceConfig;
import org.stone.beecp.BeeDataSourceConfigException;
import org.stone.beecp.objects.MockConnectionValidator;

import static org.stone.beecp.config.DsConfigFactory.createDefault;
import static org.stone.beecp.config.DsConfigFactory.createEmpty;

/**
 * @author Chris Liao
 */
public class Tc0017ConnectionValidatorTest extends TestCase {

    public void testSettingAndGetting() {
        BeeDataSourceConfig config = createEmpty();
        Class validatorClass = MockConnectionValidator.class;
        config.setAliveValidatorClassName(validatorClass.getName());
        Assert.assertEquals(validatorClass.getName(), config.getAliveValidatorClassName());
        config.setAliveValidatorClass(validatorClass);
        Assert.assertEquals(validatorClass, config.getAliveValidatorClass());
        BeeConnectionValidator validator = new MockConnectionValidator();
        config.setAliveValidator(validator);
        Assert.assertEquals(validator, config.getAliveValidator());
    }

    public void testOnCreation() throws Exception {
        MockConnectionValidator validator = new MockConnectionValidator();
        BeeDataSourceConfig config1 = createDefault();
        config1.setAliveValidator(validator);
        BeeDataSourceConfig checkConfig1 = config1.check();
        Assert.assertEquals(checkConfig1.getAliveValidator(), validator);

        BeeDataSourceConfig config2 = createDefault();
        Class<? extends MockConnectionValidator> validatorClass = MockConnectionValidator.class;
        config2.setAliveValidatorClass(validatorClass);
        Assert.assertEquals(validatorClass, config2.getAliveValidatorClass());
        BeeDataSourceConfig checkConfig2 = config2.check();
        Assert.assertNotNull(checkConfig2.getAliveValidator());

        BeeDataSourceConfig config3 = createDefault();
        String validatorClassName = MockConnectionValidator.class.getName();
        config3.setAliveValidatorClassName(validatorClassName);
        Assert.assertEquals(validatorClassName, config3.getAliveValidatorClassName());
        BeeDataSourceConfig checkConfig3 = config3.check();
        Assert.assertNotNull(checkConfig3.getAliveValidator());

        BeeDataSourceConfig config4 = createDefault();
        config4.setAliveValidatorClass(validatorClass);
        config4.setAliveValidatorClassName(validatorClassName);
        Assert.assertEquals(validatorClassName, config4.getAliveValidatorClassName());
        BeeDataSourceConfig checkConfig4 = config4.check();
        Assert.assertNotNull(checkConfig4.getAliveValidator());

        BeeDataSourceConfig config5 = createDefault();
        config5.setAliveValidatorClassName("String");
        try {
            config5.check();
        } catch (BeeDataSourceConfigException e) {
            String message = e.getMessage();
            Assert.assertTrue(message != null && message.contains("validator"));
        }
    }
}
