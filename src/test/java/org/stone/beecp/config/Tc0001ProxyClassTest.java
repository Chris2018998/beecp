package org.stone.beecp.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.stone.base.TestUtil;
import org.stone.beecp.pool.ConnectionPoolStatics;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Tc0001ProxyClassTest extends TestCase {

    public void testJdbcProxyClassesMissed() throws Exception {
        String className1 = "org/stone/beecp/pool/Borrower.class";
        File classFile1 = TestUtil.getClassPathFileAbsolutePath(className1);

        assert classFile1 != null;
        String classFileName1 = classFile1.toString();
        int pos = classFileName1.lastIndexOf(File.separator);
        String folderName = classFileName1.substring(0, pos);
        File classFile2 = new File(folderName + File.separator + "Borrower2.class");

        try {
            classFile1.renameTo(classFile2);
            Method method = ConnectionPoolStatics.class.getDeclaredMethod("checkJdbcProxyClass");
            method.setAccessible(true);
            method.invoke(null);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException e) {
            fail("checkJdbcProxyClass method missed");
        } catch (InvocationTargetException e) {
            Throwable firstCause = e.getCause();
            Assert.assertTrue(firstCause instanceof RuntimeException);
            Assert.assertTrue(firstCause.getCause() instanceof ClassNotFoundException);
        } finally {
            classFile2.renameTo(classFile1);
        }
    }
}
