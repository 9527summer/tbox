-- Lua script to allocate a nodeId using ZSET
-- KEYS[1]: ZSET Key (e.g., tbox:ids:registry:{appName})
-- ARGV[1]: Now (ms)
-- ARGV[2]: Expire Duration (ms)
-- ARGV[3]: Max Node ID (e.g., 1023)

local key = KEYS[1]
local now = tonumber(ARGV[1])
local expireDuration = tonumber(ARGV[2])
local maxId = tonumber(ARGV[3])
local newScore = now + expireDuration

-- Iterate from 0 to maxId to find an available slot
-- Strategy: Check score. If score is nil (not exists) or score < now (expired), take it.
for id = 0, maxId do
    local score = redis.call('ZSCORE', key, id)
    if not score or tonumber(score) < now then
        -- Found available ID (either empty or expired)
        redis.call('ZADD', key, newScore, id)
        return id
    end
end

-- No ID available
return -1