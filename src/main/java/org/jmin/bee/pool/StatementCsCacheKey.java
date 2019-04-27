package org.jmin.bee.pool;

/**
 * callStatement cache key
 *
 * @author Chris.Liao
 * @version 1.0
 */
public class StatementCsCacheKey{
	private String sql = null;
	private int resultSetType;
	private int resultSetConcurrency;
	private int resultSetHoldability;
	private String statementType = "CS";
	private int hashCode;
	
	public StatementCsCacheKey(String sql) {
		this.sql = sql;
		this.hashCode = this.buildHashCode();
	}

	public StatementCsCacheKey(String sql, int resultSetType, int resultSetConcurrency) {
		this.sql = sql;
		this.resultSetType = resultSetType;
		this.resultSetConcurrency = resultSetConcurrency;
		this.hashCode = this.buildHashCode();
	}

	public StatementCsCacheKey(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
		this.sql = sql;
		this.resultSetType = resultSetType;
		this.resultSetConcurrency = resultSetConcurrency;
		this.resultSetHoldability = resultSetHoldability;
		this.hashCode = this.buildHashCode();
	}

	private int buildHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + resultSetConcurrency;
		result = prime * result + resultSetHoldability;
		result = prime * result + resultSetType;
		result = prime * result + ((sql == null) ? 0 : sql.hashCode());
		result = prime * result + ((statementType == null) ? 0 : statementType.hashCode());
		return result;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StatementCsCacheKey other = (StatementCsCacheKey) obj;
		if (hashCode != other.hashCode)
			return false;
		if (resultSetConcurrency != other.resultSetConcurrency)
			return false;
		if (resultSetHoldability != other.resultSetHoldability)
			return false;
		if (resultSetType != other.resultSetType)
			return false;
		if (sql == null) {
			if (other.sql != null)
				return false;
		} else if (!sql.equals(other.sql))
			return false;
		if (statementType == null) {
			if (other.statementType != null)
				return false;
		} else if (!statementType.equals(other.statementType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		final StringBuffer buf = new StringBuffer();
		buf.append("StatementCacheKey: sql=");
		buf.append(sql);
		buf.append(", resultSetType=");
		buf.append(resultSetType);
		buf.append(", resultSetConcurrency=");
		buf.append(resultSetConcurrency);
		buf.append(", resultSetHoldability=");
		buf.append(resultSetHoldability);
		buf.append(", statementType=");
		buf.append(statementType);
		return buf.toString();
	}
}
