package com.citytravel.citytravel.service;

import com.citytravel.citytravel.config.AICostConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AICostControlService {

    private static final String DAILY_COUNT_KEY = "ai:cost:daily_count:%s";
    private static final String HOURLY_COUNT_KEY = "ai:cost:hourly_count:%s";
    private static final String DAILY_COST_KEY = "ai:cost:daily_cost:%s";
    private static final String CACHE_PREFIX = "ai:cache:";
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private AICostConfig costConfig;

    public boolean checkQuota() {
        if (!costConfig.isEnabled()) {
            return true;
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String currentHour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));

        String dailyKey = String.format(DAILY_COUNT_KEY, today);
        String hourlyKey = String.format(HOURLY_COUNT_KEY, currentHour);

        Long dailyCount = redisTemplate.opsForValue().increment(dailyKey);
        Long hourlyCount = redisTemplate.opsForValue().increment(hourlyKey);

        if (dailyCount == 1) {
            redisTemplate.expire(dailyKey, 1, TimeUnit.DAYS);
        }
        if (hourlyCount == 1) {
            redisTemplate.expire(hourlyKey, 1, TimeUnit.HOURS);
        }

        if (dailyCount > costConfig.getDailyLimit()) {
            return false;
        }
        if (hourlyCount > costConfig.getHourlyLimit()) {
            return false;
        }

        return true;
    }

    public void recordCost(int tokens) {
        if (!costConfig.isEnabled()) {
            return;
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String costKey = String.format(DAILY_COST_KEY, today);

        double cost = tokens * costConfig.getCostPerToken();
        redisTemplate.opsForValue().increment(costKey, cost);
        redisTemplate.expire(costKey, 1, TimeUnit.DAYS);
    }

    public String getCachedResult(String key) {
        if (!costConfig.isCacheEnabled()) {
            return null;
        }
        return redisTemplate.opsForValue().get(CACHE_PREFIX + key);
    }

    public void cacheResult(String key, String result) {
        if (!costConfig.isCacheEnabled()) {
            return;
        }
        redisTemplate.opsForValue().set(CACHE_PREFIX + key, result, costConfig.getCacheMinutes(), TimeUnit.MINUTES);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String currentHour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHH"));

        String dailyKey = String.format(DAILY_COUNT_KEY, today);
        String hourlyKey = String.format(HOURLY_COUNT_KEY, currentHour);
        String costKey = String.format(DAILY_COST_KEY, today);

        Long dailyCount = redisTemplate.opsForValue().get(dailyKey) != null 
            ? Long.parseLong(redisTemplate.opsForValue().get(dailyKey)) : 0L;
        Long hourlyCount = redisTemplate.opsForValue().get(hourlyKey) != null 
            ? Long.parseLong(redisTemplate.opsForValue().get(hourlyKey)) : 0L;
        Double dailyCost = redisTemplate.opsForValue().get(costKey) != null 
            ? Double.parseDouble(redisTemplate.opsForValue().get(costKey)) : 0.0;

        stats.put("dailyCount", dailyCount);
        stats.put("dailyLimit", costConfig.getDailyLimit());
        stats.put("hourlyCount", hourlyCount);
        stats.put("hourlyLimit", costConfig.getHourlyLimit());
        stats.put("dailyCost", String.format("%.4f", dailyCost));
        stats.put("maxTokens", costConfig.getMaxTokens());
        stats.put("costControlEnabled", costConfig.isEnabled());
        stats.put("cacheEnabled", costConfig.isCacheEnabled());

        return stats;
    }

    public void resetDailyStats() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dailyKey = String.format(DAILY_COUNT_KEY, today);
        String costKey = String.format(DAILY_COST_KEY, today);
        
        redisTemplate.delete(dailyKey);
        redisTemplate.delete(costKey);
    }
}