## demo

### prepare env

```shell
mkdir mysql57
docker run -itd --name mysql57 \
 -e MYSQL_ROOT_PASSWORD=root \
 -v $PWD/mysql57:/var/lib/mysql \
 -p 3306:3306 \
 mysql:5.7

mkdir mysql
docker run -itd --name mysql \
 -e MYSQL_ROOT_PASSWORD=root \
 -v $PWD/mysql:/var/lib/mysql \
 -p 3307:3306 \
 mysql
 
mkdir postgres15
docker run -d -it --name postgres15 \
 -e POSTGRES_PASSWORD=root \
 -e ALLOW_IP_RANGE=0.0.0.0/0 \
 -v $PWD/postgres15:/var/lib/postgresql/data \
 -p 5432:5432 \
 postgres:15
```

```sql
create user myuser with password 'mypassword';
create database testdb
with owner=myuser template=template0
encoding='UTF8' connection limit = 100;
grant all privileges on database testdb to myuser;
-- \c testdb
create schema test;
alter schema test owner to myuser;
grant all privileges on all tables in schema public to myuser;
grant all privileges on all tables in schema test to myuser;
```

### export-jdbc

#### mysql
```shell
jdbc-op type-table test.t_base \
    -j jdbc:mysql://127.0.0.1:3306 -u root -p root \
    --dp $HOME/.m2/repository/mysql/mysql-connector-java/8.0.27 --dc com.mysql.cj.jdbc.Driver \
    -S mysql -f ./jdbc-op/src/test/resources/mysql-types.txt -y

jdbc-op export-jdbc --databases test \
    --j1 jdbc:mysql://127.0.0.1:3306 --u1 root --p1 root \
    --dp1 $HOME/.m2/repository/mysql/mysql-connector-java/8.0.27 --dc1 com.mysql.cj.jdbc.Driver \
    --j2 jdbc:mysql://127.0.0.1:3307 --u2 root --p2 root \
    -S mysql --verbose -y
```

#### postgres

```shell
jdbc-op type-table test.t_base \
    -j jdbc:postgresql://127.0.0.1:5432/testdb -u myuser -p mypassword \
    --dp $HOME/.m2/repository/org/postgresql/postgresql/42.2.19 --dc org.postgresql.Driver \
    -S postgres -f ./jdbc-op/src/test/resources/postgresql-types.txt -y

jdbc-op export-jdbc --databases test --catalog testdb --use-metadata \
    --j1 jdbc:postgresql://127.0.0.1:5432/testdb --u1 myuser --p1 mypassword \
    --dp1 $HOME/.m2/repository/org/postgresql/postgresql/42.2.19 --dc1 org.postgresql.Driver \
    --j2 jdbc:postgresql://127.0.0.1:5433/testdb --u2 myuser --p2 mypassword \
    -S postgres --verbose -y    
```
