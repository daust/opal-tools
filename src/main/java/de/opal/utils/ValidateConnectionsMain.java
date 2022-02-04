package de.opal.utils;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import de.opal.db.SQLclUtil;
import de.opal.exporter.ExporterMain;
import de.opal.installer.config.ConfigConnectionPool;
import de.opal.installer.config.ConfigManagerConnectionPool;
import de.opal.installer.db.DBUtils;
import de.opal.installer.util.Msg;

public class ValidateConnectionsMain {

	public static final Logger log = LogManager.getLogger(ExporterMain.class.getName());

	@Option(name = "-h", aliases = "--help", usage = "show this help page", help = true)
	private boolean showHelp;

	@Option(name = "-v", aliases = "--version", usage = "show version information", help = true)
	private boolean showVersion;

	// all arguments, not options. This is the list of connection pools to work with
	@Argument
	private List<String> poolFileList = new ArrayList<String>();

	public static void main(String[] args) throws IOException {
		ValidateConnectionsMain main = new ValidateConnectionsMain();

		main.parseParameters(args);

		main.run();
	}

	private void parseParameters(String[] args) {
		ParserProperties properties = ParserProperties.defaults();
		properties.withUsageWidth(130);
		properties.withOptionSorter(null);
		CmdLineParser parser = new CmdLineParser(this, properties);

		log.debug("start parsing");
		try {

			// parse the arguments.
			parser.parseArgument(args);

			// after parsing arguments, you should check
			// if enough arguments are given.
			if (this.showHelp || this.showVersion) {
				if (this.showVersion) {
					VersionInfo.showVersionInfo(this.getClass(), "OPAL Tools", true);
				}

				// check whether jdbcURL OR connection pool is specified correctly
				if (this.showHelp) {
					showUsage(System.out, parser);
					System.exit(0);
				}
			} else {
				if (poolFileList.isEmpty() && this.showHelp == false)
					throw new CmdLineException(parser, "No connection pool filename is provided.");
			}
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			showUsage(System.err, parser);

			System.exit(1);
		}
	}

	private void showUsage(PrintStream out, CmdLineParser parser) {
		out.println(
				"\njava de.opal.utils.ValidateConnectionsMain [options...] <connection pool file1> [<connection pool file2>] [<connection pool file3>]");

		// print the list of available options
		parser.printUsage(out);

		out.println();

		// print option sample. This is useful some time
		out.println("  Example: java de.opal.exporter.ValidateConnectionsMain"
				+ parser.printExample(OptionHandlerFilter.PUBLIC));
	}

	private void run() throws IOException {
		Connection conn = null;

		for (String connFilename : poolFileList) {
			Msg.println("\n*** process connection file: " + connFilename);

			ConfigManagerConnectionPool configManagerConnectionPools = new ConfigManagerConnectionPool(connFilename);
			if (configManagerConnectionPools.hasUnencryptedPasswords()) {
				Msg.println("  Connection file contains unencrypted passwords => file will be encrypted.");
				configManagerConnectionPools
						.encryptPasswords(configManagerConnectionPools.getEncryptionKeyFilename(connFilename));
				configManagerConnectionPools.writeJSONConf();
			} else {
				Msg.println("  All passwords in this file are already encrypted.");
			}

			configManagerConnectionPools
					.decryptPasswords(configManagerConnectionPools.getEncryptionKeyFilename(connFilename));

			for (ConfigConnectionPool pool : (configManagerConnectionPools
					.getConfigDataConnectionPool()).connectionPools) {
				Msg.println("\n  process connection: " + pool.name);

				if (pool.password == null || pool.password.equals("")) {
					Msg.println("  empty password => skip check");
				} else {
					Msg.print("  check connection: " + pool.user + "@" + pool.connectString);
					try {
						conn = SQLclUtil.openConnection(pool.user, pool.password, pool.connectString);
						DBUtils.closeQuietly(conn);
						Msg.println(" => SUCCESS");
					} catch (Exception e) {
						// Msg.println(" => FAILURE\n");
						// Msg.println(e.getLocalizedMessage());
					} finally {
						DBUtils.closeQuietly(conn);
					}
				}

			}
		}
		Msg.println("");
	}
}
