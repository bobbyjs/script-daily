
```shell
# delete chars for dir
fs-op rename . --trim-char "《》" -t d -s "《.*"
# rename suck as: "a b c" to "a.b.c"
fs-op rename . -s '.* .*' --tr ' .'
```
