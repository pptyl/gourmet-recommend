-- =====================================================================
-- 优惠券秒杀 Lua 脚本（原子执行）
-- KEYS[1] = 库存 key   seckill:stock:{couponId}
-- KEYS[2] = 已购用户集合 key   seckill:users:{couponId}
-- ARGV[1] = 用户 ID
--
-- 返回值：1 抢购成功 -1 库存不足 -2 重复领取 -3 活动未初始化
-- =====================================================================
local stockKey = KEYS[1]
local userSetKey = KEYS[2]
local userId = ARGV[1]

-- 一人一单校验
if redis.call('SISMEMBER', userSetKey, userId) == 1 then
    return -2
end

local stock = redis.call('GET', stockKey)
if not stock then
    return -3
end

if tonumber(stock) <= 0 then
    return -1
end

redis.call('DECR', stockKey)
redis.call('SADD', userSetKey, userId)
return 1
