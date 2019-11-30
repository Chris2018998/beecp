package com.zaxxer.hikari.benchmark.stubs;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

public class SubDatabaseMetaData implements DatabaseMetaData {
	private Connection con;

	public SubDatabaseMetaData(Connection con) {
		this.con = con;
	}

	public Connection getConnection() throws SQLException {
		return con;
	}

	public <T> T unwrap(java.lang.Class<T> iface) throws java.sql.SQLException {
		return null;
	}

	public boolean isWrapperFor(java.lang.Class<?> iface) throws java.sql.SQLException {
		return true;
	}

	public boolean allProceduresAreCallable() throws SQLException {

		return true;
	}

	public boolean allTablesAreSelectable() throws SQLException {

		return true;
	}

	public String getURL() throws SQLException {

		return null;
	}

	public String getUserName() throws SQLException {

		return null;
	}

	public boolean isReadOnly() throws SQLException {

		return true;
	}

	public boolean nullsAreSortedHigh() throws SQLException {

		return true;
	}

	public boolean nullsAreSortedLow() throws SQLException {

		return true;
	}

	public boolean nullsAreSortedAtStart() throws SQLException {

		return true;
	}

	public boolean nullsAreSortedAtEnd() throws SQLException {

		return true;
	}

	public String getDatabaseProductName() throws SQLException {

		return "123";
	}

	public String getDatabaseProductVersion() throws SQLException {

		return "123";
	}

	public String getDriverName() throws SQLException {

		return "123";
	}

	public String getDriverVersion() throws SQLException {

		return "123";
	}

	public int getDriverMajorVersion() {

		return 1;
	}

	public int getDriverMinorVersion() {

		return 1;
	}

	public boolean usesLocalFiles() throws SQLException {

		return true;
	}

	public boolean usesLocalFilePerTable() throws SQLException {

		return true;
	}

	public boolean supportsMixedCaseIdentifiers() throws SQLException {

		return true;
	}

	public boolean storesUpperCaseIdentifiers() throws SQLException {

		return true;
	}

	public boolean storesLowerCaseIdentifiers() throws SQLException {

		return true;
	}

	public boolean storesMixedCaseIdentifiers() throws SQLException {

		return true;
	}

	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {

		return true;
	}

	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {

		return true;
	}

	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {

		return true;
	}

	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {

		return true;
	}

	public String getIdentifierQuoteString() throws SQLException {

		return "123";
	}

	public String getSQLKeywords() throws SQLException {

		return "123";
	}

	public String getNumericFunctions() throws SQLException {

		return "123";
	}

	public String getStringFunctions() throws SQLException {

		return "123";
	}

	public String getSystemFunctions() throws SQLException {

		return "123";
	}

	public String getTimeDateFunctions() throws SQLException {

		return "123";
	}

	public String getSearchStringEscape() throws SQLException {

		return "123";
	}

	public String getExtraNameCharacters() throws SQLException {

		return "123";
	}

	public boolean supportsAlterTableWithAddColumn() throws SQLException {

		return true;
	}

	public boolean supportsAlterTableWithDropColumn() throws SQLException {

		return true;
	}

	public boolean supportsColumnAliasing() throws SQLException {

		return true;
	}

	public boolean nullPlusNonNullIsNull() throws SQLException {

		return true;
	}

	public boolean supportsConvert() throws SQLException {

		return true;
	}

	public boolean supportsConvert(int paramInt1, int paramInt2) throws SQLException {

		return true;
	}

	public boolean supportsTableCorrelationNames() throws SQLException {

		return true;
	}

	public boolean supportsDifferentTableCorrelationNames() throws SQLException {

		return true;
	}

	public boolean supportsExpressionsInOrderBy() throws SQLException {

		return true;
	}

	public boolean supportsOrderByUnrelated() throws SQLException {

		return true;
	}

	public boolean supportsGroupBy() throws SQLException {

		return true;
	}

	public boolean supportsGroupByUnrelated() throws SQLException {

		return true;
	}

	public boolean supportsGroupByBeyondSelect() throws SQLException {

		return true;
	}

	public boolean supportsLikeEscapeClause() throws SQLException {

		return true;
	}

	public boolean supportsMultipleResultSets() throws SQLException {

		return true;
	}

	public boolean supportsMultipleTransactions() throws SQLException {

		return true;
	}

	public boolean supportsNonNullableColumns() throws SQLException {

		return true;
	}

	public boolean supportsMinimumSQLGrammar() throws SQLException {

		return true;
	}

	public boolean supportsCoreSQLGrammar() throws SQLException {

		return true;
	}

	public boolean supportsExtendedSQLGrammar() throws SQLException {

		return true;
	}

	public boolean supportsANSI92EntryLevelSQL() throws SQLException {

		return true;
	}

	public boolean supportsANSI92IntermediateSQL() throws SQLException {

		return true;
	}

	public boolean supportsANSI92FullSQL() throws SQLException {

		return true;
	}

	public boolean supportsIntegrityEnhancementFacility() throws SQLException {

		return true;
	}

	public boolean supportsOuterJoins() throws SQLException {

		return true;
	}

	public boolean supportsFullOuterJoins() throws SQLException {

		return true;
	}

	public boolean supportsLimitedOuterJoins() throws SQLException {

		return true;
	}

	public String getSchemaTerm() throws SQLException {

		return "";
	}

	public String getProcedureTerm() throws SQLException {

		return "";
	}

	public String getCatalogTerm() throws SQLException {

		return "";
	}

	public boolean isCatalogAtStart() throws SQLException {
		return true;
	}

	public String getCatalogSeparator() throws SQLException {
		return "";
	}

	public boolean supportsSchemasInDataManipulation() throws SQLException {
		return true;
	}

	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		return true;
	}

	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		return true;
	}

	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		return true;
	}

	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		return true;
	}

	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		return true;
	}

	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		return true;
	}

	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		return true;
	}

	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		return true;
	}

	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		return true;
	}

	public boolean supportsPositionedDelete() throws SQLException {
		return true;
	}

	public boolean supportsPositionedUpdate() throws SQLException {
		return true;
	}

	public boolean supportsSelectForUpdate() throws SQLException {
		return true;
	}

	public boolean supportsStoredProcedures() throws SQLException {
		return true;
	}

	public boolean supportsSubqueriesInComparisons() throws SQLException {
		return true;
	}

	public boolean supportsSubqueriesInExists() throws SQLException {
		return true;
	}

	public boolean supportsSubqueriesInIns() throws SQLException {
		return true;
	}

	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		return true;
	}

	public boolean supportsCorrelatedSubqueries() throws SQLException {
		return true;
	}

	public boolean supportsUnion() throws SQLException {
		return true;
	}

	public boolean supportsUnionAll() throws SQLException {
		return true;
	}

	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		return true;
	}

	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		return true;
	}

	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		return true;
	}

	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		return true;
	}

	public int getMaxBinaryLiteralLength() {
		return 1;
	}

	public int getMaxCharLiteralLength() {
		return 1;
	}

	public int getMaxColumnNameLength() {
		return 1;
	}

	public int getMaxColumnsInGroupBy() {
		return 1;
	}

	public int getMaxColumnsInIndex() {
		return 1;
	}

	public int getMaxColumnsInOrderBy() {
		return 1;
	}

	public int getMaxColumnsInSelect() {
		return 1;
	}

	public int getMaxColumnsInTable() {
		return 1;
	}

	public int getMaxConnections() {
		return 1;
	}

	public int getMaxCursorNameLength() {
		return 1;
	}

	public int getMaxIndexLength() {
		return 1;
	}

	public int getMaxSchemaNameLength() {
		return 1;
	}

	public int getMaxProcedureNameLength() {
		return 1;
	}

	public int getMaxCatalogNameLength() {
		return 1;
	}

	public int getMaxRowSize() {
		return 1;
	}

	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {

		return true;
	}

	public int getMaxStatementLength() {

		return 1;
	}

	public int getMaxStatements() {

		return 1;
	}

	public int getMaxTableNameLength() {

		return 1;
	}

	public int getMaxTablesInSelect() {

		return 1;
	}

	public int getMaxUserNameLength() {

		return 1;
	}

	public int getDefaultTransactionIsolation() {

		return 1;
	}

	public boolean supportsTransactions() throws SQLException {

		return true;
	}

	public boolean supportsTransactionIsolationLevel(int paramInt) throws SQLException {

		return true;
	}

	public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {

		return true;
	}

	public boolean supportsDataManipulationTransactionsOnly() throws SQLException {

		return true;
	}

	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {

		return true;
	}

	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {

		return true;
	}

	public ResultSet getProcedures(String paramString1, String paramString2, String paramString3) throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getProcedureColumns(String paramString1, String paramString2, String paramString3,
			String paramString4) throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getTables(String paramString1, String paramString2, String paramString3,
			String[] paramArrayOfString) throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getSchemas() throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getCatalogs() throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getTableTypes() throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getColumns(String paramString1, String paramString2, String paramString3, String paramString4)
			throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getColumnPrivileges(String paramString1, String paramString2, String paramString3,
			String paramString4) throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getTablePrivileges(String paramString1, String paramString2, String paramString3)
			throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getBestRowIdentifier(String paramString1, String paramString2, String paramString3, int paramInt,
			boolean paramBoolean) throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getVersionColumns(String paramString1, String paramString2, String paramString3)
			throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getPrimaryKeys(String paramString1, String paramString2, String paramString3) throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getImportedKeys(String paramString1, String paramString2, String paramString3)
			throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getExportedKeys(String paramString1, String paramString2, String paramString3)
			throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getCrossReference(String paramString1, String paramString2, String paramString3,
			String paramString4, String paramString5, String paramString6) throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getTypeInfo() throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getIndexInfo(String paramString1, String paramString2, String paramString3, boolean paramBoolean1,
			boolean paramBoolean2) throws SQLException {

		return new StubResultSet(null);
	}

	public boolean supportsResultSetType(int paramInt) throws SQLException {

		return true;
	}

	public boolean supportsResultSetConcurrency(int paramInt1, int paramInt2) throws SQLException {

		return true;
	}

	public boolean ownUpdatesAreVisible(int paramInt) throws SQLException {

		return true;
	}

	public boolean ownDeletesAreVisible(int paramInt) throws SQLException {

		return true;
	}

	public boolean ownInsertsAreVisible(int paramInt) throws SQLException {

		return true;
	}

	public boolean othersUpdatesAreVisible(int paramInt) throws SQLException {

		return true;
	}

	public boolean othersDeletesAreVisible(int paramInt) throws SQLException {

		return true;
	}

	public boolean othersInsertsAreVisible(int paramInt) throws SQLException {

		return true;
	}

	public boolean updatesAreDetected(int paramInt) throws SQLException {

		return true;
	}

	public boolean deletesAreDetected(int paramInt) throws SQLException {

		return true;
	}

	public boolean insertsAreDetected(int paramInt) throws SQLException {

		return true;
	}

	public boolean supportsBatchUpdates() throws SQLException {

		return true;
	}

	public ResultSet getUDTs(String paramString1, String paramString2, String paramString3, int[] paramArrayOfInt)
			throws SQLException {

		return new StubResultSet(null);
	}

	public boolean supportsSavepoints() throws SQLException {
		return true;
	}

	public boolean supportsNamedParameters() throws SQLException {
		return true;
	}

	public boolean supportsMultipleOpenResults() throws SQLException {
		return true;
	}

	public boolean supportsGetGeneratedKeys() throws SQLException {
		return true;
	}

	public ResultSet getSuperTypes(String paramString1, String paramString2, String paramString3) throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getSuperTables(String paramString1, String paramString2, String paramString3) throws SQLException {

		return new StubResultSet(null);
	}

	public ResultSet getAttributes(String paramString1, String paramString2, String paramString3, String paramString4)
			throws SQLException {

		return new StubResultSet(null);
	}

	public boolean supportsResultSetHoldability(int paramInt) throws SQLException {
		return true;
	}

	public int getResultSetHoldability() {
		return 1;
	}

	public int getDatabaseMajorVersion() {
		return 1;
	}

	public int getDatabaseMinorVersion() {
		return 1;
	}

	public int getJDBCMajorVersion() {
		return 1;
	}

	public int getJDBCMinorVersion() {
		return 1;
	}

	public int getSQLStateType() {
		return 1;
	}

	public boolean locatorsUpdateCopy() throws SQLException {
		return true;
	}

	public boolean supportsStatementPooling() throws SQLException {
		return true;
	}

	public RowIdLifetime getRowIdLifetime() throws SQLException {
		return null;
	}

	public ResultSet getSchemas(String paramString1, String paramString2) throws SQLException {
		return new StubResultSet(null);
	}

	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		return true;
	}

	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		return true;
	}

	public ResultSet getClientInfoProperties() throws SQLException {
		return new StubResultSet(null);
	}

	public ResultSet getFunctions(String paramString1, String paramString2, String paramString3) throws SQLException {
		return new StubResultSet(null);
	}

	public ResultSet getFunctionColumns(String paramString1, String paramString2, String paramString3,
			String paramString4) throws SQLException {
		return new StubResultSet(null);
	}

	public ResultSet getPseudoColumns(String paramString1, String paramString2, String paramString3,
			String paramString4) throws SQLException {
		return new StubResultSet(null);
	}

	public boolean generatedKeyAlwaysReturned() throws SQLException {
		return true;
	}

}
