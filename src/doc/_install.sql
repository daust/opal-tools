set define off;

@@xlib/XLIB_UTL_PCK.pks
show errors;
@@xlib/XLIB_UTL_PCK.pkb
show errors;

@@xlib/xlib_log_install.sql
@@xlib/xlib_conf_install.sql
@@xlib/xlib_sec_install.sql

@@xlib/XLIB_HTTP_PCK.pks
show errors;
@@xlib/XLIB_HTTP_PCK.pkb
show errors;

@@xlib/XLIB_CONTEXT_PCK.pks
show errors;
@@xlib/XLIB_CONTEXT_PCK.pkb
show errors;


@@xlib_conf_seed_data.sql
@@xlib_groups_seed_data.sql

prompt *** create initial ADMIN account: 
prompt *** User: ADMIN
prompt *** Password: admin
prompt *** The password expires immediately
declare
  l_usr_id number;
begin
  l_usr_id := xlib_sec_pck.add_user(p_usr_name => 'ADMIN', p_usr_first_name => '', p_usr_last_name => '', p_password=> 'admin');
  xlib_sec_pck.add_group_member (p_usr_id => l_usr_id, p_grp_id => xlib_sec_pck.get_grp_id(p_grp_name => 'SYSTEM_ADMIN'));
  l_usr_id := xlib_sec_pck.add_user(p_usr_name => 'DIETMAR.AUST', p_usr_first_name => 'Dietmar', p_usr_last_name => 'Aust', p_password=> 'admin');
  xlib_sec_pck.expire_password(p_usr_id => l_usr_id);
  xlib_sec_pck.add_group_member (p_usr_id => l_usr_id, p_grp_id => xlib_sec_pck.get_grp_id(p_grp_name => 'OPERATOR'));
end;
/

commit;


