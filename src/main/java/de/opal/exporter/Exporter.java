package de.opal.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.PooledConnection;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.db.SQLclUtil;
import de.opal.installer.db.DBUtils;
import de.opal.installer.util.Msg;
import de.opal.installer.util.Utils;
import oracle.dbtools.db.ResultSetFormatter;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunnerContext;
import oracle.jdbc.pool.OracleConnectionPoolDataSource;

public class Exporter {

	private static final Logger log = LogManager.getLogger(Exporter.class.getName());
	private ScriptExecutor sqlcl = null;

	private String user;
	private String pwd;
	private String connectStr;
	private String outputDir;
	private boolean skipErrors;
	private HashMap<String, ArrayList<String>> dependentObjectsMap;
	private boolean isSilent;
	private HashMap<String, String> extensionMappingsMap;
	private HashMap<String, String> directoryMappingsMap;

	/**
	 * 
	 * @param user
	 * @param pwd
	 * @param connectStr
	 */
	public Exporter(String user, String pwd, String connectStr, String outputDir, boolean skipErrors,
			HashMap<String, ArrayList<String>> dependentObjectsMap, boolean isSilent,
			HashMap<String, String> extensionMappingsMap, HashMap<String, String> directoryMappingsMap) {
		super();
		this.user = user;
		this.pwd = pwd;
		this.connectStr = connectStr;
		this.outputDir = outputDir;
		this.skipErrors = skipErrors;
		this.dependentObjectsMap = dependentObjectsMap;
		this.isSilent = isSilent;
		this.extensionMappingsMap = extensionMappingsMap;
		this.directoryMappingsMap=directoryMappingsMap;
	}

	private String computeExportFilename(String schemaName, String objectType, String objectName) {
		String filename = "";
		String suffix = "sql";
		String objectTypePath=objectType;

		if (this.extensionMappingsMap.containsKey(objectType))
			suffix = this.extensionMappingsMap.get(objectType);
		else if (this.extensionMappingsMap.containsKey("DEFAULT"))
			suffix = this.extensionMappingsMap.get("DEFAULT");
		
		if (this.directoryMappingsMap.containsKey(objectType))
			objectTypePath = this.directoryMappingsMap.get(objectType);		

		switch (objectType) {
		case "TABLE":
			filename = schemaName + File.separatorChar + objectTypePath + File.separatorChar + objectName + "." + suffix;

			break;

		default:
			filename = schemaName + File.separatorChar + objectTypePath + File.separatorChar + objectName + "." + suffix;
			break;
		}

		return filename.toLowerCase();
	}

	private String computeWhereClause(List<String> includeFilters, List<String> excludeFilters, List<String> schemas,
			List<String> includeTypes, List<String> excludeTypes) {
		String whereClause = "";

		// SCHEMAS
		StringBuilder schemaBuilder = new StringBuilder();
		for (String schema : schemas) {
			if (schemaBuilder.length() != 0) {
				schemaBuilder.append(",");
			}
			schemaBuilder.append("'" + schema + "'");
		}

		// INCLUDE
		StringBuilder includeBuilder = new StringBuilder();
		for (String flt : includeFilters) {
			if (includeBuilder.length() != 0)
				includeBuilder.append(" or ");
			if (schemas.size() == 1) {
				includeBuilder.append("object_name like '" + flt + "'");
			} else {
				includeBuilder.append("owner||'.'||object_name like '" + flt + "'");
			}

		}
		// INCLUDE TYPES
		StringBuilder includeTypesBuilder = new StringBuilder();
		for (String type : includeTypes) {
			if (includeTypesBuilder.length() != 0) {
				includeTypesBuilder.append(",");
			}
			includeTypesBuilder.append("'" + type + "'");
		}

		// EXCLUDE
		StringBuilder excludeBuilder = new StringBuilder();
		for (String flt : excludeFilters) {
			if (excludeBuilder.length() != 0) {
				excludeBuilder.append(" and ");
			}
			if (schemas.size() == 1) {
				excludeBuilder.append("object_name not like '" + flt + "'");
			} else {
				excludeBuilder.append("owner||'.'||object_name not like '" + flt + "'");
			}
		}

		// EXCLUDE TYPES
		StringBuilder excludeTypesBuilder = new StringBuilder();
		for (String type : excludeTypes) {
			if (excludeTypesBuilder.length() != 0) {
				excludeTypesBuilder.append(",");
			}
			excludeTypesBuilder.append("'" + type + "'");
		}

		whereClause = "owner in /* schemas */ (" + schemaBuilder.toString() + ")";
		if (includeBuilder.length() > 0)
			whereClause += " and /* include_filter */ (" + includeBuilder.toString() + ")";
		if (excludeBuilder.length() > 0)
			whereClause += " and /* exclude_filter */ (" + excludeBuilder.toString() + ")";
		if (includeTypesBuilder.length() > 0)
			whereClause += " and object_type in /* include_types */ (" + includeTypesBuilder.toString() + ")";
		if (excludeTypesBuilder.length() > 0)
			whereClause += " and object_type not in /* exclude_types */ (" + excludeTypesBuilder.toString() + ")";

		return whereClause;
	}

	private String mapObjectTypeToMetadata(String objectType) {
		String mapObjectType = objectType;

		mapObjectType = mapObjectType.replace(" ", "_");

		return mapObjectType;
	}

	public static String padRight(String s, int n) {
		return String.format("%-" + n + "s", s);
	}

	public static String padLeft(String s, int n) {
		return String.format("%" + n + "s", s);
	}

	/**
	 * 
	 * @param jdbcURL
	 * @throws Exception
	 */
	public void export(File customSQLInitFile, File customExportSQLFile, List<String> includeFilters,
			List<String> excludeFilters, List<String> schemas, List<String> includeTypes, List<String> excludeTypes)
			throws Exception {
		SQLclUtil sqlclUtil = new SQLclUtil();
		String schemaName = "";
		String objectType = "";
		String objectName = "";

		log.debug("start");
		try {
			// initialize connection
			sqlcl = getScriptExecutor(user, pwd, connectStr);
			if (customSQLInitFile != null) {
				Msg.println("*** run custom export script: " + customExportSQLFile + "\n");

				sqlclUtil.executeFile(customSQLInitFile, sqlcl, null);
			}

			Statement objectStmt = sqlcl.getConn().createStatement();
			// The query can be update query or can be select query
			String whereClause = computeWhereClause(includeFilters, excludeFilters, schemas, includeTypes,
					excludeTypes);
			String objectQuery = "select owner, object_name, object_type \n" + "  from all_objects \n" + " where "
					+ whereClause + "\n" + " order by 1,2,3";
			log.debug("execute query: " + objectQuery);
			Msg.println("*** The following objects will be exported:\n\n" + objectQuery);
			Msg.println("");

			if (!this.isSilent)
				Utils.waitForEnter("\n*** Please press <enter> to start the process ");

			boolean status = objectStmt.execute(objectQuery);
			if (status) {
				// query is a select query.
				ResultSet objectRS = objectStmt.getResultSet();
				while (objectRS.next()) {
					schemaName = objectRS.getString(1);
					objectName = objectRS.getString(2);
					objectType = objectRS.getString(3);

					// Msg.print(" export: " + objectName + "[" + objectType + "]: ");
					Msg.print(padRight("  export: " + objectName + "[" + objectType + "]", 47) + "=> ");
					try {
						exportObject(schemaName, objectName, objectType);
					} catch (SQLException e) {
						// log.error("sql error: "+e.getErrorCode());
						log.error("sql error: " + e.getLocalizedMessage());
						// log.error("sql error: "+e.getMessage());

						// re-raise error if errors should abort program
						if (this.skipErrors == false)
							throw (e);
					}

				}
				objectRS.close();

				// run custom export file at the end
				if (customExportSQLFile != null) {
					Msg.println("\n*** run custom export script: " + customExportSQLFile + "\n");
					
					sqlclUtil.executeFile(customExportSQLFile, sqlcl, null);
				}

			} else {
				// query can be update or any query apart from select query
				int count = objectStmt.getUpdateCount();
				System.out.println("Total records updated: " + count);
			}

		} catch (Exception e) {
			log.error(e.getMessage());
			// close connection
			closeConnection();

			// reraise exception
			throw (e);
		} finally {
			// close connection
			closeConnection();
		}
		log.debug("end");
	}

	/**
	 * Export a single object.
	 * 
	 * @param schemaName
	 * @param objectName
	 * @param objectType
	 * @throws SQLException
	 * @throws IOException
	 */
	private void exportObject(String schemaName, String objectName, String objectType)
			throws SQLException, IOException {
		String content = "";
		Statement ddlStmt = null;
		ResultSet ddlRS = null;

		// get object ddl
		try {
			ddlStmt = sqlcl.getConn().createStatement();
			// The query can be update query or can be select query
			String ddlQuery = "SELECT dbms_metadata.get_ddl(object_type=>'" + mapObjectTypeToMetadata(objectType)
					+ "', name=>'" + objectName + "', schema=>'" + schemaName + "') ddl from dual";
			boolean ddlQueryStatus = ddlStmt.execute(ddlQuery);
			if (ddlQueryStatus) {
				// query is a select query.
				ddlRS = ddlStmt.getResultSet();
				while (ddlRS.next()) {
					content = DBUtils.clobToString(ddlRS.getClob(1));
					log.debug(content);
				}
				ddlRS.close();
			} else {
				// query can be update or any query apart from select query
				int count = ddlStmt.getUpdateCount();
				log.debug("Total records updated: " + count);
			}
		} finally {
			if (ddlRS != null)
				ddlRS.close();
			if (ddlStmt != null)
				ddlStmt.close();
		}

		// export dependent objects
		if (this.dependentObjectsMap.containsKey(objectType)) {
			for (String depObjectType : this.dependentObjectsMap.get(objectType)) {
				// get dependent DDL
				try {
					ddlStmt = sqlcl.getConn().createStatement();
					// The query can be update query or can be select query
					/*
					 * DBMS_METADATA.GET_DEPENDENT_DDL ( object_type IN VARCHAR2, base_object_name
					 * IN VARCHAR2, base_object_schema IN VARCHAR2 DEFAULT NULL, version IN VARCHAR2
					 * DEFAULT 'COMPATIBLE', model IN VARCHAR2 DEFAULT 'ORACLE', transform IN
					 * VARCHAR2 DEFAULT 'DDL', object_count IN NUMBER DEFAULT 10000) RETURN CLOB;
					 */
					// Msg.println(" - " + depObjectType);
					String ddlQuery = "SELECT dbms_metadata.get_dependent_ddl(object_type=>'" + depObjectType
							+ "', base_object_name =>'" + objectName + "', base_object_schema=>'" + schemaName
							+ "') ddl from dual";
					boolean ddlQueryStatus = ddlStmt.execute(ddlQuery);
					if (ddlQueryStatus) {
						// query is a select query.
						ddlRS = ddlStmt.getResultSet();
						while (ddlRS.next()) {
							content += "\n\n" + DBUtils.clobToString(ddlRS.getClob(1));
							log.debug(content);

						}
						ddlRS.close();
					} else {
						// query can be update or any query apart from select query
						int count = ddlStmt.getUpdateCount();
						log.debug("Total records updated: " + count);
					}
				} catch (SQLException e) {
					if (e.getErrorCode() == 31608) {
						// ORA-31608: specified object of type <TYPE> not found
						// do nothing, it just indicates that no data was found for the subobject
					} else {
						throw (e);
					}
				} finally {
					if (ddlRS != null)
						ddlRS.close();
					if (ddlStmt != null)
						ddlStmt.close();
				}
			}
		}

		// write file
		String relativeFilename = computeExportFilename(schemaName, objectType, objectName);
		String filename = this.outputDir + File.separatorChar + relativeFilename;

		// output file in console
		Msg.println(relativeFilename);
		FileUtils.writeStringToFile(new File(filename), content, Charset.defaultCharset());

	}

	/**
	 * Retrieves or creates the corresponding ScriptExecutor based on the filename
	 * 
	 * @param filename
	 * @return
	 * @throws SQLException
	 */
	private ScriptExecutor getScriptExecutor(String user, String pwd, String connectStr) throws SQLException {
		Connection conn;
		ScriptExecutor sqlcl;
		ScriptRunnerContext ctx;

		conn = openConnection(user, pwd, connectStr);

		// then create ScriptRunner Context
		// create sqlcl
		sqlcl = new ScriptExecutor(conn);
		// set up context
		ctx = new ScriptRunnerContext();
		// set the output max rows
		ResultSetFormatter.setMaxRows(10000);
		// set the context
		sqlcl.setScriptRunnerContext(ctx);
		ctx.setBaseConnection(conn);

		return sqlcl;
	}

	public Connection openConnection(String user, String pwd, String connectStr) {
		Connection conn = null;

		OracleConnectionPoolDataSource ocpds;
		PooledConnection pc;

		try {

			// set cache properties
			java.util.Properties prop = new java.util.Properties();
			prop.setProperty("InitialLimit", "1");
			prop.setProperty("MinLimit", "1");
			prop.setProperty("MaxLimit", "1");

			ocpds = new OracleConnectionPoolDataSource();

			ocpds.setURL("jdbc:oracle:thin:@" + connectStr);
			ocpds.setUser(user);
			ocpds.setPassword(pwd);

			// set connection parameters
			ocpds.setConnectionProperties(prop);

			pc = ocpds.getPooledConnection();
			conn = pc.getConnection();
			conn.setAutoCommit(false);

		} catch (SQLException e) {
			Utils.throwRuntimeException("Could not connect via JDBC: " + e.getMessage());
		}

		return conn;
	}

	private void closeConnection() {
		DBUtils.closeQuietly(this.sqlcl.getConn());
	}
}
