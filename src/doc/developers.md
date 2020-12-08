# Instructions for developers to contribute

## Requirements

* Java JDK >= **Java 8**
* Gradle (https://gradle.org/) (this is optional and not required, only when upgrading the gradle environment). Currently, we are using Gradle 6.6.1 and a gradle wrapper is provided.

## Best practices

When contributing as a developer, please adhere to the best practices: https://www.datree.io/resources/github-best-practices. 
Specifically, the main branch is protected: https://docs.github.com/en/github/administering-a-repository/configuring-protected-branches.

## Setup

### Download 

First you need to download the files to a local directory, either use ``git clone`` or download them using the download link from github: ``Code > Download as zip``. 

### Using gradle

We are using the gradle build tool (https://gradle.org/), specifically with the gradle wrapper (https://tomgregory.com/what-is-the-gradle-wrapper-and-why-should-you-use-it/).

Thus, for all commands you can use `./gradlew` or `gradle.bat` instead of `gradle`. The advantage is that you don't have to download and install gradle. Also, you can always be sure to use the correct version of gradle for the build. In the instructions we will always refer to ``<gradle command>``, so on Windows replace that with ``gradlew`` or ``gradlew.bat``, on Linux and MacOS replace it with ``./gradlew``. 

### Configuring the SQLcl libraries

1. Download SQLcl libraries from: https://www.oracle.com/tools/downloads/sqldev-downloads.html

2. Register the version of the SQLcl libraries in file ``build.gradle`` and register the path to the ``.zip`` file: 
```
  // version of sqlcl to be used
  // download files from : https://www.oracle.com/tools/downloads/sqlcl-downloads.html
  // and register here
  def sqlclVersion  ='20.3.0'
  // sqlclZipFile: file location to downloaded zip file, 
  // e.g. /Users/daust/Downloads/sqlcl-20.2.0.174.1557.zip
  // this is only required when using task "opalRegisterSQLclLibraries"
  def sqlclZipFile  ='/Users/daust/Downloads/sqlcl-20.3.0.274.1916.zip'  
```

3. Extract libraries from SQLcl zip file:
```
<gradle command> opalRegisterSQLclLibraries
```

## Tasks

You can get an overview of all available build tasks by using: 
```
<gradle command> tasks --all
```

### Upating the configuration

The most important configuration can be found in the build file `build.gradle`:

```
  project.description       = "OPAL Tools - more details here: https://github.com/daust/opal-tools"
  project.version           = '2.6.0'
  
  sourceCompatibility       = 1.8
  targetCompatibility       = 1.8

  archivesBaseName          = "opal-tools"

  // version of sqlcl to be used
  // download files from : https://www.oracle.com/tools/downloads/sqlcl-downloads.html
  // and register here
  def sqlclVersion  ='20.3.0'

  // sqlclZipFile: file location to downloaded zip file, 
  // e.g. /Users/daust/Downloads/sqlcl-20.2.0.174.1557.zip
  // this is only required when using task "opalRegisterSQLclLibraries"
  def sqlclZipFile  ='/Users/daust/Downloads/sqlcl-20.3.0.274.1916.zip'  
```

## Building

### Building a version of the project

There are many different ways you can build the project ... depending on your needs: 
* cleaning the build directory
    * `<gradle command> clean`
* building the war file 
    * `<gradle command> build`
* building the distribution (without zip) into the build directory `build/install/opal-tools`
    * `<gradle command> installDist`
* building the distribution as zip into the build directory: `build/distributions`
    * `<gradle command> assembleDist`

The following naming conventions will be used: `opal-tools-<project.version>-sqlcl-<sqlclVersion>.zip`, e.g.: ``opal-tools-2.5.0-sqlcl-20.3.0.zip``. 

A *.tar file will also be created. 

## Testing / debugging

*n/a*

## Checklist for a new release

*n/a*

## Development with Eclipse

* the following task will create all required files for Eclipse
    * `<gradle command> eclipse`

Then you can just start Eclipse and import the root directory into Eclipse as a gradle project. 

## Working with Markdown (*.md) files

Markdown files are used for the documentation, they are written in Markdown syntax as described [here](https://guides.github.com/features/mastering-markdown/). In Visual Studio Code you can use plugins to display the Markdown preview in a separate window. But you can also use a Google Chrome Extension to render the markdown files in the browser, e.g. [the Markdown Viewer](https://chrome.google.com/webstore/detail/markdown-viewer/ckkdlimhmcjmikdlpkmbgfkaikojcbjk).
