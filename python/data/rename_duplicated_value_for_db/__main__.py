#!/usr/bin/env python3

"""
Create by tuke on 2020/10/20

# cd to the parent directory of on20_rename_duplicated_value_for_db/
cd ..
python3 -m on20_rename_duplicated_value_for_db \
  -H x.x.x.x -p xxx -s database_name.table_name -c column_name

"""
import argparse
import pymysql
import sys


def format_value(value):
    if type(value) is str:
        return "'%s'" % value
    else:
        return value


class Main:

    def __init__(self, host, user, password, rename_strategy,
            primary_key='id', has_effect=False, use_timestamp=False,
            time_name=None):
        self.db = None
        self.host = host
        self.user = user
        self.password = password
        self.rename_strategy = rename_strategy
        self.primary_key = primary_key
        self.has_effect = has_effect
        self.use_timestamp = use_timestamp
        self.time_name = time_name

    def __enter__(self):
        self.db = pymysql.connect(host=self.host, user=self.user,
                                  password=self.password)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.db.close()

    def has_unique_column(self, schema, column):
        cursor = self.db.cursor()
        cursor.execute(f'desc {schema}')
        rows = cursor.fetchall()
        for row in rows:
            n = row[0]
            if n != column:
                continue
            return row[3] == 'UNI'
        return None

    def rename(self, schema, column):
        # query duplicated column values
        cursor1 = self.db.cursor()
        cursor1.execute(f'select count({column}) as c, {column} from {schema} '
                        f'group by {column} having c > 1')
        rows1 = cursor1.fetchall()
        if len(rows1) == 0:
            print(
                    f"no duplicated columns in the schema '{schema}' on column '{column}'")
            return
        columns = list(map(lambda row: row[1], rows1))

        # query duplicated column ids
        values = "('%s')" % "','".join(columns)
        cursor2 = self.db.cursor()
        if self.use_timestamp:
            cursor2.execute(
                    f'select {self.primary_key}, {column}, {self.time_name} '
                    f'from {schema} where {column} in {values}')
        else:
            cursor2.execute(f'select {self.primary_key}, {column} '
                            f'from {schema} where {column} in {values}')
        rows2 = cursor2.fetchall()
        column_dict = dict()
        for row in rows2:
            pk = row[0]
            # remove right whitespace since MySQL doesn't distinguish it
            col = row[1].rstrip()
            if self.use_timestamp:
                ts = row[2]
            else:
                ts = None
            pks = column_dict.get(col)
            if pks:
                pks.append((pk, ts))
            else:
                column_dict[col] = [(pk, ts)]

        #  build update sql
        update_sql = ''
        for kv in column_dict.items():
            col, pks = kv
            pks = self.rename_strategy.sorted(pks)
            for pk in pks:
                primary_key, timestamp = pk
                new_name = self.rename_strategy.get_new_name(col, primary_key,
                                                             timestamp)
                if not new_name or new_name == col:
                    continue
                update_sql += f'update {schema} set {column} = {format_value(new_name)} where {self.primary_key} = {format_value(primary_key)};\n'
        if not update_sql:
            print(f'failed to build update sql, column_dict:\n{column_dict}')
            return
        print(update_sql)


if __name__ == '__main__':
    from .rename_strategy import STRATEGY_CONSTRUCTOR_DICT

    parser = argparse.ArgumentParser(
            usage='Rename duplicated value for a database table')
    parser.add_argument('-H', '--host', required=False, default='127.0.0.1',
                        type=str)
    parser.add_argument('-u', '--user', required=False, default='root',
                        type=str)
    parser.add_argument('-p', '--password', required=True, type=str)
    parser.add_argument('-s', '--schema', required=True, type=str,
                        help='database.table')
    parser.add_argument('-c', '--column', required=True, type=str)
    parser.add_argument('-e', '--effect', type=bool, help='perform DML or not')
    parser.add_argument('-S', '--strategy', type=str, help='rename strategy',
                        choices=('seq', 'uuid',), default='seq')
    args = parser.parse_args(sys.argv[1:])

    strategy_constructor = STRATEGY_CONSTRUCTOR_DICT[args.strategy]
    with Main(host=args.host, user=args.user, password=args.password,
              has_effect=args.effect,
              rename_strategy=strategy_constructor()) as main:
        schema_name = args.schema
        column_name = args.column
        try:
            result = main.has_unique_column(schema=schema_name,
                                            column=column_name)
            if result is None:
                print(
                        f'the column {column_name} in schema {schema_name} does not exist',
                        file=sys.stderr)
                exit(0)
            elif result is True:
                print(
                        f'the column {column_name} in schema {schema_name} has a unique index so I gonna skip it')
                exit(0)
        except pymysql.Error:
            print(f'the schema {schema_name} does not exist', file=sys.stderr)
            exit(1)

        main.rename(schema=schema_name, column=column_name)
