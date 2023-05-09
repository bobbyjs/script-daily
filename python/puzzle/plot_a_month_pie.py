import os

month_pie='''
                 *+
               @&
             ^+^
          -)~
        %$
      {!=
    *&
   ^@
  +#
 &*
$-
'''
i = 0
for line in month_pie.split('\n'):
    color = 31 + i % 6
    os.system("echo '\033[%sm %s \033[0m'" % (color, line))
    i += 1