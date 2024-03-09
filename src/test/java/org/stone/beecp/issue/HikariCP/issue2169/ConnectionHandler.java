package org.stone.beecp.issue.HikariCP.issue2169;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionHandler implements java.lang.reflect.InvocationHandler {
    private static final Object VoidObject = new Object();
    private Connection con;
    private ConnectionCloseHook hook;
    private AtomicBoolean closeInd = new AtomicBoolean();

    public ConnectionHandler(Connection con, ConnectionCloseHook hook) {
        this.con = con;
        this.hook = hook;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if ("close".equals(methodName)) {
            if (closeInd.compareAndSet(false, true)) {
                con.close();
                this.hook.onClose(con);//execute hook
            }
            return VoidObject;
        } else {
            return method.invoke(con, args);
        }
    }
}
