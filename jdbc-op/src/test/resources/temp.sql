create table $db.$tb (
  #{for column in columns}
  ${column.name} ${column.type},
  #{end}
)

insert into $db.$tb values
#{for value in values}
(value),
#{end}
