
prompt display version of sqlcl
version

prompt show current working directory
pwd

prompt *** exporting apex applications
prompt *** configure opal-export-post-script.sql if required
/*
host mkdir /tmp/opal-exporter/apex
cd /tmp/opal-exporter/apex

apex export -applicationid 344
apex export -applicationid 201
*/
