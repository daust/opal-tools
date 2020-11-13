package de.opal.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.PooledConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.db.ConnectionUtility;
import de.opal.db.SQLclUtil;
import de.opal.installer.db.DBUtils;
import de.opal.installer.util.Msg;
import de.opal.installer.util.Utils;
import de.opal.utils.FileIO;
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
	//private HashMap<String, String> extensionMappingsMap;
	//private HashMap<String, String> directoryMappingsMap;
	//private String filenameTemplate;
	private boolean filenameReplaceBlanks;
	private String workingDirectorySQLcl;
	private boolean skipExport;
	private HashMap<String, String> filenameTemplatesMap;
	private String exportTemplateDir;
	


	/**
	 * 
	 * @param user
	 * @param pwd
	 * @param connectStr
	 */
	public Exporter(String user, String pwd, String connectStr, String outputDir, boolean skipErrors,
			HashMap<String, ArrayList<String>> dependentObjectsMap, boolean isSilent,
			/*HashMap<String, String> extensionMappingsMap, HashMap<String, String> directoryMappingsMap,
			String filenameTemplate,*/ boolean filenameReplaceBlanks, String workingDirectorySQLcl, boolean skipExport,
			HashMap<String, String> filenameTemplatesMap, String exportTemplateDir) {
		super();
		this.user = user;
		this.pwd = pwd;
		this.connectStr = connectStr;
		this.outputDir = outputDir;
		this.skipErrors = skipErrors;
		this.dependentObjectsMap = dependentObjectsMap;
		this.isSilent = isSilent;
		//this.extensionMappingsMap = extensionMappingsMap;
		//this.directoryMappingsMap = directoryMappingsMap;
		//this.filenameTemplate = filenameTemplate;
		this.filenameReplaceBlanks = filenameReplaceBlanks;
		this.workingDirectorySQLcl = workingDirectorySQLcl;
		this.skipExport = skipExport;
		this.filenameTemplatesMap=filenameTemplatesMap;
		this.exportTemplateDir=exportTemplateDir;

	}

	private String computeExportFilename(String schemaName, String objectType, String objectName) {
		String filename="";
		//String suffix = "sql";
		String objectTypePath = objectType;
		String objectTypePlural = objectTypePath;
		//boolean objectTypePathChanged = false;

		// make plural form
		if (objectTypePlural.endsWith("Y"))
			objectTypePlural = StringUtils.chop(objectTypePlural) + "ie"; // remove last character
		if (objectTypePlural.endsWith("X"))
			objectTypePlural = objectTypePlural + "e"; // remove last character

		objectTypePlural += "s";

		// get filename template
		if (this.filenameTemplatesMap.containsKey(objectType))
			filename = this.filenameTemplatesMap.get(objectType);
		else if (this.filenameTemplatesMap.containsKey("DEFAULT"))
			filename = this.filenameTemplatesMap.get("DEFAULT");
		
		/*
		// map object types to suffixes
		if (this.extensionMappingsMap.containsKey(objectType))
			suffix = this.extensionMappingsMap.get(objectType);
		else if (this.extensionMappingsMap.containsKey("DEFAULT"))
			suffix = this.extensionMappingsMap.get("DEFAULT");

		if (this.directoryMappingsMap.containsKey(objectType)) {
			objectTypePath = this.directoryMappingsMap.get(objectType);
			// override both variables later
			objectTypePathChanged = true;
		}
		*/

		/*
		 * schema - schema name in lower case type - lower case type name: 'table'
		 * types_plural - lower case type name in plural: 'tables' object_name - lower
		 * case object name ext - lower case extension: 'sql' or 'pks' SCHEMA - upper
		 * case schema name TYPE - upper case object type name: 'TABLE' or 'INDEX'
		 * TYPES_PLURAL - upper case object type name in plural: 'TABLES' OBJECT_NAME -
		 * upper case object name EXT - upper case extension: 'SQL' or 'PKS'
		 */

		// use filename template to generate filename
		filename = filename.replace("#schema#", schemaName.toLowerCase());
		filename = filename.replace("#SCHEMA#", schemaName.toUpperCase());
		filename = filename.replace("#object_name#", objectName.toLowerCase());
		filename = filename.replace("#OBJECT_NAME#", objectName.toUpperCase());

		// when it is overriden on the command line ...
		// it will affect it completely .. AS IS
		//if (objectTypePathChanged) {
		//	filename = filename.replace("#object_type_plural#", objectTypePath.toLowerCase());
		//	filename = filename.replace("#OBJECT_TYPE_PLURAL#", objectTypePath.toUpperCase());
		//	filename = filename.replace("#object_type#", objectTypePath.toLowerCase());
		//	filename = filename.replace("#OBJECT_TYPE#", objectTypePath.toUpperCase());
		//} else {
			filename = filename.replace("#object_type_plural#", objectTypePlural.toLowerCase());
			filename = filename.replace("#OBJECT_TYPE_PLURAL#", objectTypePlural.toUpperCase());
			filename = filename.replace("#object_type#", objectType.toLowerCase());
			filename = filename.replace("#OBJECT_TYPE#", objectType.toUpperCase());
		//}

		//filename = filename.replace("#ext#", suffix.toLowerCase());
		//filename = filename.replace("#EXT#", suffix.toUpperCase());

		filename = filename.replace("/", "" + File.separatorChar);

		// filename = schemaName + File.separatorChar + objectTypePath +
		// File.separatorChar + objectName + "." + suffix;
		if (this.filenameReplaceBlanks)
			filename = filename.replace(" ", "_");

		return filename;
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

		// always exclude all generated objects
		whereClause += "\n   and /* exclude generated objects */ generated='N'";

		// exclude nested tables
		whereClause += "\n   and /* exclude nested tables */ (owner,object_name,object_type) not in (select owner, table_name, 'TABLE' from all_nested_tables )";

		return whereClause;
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
	public void export(List<File> preScripts, List<File> postScripts, List<String> includeFilters,
			List<String> excludeFilters, List<String> schemas, List<String> includeTypes, List<String> excludeTypes)
			throws Exception {
		String schemaName = "";
		String objectType = "";
		String objectName = "";

		ArrayList<String> errorList = new ArrayList<String>();
		int totalObjectCnt = 0;
		long startTime = System.currentTimeMillis();

		log.debug("start");
		try {
			// initialize connection
			sqlcl = getScriptExecutor(user, pwd, connectStr);

			// run pre-script(s)
			if (preScripts.size() > 0) {
				for (File preScript : preScripts) {
					Msg.println("*** run pre script: " + preScript + "\n");
					if (this.workingDirectorySQLcl != null)
						SQLclUtil.setWorkingDirectory(this.workingDirectorySQLcl, sqlcl);
					// sqlcl.setDirectory(this.workingDirectorySQLcl);

					SQLclUtil.executeFile(preScript, sqlcl, null);
				}
			}

			if (!this.skipExport) {
				/*------------------------------------------------------------
				 * run object export
				 */
				Statement objectStmt = sqlcl.getConn().createStatement();
				// The query can be update query or can be select query
				String whereClause = computeWhereClause(includeFilters, excludeFilters, schemas, includeTypes,
						excludeTypes);
				String objectQuery = "select owner, object_name, object_type\n"
						+ "    from (select owner, object_name, object_type, generated \n"
						+ "            from all_objects \n" + "          union all \n"
						+ "          select owner, constraint_name as object_name, 'REF_CONSTRAINT' as object_type, 'N' as generated\n"
						+ "            from all_constraints \n" + "           where constraint_type = 'R') \n"
						+ " where " + whereClause + "\n" + " order by 1,2,3";
				log.debug("execute query: " + objectQuery);
				Msg.println("*** The following objects will be exported:\n\n" + objectQuery);
				Msg.println("");

				if (!this.isSilent)
					Utils.waitForEnter("*** Please press <enter> to start the process ");

				boolean status = objectStmt.execute(objectQuery);
				if (status) {
					// query is a select query.
					ResultSet objectRS = objectStmt.getResultSet();
					while (objectRS.next()) {
						totalObjectCnt++;
						schemaName = objectRS.getString(1);
						objectName = objectRS.getString(2);
						objectType = objectRS.getString(3);

						Msg.print(padRight("  export: " + objectName + "[" + objectType + "]", 47) + "=> ");
						try {
							exportObject(schemaName, objectName, objectType);
						} catch (SQLException e) {
							log.error("sql error: " + e.getLocalizedMessage());
							errorList.add(objectName + "[" + objectType + "]");

							// re-raise error if errors should abort program
							if (this.skipErrors == false)
								throw (e);
						}

					}
					objectStmt.close();
					objectRS.close();
				}
			} else {
				/*------------------------------------------------------------
				 *  skip object export
				 */

				if (!this.isSilent)
					Utils.waitForEnter("\n*** Please press <enter> to start the process ");
			}

			// run post-script(s)
			if (postScripts.size() > 0) {
				for (File postScript : postScripts) {
					Msg.println("\n*** run post script: " + postScript + "\n");
					if (this.workingDirectorySQLcl != null)
						SQLclUtil.setWorkingDirectory(this.workingDirectorySQLcl, sqlcl);
					// sqlcl.setDirectory(this.workingDirectorySQLcl);

					SQLclUtil.executeFile(postScript, sqlcl, null);
				}
			}
			// sqlclUtil.setWorkingDirectory(this.workingDirectorySQLcl, sqlcl);
			// log.debug("\ncurrent working directory (after change): "
			// +
			// oracle.dbtools.common.utils.FileUtils.getCWD(sqlcl.getScriptRunnerContext()));

			displayStatsFooter(errorList, totalObjectCnt, startTime);
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

	private void displayStatsFooter(ArrayList<String> errorList, int totalObjectCnt, long startTime) {
		long finish = System.currentTimeMillis();
		long timeElapsed = finish - startTime;
		int minutes = (int) (timeElapsed / (60 * 1000));
		int seconds = (int) ((timeElapsed / 1000) % 60);
		String timeElapsedString = String.format("%d:%02d", minutes, seconds);

		if (!this.skipExport) {
			Msg.println("\n*** The export finished in " + timeElapsedString + " [mm:ss] and exported "
					+ (totalObjectCnt - errorList.size()) + "/" + totalObjectCnt + " objects successfully.");

			if (errorList.size() > 0) {
				Msg.println("");
				Msg.println("*** The following objects could not be exported due to errors");
				for (String error : errorList) {
					Msg.println("  " + error);
				}
			}
		} else {
			Msg.println("\n*** The script finished in " + timeElapsedString + " [mm:ss].");
		}
	}

	
	private String getTemplateQuery(String objectType) throws IOException {
		String query="";
		
		if (this.exportTemplateDir!=null) {
			File templateFile=new File(this.exportTemplateDir+File.separator+objectType+".sql");
			if (templateFile.exists()) {
				query=FileIO.fileToString(templateFile.getAbsolutePath());
			}
		}
		
		
		return query;
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
		PreparedStatement ddlStmt = null;
		PreparedStatement ddlStmtDependent = null;
		ResultSet ddlRS = null;
		String actualObjectType = ObjectTypeMappingMetadata.map2TypeForDBMS(objectType);
		CallableStatement ddlStmtCallable = null;

		// template query exists?
		String templateQuery = getTemplateQuery(actualObjectType);
		if (!templateQuery.isEmpty()) {
     		// get ddl based on template query
			try {
				String ddlQuery = templateQuery;
				ddlStmtCallable = sqlcl.getConn().prepareCall(ddlQuery);
				ddlStmtCallable.setString(1, schemaName);
				ddlStmtCallable.setString(2, actualObjectType);
				ddlStmtCallable.setString(3, objectName);
				ddlStmtCallable.registerOutParameter(4, Types.CLOB);
					
				ddlStmtCallable.execute();
				Clob cl = ddlStmtCallable.getClob(4);
				content = DBUtils.clobToString(cl);
				log.debug(content);
			} finally {
				if (ddlRS != null)
					ddlRS.close();
				if (ddlStmt != null)
					ddlStmt.close();
				if (ddlStmtCallable != null)
					ddlStmtCallable.close();
			}
		}else {
			// get regular object ddl
			try {
				String ddlQuery = "SELECT dbms_metadata.get_ddl(schema=>?, object_type=>?, name=>? ) ddl from dual";
				ddlStmt = sqlcl.getConn().prepareStatement(ddlQuery);
				ddlStmt.setString(1, schemaName);
				ddlStmt.setString(2, actualObjectType);
				ddlStmt.setString(3, objectName);
				
					

				boolean ddlQueryStatus = ddlStmt.execute();
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
		}

		

		// export dependent objects
		if (this.dependentObjectsMap.containsKey(objectType)) {
			for (String depObjectType : this.dependentObjectsMap.get(objectType)) {
				actualObjectType = ObjectTypeMappingMetadata.map2TypeForDBMS(depObjectType);
				log.debug("object type mapping: " + depObjectType + " => " + actualObjectType);

				// get dependent DDL
				try {
					// The query can be update query or can be select query
					/*
					 * DBMS_METADATA.GET_DEPENDENT_DDL ( object_type IN VARCHAR2, base_object_name
					 * IN VARCHAR2, base_object_schema IN VARCHAR2 DEFAULT NULL, version IN VARCHAR2
					 * DEFAULT 'COMPATIBLE', model IN VARCHAR2 DEFAULT 'ORACLE', transform IN
					 * VARCHAR2 DEFAULT 'DDL', object_count IN NUMBER DEFAULT 10000) RETURN CLOB;
					 */
					// Msg.println(" - " + depObjectType);
//					String ddlQuery = "SELECT dbms_metadata.get_dependent_ddl(object_type=>'" + actualObjectType
//							+ "', base_object_name =>'" + objectName + "', base_object_schema=>'" + schemaName
//							+ "') ddl from dual";
					String ddlQuery = "SELECT dbms_metadata.get_dependent_ddl(object_type=>?, base_object_name =>?, base_object_schema=>?) ddl from dual";
					ddlStmtDependent = sqlcl.getConn().prepareStatement(ddlQuery);
					ddlStmtDependent.setString(1, actualObjectType);
					ddlStmtDependent.setString(2, objectName);
					ddlStmtDependent.setString(3, schemaName);

					boolean ddlQueryStatus = ddlStmtDependent.execute();
					if (ddlQueryStatus) {
						// query is a select query.
						ddlRS = ddlStmtDependent.getResultSet();
						while (ddlRS.next()) {
							content += "\n\n" + DBUtils.clobToString(ddlRS.getClob(1));
							log.debug(content);

						}
						ddlRS.close();
					} else {
						// query can be update or any query apart from select query
						int count = ddlStmtDependent.getUpdateCount();
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
					if (ddlStmtDependent != null)
						ddlStmtDependent.close();
					if (ddlStmt != null)
						ddlStmt.close();
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
		org.apache.commons.io.FileUtils.writeStringToFile(new File(filename), content, Charset.defaultCharset());

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

			ocpds.setURL(ConnectionUtility.transformJDBCConnectString(connectStr));
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