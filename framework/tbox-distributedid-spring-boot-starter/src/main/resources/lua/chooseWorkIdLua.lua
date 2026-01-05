-- Lua script to allocate a nodeId using ZSET
-- KEYS[1]: ZSET Key (e.g., tbox:ids:registry:{appName})
-- ARGV[1]: Now (ms)
-- ARGV[2]: Expire Duration (ms)
-- ARGV[3]: Max Node ID (e.g., 1023)

local key = KEYS[1]
local function toNumber(v)
    if not v then
        return nil
    end
    -- 某些 RedisTemplate 的参数序列化可能会把数字序列化成带引号的 JSON 字符串，比如："123"
    if type(v) == "string" then
        v = string.gsub(v, "^%s*\"?", "")
        v = string.gsub(v, "\"?%s*$", "")
    end
    return tonumber(v)
end

local now = toNumber(ARGV[1])
local expireDuration = toNumber(ARGV[2])
local maxId = toNumber(ARGV[3])

if not now or not expireDuration or not maxId then
    return redis.error_reply("invalid args: now/expireDuration/maxId must be numbers")
end
local newScore = now + expireDuration

-- Iterate from 0 to maxId to find an available slot
-- Strategy: Check score. If score is nil (not exists) or score < now (expired), take it.
for id = 0, maxId do
    local member = tostring(id)
    local score = redis.call('ZSCORE', key, member)
    if not score or tonumber(score) < now then
        -- Found available ID (either empty or expired)
        redis.call('ZADD', key, newScore, member)
        return id
    end
end

-- No ID available
return -1
