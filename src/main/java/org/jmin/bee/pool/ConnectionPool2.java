package org.jmin.bee.pool;

import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.LockSupport;

import org.jmin.bee.BeeDataSourceConfig;

/**
 * JDBC Connection Pool Implementation
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public class ConnectionPool2 extends ConnectionPool {
	private Queue<Borrower> transferQueue = new LinkedBlockingQueue<Borrower>();

	public ConnectionPool2(BeeDataSourceConfig poolInfo) throws SQLException {
		super(poolInfo);
	}

	protected boolean existWaiting() {
		return this.transferQueue.peek() != null;
	}

	public PooledConnection waitRelease(long timeout, Borrower borrower) {
		try {
			borrower.seState(Borrower.STATE_WAIT_INIT);
			while (!this.transferQueue.offer(borrower))
				;
			if (borrower.compareAndSetState(Borrower.STATE_WAIT_INIT, Borrower.STATE_WAITING)) {
				LockSupport.parkNanos(timeout);
				if (borrower.compareAndSetState(Borrower.STATE_WAITING, Borrower.STATE_NORMAL)) {
					this.transferQueue.remove(borrower);
				}
			}
		} finally {
			borrower.seState(Borrower.STATE_NORMAL);
		}

		PooledConnection pooledCon = borrower.getTransferedConnection();
		borrower.setTransferedConnection(null);
		return pooledCon;
	}

	public void releasePooledConnection(final PooledConnection pooledConnection) throws SQLException {
		Borrower borrower = null;
		boolean isCompete = !this.poolInfo.isFairMode();
		if (isCompete)
			pooledConnection.setConnectionState(PooledConnectionState.IDLE);

		while (true) {
			if (isCompete && pooledConnection.getConnectionState() != PooledConnectionState.IDLE)
				return;

			if ((borrower = transferQueue.poll()) != null) {
				if (borrower.compareAndSetState(Borrower.STATE_WAIT_INIT, Borrower.STATE_TRANSFERED)) {
					borrower.setTransferedConnection(pooledConnection);
					return;
				} else if (borrower.compareAndSetState(Borrower.STATE_WAITING, Borrower.STATE_TRANSFERED)) {
					borrower.setTransferedConnection(pooledConnection);
					LockSupport.unpark(borrower.getThread());
					return;
				}
			} else {
				break;
			}
		} // while

		if (!isCompete)
			pooledConnection.setConnectionState(PooledConnectionState.IDLE);
	}
}
