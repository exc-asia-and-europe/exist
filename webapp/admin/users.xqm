(:
    Module: display and edit users.
:)
module namespace users="http://exist-db.org/xquery/admin-interface/users";

declare namespace util="http://exist-db.org/xquery/util";
declare namespace xdb="http://exist-db.org/xquery/xmldb";
declare namespace request="http://exist-db.org/xquery/request";

declare function users:main($user as xs:string, $password as xs:string) as element() {
    <div class="panel">
        <div class="panel-head">User Management</div>
        
        <p class="note">Note: this is work in progress. Don't use this page to manage
        users in a production environment. Use the Java client instead.</p>
        
        {
            if(not(xdb:is-admin-user($user))) then
                <div class="error">
                    Only users of the "dba" group can change user settings.
                </div>
            else
                users:process($user)
        }
    </div>
};

declare function users:process($currentUser as xs:string) as element()* {
    let $action := request:request-parameter("action", "")
    return
        if($action eq "Create") then
            users:new-user()
        else if($action eq "Change") then
            users:update-user($currentUser)
        else if($action eq "Remove") then
            users:remove-user($currentUser)
        else
            users:display()
};

declare function users:remove-user($currentUser as xs:string) as element()* {
    let $uid := request:request-parameter("uid", ()),
        $name := doc("/db/system/users.xml")//user[@uid = $uid]/@name cast as xs:string
    return
        if($name eq $currentUser) then
            <div class="error">You cannot remove the current user!</div>
        else (
            xdb:delete-user($name),
            <div class="actions">User {$name} removed.</div>
        ),
        users:display()
};

declare function users:new-user() as element()* {
    let $name := request:request-parameter("name", ()),
        $grp := request:request-parameter("groups", ()),
        $groups :=
            if ($grp) then
                if(contains($grp, ",")) then tokenize($grp, "\s*,\s*")
                else $grp
            else (),
        $pass1 := request:request-parameter("pass1", ""),
        $pass2 := request:request-parameter("pass2", ""),
        $home := request:request-parameter("home", ())
    return
        if($pass1 != $pass2) then (
            <div class="error">Passwords are not identical.</div>,
            users:correct-user(-1, $name, $grp, $home)
        ) else if(empty($groups)) then (
            <div class="error">Please specify one group at least.</div>,
            users:correct-user(-1, $name, $grp, $home)
        ) else (
            xdb:create-user($name, $pass1, $groups, $home),
            users:display()
        )
};

declare function users:update-user($currentUser as xs:string) as element()* {
    let $name := request:request-parameter("name", ()),
        $grp := request:request-parameter("groups", ()),
        $groups :=
            if ($grp) then
                if(contains($grp, ",")) then tokenize($grp, "\s*,\s*")
                else $grp
            else (),
        $pass1 := request:request-parameter("pass1", ""),
        $pass2 := request:request-parameter("pass2", ""),
        $nopass := request:request-parameter("nopass", ()),
        $pass :=
            if($nopass) then () else $pass1,
        $home := request:request-parameter("home", ()),
        $uid := request:request-parameter("uid", "")
    return
        if(not($nopass) and $pass1 ne $pass2) then (
            <div class="error">Passwords are not identical.</div>,
            users:correct-user($uid, $name, $grp, $home)
        ) else if(empty($groups)) then (
            <div class="error">Please specify one group at least.</div>,
            users:correct-user($uid, $name, $grp, $home)
        ) else (
            xdb:change-user($name, $pass, $groups, $home),
            if($currentUser eq $name) then
                request:set-session-attribute("password", $pass)
            else
                (),
            users:display()
        )
};

declare function users:display() as element() {
    <form action="{request:encode-url(request:request-uri())}" method="GET">
        <table cellpadding="5" id="browse">
            <tr>
                <th/>
                <th>Name</th>
                <th>Groups</th>
                <th>Home</th>
            </tr>
            {
                for $user in doc("/db/system/users.xml")//users/user
                let $name := xs:string($user/@name),
                    $groups := string-join($user/group, ", "),
                    $home := if($user/@home) then xs:string($user/@home) else "not set"
                order by $name
                return
                    <tr>
                        <td><input type="radio" name="uid" value="{$user/@uid}"/></td>
                        <td>{$name}</td>
                        <td>{$groups}</td>
                        <td>{$home}</td>
                    </tr>
            }
        </table>
        <table class="actions">
            <tr>
                <td>
                    <input type="submit" name="action" value="Edit"/>
                    <input type="submit" name="action" value="New User"/>
                    <input type="submit" name="action" value="Remove"/>
                </td>
            </tr>
        </table>
        {
                let $action := request:request-parameter("action", ""),
                    $uid := request:request-parameter("uid", "")
                return
                    if ($action eq "Edit") then
                        let $user := doc("/db/system/users.xml")//users/user[@uid = $uid]
                        return
                            users:edit-user(xs:integer($uid), $user/@name, string-join($user/group, ", "), $user/@home)
                    else if($action eq "New User") then
                        users:edit-user(-1, "", "", ())
                    else
                        ()
        }
        <input type="hidden" name="panel" value="users"/>
    </form>
};

declare function users:correct-user($uid as xs:integer, $name as xs:string, $groups as xs:string, $home as xs:string?) as element() {
    <form action="{request:encode-url(request:request-uri())}" method="GET">
        {users:edit-user($uid, $name, $groups, $home)}
        <input type="hidden" name="panel" value="users"/>
    </form>
};
            
declare function users:edit-user($uid as xs:integer, $name as xs:string, $groups as xs:string, $home as xs:string?) as element() {
    <table border="0" cellspacing="5">
        <tr>
            <td>User:</td>
            {
                if($name eq "") then
                    <td colspan="2"><input type="text" name="name"/></td>
                else
                    <td colspan="2"><input type="hidden" name="name" value="{$name}"/>{$name}</td>
            }
        </tr>
        <tr>
            <td>Groups:</td>
            <td><input type="text" name="groups" value="{$groups}"/></td>
            <td class="help">A comma-separated list of groups.
            Note: non-existing groups will be created automatically.</td>
        </tr>
        <tr>
            <td>Password:</td>
            <td colspan="2"><input type="password" name="pass1"/></td>
        </tr>
        <tr>
            <td>Repeat:</td>
            <td colspan="2"><input type="password" name="pass2"/></td>
        </tr>
        {
            if($uid ge 0) then
                <td colspan="3"><input type="checkbox" name="nopass"/> Leave password unchanged.</td>
            else
                ()
        }
        <tr>
            <td>Home Collection:</td>
            <td><input type="text" name="home" value="{$home}"/></td>
            <td class="help">Optional: assign a home collection, e.g. /db/home/me.
            The user will be the owner of this collection.</td>
        </tr>
        <tr>
        {
            if($uid lt 0) then
                <td colspan="3"><input type="submit" name="action" value="Create"/></td>
            else
                <td><input type="submit" name="action" value="Change"/></td>
        }
        </tr>
        <input type="hidden" name="uid" value="{$uid}"/>
    </table>
};
