
# 应纳税所得额
taxable_income_template = "%8d(应纳税所得额) = %8d(累计收入) - %6d(累计减除费用) - %6d(累计专项扣除) - %6d(累计专项附加扣除)"
# 应纳税额
payable_template = "%8.f(应纳税额) = %8d(应纳税所得额) * %2d%%(税率) - %6d(速算扣除数) - %f(已预缴预扣税额)"

def tax_rate(s):
  if (s <= 36000):
    return 0.03, 0
  elif (s < 144000):
    return 0.1, 2520
  elif (s < 300000):
    return 0.2, 16920
  elif (s < 420000):
    return 0.25, 31920
  elif (s < 660000):
    return 0.3, 52920
  elif (s < 960000):
    return 0.35, 85920
  else:
    return 0.45, 181920

"""
count the rax

:param paid: 已预缴预扣税额
:param income: 累计收入
:param base: 累计减除费用
:param deduction: 累计专项扣除 
:param additional_deduction: 累计专项附加扣除a
"""
def tax_count(paid, income, base, deduction, additional_deduction):
  # 计算应纳税所得额
  taxable_income = income - base - deduction - additional_deduction
  print(taxable_income_template % (taxable_income, income, base, deduction, additional_deduction))

  # 获取税率和速算扣除数
  rate, exclusion = tax_rate(taxable_income)
  # 计算新的已预缴预扣税额
  next_paid = taxable_income * rate
  tax = next_paid - exclusion - paid
  print(payable_template % (tax, taxable_income, rate * 100, exclusion, paid))
  print()
  return tax, next_paid

# 12个月的工资
m = 20000
m1 = m2 = m3 = m4 = m5 = m6 = m7 = m8 = m9 = m10 = m11 = m
m12 = m + m * 3
# 累计专项扣除
d = 0.12 * m + m * 0.6 * 0.1
# 累计专项附加扣除
ad = 1500
# 累计减除费用
b = 5000

t = [0,0,0,0,0,0,0,0,0,0,0,0]
# 一月份
t[0] = tax_count( 0, m1, 1 * b, 1 * d, 1 * ad )
# 二月份
t[1] = tax_count( t[0][1], m1 + m2, 2 * b, 2 * d, 2 * ad )
# 三月份
t[2] = tax_count( t[1][1], m1 + m2 + m3, 3 * b, 3 * d, 3 * ad )
# 四月份
t[3] = tax_count( t[2][1], m1 + m2 + m3 + m4, 4 * b, 4 * d, 4 * ad )
# 五月份
t[4] = tax_count( t[3][1], m1 + m2 + m3 + m4 + m5, 5 * b, 5 * d, 5 * ad )
# 六月份
t[5] = tax_count( t[4][1], m1 + m2 + m3 + m4 + m5 + m6, 6 * b, 6 * d, 6 * ad )
# 七月份
t[6] = tax_count( t[5][1], m1 + m2 + m3 + m4 + m5 + m6 + m7, 7 * b, 7 * d, 7 * ad )
# 八月份
t[7] = tax_count( t[6][1], m1 + m2 + m3 + m4 + m5 + m6 + m7 + m8, 8 * b, 8 * d, 8 * ad )
# 九月份
t[8] = tax_count( t[7][1], m1 + m2 + m3 + m4 + m5 + m6 + m7 + m8 + m9, 9 * b, 9 * d, 9 * ad )
# 十月份
t[9] = tax_count( t[8][1], m1 + m2 + m3 + m4 + m5 + m6 + m7 + m8 + m9 + m10, 10 * b, 10 * d, 10 * ad )
# 十一月份
t[10] = tax_count( t[9][1], m1 + m2 + m3 + m4 + m5 + m6 + m7 + m8 + m9 + m10 + m11, 11 * b, 11 * d, 11 * ad )
# 十二月份
t[11] = tax_count( t[10][1], m1 + m2 + m3 + m4 + m5 + m6 + m7 + m8 + m9 + m10 + m11 + m12, 12 * b, 12 * d, 12 * ad )

i = 0
total = 0
for (tax, paid) in t:
  i += 1
  total += tax

print("total %.0f" % total)
print("  avg %.0f" % (total/ 12))
