package de.opal.installer.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.sql.PooledConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.db.ConnectionUtility;
import de.opal.installer.config.ConfigConnectionPool;
import de.opal.installer.util.Utils;
import oracle.jdbc.pool.OracleConnectionPoolDataSource;

/**
 * Singleton class
 */
public class ConnectionManager {

	// ----------------------------------------------------
	// public fields
	// ----------------------------------------------------

	// ----------------------------------------------------
	// private fields
	// ----------------------------------------------------

	private static ConnectionManager _instance;
	private HashMap<String, ConfigConnectionPool> dataSourceDefs = new HashMap<String, ConfigConnectionPool>();
	
	private HashMap<String, OracleConnectionPoolDataSource> dataSources = new HashMap<String, OracleConnectionPoolDataSource>();
	private static Logger logger = LogManager.getLogger(ConnectionManager.class.getName());

	// ----------------------------------------------------
	// Constructors
	// ----------------------------------------------------

	/**
	 * returns Singleton Instance
	 * 
	 * @return ConnectionManager
	 */
	public static synchronized ConnectionManager getInstance() {
		if (_instance == null)
			_instance = new ConnectionManager();
		return _instance;
	}

	private ConnectionManager() {
		// initialize Connection Manager

	}
	
	public void initialize(ArrayList<ConfigConnectionPool> connectionPools) {
		// store definitions in hashmap
		for (ConfigConnectionPool configConnectionPool : connectionPools) {
			dataSourceDefs.put(configConnectionPool.name, configConnectionPool);
		}
	}

	// ----------------------------------------------------
	// Methods / Functions
	// ----------------------------------------------------

	public void openConnections(ArrayList<ConfigConnectionPool> connectionPools) {
		// create new connections for all definitions
		for (ConfigConnectionPool configConnectionPool : connectionPools) {
			openConnection(configConnectionPool);
		}
	}

	public Connection openConnection(ConfigConnectionPool configConnectionPool) {
		Connection conn = null;

		logger.debug("create new connection pool for:" + configConnectionPool.toString());

		OracleConnectionPoolDataSource ocpds;
		PooledConnection pc;

		try {
			logger.trace("retrieve connectionPoolDataSource from HashMap first");
			ocpds = this.dataSources.get(configConnectionPool.name);

			if (ocpds == null) {
				logger.trace("dataSource not found in HashMap, initialize a new connection pool and store in HashMap");

				// set cache properties
				java.util.Properties prop = new java.util.Properties();
				prop.setProperty("InitialLimit", "1");
				prop.setProperty("MinLimit", "1");
				prop.setProperty("MaxLimit", "1");

			    ocpds = new OracleConnectionPoolDataSource();
			    
				ocpds.setURL(ConnectionUtility.transformJDBCConnectString(configConnectionPool.connectString));
				ocpds.setUser(configConnectionPool.user);
				ocpds.setPassword(configConnectionPool.password);

				// set connection parameters
				ocpds.setConnectionProperties(prop);

				// store in hashmap
				dataSources.put(configConnectionPool.name, ocpds);
			}

			pc = ocpds.getPooledConnection();
			conn = pc.getConnection();
			conn.setAutoCommit(false);

			logger.info("successfully connected to " + configConnectionPool.connectString);
		} catch (SQLException e) {
			Utils.throwRuntimeException("Could not connect via JDBC: " + e.getMessage());
		}
		
		return conn;
	}
	

	/**
	 * retrieves a connection from the pool or creates a new one
	 * 
	 * @param dsName
	 * @return
	 * @throws SQLException
	 */
	public Connection getConnection(String dsName) throws SQLException {
		Connection conn;
		OracleConnectionPoolDataSource ocpds;
		PooledConnection pc;

		logger.debug("get connection for  new connection pool for:" + dsName);
		ocpds = this.dataSources.get(dsName);
		
		if (ocpds==null) {
			// connection does not yet exist, create a new one
			// first determine connection pool definition for data source
			ConfigConnectionPool configConnectionPool = this.dataSourceDefs.get(dsName);
			
			if (configConnectionPool == null)
				throw new RuntimeException("Connection for data source \"" + dsName + "\" could not be found in connection pool file.");
			
			//ConfigConnectionPool configConnectionPool
			conn = openConnection( configConnectionPool );
		}else {
			// connection exists 
			pc = ocpds.getPooledConnection();
			conn = pc.getConnection();
		}

		return conn;
	}
	
	public void closeConnection(String dsName) throws SQLException {
		// close this one connection
		Connection conn = getConnection( dsName );
		conn.close();
		conn=null;
	}
	public void closeConnection(Connection conn) throws SQLException {
		// close this one connection
		conn.close();
		conn=null;
	}

	public void closeAllConnections() {
		// close all current connections
		this.dataSources.forEach((k,v)->{
			try {
				DBUtils.closeQuietly(v.getConnection());
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
	}

	// ----------------------------------------------------
	// Getter / Setter
	// ----------------------------------------------------

}
