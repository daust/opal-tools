define PREFIX=OPAL2

drop table &PREFIX._INSTALLER_DETAILS purge;
drop table &PREFIX._INSTALLER_PATCHES purge;

--------------------------------------------------------
--  DDL for Table &PREFIX._INSTALLER_DETAILS
--------------------------------------------------------

  CREATE TABLE "&PREFIX._INSTALLER_DETAILS" 
   (	"DET_ID" NUMBER, 
	"DET_FILENAME" VARCHAR2(4000 CHAR), 
	"DET_INSTALLED_ON" DATE, 
	"DET_PAT_ID" NUMBER
   ) ;
--------------------------------------------------------
--  DDL for Table &PREFIX._INSTALLER_PATCHES
--------------------------------------------------------

  CREATE TABLE "&PREFIX._INSTALLER_PATCHES" 
   (	"PAT_ID" NUMBER, 
	"PAT_APPLICATION" VARCHAR2(100 CHAR), 
	"PAT_NAME" VARCHAR2(100 CHAR), 
	"PAT_VERSION" VARCHAR2(100 CHAR), 
	"PAT_AUTHOR" VARCHAR2(100 CHAR), 
	"PAT_TARGET_SYSTEM" VARCHAR2(50 CHAR), 
	"PAT_STARTED_ON" DATE, 
	"PAT_ENDED_ON" DATE, 
	"PAT_DESCRIPTION" VARCHAR2(4000 CHAR), 
	"PAT_CONFIG_FILENAME" VARCHAR2(4000 CHAR), 
	"PAT_CONN_POOL_FILENAME" VARCHAR2(4000 CHAR)
   ) ;
--------------------------------------------------------
--  DDL for Index &PREFIX._INSTALLER_DETAILS_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "&PREFIX._INSTALLER_DETAILS_PK" ON "&PREFIX._INSTALLER_DETAILS" ("DET_ID") 
  ;
--------------------------------------------------------
--  DDL for Index &PREFIX._INSTALLER_PATCHES_PK
--------------------------------------------------------

  CREATE UNIQUE INDEX "&PREFIX._INSTALLER_PATCHES_PK" ON "&PREFIX._INSTALLER_PATCHES" ("PAT_ID") 
  ;
--------------------------------------------------------
--  Constraints for Table &PREFIX._INSTALLER_DETAILS
--------------------------------------------------------

  ALTER TABLE "&PREFIX._INSTALLER_DETAILS" ADD CONSTRAINT "&PREFIX._INSTALLER_DETAILS_PK" PRIMARY KEY ("DET_ID") ENABLE;
  ALTER TABLE "&PREFIX._INSTALLER_DETAILS" MODIFY ("DET_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Constraints for Table &PREFIX._INSTALLER_PATCHES
--------------------------------------------------------

  ALTER TABLE "&PREFIX._INSTALLER_PATCHES" ADD CONSTRAINT "&PREFIX._INSTALLER_PATCHES_PK" PRIMARY KEY ("PAT_ID") ENABLE;
  ALTER TABLE "&PREFIX._INSTALLER_PATCHES" MODIFY ("PAT_ID" NOT NULL ENABLE);
--------------------------------------------------------
--  Ref Constraints for Table &PREFIX._INSTALLER_DETAILS
--------------------------------------------------------

  ALTER TABLE "&PREFIX._INSTALLER_DETAILS" ADD CONSTRAINT "&PREFIX._INSTALLER_DETAILS_FK1" FOREIGN KEY ("DET_PAT_ID")
	  REFERENCES "&PREFIX._INSTALLER_PATCHES" ("PAT_ID") ENABLE;
