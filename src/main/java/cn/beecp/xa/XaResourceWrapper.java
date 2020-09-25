/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.xa;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * XAResource Wrapper
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class XaResourceWrapper implements XAResource {
    private XAResource delegate;
    private XaConnectionWrapper xaConnectionWrapper;

    public XaResourceWrapper(XAResource delegate, XaConnectionWrapper xaConnectionWrapper) {
        this.delegate = delegate;
        this.xaConnectionWrapper = xaConnectionWrapper;
    }

    public void commit(Xid var1, boolean var2) throws XAException {
        xaConnectionWrapper.checkClosedForXa();
        delegate.commit(var1, var2);
    }

    public void end(Xid var1, int var2) throws XAException {
        xaConnectionWrapper.checkClosedForXa();
        delegate.end(var1, var2);
    }

    public void forget(Xid var1) throws XAException {
        xaConnectionWrapper.checkClosedForXa();
        delegate.forget(var1);
    }

    public int getTransactionTimeout() throws XAException {
        xaConnectionWrapper.checkClosedForXa();
        return delegate.getTransactionTimeout();
    }

    public boolean isSameRM(XAResource var1) throws XAException {
        xaConnectionWrapper.checkClosedForXa();
        return delegate.isSameRM(var1);
    }

    public int prepare(Xid var1) throws XAException {
        xaConnectionWrapper.checkClosedForXa();
        return delegate.prepare(var1);
    }

    public Xid[] recover(int var1) throws XAException {
        xaConnectionWrapper.checkClosedForXa();
        return delegate.recover(var1);
    }

    public void rollback(Xid var1) throws XAException {
        xaConnectionWrapper.checkClosedForXa();
        delegate.rollback(var1);
    }

    public boolean setTransactionTimeout(int var1) throws XAException {
        xaConnectionWrapper.checkClosedForXa();
        return delegate.setTransactionTimeout(var1);
    }

    public void start(Xid var1, int var2) throws XAException {
        xaConnectionWrapper.checkClosedForXa();
        delegate.start(var1, var2);
    }
}
