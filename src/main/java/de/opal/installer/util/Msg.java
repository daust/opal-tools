package de.opal.installer.util;

/**
 * Singleton class
 */
public class Msg {

	private static Msg _instance;
	
	private Msg(){
		//TODO: put your code here
	}

	/**
	 * returns Singleton Instance
	 * @return Msg
	 */	
	public static synchronized Msg getInstance(){
		if( _instance == null ) _instance = new Msg();
		return _instance;
	}

	public static void println( String msg ) {
//		System.out.println( "[OPAL Installer] "+ msg );
		System.out.println( msg );
	}
	
	public static void print( String msg ) {
//		System.out.print( "[OPAL Installer] "+ msg );
		System.out.print( msg );
	}
}
