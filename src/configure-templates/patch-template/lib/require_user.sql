PROMPT *** This script must be run as ^1.  This script will exit
PROMPT *** below if run as any other user.
set verify off;
whenever sqlerror exit;
select 'User is ^1' check_user from dual
where 1 = decode(USER,'^1',1,'NOT');
whenever sqlerror continue;
set verify on;


