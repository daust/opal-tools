package de.opal.exporter;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.db.ConnectionUtility;
import de.opal.db.SQLclUtil;
import de.opal.installer.util.Msg;
import de.opal.installer.util.Utils;
import de.opal.utils.FileIO;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;

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
	// private HashMap<String, String> extensionMappingsMap;
	// private HashMap<String, String> directoryMappingsMap;
	// private String filenameTemplate;
	private boolean filenameReplaceBlanks;
	private String workingDirectorySQLcl;
	private boolean skipExport;
	private HashMap<String, String> filenameTemplatesMap;
	private String exportTemplateDir;

	private ConnectionUtility connPool;
	private int parallelThreads;

	/**
	 * 
	 * @param user
	 * @param pwd
	 * @param connectStr
	 */
	public Exporter(String user, String pwd, String connectStr, String outputDir, boolean skipErrors,
			HashMap<String, ArrayList<String>> dependentObjectsMap, boolean isSilent,
			/*
			 * HashMap<String, String> extensionMappingsMap, HashMap<String, String>
			 * directoryMappingsMap, String filenameTemplate,
			 */ boolean filenameReplaceBlanks, String workingDirectorySQLcl, boolean skipExport,
			HashMap<String, String> filenameTemplatesMap, String exportTemplateDir, int parallelThreads) {
		super();
		this.user = user;
		this.pwd = pwd;
		this.connectStr = connectStr;
		this.outputDir = outputDir;
		this.skipErrors = skipErrors;
		this.dependentObjectsMap = dependentObjectsMap;
		this.isSilent = isSilent;
		// this.extensionMappingsMap = extensionMappingsMap;
		// this.directoryMappingsMap = directoryMappingsMap;
		// this.filenameTemplate = filenameTemplate;
		this.filenameReplaceBlanks = filenameReplaceBlanks;
		this.workingDirectorySQLcl = workingDirectorySQLcl;
		this.skipExport = skipExport;
		this.filenameTemplatesMap = filenameTemplatesMap;
		this.exportTemplateDir = exportTemplateDir;
		this.parallelThreads = parallelThreads;

	}

	private String computeExportFilename(String schemaName, String objectType, String objectName) {
		String filename = "";
		// String suffix = "sql";
		String objectTypePath = objectType;
		String objectTypePlural = objectTypePath;
		// boolean objectTypePathChanged = false;

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

		filename = filename.replace("#object_type_plural#", objectTypePlural.toLowerCase());
		filename = filename.replace("#OBJECT_TYPE_PLURAL#", objectTypePlural.toUpperCase());
		filename = filename.replace("#object_type#", objectType.toLowerCase());
		filename = filename.replace("#OBJECT_TYPE#", objectType.toUpperCase());

		filename = filename.replace("/", "" + File.separatorChar);

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
			// first initialize the connection pool (we need parallelThreads + 1 for the
			// main query)
			this.connPool = ConnectionUtility.getInstance();
			this.connPool.initializeConnPool(this.parallelThreads + 1, this.user, this.pwd, this.connectStr);

			// initialize connection
			sqlcl = SQLclUtil.getScriptExecutor(user, pwd, connectStr);

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

				startTime = System.currentTimeMillis();

				boolean status = objectStmt.execute(objectQuery);
				if (status) {

					Msg.println("*** run pre scripts\n");
					SQLclUtil.executeScripts(sqlcl, preScripts, workingDirectorySQLcl, true);

					// run in parallel
					ExecutorService executors = Executors.newFixedThreadPool(this.parallelThreads);
					// thread list
					List<Future<Integer>> futures = new ArrayList<Future<Integer>>();

					// query is a select query.
					ResultSet objectRS = objectStmt.getResultSet();
					while (objectRS.next()) {
						totalObjectCnt++;
						schemaName = objectRS.getString(1);
						objectName = objectRS.getString(2);
						objectType = objectRS.getString(3);

						String actualObjectType = ObjectTypeMappingMetadata.map2TypeForDBMS(objectType);
						String relativeFilename = computeExportFilename(schemaName, objectType, objectName);
						String exportFilename = this.outputDir + File.separatorChar + relativeFilename;

						String templateQuery = getTemplateQuery(actualObjectType);
						Callable<Integer> w = new ExporterCallable(schemaName, objectName, objectType, templateQuery,
								sqlcl, dependentObjectsMap, relativeFilename, exportFilename, errorList, preScripts,
								user, pwd, connectStr, workingDirectorySQLcl, parallelThreads);

						Future<Integer> future = executors.submit(w);
						futures.add(future);

					}
					// now check parallel execution
					try {

						for (int i = 0; i < futures.size(); i++) {
							try {
								// System.out.println("Result from Future " + i + ":" + futures.get(i).get());
								futures.get(i).get();
							} catch (InterruptedException e) {

								log.error(e.getLocalizedMessage());
							} catch (ExecutionException e) {
								if (e.getCause() instanceof SQLException) {

									// re-raise error if errors should abort program
									if (this.skipErrors == false)
										throw (e);
									else
										log.error("sql error: " + e.getLocalizedMessage());
								} else
									log.error(e.getLocalizedMessage());
							}
						}
					} finally {
						executors.shutdown();
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

				startTime = System.currentTimeMillis();
			}

			Msg.println("*** run post scripts\n");
			SQLclUtil.executeScripts(sqlcl, preScripts, workingDirectorySQLcl, true);

			displayStatsFooter(errorList, totalObjectCnt, startTime);
		} catch (Exception e) {
			//log.error(e.getMessage());
			// close connection
			SQLclUtil.closeConnection(sqlcl.getConn());

			// reraise exception
			throw (e);
		} finally {
			// close connection
			SQLclUtil.closeConnection(sqlcl.getConn());
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
		String query = "";

		if (this.exportTemplateDir != null) {
			File templateFile = new File(this.exportTemplateDir + File.separator + objectType + ".sql");
			if (templateFile.exists()) {
				query = FileIO.fileToString(templateFile.getAbsolutePath());
			}
		}

		return query;
	}
}