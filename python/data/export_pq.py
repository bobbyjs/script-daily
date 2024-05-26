import argparse
import json
import os.path

import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq

'''
convert csv/json to parquet

https://filesampleshub.com/format/code/parquet
'''

__type_mappings = {
    int: pa.int64(),
    str: pa.string(),
    bool: pa.bool_(),
    float: pa.float64()
}


def get_type_and_value(v):
    pa_type = __type_mappings.get(type(v))
    if not pa_type:
        json.dumps(v)
        pa_type = pa.string()
    return pa_type, v


def export(input_file: str, fmt: str, headless: bool, verbose: bool):
    if not fmt:
        fmt = os.path.splitext(input_file)[1][1:]
    if fmt in ['json', 'jsonl']:
        df = parse_jsonl(input_file)
    elif fmt in ['csv', 'tsv']:
        df = parse_csv(input_file, headless)
    else:
        raise ValueError(f"Unsupported format: {fmt}")
    if verbose:
        print(f"df: \n{df}")
        print(f"df[0]: \n{df.values[0]}")
        print([f"{type(v), v}" for v in df.values[0]])
    schema_rows = []
    for i in range(len(df.values[0])):
        v = df.values[0][i]
        col_name = df.columns[i]
        pa_type, _ = get_type_and_value(v)
        schema_rows.append((col_name, pa_type))

    schema = pa.schema(schema_rows)
    if verbose:
        print(f"schema: \n{schema}")
    table = pa.Table.from_pandas(df, schema=schema)

    output_name = f"{os.path.splitext(input_file)[0]}.parquet"
    print("writing to " + output_name)
    pq.write_table(table, output_name)


def parse_jsonl(input_file: str):
    return pd.read_json(input_file, lines=True)


def parse_csv(input_file: str, headless: bool) -> pd.DataFrame:
    if headless:
        df = pd.read_csv(input_file, header=None)
        df.columns = [f"c{i + 1}" for i in df.columns]
        return df
    else:
        return pd.read_csv(input_file, header=None)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', type=str, required=True,
                        help='input file')
    parser.add_argument('-f', '--format', type=str, help='format')
    parser.add_argument('--headless', action='store_true', help='output file')
    parser.add_argument('-v', '--verbose', action='store_true',
                        help='verbose mode')
    args = parser.parse_args()
    export(args.input, args.format, args.headless, args.verbose)
