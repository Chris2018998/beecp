package org.stone.beecp.issue.HikariCP.issue2176;

public class SnowflakeKeyStore {
    private String newKey;

    private String oldKey;

    public SnowflakeKeyStore(String firstKey) {
        this.newKey = firstKey;
    }

    public synchronized boolean existsKey(String key) {
        return key == newKey || key == oldKey;
    }

    public synchronized String getNewKey() {//call in SnowConnectionFactory
        return this.newKey;
    }

    public synchronized void setNewKey(String newKey) {//put your new key
        this.oldKey = this.newKey;
        this.newKey = newKey;
    }
}
