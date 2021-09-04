#coding=utf-8
a = (1..10).to_a
card = a + a + a +a;
ca  = card.combination(2).to_a
# 庄家入账总合
$sums = 0
# 玩家入账总合
$sumc = 0
for i in 1..100000
    # 随机抽取的两组牌
	xc = ca.sample
	xs = ca.sample
	if xc[0] == xc[1]
		if xs[0] == xs[1]
			# 都是对子时
			case xs[0] <=> xc[0]
				when 1, 0 then $sums += 1
				else $sumc += 2	
			end
		else
			$sumc += 2
		end
	else
		if xs[0] == xs[1]
			$sums += 1
		else
			# 都不是对子时
			cn = (xc[0] + xc[1]) % 10
			sn = (xs[0] + xs[1]) % 10
			case sn <=> cn
				when 1, 0 then $sums += 1
				else $sumc += 1		
			end
		end
	end
end

print '庄家入账总合=', $sums, ';  玩家入账总合=', $sumc, ' 玩家胜率=', "%6s" % (100 * $sumc / ($sumc + $sums + 0.0)), '%'



