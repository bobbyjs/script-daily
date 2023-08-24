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

### export-jdbc

**1. make some data to source**

```shell
jdbc-op type-table test.t_base \
    -j jdbc:mysql://127.0.0.1:3306 -u root -p root \
    --dp $HOME/.m2/repository/mysql/mysql-connector-java/8.0.27 --dc com.mysql.cj.jdbc.Driver \
    -S mysql -f ./jdbc-op/src/test/resources/mysql-types.txt -y
```

**2. migrate**

```shell
jdbc-op export-jdbc --databases test \
    --j1 jdbc:mysql://127.0.0.1:3306 --u1 root --p1 root \
    --dp1 $HOME/.m2/repository/mysql/mysql-connector-java/8.0.27 --dc1 com.mysql.cj.jdbc.Driver \
    --j2 jdbc:mysql://127.0.0.1:3307 --u2 root --p2 root \
    -S mysql --verbose -y
```

### type-table

```shell
jdbc-op type-table t_base \
    -j jdbc:postgresql://127.0.0.1:5432/postgres -u postgres -p root \
    --dp $HOME/.m2/repository/org/postgresql/postgresql/42.2.19 --dc org.postgresql.Driver \
    -S postgres -f ./jdbc-op/src/test/resources/postgresql-types.txt -y
```
