/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stone.tools.exception.BeanException;
import org.stone.tools.exception.PropertyValueConvertException;
import org.stone.tools.exception.PropertyValueSetFailedException;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import static org.stone.tools.CommonUtil.isBlank;

/**
 * A bean util(Not recommend use it other projects)
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeanUtil {
    public static final String Separator_Comma = ",";
    //middle-line:separator symbol in configuration properties name
    public static final String Separator_MiddleLine = "-";
    //under-line:separator symbol in configuration properties name
    public static final String Separator_UnderLine = "_";
    //a SLF4 logger used in stone project
    public static final Logger CommonLog = LoggerFactory.getLogger(BeanUtil.class);
    //Class loader
    private static final ClassLoader classLoader = BeanUtil.class.getClassLoader();

    /**
     * set a field accessible under AccessController
     *
     * @param field reflection access field
     */
    public static void setAccessible(final Field field) {
        if (!field.isAccessible()) {
            AccessController.doPrivileged(new PrivilegedAction<Field>() {
                public Field run() {
                    field.setAccessible(true);
                    return field;
                }
            });
        }
    }

    /**
     * set a method accessible under AccessController
     *
     * @param method reflection access method
     */
    public static void setAccessible(final Method method) {
        if (!method.isAccessible()) {
            AccessController.doPrivileged(new PrivilegedAction<Method>() {
                public Method run() {
                    method.setAccessible(true);
                    return method;
                }
            });
        }
    }

    /**
     * finds out all properties set method with public modifier from a bean class
     *
     * @param beanClass is target class
     * @return a map contains found methods
     */
    public static Map<String, Method> getClassSetMethodMap(Class<?> beanClass) {
        Method[] methods = beanClass.getMethods();
        HashMap<String, Method> methodMap = new LinkedHashMap<>(methods.length);
        for (Method method : methods) {
            String methodName = method.getName();
            if (method.getParameterTypes().length == 1 && methodName.startsWith("set") && methodName.length() > 3) {
                methodMap.put(methodName.substring(3, 4).toLowerCase() + methodName.substring(4), method);
            }
        }
        return methodMap;
    }


    /**
     * gets property value(a string) from a properties map with property name.Three kinds of format conversion are supported on
     * propertyName to get value from properties value map,for example: if input a 'maxActive' propertyName,ordered
     * conversion are below
     * <p>
     * conversion1(hump): maxActive
     * conversion2(middle-line): max-active
     * conversion2(under_line): max_active
     *
     * @param valueMap     is a properties value map
     * @param propertyName is a value search key
     * @return mapped value
     */
    public static String getPropertyValue(Map<String, String> valueMap, final String propertyName) {
        String value = valueMap.get(propertyName);
        if (value != null) return value;
        value = valueMap.get(propertyNameToFieldId(propertyName, Separator_MiddleLine));
        if (value != null) return value;
        value = valueMap.get(propertyNameToFieldId(propertyName, Separator_UnderLine));
        if (value != null) return value;

        String firstChar = propertyName.substring(0, 1);
        if (Character.isLowerCase(firstChar.charAt(0))) {//try again if first char is lowercase
            return valueMap.get(firstChar.toUpperCase() + propertyName.substring(1));
        }
        return null;
    }

    /**
     * gets property value(any) from a properties map with property name.Three kinds of format conversion are supported on
     * propertyName to get value from properties value map,for example: if input a 'maxActive' propertyName,ordered
     * conversion are below
     * <p>
     * conversion1(hump): maxActive
     * conversion2(middle-line): max-active
     * conversion2(under_line): max_active
     *
     * @param valueMap     is a properties value map
     * @param propertyName is a value search key
     * @return mapped value
     */
    private static Object getFieldValue(Map<String, ?> valueMap, final String propertyName) {
        Object value = valueMap.get(propertyName);
        if (value != null) return value;
        value = valueMap.get(propertyNameToFieldId(propertyName, Separator_MiddleLine));
        if (value != null) return value;
        value = valueMap.get(propertyNameToFieldId(propertyName, Separator_UnderLine));
        if (value != null) return value;

        String firstChar = propertyName.substring(0, 1);
        if (Character.isLowerCase(firstChar.charAt(0))) {
            return valueMap.get(firstChar.toUpperCase() + propertyName.substring(1));
        }
        return null;
    }

    /**
     * converts a property name format,insert specified separators before all upper case chars and covert them to
     * lower case chars
     *
     * @param propertyName is a target conversion name
     * @param separator    is a separator string inserted to conversion string
     * @return a conversion string
     */
    public static String propertyNameToFieldId(String propertyName, String separator) {
        char[] chars = propertyName.toCharArray();
        StringBuilder sb = new StringBuilder(chars.length);
        for (char c : chars) {
            if (Character.isUpperCase(c)) {
                sb.append(separator).append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * sets mapping properties value on a bean
     *
     * @param bean     is target set object
     * @param valueMap properties value store
     * @throws BeanException if bean is null
     */
    public static void setPropertiesValue(Object bean, Map<String, ?> valueMap) throws BeanException {
        if (bean == null) throw new BeanException("Bean can't be null");
        setPropertiesValue(bean, getClassSetMethodMap(bean.getClass()), valueMap);
    }

    /**
     * sets mapping properties value on a bean
     *
     * @param bean         is target set object
     * @param setMethodMap set method map
     * @param valueMap     properties value store
     * @throws BeanException if bean is null
     */
    public static void setPropertiesValue(Object bean, Map<String, Method> setMethodMap, Map<String, ?> valueMap) throws BeanException {
        if (bean == null) throw new BeanException("Bean can't be null");
        if (setMethodMap == null || setMethodMap.isEmpty() || valueMap == null || valueMap.isEmpty()) return;
        for (Map.Entry<String, Method> entry : setMethodMap.entrySet()) {
            String propertyName = entry.getKey();
            Method setMethod = entry.getValue();

            //1: gets property value from value map
            Object setValue = getFieldValue(valueMap, propertyName);

            //2: converts value to target type
            if (setValue != null) {
                Class<?> type = setMethod.getParameterTypes()[0];
                try {
                    setValue = convert(setValue, type, setMethod);
                } catch (Throwable e) {
                    throw new PropertyValueConvertException("Failed to convert value[" + setValue + "]to property type(" + propertyName + ":" + type + ")", e);
                }

                //3: injects converted value on bean
                try {
                    setMethod.invoke(bean, setValue);
                } catch (IllegalAccessException e) {
                    throw new PropertyValueSetFailedException("Failed to set value on property[" + propertyName + "],message:" + e.getMessage(), e);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getTargetException();
                    if (cause == null) {
                        throw new PropertyValueSetFailedException("Failed to set value on property[" + propertyName + "],message:" + e.getMessage(), e);
                    } else {
                        throw new PropertyValueSetFailedException("Failed to set value on property[" + propertyName + "],message:" + cause.getMessage(), cause);
                    }
                }
            }
        }
    }

    /**
     * attempt to load class with a class name
     *
     * @param className to be loaded
     * @return a loaded class
     * @throws ClassNotFoundException when class not found
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return Class.forName(className, true, classLoader);
    }

    /**
     * attempt to load class with a class name
     *
     * @param className to be loaded
     * @return a loaded class
     * @throws ClassNotFoundException when class not found
     * @throws InstantiationException when class not found
     * @throws IllegalAccessException when class not found
     */
    public static Object createClassInstance(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        return Class.forName(className, true, classLoader).newInstance();
    }

    /**
     * Create instance for a bean class
     *
     * @param beanClassName   is need be instantiated
     * @param parentClass     is parent class for type check(it may be an interface should be implemented by bean class)
     * @param objectClassType is a desc of bean class
     * @return an instance of bean class
     * @throws BeanException when create failed
     */
    public static Object createClassInstance(String beanClassName, Class<?> parentClass, String objectClassType) throws ClassNotFoundException, BeanException {
        return createClassInstance(loadClass(beanClassName), parentClass != null ? new Class[]{parentClass} : null, objectClassType);
    }

    /**
     * Create instance for a bean class
     *
     * @param beanClass       is need be instantiated
     * @param parentClass     is parent class for type check(it may be an interface should be implemented by bean class)
     * @param objectClassType is a desc of bean class
     * @return an instance of bean class
     * @throws BeanException when create failed
     */
    public static Object createClassInstance(Class<?> beanClass, Class<?> parentClass, String objectClassType) throws BeanException {
        return createClassInstance(beanClass, parentClass != null ? new Class[]{parentClass} : null, objectClassType);
    }

    /**
     * Create instance for a bean class
     *
     * @param beanClass     is need be instantiated
     * @param parentClasses is an array for type check(bean parent class and interfaces)
     * @param beanClassType is a desc of bean class
     * @return an instance of bean class
     * @throws BeanException when create failed
     */
    public static Object createClassInstance(Class<?> beanClass, Class<?>[] parentClasses, String beanClassType) throws BeanException {
        //1: null class check
        if (beanClass == null)
            throw new BeanException("Bean class can't be null");
        //2:check class abstract modifier
        int modifiers = beanClass.getModifiers();
        if (Modifier.isAbstract(modifiers))
            throw new BeanException("Bean class can't be abstract");
        //3:check class public modifier
        if (!Modifier.isPublic(modifiers))
            throw new BeanException("Bean class must be public");

        //4:check extension
        if (parentClasses != null && parentClasses.length > 0) {
            int parentClassCount = 0;
            boolean isSubClass = false;//pass when match one
            for (Class<?> parentClass : parentClasses) {
                if (parentClass == null) continue;
                parentClassCount++;
                if (parentClass.isAssignableFrom(beanClass)) {
                    isSubClass = true;
                    break;
                }
            }
            if (parentClassCount > 0 && !isSubClass)
                throw new BeanException("Can‘t create instance on class[" + beanClass.getName() + "]which must extend from one of type[" + getClassName(parentClasses) + "]at least,creation category[" + beanClassType + "]");
        }

        //5: create instance with constructor
        try {
            return beanClass.getConstructor().newInstance();
        } catch (Throwable e) {
            throw new BeanException("Failed to create instance on class[" + beanClass + "]", e);
        }
    }

    /**
     * builds class array to a string contains class names
     *
     * @param classes is a classes array
     * @return a string
     */
    private static String getClassName(Class<?>[] classes) {
        StringBuilder buf = new StringBuilder(classes.length * 10);
        for (Class<?> clazz : classes) {
            if (clazz == null) continue;
            if (buf.length() > 0) buf.append(",");
            buf.append(clazz.getName());
        }
        return buf.toString();
    }

    /**
     * converts value to specified type
     *
     * @param propValue  is value of the property
     * @param targetType target conversion type
     * @return converted value
     */
    private static Object convert(Object propValue, Class<?> targetType, Method setMethod) throws Exception {
        if (targetType.isInstance(propValue)) {
            return propValue;
        } else if (targetType == String.class) {
            return propValue.toString();
        }

        String text = propValue.toString();
        if (isBlank(text)) return null;
        text = text.trim();

        if (targetType == char.class || targetType == Character.class) {
            return Character.valueOf(text.charAt(0));
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.valueOf(text);
        } else if (targetType == byte.class || targetType == Byte.class) {
            return Byte.valueOf(text);
        } else if (targetType == short.class || targetType == Short.class) {
            return Short.valueOf(text);
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.valueOf(text);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.valueOf(text);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.valueOf(text);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.valueOf(text);
        } else if (targetType == BigInteger.class) {
            return new BigInteger(text);
        } else if (targetType == BigDecimal.class) {
            return new BigDecimal(text);
        } else if (targetType == Class.class) {
            return loadClass(text);
        } else if (targetType.isArray()) {
            String[] textArray = text.split(Separator_Comma);
            int elementSize = textArray.length;
            Class<?> elementType = targetType.getComponentType();
            Object elementArray = Array.newInstance(elementType, elementSize);
            for (int i = 0; i < elementSize; i++) {
                Array.set(elementArray, i, convert(textArray[i], elementType, null));
            }
            return elementArray;
        } else if (Collection.class.isAssignableFrom(targetType)) {
            if (setMethod == null) return null;

            Collection collection;
            if (!Modifier.isAbstract(targetType.getModifiers())) {
                collection = (Collection<?>) targetType.newInstance();
            } else if (Set.class.isAssignableFrom(targetType)) {
                collection = new HashSet<>(1);
            } else {
                collection = new ArrayList<>(1);
            }

            Type genericParameterType = setMethod.getGenericParameterTypes()[0];
            if (genericParameterType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericParameterType;
                Class<?> elementType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
                for (String s : text.split(Separator_Comma)) {
                    collection.add(convert(s, elementType, null));
                }
            } else {
                Collections.addAll(collection, text.split(Separator_Comma));
            }
            return collection;
        } else {
            Object objInstance = Class.forName(text, true, classLoader).newInstance();
            if (targetType.isInstance(objInstance)) return objInstance;
            throw new ClassCastException();
        }
    }
}
