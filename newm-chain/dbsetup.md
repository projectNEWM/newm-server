```
$ sudo -i -u postgres
$ createuser --interactive
Enter name of role to add: newmchain
Shall the new role be a superuser? (y/n) n
Shall the new role be allowed to create databases? (y/n) n
Shall the new role be allowed to create more new roles? (y/n) n
$ psql
psql (14.5 (Ubuntu 14.5-0ubuntu0.22.04.1))
Type "help" for help.

postgres=# CREATE DATABASE newmchain;
CREATE DATABASE
postgres=# ALTER USER newmchain WITH PASSWORD '*****************';
ALTER ROLE
postgres=# ALTER USER newmchain VALID UNTIL 'infinity';
ALTER ROLE
postgres=# ALTER DATABASE newmchain OWNER TO newmchain;
ALTER DATABASE
postgres=# \q
$ exit
```