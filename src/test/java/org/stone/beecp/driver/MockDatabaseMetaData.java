/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0
 */
package org.stone.beecp.driver;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;

/**
 * @author Chris Liao
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
        return false;
    }

    public boolean allTablesAreSelectable() {
        return false;
    }

    public String getURL() {
        return null;
    }

    public String getUserName() {
        return null;
    }

    public boolean isReadOnly() {
        return false;
    }

    public boolean nullsAreSortedHigh() {
        return false;
    }

    public boolean nullsAreSortedLow() {
        return false;
    }

    public boolean nullsAreSortedAtStart() {
        return false;
    }

    public boolean nullsAreSortedAtEnd() {
        return false;
    }

    public String getDatabaseProductName() {
        return null;
    }

    public String getDatabaseProductVersion() {
        return null;
    }

    public String getDriverName() {
        return null;
    }

    public String getDriverVersion() {
        return null;
    }

    public int getDriverMajorVersion() {
        return returnNumberOne();
    }

    public int getDriverMinorVersion() {
        return 0;
    }

    public boolean usesLocalFiles() {
        return false;
    }

    public boolean usesLocalFilePerTable() {
        return false;
    }

    public boolean supportsMixedCaseIdentifiers() {
        return false;
    }

    public boolean storesUpperCaseIdentifiers() {
        return false;
    }

    public boolean storesLowerCaseIdentifiers() {
        return false;
    }

    public boolean storesMixedCaseIdentifiers() {
        return false;
    }

    public boolean supportsMixedCaseQuotedIdentifiers() {
        return false;
    }

    public boolean storesUpperCaseQuotedIdentifiers() {
        return false;
    }

    public boolean storesLowerCaseQuotedIdentifiers() {
        return false;
    }

    public boolean storesMixedCaseQuotedIdentifiers() {
        return false;
    }

    public String getIdentifierQuoteString() {
        return null;
    }

    public String getSQLKeywords() {
        return null;
    }

    public String getNumericFunctions() {
        return null;
    }

    public String getStringFunctions() {
        return null;
    }

    public String getSystemFunctions() {
        return null;
    }

    public String getTimeDateFunctions() {
        return null;
    }

    public String getSearchStringEscape() {
        return null;
    }

    public String getExtraNameCharacters() {
        return null;
    }

    public boolean supportsAlterTableWithAddColumn() {
        return false;
    }

    public boolean supportsAlterTableWithDropColumn() {
        return false;
    }

    public boolean supportsColumnAliasing() {
        return false;
    }

    public boolean nullPlusNonNullIsNull() {
        return false;
    }

    public boolean supportsConvert() {
        return false;
    }

    public boolean supportsConvert(int fromType, int toType) {
        return false;
    }

    public boolean supportsTableCorrelationNames() {
        return false;
    }

    public boolean supportsDifferentTableCorrelationNames() {
        return false;
    }

    public boolean supportsExpressionsInOrderBy() {
        return false;
    }

    public boolean supportsOrderByUnrelated() {
        return false;
    }

    public boolean supportsGroupBy() {
        return false;
    }

    public boolean supportsGroupByUnrelated() {
        return false;
    }

    public boolean supportsGroupByBeyondSelect() {
        return false;
    }

    public boolean supportsLikeEscapeClause() {
        return false;
    }

    public boolean supportsMultipleResultSets() {
        return false;
    }

    public boolean supportsMultipleTransactions() {
        return false;
    }

    public boolean supportsNonNullableColumns() {
        return false;
    }

    public boolean supportsMinimumSQLGrammar() {
        return false;
    }

    public boolean supportsCoreSQLGrammar() {
        return false;
    }

    public boolean supportsExtendedSQLGrammar() {
        return false;
    }

    public boolean supportsANSI92EntryLevelSQL() {
        return false;
    }

    public boolean supportsANSI92IntermediateSQL() {
        return false;
    }

    public boolean supportsANSI92FullSQL() {
        return false;
    }

    public boolean supportsIntegrityEnhancementFacility() {
        return false;
    }

    public boolean supportsOuterJoins() {
        return false;
    }

    public boolean supportsFullOuterJoins() {
        return false;
    }

    public boolean supportsLimitedOuterJoins() {
        return false;
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
        return false;
    }

    public String getCatalogSeparator() {
        return returnEmpty();
    }

    public boolean supportsSchemasInDataManipulation() {
        return false;
    }

    public boolean supportsSchemasInProcedureCalls() {
        return false;
    }

    public boolean supportsSchemasInTableDefinitions() {
        return false;
    }

    public boolean supportsSchemasInIndexDefinitions() {
        return false;
    }

    public boolean supportsSchemasInPrivilegeDefinitions() {
        return false;
    }

    public boolean supportsCatalogsInDataManipulation() {
        return false;
    }

    public boolean supportsCatalogsInProcedureCalls() {
        return false;
    }

    public boolean supportsCatalogsInTableDefinitions() {
        return false;
    }

    public boolean supportsCatalogsInIndexDefinitions() {
        return false;
    }

    public boolean supportsCatalogsInPrivilegeDefinitions() {
        return false;
    }

    public boolean supportsPositionedDelete() {
        return false;
    }

    public boolean supportsPositionedUpdate() {
        return false;
    }

    public boolean supportsSelectForUpdate() {
        return false;
    }

    public boolean supportsStoredProcedures() {
        return false;
    }

    public boolean supportsSubqueriesInComparisons() {
        return false;
    }

    public boolean supportsSubqueriesInExists() {
        return false;
    }

    public boolean supportsSubqueriesInIns() {
        return false;
    }

    public boolean supportsSubqueriesInQuantifieds() {
        return false;
    }

    public boolean supportsCorrelatedSubqueries() {
        return false;
    }

    public boolean supportsUnion() {
        return false;
    }

    public boolean supportsUnionAll() {
        return false;
    }

    public boolean supportsOpenCursorsAcrossCommit() {
        return false;
    }

    public boolean supportsOpenCursorsAcrossRollback() {
        return false;
    }

    public boolean supportsOpenStatementsAcrossCommit() {
        return false;
    }

    public boolean supportsOpenStatementsAcrossRollback() {
        return false;
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
        return false;
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
        return false;
    }

    public boolean supportsTransactionIsolationLevel(int level) {
        return false;
    }

    public boolean supportsDataDefinitionAndDataManipulationTransactions() {
        return false;
    }

    public boolean supportsDataManipulationTransactionsOnly() {
        return false;
    }

    public boolean dataDefinitionCausesTransactionCommit() {
        return false;
    }

    public boolean dataDefinitionIgnoredInTransactions() {
        return false;
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
        return false;
    }

    public boolean supportsResultSetConcurrency(int type, int concurrency) {
        return false;
    }

    public boolean ownUpdatesAreVisible(int type) {
        return false;
    }

    public boolean ownDeletesAreVisible(int type) {
        return false;
    }

    public boolean ownInsertsAreVisible(int type) {
        return false;
    }

    public boolean othersUpdatesAreVisible(int type) {
        return false;
    }

    public boolean othersDeletesAreVisible(int type) {
        return false;
    }

    public boolean othersInsertsAreVisible(int type) {
        return false;
    }

    public boolean updatesAreDetected(int type) {
        return false;
    }

    public boolean deletesAreDetected(int type) {
        return false;
    }

    public boolean insertsAreDetected(int type) {
        return false;
    }

    public boolean supportsBatchUpdates() {
        return false;
    }

    public ResultSet getUDTs(String catalog, String schemaPattern, String typeNamePattern, int[] types) {
        return new MockResultSet();
    }

    public boolean supportsSavepoints() {
        return false;
    }

    public boolean supportsNamedParameters() {
        return false;
    }

    public boolean supportsMultipleOpenResults() {
        return false;
    }

    public boolean supportsGetGeneratedKeys() {
        return false;
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
        return false;
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
        return false;
    }

    public boolean supportsStatementPooling() {
        return false;
    }

    public RowIdLifetime getRowIdLifetime() {
        return null;
    }

    public ResultSet getSchemas(String catalog, String schemaPattern) {
        return new MockResultSet();
    }

    public boolean supportsStoredFunctionsUsingCallSyntax() {
        return false;
    }

    public boolean autoCommitFailureClosesAllResultSets() {
        return false;
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
        return false;
    }

    private String returnEmpty() {
        return "";
    }

    private int returnNumberOne() {
        return 1;
    }
}
