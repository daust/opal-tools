package de.opal.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.db.SQLclUtil;
import de.opal.installer.db.DBUtils;
import de.opal.installer.util.Msg;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;

public class ExporterCallable implements Callable<Integer> {
	private static final Logger log = LogManager.getLogger(ExporterCallable.class.getName());

	private String schemaName = null;
	private String objectType = null;
	private String objectName = null;
	private String templateQuery = null;
	private ScriptExecutor sqlcl = null;
	private HashMap<String, ArrayList<String>> dependentObjectsMap;
	private String exportFilename;
	private String relativeFilename;
	private ArrayList<String> errorList;
	private List<File> preScripts;
	private String user;
	private String pwd;
	private String connectStr;
	private String workingDirectorySQLcl;
	private int parallelThreads;

	public ExporterCallable(String schemaName, String objectName, String objectType, String templateQuery,
			ScriptExecutor sqlcl, HashMap<String, ArrayList<String>> dependentObjectsMap, String relativeFilename,
			String exportFilename, ArrayList<String> errorList, List<File> preScripts, String user, String pwd,
			String connectStr, String workingDirectorySQLcl, int parallelThreads) {
		this.schemaName = schemaName;
		this.objectType = objectType;
		this.objectName = objectName;
		this.templateQuery = templateQuery;
		this.sqlcl = sqlcl;
		this.dependentObjectsMap = dependentObjectsMap;
		this.exportFilename = exportFilename;
		this.errorList = errorList;
		this.preScripts = preScripts;
		this.user = user;
		this.pwd = pwd;
		this.connectStr = connectStr;
		this.workingDirectorySQLcl = workingDirectorySQLcl;
		this.parallelThreads = parallelThreads;
		this.relativeFilename = relativeFilename;
	}

	@Override
	public Integer call() throws SQLException, IOException {
		Integer retVal = 0; // 0=success
		ScriptExecutor sqlcl = this.sqlcl;

		try {
			if (this.parallelThreads > 1) {
				// get new database connection and execute the prescripts
				sqlcl = SQLclUtil.getScriptExecutor(user, pwd, connectStr);
				SQLclUtil.executeScripts(sqlcl, preScripts, workingDirectorySQLcl, false);
			}

			String content = "";
			PreparedStatement ddlStmt = null;
			PreparedStatement ddlStmtDependent = null;
			ResultSet ddlRS = null;
			String actualObjectType = ObjectTypeMappingMetadata.map2TypeForDBMS(objectType);
			CallableStatement ddlStmtCallable = null;
			String errorMessageString = schemaName + "." + objectName + "[" + objectType + "]";

			// template query exists?
			// String templateQuery = getTemplateQuery(actualObjectType);

			if (!templateQuery.isEmpty()) {
				// get ddl based on template query
				try {
					String ddlQuery = templateQuery;
					ddlStmtCallable = sqlcl.getConn().prepareCall(ddlQuery);
					ddlStmtCallable.setString("schema_name", schemaName);
					ddlStmtCallable.setString("object_type", actualObjectType);
					ddlStmtCallable.setString("object_name", objectName);
					ddlStmtCallable.registerOutParameter("retval", Types.CLOB);

					ddlStmtCallable.execute();
					Clob cl = ddlStmtCallable.getClob("retval");
					content = DBUtils.clobToString(cl);
					log.debug(content);
				} catch (Exception e) {
					errorList.add(errorMessageString);
					throw (e);
				} finally {
					if (ddlRS != null)
						ddlRS.close();
					if (ddlStmt != null)
						ddlStmt.close();
					if (ddlStmtCallable != null)
						ddlStmtCallable.close();
				}
			} else {
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
					}
				} catch (Exception e) {
					errorList.add(errorMessageString);
					throw (e);
				} finally {
					if (ddlRS != null)
						ddlRS.close();
					if (ddlStmt != null)
						ddlStmt.close();
				}
			}

			// export dependent objects
			if (this.dependentObjectsMap.containsKey(objectType) || this.dependentObjectsMap.containsKey("DEFAULT")) {

				// merge object dependencies with DEFAULT

				Set<String> set = new LinkedHashSet<>();
				if (this.dependentObjectsMap.get(objectType) != null)
					set.addAll(this.dependentObjectsMap.get(objectType));
				if (this.dependentObjectsMap.get("DEFAULT") != null)
					set.addAll(this.dependentObjectsMap.get("DEFAULT"));
				List<String> mergedList = new ArrayList<>(set);

				for (String depObjectType : mergedList) {
					actualObjectType = ObjectTypeMappingMetadata.map2TypeForDBMS(depObjectType);
					log.debug("object type mapping: " + depObjectType + " => " + actualObjectType);

					// get dependent DDL
					try {
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
						}
					} catch (SQLException e) {
						if (e.getErrorCode() == 31608) {
							// ORA-31608: specified object of type <TYPE> not found
							// do nothing, it just indicates that no data was found for the subobject
						} else {
							errorList.add(errorMessageString);

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

			// output file in console
			Msg.println(
					StringUtils.rightPad("  " + objectName + "[" + objectType + "]", 50) + "=> " + relativeFilename);
			org.apache.commons.io.FileUtils.writeStringToFile(new File(exportFilename), content,
					Charset.defaultCharset());

		} finally {
			if (this.parallelThreads > 1)
				SQLclUtil.closeConnection(sqlcl.getConn());
		}
		return retVal;
	}
}
