package de.opal.tests;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

public class OracleProxyConnectUCP {
  static final String DB_URL = "jdbc:oracle:thin:@vm1:1521:xe";
  
  static final String DB_USER = "daust[jri_test]";
  
  static final String DB_PASSWORD = "daust";
  
  static final String CONN_FACTORY_CLASS_NAME = "oracle.jdbc.pool.OracleDataSource";
  
  public static void main(String[] args) throws Exception {
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
    pds.setURL("jdbc:oracle:thin:@vm1:1521:xe");
    pds.setUser("daust[jri_test]");
    pds.setPassword("daust");
    pds.setConnectionPoolName("JDBC_UCP_POOL");
    pds.setInitialPoolSize(5);
    pds.setMinPoolSize(5);
    pds.setMaxPoolSize(20);
    pds.setTimeoutCheckInterval(5);
    pds.setInactiveConnectionTimeout(10);
    try {
      Connection conn = pds.getConnection();
      try {
        System.out.println("Available connections after checkout: " + pds
            .getAvailableConnectionsCount());
        System.out.println("Borrowed connections after checkout: " + pds
            .getBorrowedConnectionsCount());
        doSQLWork(conn);
        if (conn != null)
          conn.close(); 
      } catch (Throwable throwable) {
        if (conn != null)
          try {
            conn.close();
          } catch (Throwable throwable1) {
            throwable.addSuppressed(throwable1);
          }  
        throw throwable;
      } 
    } catch (SQLException e) {
      System.out.println("UCPSample - SQLException occurred : " + e
          .getMessage());
    } 
    System.out.println("Available connections after checkin: " + pds
        .getAvailableConnectionsCount());
    System.out.println("Borrowed connections after checkin: " + pds
        .getBorrowedConnectionsCount());
  }
  
  public static void doSQLWork(Connection conn) {
    try {
      conn.setAutoCommit(false);
      Statement statement = conn.createStatement();
      statement.executeUpdate("create table EMP(EMPLOYEEID NUMBER,EMPLOYEENAME VARCHAR2 (20))");
      System.out.println("New table EMP is created");
      statement.executeUpdate("insert into EMP values(1, 'Jennifer Jones')");
      statement.executeUpdate("insert into EMP values(2, 'Alex Debouir')");
      System.out.println("Two records are inserted.");
      statement.executeUpdate("update EMP set EMPLOYEENAME='Alex Deborie' where EMPLOYEEID=2");
      System.out.println("One record is updated.");
      ResultSet resultSet = statement.executeQuery("select * from EMP");
      System.out.println("\nNew table EMP contains:");
      System.out.println("EMPLOYEEID EMPLOYEENAME");
      System.out.println("--------------------------");
      while (resultSet.next())
        System.out.println(resultSet.getInt(1) + " " + resultSet.getString(2)); 
      System.out.println("\nSuccessfully tested a connection from UCP");
    } catch (SQLException e) {
      System.out.println("UCPSample - doSQLWork()- SQLException occurred : " + e
          .getMessage());
    } finally {
      try {
        Statement statement = conn.createStatement();
        try {
          statement.execute("drop table EMP");
          if (statement != null)
            statement.close(); 
        } catch (Throwable throwable) {
          if (statement != null)
            try {
              statement.close();
            } catch (Throwable throwable1) {
              throwable.addSuppressed(throwable1);
            }  
          throw throwable;
        } 
      } catch (SQLException e) {
        System.out.println("UCPSample - doSQLWork()- SQLException occurred : " + e
            .getMessage());
      } 
    } 
  }
}
