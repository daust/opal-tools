/*
 $Id: DBUtils.java 56 2013-05-13 07:06:46Z dietmar.aust $

 Purpose  :  

 $LastChangedDate: 2013-05-13 09:06:46 +0200 (Mon, 13 May 2013) $
 $LastChangedBy: dietmar.aust $ 

 Date        Author          Comment
 --------------------------------------------------------------------------------------
 01.08.2012  D. Aust         Initial creation

 */

package de.opal.installer.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.exporter.Exporter;

public class DBUtils {

	private static final Logger log = LogManager.getLogger(DBUtils.class.getName());

	public static void closeQuietly(Connection conn) {
		if (conn != null) {
			try {
				conn.close();
				conn = null;
			} catch (SQLException e) {
				/* ignored */
			}
		}
	}

	public static void closeQuietly(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
				stmt = null;
			} catch (SQLException e) {
				/* ignored */
			}
		}
	}

	public static void closeQuietly(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
				rs = null;
			} catch (SQLException e) {
				/* ignored */
			}
		}
	}

	public static void closeQuietly(Connection conn, Statement stmt, ResultSet rs) {
		closeQuietly(rs);
		closeQuietly(stmt);
		closeQuietly(conn);
	}

	public static String nvl(String p, String defaultValue) {
		if (p == null)
			return defaultValue;
		if (p.equals(""))
			return defaultValue;

		return p;
	}

	public static Boolean nvl(Boolean p, Boolean defaultValue) {
		if (p == null)
			return defaultValue;
		if (p.equals(""))
			return defaultValue;

		return p;
	}

	public static Integer nvl(Integer p, Integer defaultValue) {
		if (p == null)
			return defaultValue;
		if (p.equals(""))
			return defaultValue;

		return p;
	}

	public static String clobToString(java.sql.Clob data) {
		final StringBuilder sb = new StringBuilder();

		try {
			final Reader reader = data.getCharacterStream();
			final BufferedReader br = new BufferedReader(reader);

			int b;
			while (-1 != (b = br.read())) {
				sb.append((char) b);
			}

			br.close();
		} catch (SQLException e) {
			log.error("SQL. Could not convert CLOB to string", e);
			return e.toString();
		} catch (IOException e) {
			log.error("IO. Could not convert CLOB to string", e);
			return e.toString();
		}

		return sb.toString();
	}

}
