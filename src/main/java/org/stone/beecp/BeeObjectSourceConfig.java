/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.beeop;

import org.stone.beeop.pool.KeyedObjectPool;
import org.stone.beeop.pool.ObjectPoolStatics;
import org.stone.beeop.pool.PoolThreadFactory;
import org.stone.tools.CommonUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.stone.beeop.pool.ObjectPoolStatics.createClassInstance;
import static org.stone.tools.CommonUtil.isBlank;
import static org.stone.tools.CommonUtil.trimString;

/**
 * Bee object source configuration object
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeObjectSourceConfig implements BeeObjectSourceConfigJmxBean {
    //pool name generation index which is an atomic integer start with 1
    private static final AtomicInteger PoolNameIndex = new AtomicInteger(1);
    //properties are injected to object factory while pool initialization
    private final Map<String, Object> factoryProperties = new HashMap<String, Object>();

    //pool name for log trace,if null or empty,a generation name will be assigned to it after configuration check passed
    private String poolName;
    //work mode of pool semaphore,default:unfair mode
    private boolean fairMode;
    //creation size of initial objects,default is zero
    private int initialSize;
    //creation mode of initial objects;default is false(synchronization mode)
    private boolean asyncCreateInitObject;
    //max object key size(pool capacity size = (maxObjectKeySize * maxActive),default is 50
    private int maxObjectKeySize = 50;
    //maximum of objects in instance pool,default is 10(default range: 10 =< number <=50)
    private int maxActive = Math.min(Math.max(10, CommonUtil.NCPU), 50);
    //max permits size of instance pool semaphore
    private int borrowSemaphoreSize = Math.min(this.maxActive / 2, CommonUtil.NCPU);
    //milliseconds:max wait time in pool to get objects for borrowers,default is 8000 milliseconds(8 seconds)
    private long maxWait = SECONDS.toMillis(8);
    //milliseconds: max idle time on unused objects which removed from pool,default is 18000 milliseconds(3 minutes)
    private long idleTimeout = MINUTES.toMillis(3);
    //milliseconds: max inactive time on borrowed objects,which recycled to pool by force to avoid objects leak,default is zero
    private long holdTimeout;

    //seconds:max wait time to get validation result on a test object,default is 3 seconds.
    private int aliveTestTimeout = 3;
    //milliseconds: a gap time value since from last active time,assume object is alive and need't do test on it,default is 500 milliseconds
    private long aliveAssumeTime = 500L;
    //milliseconds: interval time to scan idle objects or leak objects,default is 18000 milliseconds(3 minutes)
    private long timerCheckInterval = MINUTES.toMillis(3);
    //indicator on close using objects directly while pool clear,default is false
    private boolean forceCloseUsingOnClear;
    //milliseconds: a delay time value to close using objects return to pool,if still exists using,then continue to next delay,default is 3000 milliseconds
    private long delayTimeForNextClear = 3000L;

    //enable indicator to register configuration and pool to Jmx,default is false
    private boolean enableJmx;
    //enable indicator to print pool runtime log,default is false
    private boolean printRuntimeLog;
    //enable indicator to print configuration items on pool initialization,default is false
    private boolean printConfigInfo;
    //exclusion list on config items print,default is null
    private List<String> configPrintExclusionList;


    //object interfaces
    private Class[] objectInterfaces;
    //object interface names
    private String[] objectInterfaceNames;

    //object factory(priority-1)
    private RawObjectFactory objectFactory;
    //object factory class(priority-2)
    private Class objectFactoryClass;
    //object factory class name(priority-3)
    private String objectFactoryClassName;

    //method call filter(priority-1)
    private RawObjectMethodFilter objectMethodFilter;
    //object method call filter class(priority-2)
    private Class objectMethodFilterClass;
    //object method call filter class name(priority-3)
    private String objectMethodFilterClassName;


    //work thread factory(priority-1)
    private BeeObjectPoolThreadFactory threadFactory;
    //class of thread factory(priority-2)
    private Class threadFactoryClass;
    //class name of thread factory(priority-3),if not set,default factory will be applied in pool
    private String threadFactoryClassName = PoolThreadFactory.class.getName();

    //pool implementation class name
    private String poolImplementClassName = KeyedObjectPool.class.getName();

    //***************************************************************************************************************//
    //                                     1: constructors(4)                                                        //
    //***************************************************************************************************************//
    public BeeObjectSourceConfig() {
    }

    //load configuration from properties file
    public BeeObjectSourceConfig(File propertiesFile) {
        loadFromPropertiesFile(propertiesFile);
    }

    //load configuration from properties file
    public BeeObjectSourceConfig(String propertiesFileName) {
        loadFromPropertiesFile(propertiesFileName);
    }

    //load configuration from properties
    public BeeObjectSourceConfig(Properties configProperties) {
        loadFromProperties(configProperties);
    }

    //***************************************************************************************************************//
    //                                     2: base configuration(40)                                                 //
    //***************************************************************************************************************//
    public String getPoolName() {
        return this.poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = trimString(poolName);
    }

    public boolean isFairMode() {
        return this.fairMode;
    }

    public void setFairMode(boolean fairMode) {
        this.fairMode = fairMode;
    }

    public int getInitialSize() {
        return this.initialSize;
    }

    public void setInitialSize(int initialSize) {
        if (initialSize >= 0) this.initialSize = initialSize;
    }

    public boolean isAsyncCreateInitObject() {
        return asyncCreateInitObject;
    }

    public void setAsyncCreateInitObject(boolean asyncCreateInitObject) {
        this.asyncCreateInitObject = asyncCreateInitObject;
    }

    public int getMaxActive() {
        return this.maxActive;
    }

    public void setMaxActive(int maxActive) {
        if (maxActive > 0) {
            this.maxActive = maxActive;
            borrowSemaphoreSize = (maxActive > 1) ? Math.min(maxActive / 2, CommonUtil.NCPU) : 1;
        }
    }

    public int getMaxObjectKeySize() {
        return maxObjectKeySize;
    }

    public void setMaxObjectKeySize(int maxObjectKeySize) {
        if (maxObjectKeySize >= 1) this.maxObjectKeySize = maxObjectKeySize;
    }

    public int getBorrowSemaphoreSize() {
        return this.borrowSemaphoreSize;
    }

    public void setBorrowSemaphoreSize(int borrowSemaphoreSize) {
        if (borrowSemaphoreSize > 0) this.borrowSemaphoreSize = borrowSemaphoreSize;
    }

    public long getMaxWait() {
        return this.maxWait;
    }

    public void setMaxWait(long maxWait) {
        if (maxWait > 0L) this.maxWait = maxWait;
    }

    public long getIdleTimeout() {
        return this.idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (idleTimeout > 0L) this.idleTimeout = idleTimeout;
    }

    public long getHoldTimeout() {
        return this.holdTimeout;
    }

    public void setHoldTimeout(long holdTimeout) {
        if (holdTimeout >= 0L) this.holdTimeout = holdTimeout;
    }

    public int getAliveTestTimeout() {
        return this.aliveTestTimeout;
    }

    public void setAliveTestTimeout(int aliveTestTimeout) {
        if (aliveTestTimeout >= 0) this.aliveTestTimeout = aliveTestTimeout;
    }

    public long getAliveAssumeTime() {
        return this.aliveAssumeTime;
    }

    public void setAliveAssumeTime(long aliveAssumeTime) {
        if (aliveAssumeTime >= 0L) this.aliveAssumeTime = aliveAssumeTime;
    }

    public long getTimerCheckInterval() {
        return this.timerCheckInterval;
    }

    public void setTimerCheckInterval(long timerCheckInterval) {
        if (timerCheckInterval > 0L) this.timerCheckInterval = timerCheckInterval;
    }

    public boolean isForceCloseUsingOnClear() {
        return this.forceCloseUsingOnClear;
    }

    public void setForceCloseUsingOnClear(boolean forceCloseUsingOnClear) {
        this.forceCloseUsingOnClear = forceCloseUsingOnClear;
    }

    public long getDelayTimeForNextClear() {
        return this.delayTimeForNextClear;
    }

    public void setDelayTimeForNextClear(long delayTimeForNextClear) {
        if (delayTimeForNextClear >= 0L) this.delayTimeForNextClear = delayTimeForNextClear;
    }

    public boolean isEnableJmx() {
        return this.enableJmx;
    }

    public void setEnableJmx(boolean enableJmx) {
        this.enableJmx = enableJmx;
    }

    public boolean isPrintRuntimeLog() {
        return this.printRuntimeLog;
    }

    public void setPrintRuntimeLog(boolean printRuntimeLog) {
        this.printRuntimeLog = printRuntimeLog;
    }

    public boolean isPrintConfigInfo() {
        return this.printConfigInfo;
    }

    public void setPrintConfigInfo(boolean printConfigInfo) {
        this.printConfigInfo = printConfigInfo;
    }

    public void addConfigPrintExclusion(String fieldName) {
        if (configPrintExclusionList == null) {
            configPrintExclusionList = new ArrayList<>(1);
            configPrintExclusionList.add(fieldName);
        } else if (!configPrintExclusionList.contains(fieldName)) {
            configPrintExclusionList.add(fieldName);
        }
    }

    public void clearAllConfigPrintExclusion() {
        if (configPrintExclusionList != null) this.configPrintExclusionList.clear();
    }

    public boolean removeConfigPrintExclusion(String fieldName) {
        return configPrintExclusionList != null && configPrintExclusionList.remove(fieldName);
    }

    public boolean existConfigPrintExclusion(String fieldName) {
        return configPrintExclusionList != null && configPrintExclusionList.contains(fieldName);
    }

    //***************************************************************************************************************//
    //                                     3: creation configuration(20)                                             //
    //***************************************************************************************************************//
    public Class[] getObjectInterfaces() {
        return objectInterfaces;
    }

    public void setObjectInterfaces(Class[] interfaces) {
        this.objectInterfaces = interfaces;
    }

    public String[] getObjectInterfaceNames() {
        return this.objectInterfaceNames;
    }

    public void setObjectInterfaceNames(String[] interfaceNames) {
        this.objectInterfaceNames = interfaceNames;
    }

    public Class getObjectFactoryClass() {
        return this.objectFactoryClass;
    }

    public void setObjectFactoryClass(Class objectFactoryClass) {
        this.objectFactoryClass = objectFactoryClass;
    }

    public String getObjectFactoryClassName() {
        return this.objectFactoryClassName;
    }

    public void setObjectFactoryClassName(String objectFactoryClassName) {
        this.objectFactoryClassName = trimString(objectFactoryClassName);
    }

    public RawObjectFactory getObjectFactory() {
        return this.objectFactory;
    }

    public void setRawObjectFactory(RawObjectFactory factory) {
        this.objectFactory = factory;
    }

    public Class getObjectMethodFilterClass() {
        return objectMethodFilterClass;
    }

    public void setObjectMethodFilterClass(Class objectMethodFilterClass) {
        this.objectMethodFilterClass = objectMethodFilterClass;
    }

    public String getObjectMethodFilterClassName() {
        return objectMethodFilterClassName;
    }

    public void setObjectMethodFilterClassName(String objectMethodFilterClassName) {
        this.objectMethodFilterClassName = objectMethodFilterClassName;
    }

    public RawObjectMethodFilter getObjectMethodFilter() {
        return objectMethodFilter;
    }

    public void setObjectMethodFilter(RawObjectMethodFilter objectMethodFilter) {
        this.objectMethodFilter = objectMethodFilter;
    }

    public Object getFactoryProperty(String key) {
        return this.factoryProperties.get(key);
    }

    public Object removeFactoryProperty(String key) {
        return this.factoryProperties.remove(key);
    }

    public void addFactoryProperty(String key, Object value) {
        if (!isBlank(key) && value != null) this.factoryProperties.put(key, value);
    }

    public void addFactoryProperty(String propertyText) {
        if (!isBlank(propertyText)) {
            String[] attributeArray = propertyText.split("&");
            for (String attribute : attributeArray) {
                String[] pair = attribute.split("=");
                if (pair.length == 2) {
                    this.factoryProperties.put(pair[0].trim(), pair[1].trim());
                } else {
                    pair = attribute.split(":");
                    if (pair.length == 2) {
                        this.factoryProperties.put(pair[0].trim(), pair[1].trim());
                    }
                }
            }
        }
    }

    //***************************************************************************************************************//
    //                                     3: pool work configuration(8)                                             //
    //***************************************************************************************************************//
    public BeeObjectPoolThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(BeeObjectPoolThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public Class getThreadFactoryClass() {
        return threadFactoryClass;
    }

    public void setThreadFactoryClass(Class threadFactoryClass) {
        this.threadFactoryClass = threadFactoryClass;
    }

    public String getThreadFactoryClassName() {
        return threadFactoryClassName;
    }

    public void setThreadFactoryClassName(String threadFactoryClassName) {
        this.threadFactoryClassName = threadFactoryClassName;
    }

    public String getPoolImplementClassName() {
        return this.poolImplementClassName;
    }

    public void setPoolImplementClassName(String poolImplementClassName) {
        if (!isBlank(poolImplementClassName))
            this.poolImplementClassName = trimString(poolImplementClassName);
    }

    //***************************************************************************************************************//
    //                                     4: configuration file load(3)                                             //
    //***************************************************************************************************************//
    public void loadFromPropertiesFile(String filename) {
        if (isBlank(filename)) throw new IllegalArgumentException("Configuration properties file can't be null");

        File file = new File(filename);
        if (file.exists()) {
            this.loadFromPropertiesFile(file);
        } else {//try to load config file from classpath
            Class selfClass = BeeObjectSourceConfig.class;
            InputStream propertiesStream = selfClass.getResourceAsStream(filename);
            if (propertiesStream == null) propertiesStream = selfClass.getClassLoader().getResourceAsStream(filename);

            Properties prop = new Properties();
            try {
                prop.load(propertiesStream);
                loadFromProperties(prop);
            } catch (IOException e) {
                throw new IllegalArgumentException("Configuration properties file load failed", e);
            } finally {
                if (propertiesStream != null) {
                    try {
                        propertiesStream.close();
                    } catch (Throwable e) {
                        //do nothing
                    }
                }
            }
        }
    }

    public void loadFromPropertiesFile(File file) {
        if (file == null) throw new IllegalArgumentException("Configuration properties file can't be null");
        if (!file.exists()) throw new IllegalArgumentException("Configuration properties file not found:" + file);
        if (!file.isFile()) throw new IllegalArgumentException("Target object is not a valid file");
        if (!file.getAbsolutePath().toLowerCase(Locale.US).endsWith(".properties"))
            throw new IllegalArgumentException("Target file is not a properties file");

        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            Properties configProperties = new Properties();
            configProperties.load(stream);
            this.loadFromProperties(configProperties);
        } catch (BeeObjectSourceConfigException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeeObjectSourceConfigException("Failed to load configuration properties file:", e);
        } finally {
            if (stream != null) try {
                stream.close();
            } catch (Throwable e) {
                //do nothing
            }
        }
    }

    public void loadFromProperties(Properties configProperties) {
        if (configProperties == null || configProperties.isEmpty())
            throw new IllegalArgumentException("Configuration properties can't be null or empty");

        //1:load configuration item values from outside properties
        synchronized (configProperties) {//synchronization mode
            Map<String, Object> setValueMap = new HashMap<String, Object>(configProperties.size());
            for (String propertyName : configProperties.stringPropertyNames()) {
                setValueMap.put(propertyName, configProperties.getProperty(propertyName));
            }

            //2:inject item value from map to this dataSource config object
            ObjectPoolStatics.setPropertiesValue(this, setValueMap);

            //3:try to find 'factoryProperties' config value
            this.addFactoryProperty(ObjectPoolStatics.getPropertyValue(configProperties, "factoryProperties"));
            String factoryPropertiesSize = ObjectPoolStatics.getPropertyValue(configProperties, "factoryProperties.size");
            if (!isBlank(factoryPropertiesSize)) {
                int size = 0;
                try {
                    size = Integer.parseInt(factoryPropertiesSize.trim());
                } catch (Throwable e) {
                    //do nothing
                }
                for (int i = 1; i <= size; i++)
                    this.addFactoryProperty(ObjectPoolStatics.getPropertyValue(configProperties, "factoryProperties." + i));
            }

            //5:try to find 'objectInterfaceNames' config value
            String objectInterfaceNames = ObjectPoolStatics.getPropertyValue(configProperties, "objectInterfaceNames");
            if (!isBlank(objectInterfaceNames))
                setObjectInterfaceNames(objectInterfaceNames.split(","));

            //6:try to find 'objectInterfaces' config value
            String objectInterfaceNames2 = ObjectPoolStatics.getPropertyValue(configProperties, "objectInterfaces");
            if (!isBlank(objectInterfaceNames2)) {
                String[] objectInterfaceNameArray = objectInterfaceNames2.split(",");
                Class[] objectInterfaces = new Class[objectInterfaceNameArray.length];
                for (int i = 0, l = objectInterfaceNameArray.length; i < l; i++) {
                    try {
                        objectInterfaces[i] = Class.forName(objectInterfaceNameArray[i]);
                    } catch (ClassNotFoundException e) {
                        throw new BeeObjectSourceConfigException("Class not found:" + objectInterfaceNameArray[i]);
                    }
                }
                setObjectInterfaces(objectInterfaces);
            }
        }
    }

    //***************************************************************************************************************//
    //                                     5: configuration check and object factory create methods(4)               //
    //***************************************************************************************************************//
    //check pool configuration
    public BeeObjectSourceConfig check() {
        if (initialSize > this.maxActive)
            throw new BeeObjectSourceConfigException("initialSize must not be greater than 'maxActive'");

        //1:try to create object factory
        RawObjectFactory objectFactory = this.createObjectFactory();
        if (objectFactory.getDefaultKey() == null)
            throw new BeeObjectSourceConfigException("Default key from factory can't be null");

        //2:try to create method filter
        RawObjectMethodFilter tempMethodFilter = this.tryCreateMethodFilter();

        //3:load object implemented interfaces
        Class[] tempObjectInterfaces = this.loadObjectInterfaces();

        //create pool thread factory
        BeeObjectPoolThreadFactory threadFactory = this.createThreadFactory();

        //4:create a checked configuration and copy local fields to it
        BeeObjectSourceConfig checkedConfig = new BeeObjectSourceConfig();
        copyTo(checkedConfig);

        //5:set temp to config
        checkedConfig.objectFactory = objectFactory;
        checkedConfig.threadFactory = threadFactory;
        if (tempMethodFilter != null) checkedConfig.objectMethodFilter = tempMethodFilter;
        if (tempObjectInterfaces != null) checkedConfig.objectInterfaces = tempObjectInterfaces;
        if (isBlank(checkedConfig.poolName)) checkedConfig.poolName = "KeyPool-" + PoolNameIndex.getAndIncrement();
        return checkedConfig;
    }

    void copyTo(BeeObjectSourceConfig config) {
        List<String> excludeFieldList = new ArrayList<String>(3);
        excludeFieldList.add("factoryProperties");
        excludeFieldList.add("objectInterfaces");
        excludeFieldList.add("objectInterfaceNames");

        //1:copy primitive type fields
        String fieldName = "";
        try {
            for (Field field : BeeObjectSourceConfig.class.getDeclaredFields()) {
                if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers()) || excludeFieldList.contains(field.getName()))
                    continue;
                Object fieldValue = field.get(this);
                fieldName = field.getName();

                if (this.printConfigInfo)
                    ObjectPoolStatics.CommonLog.info("{}.{}={}", this.poolName, fieldName, fieldValue);
                field.set(config, fieldValue);
            }
        } catch (Throwable e) {
            throw new BeeObjectSourceConfigException("Failed to filled value on field[" + fieldName + "]", e);
        }

        //2:copy 'objectInterfaces'
        Class[] interfaces = this.objectInterfaces == null ? null : new Class[this.objectInterfaces.length];
        if (interfaces != null) {
            System.arraycopy(this.objectInterfaces, 0, interfaces, 0, interfaces.length);
            for (int i = 0, l = interfaces.length; i < l; i++)
                if (this.printConfigInfo)
                    ObjectPoolStatics.CommonLog.info("{}.objectInterfaces[{}]={}", this.poolName, i, interfaces[i]);
            config.setObjectInterfaces(interfaces);
        }

        //3:copy 'objectInterfaceNames'
        String[] interfaceNames = (this.objectInterfaceNames == null) ? null : new String[this.objectInterfaceNames.length];
        if (interfaceNames != null) {
            System.arraycopy(this.objectInterfaceNames, 0, interfaceNames, 0, interfaceNames.length);
            for (int i = 0, l = this.objectInterfaceNames.length; i < l; i++)
                if (this.printConfigInfo)
                    ObjectPoolStatics.CommonLog.info("{}.objectInterfaceNames[{}]={}", this.poolName, i, this.objectInterfaceNames[i]);
            config.setObjectInterfaceNames(interfaceNames);
        }
    }

    private Class[] loadObjectInterfaces() throws BeeObjectSourceConfigException {
        //1: if objectInterfaces field value is not null,then check it and return it
        if (objectInterfaces != null) {
            for (int i = 0, l = objectInterfaces.length; i < l; i++) {
                if (objectInterfaces[i] == null)
                    throw new BeeObjectSourceConfigException("interfaces array[" + i + "]is null");
                if (!objectInterfaces[i].isInterface())
                    throw new BeeObjectSourceConfigException("interfaces array[" + i + "]is not valid interface");
            }
            return objectInterfaces;
        }

        //2: try to load interfaces by names
        if (this.objectInterfaceNames != null) {
            Class[] objectInterfaces = new Class[this.objectInterfaceNames.length];
            for (int i = 0; i < this.objectInterfaceNames.length; i++) {
                try {
                    if (isBlank(this.objectInterfaceNames[i]))
                        throw new BeeObjectSourceConfigException("objectInterfaceNames[" + i + "]is empty or null");
                    objectInterfaces[i] = Class.forName(this.objectInterfaceNames[i]);
                } catch (ClassNotFoundException e) {
                    throw new BeeObjectSourceConfigException("Not found objectInterfaceNames[" + i + "]:" + this.objectInterfaceNames[i], e);
                }
            }
            return objectInterfaces;
        }
        return null;
    }

    private RawObjectMethodFilter tryCreateMethodFilter() {
        //1:if exists method filter then return it directly
        if (this.objectMethodFilter != null) return objectMethodFilter;

        //2: create method filter
        if (objectMethodFilterClass != null || !isBlank(objectMethodFilterClassName)) {
            Class filterClass = null;
            try {
                filterClass = objectMethodFilterClass != null ? objectMethodFilterClass : Class.forName(objectMethodFilterClassName);
                return (RawObjectMethodFilter) ObjectPoolStatics.createClassInstance(filterClass, RawObjectMethodFilter.class, "object method filter");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found object filter class:" + objectMethodFilterClassName);
            } catch (BeeObjectSourceConfigException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object method filter by class:" + filterClass, e);
            }
        }

        return null;
    }

    private RawObjectFactory createObjectFactory() {
        //1: copy from member field of configuration
        RawObjectFactory rawObjectFactory = this.objectFactory;

        //2: create factory instance
        if (rawObjectFactory == null && (objectFactoryClass != null || objectFactoryClassName != null)) {
            Class factoryClass = null;
            try {
                factoryClass = objectFactoryClass != null ? objectFactoryClass : Class.forName(objectFactoryClassName);
                rawObjectFactory = (RawObjectFactory) ObjectPoolStatics.createClassInstance(factoryClass, RawObjectFactory.class, "object factory");
            } catch (ClassNotFoundException e) {
                throw new BeeObjectSourceConfigException("Not found object factory class:" + objectFactoryClassName, e);
            } catch (BeeObjectSourceConfigException e) {
                throw e;
            } catch (Throwable e) {
                throw new BeeObjectSourceConfigException("Failed to create object factory by class:" + factoryClass, e);
            }
        }

        //3: throw check failure exception
        if (rawObjectFactory == null)
            throw new BeeObjectSourceConfigException("Must provide one of config items[objectFactory,objectClassName,objectFactoryClassName]");

        //4: inject properties to factory
        if (!factoryProperties.isEmpty())
            ObjectPoolStatics.setPropertiesValue(rawObjectFactory, factoryProperties);

        return rawObjectFactory;
    }

    //create Thread factory
    private BeeObjectPoolThreadFactory createThreadFactory() throws BeeObjectSourceConfigException {
        //step1: if exists thread factory,then return it
        if (this.threadFactory != null) return this.threadFactory;

        //step2: configuration of thread factory
        if (this.threadFactoryClass == null && isBlank(this.threadFactoryClassName))
            throw new BeeObjectSourceConfigException("Must provide one of config items[threadFactory,threadFactoryClass,threadFactoryClassName]");

        //step3: create thread factory by class or class name
        Class<?> threadFactClass = null;
        try {
            threadFactClass = this.threadFactoryClass != null ? this.threadFactoryClass : Class.forName(this.threadFactoryClassName);
            return (BeeObjectPoolThreadFactory) createClassInstance(threadFactClass, PoolThreadFactory.class, "pool thread factory");
        } catch (ClassNotFoundException e) {
            throw new BeeObjectSourceConfigException("Not found thread factory class:" + threadFactoryClassName, e);
        } catch (BeeObjectSourceConfigException e) {
            throw e;
        } catch (Throwable e) {
            throw new BeeObjectSourceConfigException("Failed to create pool thread factory by class:" + threadFactClass, e);
        }
    }
}

