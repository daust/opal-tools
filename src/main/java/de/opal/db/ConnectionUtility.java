package de.opal.db;

import java.sql.Connection;
import java.sql.SQLException;

import oracle.jdbc.OracleConnection;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

public class ConnectionUtility {
	private static ConnectionUtility instance;
	private int poolSize = 0;
	private String user;
	private String pwd;
	private String connectStr;
	private final static String CONN_FACTORY_CLASS_NAME = "oracle.jdbc.pool.OracleDataSource";
	private PoolDataSource pds;

	/**
	 * Singleton pattern, get instance
	 * 
	 * @param jndiPrefix the jndi prefix
	 * 
	 * @return ConnectionUtility
	 */
	public static synchronized ConnectionUtility getInstance() {
		if (instance == null) {
			instance = new ConnectionUtility();
		}
		return instance;
	}

	public void initializeConnPool(int poolSize, String user, String pwd, String connectStr) throws SQLException {
		this.poolSize = poolSize;
		this.user = user;
		this.pwd = pwd;
		this.connectStr = ConnectionUtility.transformJDBCConnectString(connectStr);

		// Get the PoolDataSource for UCP
		pds = PoolDataSourceFactory.getPoolDataSource();

		// Set the connection factory first before all other properties
		pds.setConnectionFactoryClassName(CONN_FACTORY_CLASS_NAME);
		pds.setURL(this.connectStr);
		pds.setUser(this.user);
		pds.setPassword(this.pwd);
		pds.setConnectionPoolName("JDBC_UCP_POOL");

		// Default is 0. Set the initial number of connections to be created
		// when UCP is started.
		pds.setInitialPoolSize(this.poolSize);

		// Default is 0. Set the minimum number of connections
		// that is maintained by UCP at runtime.
		pds.setMinPoolSize(this.poolSize);

		// Default is Integer.MAX_VALUE (2147483647). Set the maximum number of
		// connections allowed on the connection pool.
		pds.setMaxPoolSize(this.poolSize);

		// Default is 30secs. Set the frequency in seconds to enforce the timeout
		// properties. Applies to inactiveConnectionTimeout(int secs),
		// AbandonedConnectionTimeout(secs)& TimeToLiveConnectionTimeout(int secs).
		// Range of valid values is 0 to Integer.MAX_VALUE. .
		pds.setTimeoutCheckInterval(5);

		// Default is 0. Set the maximum time, in seconds, that a
		// connection remains available in the connection pool.
		pds.setInactiveConnectionTimeout(10);
	}

	public Connection getConnection() throws SQLException {
		OracleConnection conn = null;

		// Get the database connection from UCP.
		try {
			conn = (OracleConnection) pds.getConnection();
			conn.setImplicitCachingEnabled(true);
			conn.setStatementCacheSize(10);

			//System.out.println("Available connections after checkout: " + pds.getAvailableConnectionsCount());
			//System.out.println("Borrowed connections after checkout: " + pds.getBorrowedConnectionsCount());
		} catch (SQLException e) {
			System.out.println("UCPSample - " + "SQLException occurred : " + e.getMessage());
		}
		//System.out.println("Available connections after checkin: " + pds.getAvailableConnectionsCount());
		//System.out.println("Borrowed connections after checkin: " + pds.getBorrowedConnectionsCount());

		return conn;
	}

	public static String transformJDBCConnectString(String connectString) {
		String transformedURL = connectString;

		// already contains the full url
		if (transformedURL.startsWith("jdbc:")) {
			// ok
		} else {
			transformedURL = "jdbc:oracle:thin:@" + transformedURL;
		}

		return transformedURL;
	}
}
