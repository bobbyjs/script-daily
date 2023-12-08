from tax import tax_rate
import sys

base_tax_map = [
    (0, 0.03),
    (36000, 0.1),
    (144000, 0.2),
    (300000, 0.25),
    (420000, 0.3),
    (660000, 0.4),
    (960000, 0.45)
]
base_tax_map.reverse()

def total_tax(salary: float, tax_detail: list):
    res = 0
    taxing_salary = salary - 5000 * 12
    for (tax_base, rate) in base_tax_map:
        if taxing_salary > tax_base:
            res += (taxing_salary - tax_base) * rate
            taxing_salary = tax_base
    return res


if __name__ == '__main__':
    argv = sys.argv[1:]
    month_salary = argv[0]
    base = argv[1]
    deduct_rate = argv[2]
    salary = float(month_salary) * float(base)
    salary -= salary * (float(deduct_rate) / 100)

    tax_detail = []
    real_tax = total_tax(salary, tax_detail)
    real_rate = real_tax / salary
    real_salary = salary - real_tax
    print(f"年度总纳税额：{real_tax}, 最终纳税税率：{real_rate}，年度税后工资：{real_salary}")

"""
基本养老保险 base_salary * 4%
基本医疗保险 base_salary * 1%
失业保险 base_salary * 0.25%
住房公积金 base_salary * 3.5%
12%公积金则 11.25%, 7%公积金则7.75%
"""
