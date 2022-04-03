/*
 * Copyright(C) Chris2018998
 * Contact:Chris2018998@tom.com
 *
 * Licensed under GNU Lesser General Public License v2.1
 */
package cn.beecp.test.mock;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;

/**
 * @author Chris.Liao
 * @version 1.0
 */
public class MockDatabaseMetaData extends MockBase implements DatabaseMetaData {
    private final MockConnection connection;

    MockDatabaseMetaData(MockConnection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean allProceduresAreCallable() {
        return returnFalse();
    }

    public boolean allTablesAreSelectable() {
        return returnFalse();
    }

    public String getURL() {
        return returnNull();
    }

    public String getUserName() {
        return returnNull();
    }

    public boolean isReadOnly() {
        return returnFalse();
    }

    public boolean nullsAreSortedHigh() {
        return returnFalse();
    }

    public boolean nullsAreSortedLow() {
        return returnFalse();
    }

    public boolean nullsAreSortedAtStart() {
        return returnFalse();
    }

    public boolean nullsAreSortedAtEnd() {
        return returnFalse();
    }

    public String getDatabaseProductName() {
        return returnNull();
    }

    public String getDatabaseProductVersion() {
        return returnNull();
    }

    public String getDriverName() {
        return returnNull();
    }

    public String getDriverVersion() {
        return returnNull();
    }

    public int getDriverMajorVersion() {
        return returnNumberOne();
    }

    public int getDriverMinorVersion() {
        return 0;
    }

    public boolean usesLocalFiles() {
        return returnFalse();
    }

    public boolean usesLocalFilePerTable() {
        return returnFalse();
    }

    public boolean supportsMixedCaseIdentifiers() {
        return returnFalse();
    }

    public boolean storesUpperCaseIdentifiers() {
        return returnFalse();
    }

    public boolean storesLowerCaseIdentifiers() {
        return returnFalse();
    }

    public boolean storesMixedCaseIdentifiers() {
        return returnFalse();
    }

    public boolean supportsMixedCaseQuotedIdentifiers() {
        return returnFalse();
    }

    public boolean storesUpperCaseQuotedIdentifiers() {
        return returnFalse();
    }

    public boolean storesLowerCaseQuotedIdentifiers() {
        return returnFalse();
    }

    public boolean storesMixedCaseQuotedIdentifiers() {
        return returnFalse();
    }

    public String getIdentifierQuoteString() {
        return returnNull();
    }

    public String getSQLKeywords() {
        return returnNull();
    }

    public String getNumericFunctions() {
        return returnNull();
    }

    public String getStringFunctions() {
        return returnNull();
    }

    public String getSystemFunctions() {
        return returnNull();
    }

    public String getTimeDateFunctions() {
        return returnNull();
    }

    public String getSearchStringEscape() {
        return returnNull();
    }

    public String getExtraNameCharacters() {
        return returnNull();
    }

    public boolean supportsAlterTableWithAddColumn() {
        return returnFalse();
    }

    public boolean supportsAlterTableWithDropColumn() {
        return returnFalse();
    }

    public boolean supportsColumnAliasing() {
        return returnFalse();
    }

    public boolean nullPlusNonNullIsNull() {
        return returnFalse();
    }

    public boolean supportsConvert() {
        return returnFalse();
    }

    public boolean supportsConvert(int fromType, int toType) {
        return returnFalse();
    }

    public boolean supportsTableCorrelationNames() {
        return returnFalse();
    }

    public boolean supportsDifferentTableCorrelationNames() {
        return returnFalse();
    }

    public boolean supportsExpressionsInOrderBy() {
        return returnFalse();
    }

    public boolean supportsOrderByUnrelated() {
        return returnFalse();
    }

    public boolean supportsGroupBy() {
        return returnFalse();
    }

    public boolean supportsGroupByUnrelated() {
        return returnFalse();
    }

    public boolean supportsGroupByBeyondSelect() {
        return returnFalse();
    }

    public boolean supportsLikeEscapeClause() {
        return returnFalse();
    }

    public boolean supportsMultipleResultSets() {
        return returnFalse();
    }

    public boolean supportsMultipleTransactions() {
        return returnFalse();
    }

    public boolean supportsNonNullableColumns() {
        return returnFalse();
    }

    public boolean supportsMinimumSQLGrammar() {
        return returnFalse();
    }

    public boolean supportsCoreSQLGrammar() {
        return returnFalse();
    }

    public boolean supportsExtendedSQLGrammar() {
        return returnFalse();
    }

    public boolean supportsANSI92EntryLevelSQL() {
        return returnFalse();
    }

    public boolean supportsANSI92IntermediateSQL() {
        return returnFalse();
    }

    public boolean supportsANSI92FullSQL() {
        return returnFalse();
    }

    public boolean supportsIntegrityEnhancementFacility() {
        return returnFalse();
    }

    public boolean supportsOuterJoins() {
        return returnFalse();
    }

    public boolean supportsFullOuterJoins() {
        return returnFalse();
    }

    public boolean supportsLimitedOuterJoins() {
        return returnFalse();
    }

    public String getSchemaTerm() {
        return returnEmpty();
    }

    public String getProcedureTerm() {
        return returnEmpty();
    }

    public String getCatalogTerm() {
        return returnEmpty();
    }

    public boolean isCatalogAtStart() {
        return returnFalse();
    }

    public String getCatalogSeparator() {
        return returnEmpty();
    }

    public boolean supportsSchemasInDataManipulation() {
        return returnFalse();
    }

    public boolean supportsSchemasInProcedureCalls() {
        return returnFalse();
    }

    public boolean supportsSchemasInTableDefinitions() {
        return returnFalse();
    }

    public boolean supportsSchemasInIndexDefinitions() {
        return returnFalse();
    }

    public boolean supportsSchemasInPrivilegeDefinitions() {
        return returnFalse();
    }

    public boolean supportsCatalogsInDataManipulation() {
        return returnFalse();
    }

    public boolean supportsCatalogsInProcedureCalls() {
        return returnFalse();
    }

    public boolean supportsCatalogsInTableDefinitions() {
        return returnFalse();
    }

    public boolean supportsCatalogsInIndexDefinitions() {
        return returnFalse();
    }

    public boolean supportsCatalogsInPrivilegeDefinitions() {
        return returnFalse();
    }

    public boolean supportsPositionedDelete() {
        return returnFalse();
    }

    public boolean supportsPositionedUpdate() {
        return returnFalse();
    }

    public boolean supportsSelectForUpdate() {
        return returnFalse();
    }

    public boolean supportsStoredProcedures() {
        return returnFalse();
    }

    public boolean supportsSubqueriesInComparisons() {
        return returnFalse();
    }

    public boolean supportsSubqueriesInExists() {
        return returnFalse();
    }

    public boolean supportsSubqueriesInIns() {
        return returnFalse();
    }

    public boolean supportsSubqueriesInQuantifieds() {
        return returnFalse();
    }

    public boolean supportsCorrelatedSubqueries() {
        return returnFalse();
    }

    public boolean supportsUnion() {
        return returnFalse();
    }

    public boolean supportsUnionAll() {
        return returnFalse();
    }

    public boolean supportsOpenCursorsAcrossCommit() {
        return returnFalse();
    }

    public boolean supportsOpenCursorsAcrossRollback() {
        return returnFalse();
    }

    public boolean supportsOpenStatementsAcrossCommit() {
        return returnFalse();
    }

    public boolean supportsOpenStatementsAcrossRollback() {
        return returnFalse();
    }

    public int getMaxBinaryLiteralLength() {
        return returnNumberOne();
    }

    public int getMaxCharLiteralLength() {
        return returnNumberOne();
    }

    public int getMaxColumnNameLength() {
        return returnNumberOne();
    }

    public int getMaxColumnsInGroupBy() {
        return returnNumberOne();
    }

    public int getMaxColumnsInIndex() {
        return returnNumberOne();
    }

    public int getMaxColumnsInOrderBy() {
        return returnNumberOne();
    }

    public int getMaxColumnsInSelect() {
        return returnNumberOne();
    }

    public int getMaxColumnsInTable() {
        return returnNumberOne();
    }

    public int getMaxConnections() {
        return returnNumberOne();
    }

    public int getMaxCursorNameLength() {
        return returnNumberOne();
    }

    public int getMaxIndexLength() {
        return returnNumberOne();
    }

    public int getMaxSchemaNameLength() {
        return returnNumberOne();
    }

    public int getMaxProcedureNameLength() {
        return returnNumberOne();
    }

    public int getMaxCatalogNameLength() {
        return returnNumberOne();
    }

    public int getMaxRowSize() {
        return returnNumberOne();
    }

    public boolean doesMaxRowSizeIncludeBlobs() {
        return returnFalse();
    }

    public int getMaxStatementLength() {
        return returnNumberOne();
    }

    public int getMaxStatements() {
        return returnNumberOne();
    }

    public int getMaxTableNameLength() {
        return returnNumberOne();
    }

    public int getMaxTablesInSelect() {
        return returnNumberOne();
    }

    public int getMaxUserNameLength() {
        return returnNumberOne();
    }

    public int getDefaultTransactionIsolation() {
        return returnNumberOne();
    }

    public boolean supportsTransactions() {
        return returnFalse();
    }

    public boolean supportsTransactionIsolationLevel(int level) {
        return returnFalse();
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions() {
        return returnFalse();
    }

    public boolean supportsDataManipulationTransactionsOnly() {
        return returnFalse();
    }

    public boolean dataDefinitionCausesTransactionCommit() {
        return returnFalse();
    }

    public boolean dataDefinitionIgnoredInTransactions() {
        return returnFalse();
    }

    public ResultSet getProcedures(String catalog, String schemaPattern, String procedureNamePattern) {
        return new MockResultSet();
    }

    public ResultSet getProcedureColumns(String catalog, String schemaPattern, String procedureNamePattern, String columnNamePattern) {
        return new MockResultSet();
    }

    public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) {
        return new MockResultSet();
    }

    public ResultSet getSchemas() {
        return new MockResultSet();
    }

    public ResultSet getCatalogs() {
        return new MockResultSet();
    }

    public ResultSet getTableTypes() {
        return new MockResultSet();
    }

    public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) {
        return new MockResultSet();
    }

    public ResultSet getColumnPrivileges(String catalog, String schema, String table, String columnNamePattern) {
        return new MockResultSet();
    }

    public ResultSet getTablePrivileges(String catalog, String schemaPattern, String tableNamePattern) {
        return new MockResultSet();
    }

    public ResultSet getBestRowIdentifier(String catalog, String schema, String table, int scope, boolean nullable) {
        return new MockResultSet();
    }

    public ResultSet getVersionColumns(String catalog, String schema, String table) {
        return new MockResultSet();
    }

    public ResultSet getPrimaryKeys(String catalog, String schema, String table) {
        return new MockResultSet();
    }

    public ResultSet getImportedKeys(String catalog, String schema, String table) {
        return new MockResultSet();
    }

    public ResultSet getExportedKeys(String catalog, String schema, String table) {
        return new MockResultSet();
    }

    public ResultSet getCrossReference(String parentCatalog, String parentSchema, String parentTable, String foreignCatalog, String foreignSchema, String foreignTable) {
        return new MockResultSet();
    }

    public ResultSet getTypeInfo() {
        return new MockResultSet();
    }

    public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) {
        return new MockResultSet();
    }

    public boolean supportsResultSetType(int type) {
        return returnFalse();
    }

    public boolean supportsResultSetConcurrency(int type, int concurrency) {
        return returnFalse();
    }

    public boolean ownUpdatesAreVisible(int type) {
        return returnFalse();
    }

    public boolean ownDeletesAreVisible(int type) {
        return returnFalse();
    }

    public boolean ownInsertsAreVisible(int type) {
        return returnFalse();
    }

    public boolean othersUpdatesAreVisible(int type) {
        return returnFalse();
    }

    public boolean othersDeletesAreVisible(int type) {
        return returnFalse();
    }

    public boolean othersInsertsAreVisible(int type) {
        return returnFalse();
    }

    public boolean updatesAreDetected(int type) {
        return returnFalse();
    }

    public boolean deletesAreDetected(int type) {
        return returnFalse();
    }

    public boolean insertsAreDetected(int type) {
        return returnFalse();
    }

    public boolean supportsBatchUpdates() {
        return returnFalse();
    }

    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) {
        return new MockResultSet();
    }

    public boolean supportsSavepoints() {
        return returnFalse();
    }

    public boolean supportsNamedParameters() {
        return returnFalse();
    }

    public boolean supportsMultipleOpenResults() {
        return returnFalse();
    }

    public boolean supportsGetGeneratedKeys() {
        return returnFalse();
    }

    public ResultSet getSuperTypes(String catalog, String schemaPattern, String typeNamePattern) {
        return new MockResultSet();
    }

    public ResultSet getSuperTables(String catalog, String schemaPattern, String tableNamePattern) {
        return new MockResultSet();
    }

    public ResultSet getAttributes(String catalog, String schemaPattern, String typeNamePattern, String attributeNamePattern) {
        return new MockResultSet();
    }

    public boolean supportsResultSetHoldability(int holdability) {
        return returnFalse();
    }

    public int getResultSetHoldability() {
        return returnNumberOne();
    }

    public int getDatabaseMajorVersion() {
        return returnNumberOne();
    }

    public int getDatabaseMinorVersion() {
        return returnNumberOne();
    }

    public int getJDBCMajorVersion() {
        return returnNumberOne();
    }

    public int getJDBCMinorVersion() {
        return returnNumberOne();
    }

    public int getSQLStateType() {
        return returnNumberOne();
    }

    public boolean locatorsUpdateCopy() {
        return returnFalse();
    }

    public boolean supportsStatementPooling() {
        return returnFalse();
    }

    public RowIdLifetime getRowIdLifetime() {
        return null;
    }

    public ResultSet getSchemas(String catalog, String schemaPattern) {
        return new MockResultSet();
    }

    public boolean supportsStoredFunctionsUsingCallSyntax() {
        return returnFalse();
    }

    public boolean autoCommitFailureClosesAllResultSets() {
        return returnFalse();
    }

    public ResultSet getClientInfoProperties() {
        return new MockResultSet();
    }

    public ResultSet getFunctions(String catalog, String schemaPattern, String functionNamePattern) {
        return new MockResultSet();
    }

    public ResultSet getFunctionColumns(String catalog, String schemaPattern, String functionNamePattern, String columnNamePattern) {
        return new MockResultSet();
    }

    public ResultSet getPseudoColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) {
        return new MockResultSet();
    }

    public boolean generatedKeyAlwaysReturned() {
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
