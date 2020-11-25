package de.opal.installer;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.installer.config.PatchDependency;
import de.opal.installer.config.RegistryTarget;
import de.opal.installer.util.Msg;
import liquibase.util.StringUtils;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;
import oracle.jdbc.OracleResultSet;

public class PatchRegistry {
	public static final Logger logger = LogManager.getLogger(PatchRegistry.class.getName());

	private ArrayList<RegistryTarget> registryTargets;
	private Installer installer;

	/**
	 * @param registryTargets
	 */
	public PatchRegistry(ArrayList<RegistryTarget> registryTargets, Installer installer) {
		this.registryTargets = registryTargets;
		this.installer = installer;

		initializePatchTables();
	}

	private void initializePatchTables() {
		String cmdPatchTable = "CREATE TABLE #PREFIX#_installer_patches (\n" + "    pat_id                  NUMBER\n"
				+ "        NOT NULL ENABLE,\n" + "    pat_application         VARCHAR2(100 CHAR),\n"
				+ "    pat_name                VARCHAR2(100 CHAR),\n"
				+ "    pat_version             VARCHAR2(100 CHAR),\n"
				+ "    pat_author              VARCHAR2(100 CHAR),\n"
				+ "    pat_target_system       varchar2(50 char),\n" + "    pat_started_on          DATE,\n"
				+ "    pat_ended_on            DATE,\n" + "    pat_description         VARCHAR2(4000 CHAR),\n"
				+ "    pat_config_filename     VARCHAR2(4000 CHAR),\n"
				+ "    pat_conn_pool_filename  VARCHAR2(4000 CHAR),\n"
				+ "    CONSTRAINT #PREFIX#_installer_patches_pk PRIMARY KEY ( pat_id )\n" + ");";

		String cmdPatchDetails = "create table #PREFIX#_installer_details(\n"
				+ "    det_id                  NUMBER  NOT NULL ENABLE,\n"
				+ "    det_filename            VARCHAR2(4000 CHAR),\n" + "    det_installed_on        DATE,\n"
				+ "    det_pat_id              number,\n"
				+ "    CONSTRAINT #PREFIX#_installer_details_pk PRIMARY KEY ( det_id ),\n"
				+ "    CONSTRAINT #PREFIX#_INSTALLER_DETAILS_FK1 FOREIGN KEY (DET_PAT_ID)\n"
				+ "	  REFERENCES #PREFIX#_INSTALLER_PATCHES (PAT_ID) ENABLE\n" + ");\n" + "";

		if (registryTargets != null) {
			for (RegistryTarget registryTarget : this.registryTargets) {

				// get data source name
				String dsName = registryTarget.connectionPoolName;
				try {
					// get script executor for the data source
					ScriptExecutor sqlcl = this.installer.getScriptExecutorByDsName(dsName);
					// execute statement
					cmdPatchTable = cmdPatchTable.replace("#PREFIX#", registryTarget.tablePrefix);
					this.installer.executeStatement(cmdPatchTable, sqlcl);
				} catch (SQLException | IOException e) {
					e.printStackTrace();
				}
				try {
					// get script executor for the data source
					ScriptExecutor sqlcl = this.installer.getScriptExecutorByDsName(dsName);
					// execute statement
					cmdPatchDetails = cmdPatchDetails.replace("#PREFIX#", registryTarget.tablePrefix);
					this.installer.executeStatement(cmdPatchDetails, sqlcl);
				} catch (SQLException | IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void registerPatch(String application, String packageName, String version, String author,
			String configFilename, String connectionPoolFilename, String description, String targetSystemName)
			throws SQLException {

		String cmd = "INSERT INTO #PREFIX#_installer_patches (\n" + "    pat_id,\n" + "    pat_application,\n"
				+ "    pat_name,\n" + "    pat_version,\n" + "    pat_author,\n" + "    pat_target_system,\n"
				+ "    pat_config_filename,\n" + "    pat_conn_pool_filename,\n" + "    pat_started_on,\n"
				+ "    pat_description\n" + ") VALUES (\n" + "    (\n" + "        SELECT\n"
				+ "            nvl(MAX(pat_id)+1,1)\n" + "        FROM\n" + "            #PREFIX#_installer_patches\n"
				+ "    ),\n" + "    ?,\n" + "    ?,\n" + "    ?,\n" + "    ?,\n" + "    ?,\n" + "    ?,\n" + "    ?,\n"
				+ "    sysdate,\n" + "    ?\n" + ")";

		if (registryTargets != null) {
			for (RegistryTarget registryTarget : this.registryTargets) {

				// get data source name
				String dsName = registryTarget.connectionPoolName;

				// call statement
				CallableStatement cs = null;
				try {
					// get script executor for the data source
					ScriptExecutor sqlcl = this.installer.getScriptExecutorByDsName(dsName);
					// execute statement
					cmd = cmd.replace("#PREFIX#", registryTarget.tablePrefix);
					// this.installer.executeStatement(cmd, sqlcl);

					cs = sqlcl.getConn().prepareCall(cmd);
					cs.setString(1, StringUtils.substring(application, 0, 99));
					cs.setString(2, StringUtils.substring(packageName, 0, 99));
					cs.setString(3, StringUtils.substring(version, 0, 99));
					cs.setString(4, StringUtils.substring(author, 0, 99));
					cs.setString(5, StringUtils.substring(targetSystemName, 0, 49));
					cs.setString(6, StringUtils.substring(configFilename, 0, 3999));
					cs.setString(7, StringUtils.substring(connectionPoolFilename, 0, 3999));
					cs.setString(8, StringUtils.substring(description, 0, 3999));

					cs.execute();
					cs.execute("commit");

					cs.close();

					// this.installer.executeStatement("commit", sqlcl);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					cs.close();
				}
			}
		}
	}

	public void checkPatchDependencies(String application, String packageName, String version, String targetSystemName,
			ArrayList<PatchDependency> patchDependencies) throws SQLException {

		String cmd = "select count(*) from #PREFIX#_installer_patches where (pat_application=nvl(?,pat_application) and pat_name=nvl(?,pat_name) and pat_version=nvl(?,pat_version) and pat_target_system=?) and pat_ended_on is not null";

		Msg.println("*** Check patch dependencies\n");

		if (registryTargets != null) {
			for (RegistryTarget registryTarget : this.registryTargets) {

				// get data source name
				String dsName = registryTarget.connectionPoolName;

				for (PatchDependency patchDependency : patchDependencies) {

					// call statement
					PreparedStatement cs = null;
					OracleResultSet objectRS = null;
					try {
						// get script executor for the data source
						ScriptExecutor sqlcl = this.installer.getScriptExecutorByDsName(dsName);
						// execute statement
						cmd = cmd.replace("#PREFIX#", registryTarget.tablePrefix);
						logger.debug("CheckPatchDependencies: " + cmd);
						cs = sqlcl.getConn().prepareCall(cmd);
						cs.setString(1, StringUtils.substring(patchDependency.application, 0, 99));
						cs.setString(2, StringUtils.substring(patchDependency.patch, 0, 99));
						cs.setString(3, StringUtils.substring(patchDependency.version, 0, 99));
						cs.setString(4, StringUtils.substring(targetSystemName, 0, 49));

						logger.debug("  application:" + patchDependency.application);
						logger.debug("  patch:" + patchDependency.patch);
						logger.debug("  version:" + patchDependency.version);
						logger.debug("  targetSystemName:" + targetSystemName);

						cs.execute();

						objectRS = (OracleResultSet) cs.getResultSet();
						objectRS.next();
						int cnt = objectRS.getInt(1);

						if (cnt > 0) {
							logger.debug("  dependency check was successful!");
							Msg.println("  Dependency check PASSED: " + patchDependency.toString());
						} else {
							logger.debug("  dependency check FAILED");
							throw new RuntimeException("Patch dependency check failed: " + patchDependency.toString());
						}
					} catch (SQLException e) {
						e.printStackTrace();
					} finally {
						if (objectRS != null)
							objectRS.close();
						if (cs != null)
							cs.close();
					}
				}
				Msg.println("");
			}
		}
	}

	public void finalizePatch() throws SQLException {

		String cmd = "UPDATE #PREFIX#_installer_patches\n" + "   SET pat_ended_on = SYSDATE\n"
				+ " WHERE pat_id = (SELECT MAX (pat_id) FROM #PREFIX#_installer_patches)";

		if (registryTargets != null) {
			for (RegistryTarget registryTarget : this.registryTargets) {

				// get data source name
				String dsName = registryTarget.connectionPoolName;

				// call statement
				CallableStatement cs = null;
				try {
					// get script executor for the data source
					ScriptExecutor sqlcl = this.installer.getScriptExecutorByDsName(dsName);
					// execute statement
					cmd = cmd.replace("#PREFIX#", registryTarget.tablePrefix);

					cs = sqlcl.getConn().prepareCall(cmd);
					cs.execute();
					cs.execute("commit");

					cs.close();

					// this.installer.executeStatement("commit", sqlcl);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					cs.close();
				}
			}
		}
	}

	public void registerFile(String filename) throws SQLException {

		String cmd = "INSERT INTO #PREFIX#_installer_details (\n" + "    det_id,\n" + "    det_filename,\n"
				+ "    det_installed_on,\n" + "    det_pat_id\n" + ") VALUES (\n" + "    (\n" + "        SELECT\n"
				+ "            nvl(\n" + "                MAX(det_id) + 1, 1\n" + "            )\n" + "        FROM\n"
				+ "            #PREFIX#_installer_details\n" + "    ),\n" + "    ?,\n" + "    sysdate,\n" + "    (\n"
				+ "        SELECT\n" + "            MAX(pat_id)\n" + "        FROM\n"
				+ "            #PREFIX#_installer_patches\n" + "    )\n" + ")";

		if (registryTargets != null) {
			for (RegistryTarget registryTarget : this.registryTargets) {

				// get data source name
				String dsName = registryTarget.connectionPoolName;

				// call statement
				CallableStatement cs = null;
				try {
					// get script executor for the data source
					ScriptExecutor sqlcl = this.installer.getScriptExecutorByDsName(dsName);
					// execute statement
					cmd = cmd.replace("#PREFIX#", registryTarget.tablePrefix);
					// this.installer.executeStatement(cmd, sqlcl);

					cs = sqlcl.getConn().prepareCall(cmd);
					cs.setString(1, StringUtils.substring(filename, 0, 3999));
					cs.execute();
					cs.execute("commit");
					cs.close();

					// this.installer.executeStatement("commit", sqlcl);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					cs.close();
				}
			}
		}
	}
}
