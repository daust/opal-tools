# Troubleshooting

### Warning message: ``Unable to get Charset 'cp65001' for property 'sun.stdout.encoding', using default windows-1250 and continuing``.

This message only indicates that the character set for the console output cannot be determined from the current shell environment. See more details and workarounds here: https://github.com/daust/opal-installer/issues/8. 

### OPAL Installer: Java exception ``java.lang.AssertionError: sqlplus comment``

You might see the following error message when executing a sql file (e.g. an APEX export file): 
```
java.lang.AssertionError: sqlplus comment
	at oracle.dbtools.parser.NekotRexel.tokenize(NekotRexel.java:128)
	at oracle.dbtools.parser.NekotRexel.parse(NekotRexel.java:314)
	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:527)
	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:482)
	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:475)
	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:459)
	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:425)
	at oracle.dbtools.parser.Lexer.parse(Lexer.java:11)
	at oracle.dbtools.raptor.newscriptrunner.ScriptRunner.runPLSQL(ScriptRunner.java:330)
	at oracle.dbtools.raptor.newscriptrunner.ScriptRunner.run(ScriptRunner.java:245)
	at oracle.dbtools.raptor.newscriptrunner.ScriptExecutor.run(ScriptExecutor.java:344)
	at oracle.dbtools.raptor.newscriptrunner.ScriptExecutor.run(ScriptExecutor.java:227)
	at de.opal.installer.Installer.executeFile(Installer.java:483)
	at de.opal.installer.Installer.processTree(Installer.java:431)
	at de.opal.installer.Installer.run(Installer.java:283)
	at de.opal.installer.InstallerMain.main(InstallerMain.java:72)
```
This seems to be an issue with the tokenizer in SQLcl. The statement itself is executed properly nevertheless. Thus you can ignore it. 

Here are more details on it: https://twitter.com/daust_de/status/1331865412984844289 . 

