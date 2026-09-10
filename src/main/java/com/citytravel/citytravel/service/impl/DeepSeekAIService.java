package com.citytravel.citytravel.service.impl;

import com.citytravel.citytravel.config.AICostConfig;
import com.citytravel.citytravel.service.AICostControlService;
import com.citytravel.citytravel.service.AIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DeepSeekAIService implements AIService {

    @Value("${ai.deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;
    
    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Autowired(required = false)
    private AICostControlService costControlService;
    
    @Autowired(required = false)
    private AICostConfig costConfig;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public DeepSeekAIService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String generateTripPlan(int days, String budget, List<String> preferences, String destination) {
        String budgetDesc = getBudgetDescription(budget);
        String preferencesDesc = String.join(", ", preferences);
        
        String prompt = "You are a professional travel planner. Please generate a detailed travel plan based on the following information:\n\n" +
                "Destination: " + destination + "\n" +
                "Travel Days: " + days + "\n" +
                "Budget Level: " + budgetDesc + "\n" +
                "Preferences: " + preferencesDesc + "\n\n" +
                "Please output the travel plan in JSON format:\n" +
                "{\n" +
                "  \"plan\": [\n" +
                "    {\n" +
                "      \"day\": 1,\n" +
                "      \"theme\": \"Theme Name\",\n" +
                "      \"activities\": [\n" +
                "        {\n" +
                "          \"name\": \"Attraction Name\",\n" +
                "          \"description\": \"Description\",\n" +
                "          \"location\": \"Location\",\n" +
                "          \"duration\": 2,\n" +
                "          \"cost\": 100,\n" +
                "          \"tips\": \"Tips\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"homestay\": \"Recommended Accommodation\",\n" +
                "      \"budget\": 500\n" +
                "    }\n" +
                "  ],\n" +
                "  \"totalBudget\": 1500,\n" +
                "  \"tips\": \"Travel Tips\"\n" +
                "}";

        String cacheKey = "trip_plan_" + destination + "_" + days + "_" + budget + "_" + preferencesDesc.hashCode();
        return callDeepSeekAPIWithCache(prompt, cacheKey);
    }

    @Override
    public String generateDescription(String placeName, String type) {
        String prompt = "Please generate an attractive description for this " + type + " attraction:\n\n" +
                "Attraction Name: " + placeName + "\n\n" +
                "Requirements:\n" +
                "1. Vivid and interesting description highlighting unique features\n" +
                "2. Include historical background or unique aspects\n" +
                "3. Suitable for tourism promotion\n" +
                "4. 100-200 words";

        String cacheKey = "description_" + placeName + "_" + type;
        return callDeepSeekAPIWithCache(prompt, cacheKey);
    }

    @Override
    public String generateStrategy(String destination, int days, String preferences) {
        String prompt = "Please generate a " + days + "-day travel guide for " + destination + ".\n\n" +
                "User Preferences: " + preferences + "\n\n" +
                "The guide should include:\n" +
                "1. Daily itinerary (attractions, dining, transportation)\n" +
                "2. Must-experience activities\n" +
                "3. Food recommendations\n" +
                "4. Accommodation suggestions\n" +
                "5. Budget reference\n" +
                "6. Travel tips\n\n" +
                "Please output in detailed Markdown format.";

        String cacheKey = "strategy_" + destination + "_" + days + "_" + preferences.hashCode();
        return callDeepSeekAPIWithCache(prompt, cacheKey);
    }

    private String callDeepSeekAPIWithCache(String prompt, String cacheKey) {
        if (costControlService != null) {
            String cached = costControlService.getCachedResult(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        String result = callDeepSeekAPI(prompt);
        
        if (costControlService != null && !isErrorResponse(result)) {
            costControlService.cacheResult(cacheKey, result);
        }
        
        return result;
    }

    private String callDeepSeekAPI(String prompt) {
        if (!isConfigured()) {
            return errorJson("AI服务未配置");
        }

        if (costControlService != null && !costControlService.checkQuota()) {
            return errorJson("AI调用次数已达上限，请稍后再试");
        }

        try {
            int maxTokens = costConfig != null ? costConfig.getMaxTokens() : 2000;
            
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> requestMessage = new LinkedHashMap<>();
            requestMessage.put("role", "user");
            requestMessage.put("content", prompt);
            messages.add(requestMessage);
            payload.put("messages", messages);
            payload.put("temperature", 0.7);
            payload.put("max_tokens", maxTokens);
            String jsonBody = objectMapper.writeValueAsString(payload);

            RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return errorJson("AI服务暂时不可用（HTTP " + response.code() + "）");
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode choices = root.get("choices");
                
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null && message.has("content")) {
                        String content = message.get("content").asText();
                        
                        if (costControlService != null) {
                            int tokens = estimateTokens(prompt + content);
                            costControlService.recordCost(tokens);
                        }
                        
                        return content;
                    }
                }
                
                return errorJson("AI服务返回格式异常");
            }
        } catch (Exception e) {
            return errorJson("AI服务请求失败，请稍后重试");
        }
    }

    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int characters = text.getBytes(StandardCharsets.UTF_8).length;
        return (int) Math.ceil(characters / 3.0);
    }

    private String getBudgetDescription(String budget) {
        switch (budget) {
            case "budget-low": return "Economy (about 200-300 RMB/day)";
            case "budget-medium": return "Comfort (about 500-800 RMB/day)";
            case "budget-high": return "Luxury (about 1000+ RMB/day)";
            default: return "Medium budget";
        }
    }

    private boolean isErrorResponse(String result) {
        try {
            return objectMapper.readTree(result).has("error");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String errorJson(String message) {
        try {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("error", message);
            return objectMapper.writeValueAsString(error);
        } catch (Exception ignored) {
            return "{\"error\":\"AI服务异常\"}";
        }
    }
}
