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

postgresql.conf settings for production from https://pgtune.leopard.in.ua/:
```
# DB Version: 14
# OS Type: linux
# DB Type: oltp
# Total Memory (RAM): 32 GB
# CPUs num: 8
# Connections num: 100
# Data Storage: ssd

max_connections = 100
shared_buffers = 8GB
effective_cache_size = 24GB
maintenance_work_mem = 2GB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200
work_mem = 20971kB
huge_pages = try
min_wal_size = 2GB
max_wal_size = 8GB
max_worker_processes = 8
max_parallel_workers_per_gather = 4
max_parallel_workers = 8
max_parallel_maintenance_workers = 4
```
