[//]: # (Infos about this file)
[//]: # (Markdown Syntax: https://guides.github.com/features/mastering-markdown/)

# Version 2.7.2 (release: 12.02.2021)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/13?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/13?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/13?closed=1)

## Changed behaviour
  * *none*

## Upgrade instructions
  In order to upgrade from version 2.6.0 to 2.7.2 you only need to replace the /lib directory:
  * Delete *all* files from /lib directory
  * Copy *all* files from the /lib directory of the distribution

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 

# Version 2.7.1 (release: 11.02.2021)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/12?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/12?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/12?closed=1)

## Changed behaviour
  * *none*

## Upgrade instructions
  In order to upgrade from version 2.6.0 to 2.7.1 you only need to replace the /lib directory:
  * Delete *all* files from /lib directory
  * Copy *all* files from the /lib directory of the distribution

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 

# Version 2.7.0 (release: 10.02.2021)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/11?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/11?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/11?closed=1)

## Changed behaviour
  * *none*

## Upgrade instructions
  In order to upgrade from version 2.6.0 to 2.7.0 you only need to replace the /lib directory:
  * Delete *all* files from /lib directory
  * Copy *all* files from the /lib directory of the distribution

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 

# Version 2.6.0 (release: 11.12.2020)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/10?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/10?closed=1)
  * changed names in documentation and the default in the setup routine, so that it matches the video and instructions:
    - ``scott hr`` => ``schema1 schema2``

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/10?closed=1)

## Changed behaviour
  * OPTIONAL: The naming of ``SourceFilesCopy.txt`` and ``SourceFilesReference.txt`` to ``SourceFilesCopy.conf`` and ``SourceFilesReference.conf``. These files are called from the shell scripts and passed as parameters. Your existing patches will continue to work. New releases will generate different patch-templates that will use these different filenames in the filesystem and call them automatically in the shell scripts. 
  * MANDATORY: In order to streamline the naming of properties, the following attributes have changed in the ``opal-installer.json`` file (else these properties will no longer be read):
    - ``matchRegEx => fileRegex``
    - ``sqlFileRegEx => sqlFileRegex``
    ![initialize script](resources/v2.6.0-change-opal-installer.json.png)

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 

# Version 2.5.0 (release: 03.12.2020)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/9?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/9?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/9?closed=1)

## Changed behaviour
  * *none*

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 

# Version 2.4.1 (release: 30.11.2020)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/8?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/8?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/8?closed=1)

## Changed behaviour
  * the file ``PatchFiles.txt`` is split into two files: 
    - ``SourceFilesCopy.conf`` (picked up by 1.copy-source-files)
    - ``SourceFilesReference.conf`` (picked up by 2.validate and 3.install)
  * the script ``opal-tools/bin/opal-copy-patch-files.sh|cmd`` is renamed to ``opal-tools/bin/opal-copy-source-files.sh|cmd``
  * the patch script ``1.copy-patch-files.sh|cmd`` is renamed to ``1.copy-source-files.sh|cmd``
  * the class ``CopyPatchFilesMain`` was renamed to ``CopySourceFilesMain`` and is updated in ``opal-tools/bin/opal-copy-source-files.sh|cmd``
  * the patch ``scripts 1.copy, 2.validate, 3.install`` have all been modified, because the command line switches have been renamed: 
    - ``--source-list-file``
    - ``--source-dir``
    - ``--target-dir``
  * the file ``opal-tools/bin/initialize-patch.sh|cmd`` is modified, the command line switches have changed for the copy command: 
    - ``--source-dir``
    - ``--target-dir``

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 

# Version 2.4.0 (release: 29.11.2020)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/7?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/7?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/7?closed=1)

## Changed behaviour
  * the file ``PatchFiles.txt`` is split into two files: 
    - ``SourceFilesCopy.conf`` (picked up by 1.copy-source-files)
    - ``SourceFilesReference.conf`` (picked up by 2.validate and 3.install)
  * the script ``opal-tools/bin/opal-copy-patch-files.sh|cmd`` is renamed to ``opal-tools/bin/opal-copy-source-files.sh|cmd``
  * the patch script ``1.copy-patch-files.sh|cmd`` is renamed to ``1.copy-source-files.sh|cmd``

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 

# Version 2.3.0 (release: 26.11.2020)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/6?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/6?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/6?closed=1)

## Changed behaviour
  * *none*

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 

# Version 2.2.0 (release: 24.11.2020)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/5?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/5?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/5?closed=1)

## Changed behaviour
  * *none*

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 

# Version 2.1.0 (pre-release: 16.11.2020)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/4?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/4?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/4?closed=1)

## Changed behaviour
  * *none*

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 

# Version 2.0.0 (pre-release: 12.11.2020)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/3?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/3?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/3?closed=1)

## Changed behaviour
  * Command line switches are new and the old positional parameters have been removed. 
  * Thus, all batch files will break. 

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * Command line switches are new and the old positional parameters have been removed. 
  * Thus, all batch files will break. 

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 


# Version 1.1.2 (pre-release: 06.11.2020)

You can see the full list of issues with details in the [milestones page](https://github.com/daust/opal-tools/milestone/2?closed=1). 

## New features
  * see [milestones page](https://github.com/daust/opal-tools/milestone/2?closed=1)

## Bug fixes
  * see [milestones page](https://github.com/daust/opal-tools/milestone/2?closed=1)

## Changed behaviour
  * *none*

## Deprecated features (still available but will go away eventually)
  * *none*

## Obsoleted features (no longer available)
  * *none*

## Known issues
  * Please check the current list of open issues: https://github.com/daust/opal-tools/issues . 


