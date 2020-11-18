# Developer Information

Information for developers.

## Installation

* Download files

* Unzip files

* Run setup

On Windows, run: 
```
setup.cmd [base directory]
```

On MacOS/Linux, run: 
```
./setup.sh [base directory]
```

The parameter ``base directory`` is optional. When this parameter is passed, it will be used to generate the defaults for the respective directories. But those directories can still be changed when being prompted. 

## Initial configuration

## Usage


## Testing / Debugging

* Installer 
	* program arguments: 
	``/Users/daust/Dropbox/Projekt/prj_installer/samples/patch2/config.json``
	
* Installer (copyFiles)
	* program arguments: 
	``copyPatchFiles /Users/daust/Dropbox/50.Projects/prj_installer/temp/19.1.0/sql /Users/daust/Dropbox/70.Activities/2020/2020-03-APEX-Projekte-vereinfachen/src-svn/trunk/scheme2ddl/output /Users/daust/Dropbox/50.Projects/prj_installer/templates/patch-directory/PatchFiles.txt``

* Installer (executePatch)
	* program arguments: 
	``executePatch EXECUTE /Users/daust/Dropbox/50.Projects/prj_installer/temp/19.1.0/installer-linux.json /Users/daust/Dropbox/50.Projects/prj_installer/temp/19.1.0/connectionPoolsEntwicklung.json``

* Installer (initPatch)
	* program arguments: 
	``initPatch /Users/daust/Dropbox/50.Projects/prj_installer/temp/19.1.0 /Users/daust/Dropbox/50.Projects/prj_installer/templates/patch-directory``

* Installer (validatePatch)
	* program arguments: 
	``executePatch VALIDATE_ONLY /Users/daust/Dropbox/50.Projects/prj_installer/temp/19.1.0/installer-linux.json /Users/daust/Dropbox/50.Projects/prj_installer/temp/19.1.0/connectionPoolsEntwicklung.json``

* vm arguments (debug)
	``-Dlog4j.configurationFile=src/conf/log4j2-debug.xml -Djava.util.logging.config.file=src/conf/log4j-debug.properties``

* vm arguments (production)
	``-Dlog4j.configurationFile=src/conf/log4j2.xml -Djava.util.logging.config.file=src/conf/log4j.properties``


## Development snippets

```
# build installDir
gradle installDist

# go to target directory
cd /Users/daust/Dropbox/50.Projects/github/opal-installer/build/install/opal-installer

# run initPatch

# run copyFiles

# run validatePatch

# run execute

```	
	

## Instructions on how to use the installer on Linux

Instructions on how to configure your Linux environment (Gnome) so that the ``.sh`` files are executed automatically when you double-click them. 

```
vi /usr/share/applications/terminal.desktop
```
```
[Desktop Entry]
Name=Terminal
Exec=gnome-terminal --tab --title="Execute in shell" --command="bash -c '%F; $SHELL'"
Type=Application
MimeType=text/plain;
Terminal=true
X-KeepTerminal=true
```

It is best to use the a dark background for the terminal / shell. You can configure that manually by using:

``Edit > Preferences``
or 
``Edit > Profile Preferences``

