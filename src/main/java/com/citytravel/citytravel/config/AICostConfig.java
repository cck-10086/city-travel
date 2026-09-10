package com.citytravel.citytravel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.cost")
public class AICostConfig {
    
    private boolean enabled = true;
    
    private int dailyLimit = 100;
    
    private int hourlyLimit = 10;
    
    private int maxTokens = 2000;
    
    private double costPerToken = 0.00000015;
    
    private boolean cacheEnabled = true;
    
    private int cacheMinutes = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getHourlyLimit() {
        return hourlyLimit;
    }

    public void setHourlyLimit(int hourlyLimit) {
        this.hourlyLimit = hourlyLimit;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getCostPerToken() {
        return costPerToken;
    }

    public void setCostPerToken(double costPerToken) {
        this.costPerToken = costPerToken;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public int getCacheMinutes() {
        return cacheMinutes;
    }

    public void setCacheMinutes(int cacheMinutes) {
        this.cacheMinutes = cacheMinutes;
    }
}