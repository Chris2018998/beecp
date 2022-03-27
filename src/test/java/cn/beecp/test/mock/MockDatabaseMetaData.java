/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.mock;

import java.sql.*;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class MockDatabaseMetaData extends MockBase implements DatabaseMetaData {
    private final MockConnection connection;

    MockDatabaseMetaData(MockConnection connection) {
        this.connection = connection;
    }

    public Connection getConnection() throws SQLException {
        return connection;
    }

    public boolean allProceduresAreCallable() throws SQLException {
        return returnFalse();
    }

    public boolean allTablesAreSelectable() throws SQLException {
        return returnFalse();
    }

    public String getURL() throws SQLException {
        return returnNull();
    }

    public String getUserName() throws SQLException {
        return returnNull();
    }

    public boolean isReadOnly() throws SQLException {
        return returnFalse();
    }

    public boolean nullsAreSortedHigh() throws SQLException {
        return returnFalse();
    }

    public boolean nullsAreSortedLow() throws SQLException {
        return returnFalse();
    }

    public boolean nullsAreSortedAtStart() throws SQLException {
        return returnFalse();
    }

    public boolean nullsAreSortedAtEnd() throws SQLException {
        return returnFalse();
    }

    public String getDatabaseProductName() throws SQLException {
        return returnNull();
    }

    public String getDatabaseProductVersion() throws SQLException {
        return returnNull();
    }

    public String getDriverName() throws SQLException {
        return returnNull();
    }

    public String getDriverVersion() throws SQLException {
        return returnNull();
    }

    public int getDriverMajorVersion() {
        return returnNumberOne();
    }

    public int getDriverMinorVersion() {
        return 0;
    }

    public boolean usesLocalFiles() throws SQLException {
        return returnFalse();
    }

    public boolean usesLocalFilePerTable() throws SQLException {
        return returnFalse();
    }

    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return returnFalse();
    }

    public boolean storesUpperCaseIdentifiers() throws SQLException {
        return returnFalse();
    }

    public boolean storesLowerCaseIdentifiers() throws SQLException {
        return returnFalse();
    }

    public boolean storesMixedCaseIdentifiers() throws SQLException {
        return returnFalse();
    }

    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        return returnFalse();
    }

    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        return returnFalse();
    }

    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        return returnFalse();
    }

    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return returnFalse();
    }

    public String getIdentifierQuoteString() throws SQLException {
        return returnNull();
    }

    public String getSQLKeywords() throws SQLException {
        return returnNull();
    }

    public String getNumericFunctions() throws SQLException {
        return returnNull();
    }

    public String getStringFunctions() throws SQLException {
        return returnNull();
    }

    public String getSystemFunctions() throws SQLException {
        return returnNull();
    }

    public String getTimeDateFunctions() throws SQLException {
        return returnNull();
    }

    public String getSearchStringEscape() throws SQLException {
        return returnNull();
    }

    public String getExtraNameCharacters() throws SQLException {
        return returnNull();
    }

    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return returnFalse();
    }

    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return returnFalse();
    }

    public boolean supportsColumnAliasing() throws SQLException {
        return returnFalse();
    }

    public boolean nullPlusNonNullIsNull() throws SQLException {
        return returnFalse();
    }

    public boolean supportsConvert() throws SQLException {
        return returnFalse();
    }

    public boolean supportsConvert(int fromType, int toType) throws SQLException {
        return returnFalse();
    }

    public boolean supportsTableCorrelationNames() throws SQLException {
        return returnFalse();
    }

    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        return returnFalse();
    }

    public boolean supportsExpressionsInOrderBy() throws SQLException {
        return returnFalse();
    }

    public boolean supportsOrderByUnrelated() throws SQLException {
        return returnFalse();
    }

    public boolean supportsGroupBy() throws SQLException {
        return returnFalse();
    }

    public boolean supportsGroupByUnrelated() throws SQLException {
        return returnFalse();
    }

    public boolean supportsGroupByBeyondSelect() throws SQLException {
        return returnFalse();
    }

    public boolean supportsLikeEscapeClause() throws SQLException {
        return returnFalse();
    }

    public boolean supportsMultipleResultSets() throws SQLException {
        return returnFalse();
    }

    public boolean supportsMultipleTransactions() throws SQLException {
        return returnFalse();
    }

    public boolean supportsNonNullableColumns() throws SQLException {
        return returnFalse();
    }

    public boolean supportsMinimumSQLGrammar() throws SQLException {
        return returnFalse();
    }

    public boolean supportsCoreSQLGrammar() throws SQLException {
        return returnFalse();
    }

    public boolean supportsExtendedSQLGrammar() throws SQLException {
        return returnFalse();
    }

    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return returnFalse();
    }

    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return returnFalse();
    }

    public boolean supportsANSI92FullSQL() throws SQLException {
        return returnFalse();
    }

    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        return returnFalse();
    }

    public boolean supportsOuterJoins() throws SQLException {
        return returnFalse();
    }

    public boolean supportsFullOuterJoins() throws SQLException {
        return returnFalse();
    }

    public boolean supportsLimitedOuterJoins() throws SQLException {
        return returnFalse();
    }

    public String getSchemaTerm() throws SQLException {
        return returnEmpty();
    }

    public String getProcedureTerm() throws SQLException {
        return returnEmpty();
    }

    public String getCatalogTerm() throws SQLException {
        return returnEmpty();
    }

    public boolean isCatalogAtStart() throws SQLException {
        return returnFalse();
    }

    public String getCatalogSeparator() throws SQLException {
        return returnEmpty();
    }

    public boolean supportsSchemasInDataManipulation() throws SQLException {
        return returnFalse();
    }

    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        return returnFalse();
    }

    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        return returnFalse();
    }

    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        return returnFalse();
    }

    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        return returnFalse();
    }

    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        return returnFalse();
    }

    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return returnFalse();
    }

    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        return returnFalse();
    }

    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        return returnFalse();
    }

    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        return returnFalse();
    }

    public boolean supportsPositionedDelete() throws SQLException {
        return returnFalse();
    }

    public boolean supportsPositionedUpdate() throws SQLException {
        return returnFalse();
    }

    public boolean supportsSelectForUpdate() throws SQLException {
        return returnFalse();
    }

    public boolean supportsStoredProcedures() throws SQLException {
        return returnFalse();
    }

    public boolean supportsSubqueriesInComparisons() throws SQLException {
        return returnFalse();
    }

    public boolean supportsSubqueriesInExists() throws SQLException {
        return returnFalse();
    }

    public boolean supportsSubqueriesInIns() throws SQLException {
        return returnFalse();
    }

    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return returnFalse();
    }

    public boolean supportsCorrelatedSubqueries() throws SQLException {
        return returnFalse();
    }

    public boolean supportsUnion() throws SQLException {
        return returnFalse();
    }

    public boolean supportsUnionAll() throws SQLException {
        return returnFalse();
    }

    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return returnFalse();
    }

    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        return returnFalse();
    }

    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        return returnFalse();
    }

    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        return returnFalse();
    }

    public int getMaxBinaryLiteralLength() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxCharLiteralLength() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxColumnNameLength() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxColumnsInGroupBy() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxColumnsInIndex() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxColumnsInOrderBy() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxColumnsInSelect() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxColumnsInTable() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxConnections() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxCursorNameLength() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxIndexLength() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxSchemaNameLength() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxProcedureNameLength() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxCatalogNameLength() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxRowSize() throws SQLException {
        return returnNumberOne();
    }

    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return returnFalse();
    }

    public int getMaxStatementLength() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxStatements() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxTableNameLength() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxTablesInSelect() throws SQLException {
        return returnNumberOne();
    }

    public int getMaxUserNameLength() throws SQLException {
        return returnNumberOne();
    }

    public int getDefaultTransactionIsolation() throws SQLException {
        return returnNumberOne();
    }

    public boolean supportsTransactions() throws SQLException {
        return returnFalse();
    }

    public boolean supportsTransactionIsolationLevel(int level) throws SQLException {
        return returnFalse();
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        return returnFalse();
    }

    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        return returnFalse();
    }

    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        return returnFalse();
    }

    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        return returnFalse();
    }

    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getSchemas() throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getCatalogs() throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getTableTypes() throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getVersionColumns(String catalog, String schema, String table) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getImportedKeys(String catalog, String schema, String table) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getExportedKeys(String catalog, String schema, String table) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getTypeInfo() throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) throws SQLException {
        return new MockResultSet();
    }

    public boolean supportsResultSetType(int type) throws SQLException {
        return returnFalse();
    }

    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SQLException {
        return returnFalse();
    }

    public boolean ownUpdatesAreVisible(int type) throws SQLException {
        return returnFalse();
    }

    public boolean ownDeletesAreVisible(int type) throws SQLException {
        return returnFalse();
    }

    public boolean ownInsertsAreVisible(int type) throws SQLException {
        return returnFalse();
    }

    public boolean othersUpdatesAreVisible(int type) throws SQLException {
        return returnFalse();
    }

    public boolean othersDeletesAreVisible(int type) throws SQLException {
        return returnFalse();
    }

    public boolean othersInsertsAreVisible(int type) throws SQLException {
        return returnFalse();
    }

    public boolean updatesAreDetected(int type) throws SQLException {
        return returnFalse();
    }

    public boolean deletesAreDetected(int type) throws SQLException {
        return returnFalse();
    }

    public boolean insertsAreDetected(int type) throws SQLException {
        return returnFalse();
    }

    public boolean supportsBatchUpdates() throws SQLException {
        return returnFalse();
    }

    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) throws SQLException {
        return new MockResultSet();
    }

    public boolean supportsSavepoints() throws SQLException {
        return returnFalse();
    }

    public boolean supportsNamedParameters() throws SQLException {
        return returnFalse();
    }

    public boolean supportsMultipleOpenResults() throws SQLException {
        return returnFalse();
    }

    public boolean supportsGetGeneratedKeys() throws SQLException {
        return returnFalse();
    }

    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public boolean supportsResultSetHoldability(int holdability) throws SQLException {
        return returnFalse();
    }

    public int getResultSetHoldability() throws SQLException {
        return returnNumberOne();
    }

    public int getDatabaseMajorVersion() throws SQLException {
        return returnNumberOne();
    }

    public int getDatabaseMinorVersion() throws SQLException {
        return returnNumberOne();
    }

    public int getJDBCMajorVersion() throws SQLException {
        return returnNumberOne();
    }

    public int getJDBCMinorVersion() throws SQLException {
        return returnNumberOne();
    }

    public int getSQLStateType() throws SQLException {
        return returnNumberOne();
    }

    public boolean locatorsUpdateCopy() throws SQLException {
        return returnFalse();
    }

    public boolean supportsStatementPooling() throws SQLException {
        return returnFalse();
    }

    public RowIdLifetime getRowIdLifetime() throws SQLException {
        return null;
    }

    public ResultSet getSchemas(String catalog, String schemaPattern) throws SQLException {
        return new MockResultSet();
    }

    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        return returnFalse();
    }

    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return returnFalse();
    }

    public ResultSet getClientInfoProperties() throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
        return new MockResultSet();
    }

    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return returnFalse();
    }

    private String returnEmpty() {
        return "";
    }

    private String returnNull() {
        return null;
    }

    private int returnNumberOne() {
        return 1;
    }

    private boolean returnFalse() {
        return false;
    }
}
