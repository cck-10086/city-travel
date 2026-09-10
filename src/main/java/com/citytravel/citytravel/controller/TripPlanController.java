package com.citytravel.citytravel.controller;

import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.service.AIService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api/trip-plan")
public class TripPlanController {

    private static final Set<String> ALLOWED_BUDGETS =
            new HashSet<>(Arrays.asList("budget-low", "budget-medium", "budget-high"));
    private static final Set<String> ALLOWED_PREFERENCES =
            new HashSet<>(Arrays.asList("nature", "culture", "relax", "shopping", "food"));

    private static final List<Map<String, Object>> ATTRACTIONS = Arrays.asList(
        mapOf("name", "麦积山石窟", "type", "culture", "location", "天水市麦积区", "budget", 80, "duration", 3),
        mapOf("name", "伏羲庙", "type", "culture", "location", "天水市秦州区", "budget", 40, "duration", 2),
        mapOf("name", "南郭寺", "type", "culture", "location", "天水市秦州区", "budget", 30, "duration", 2),
        mapOf("name", "玉泉观", "type", "culture", "location", "天水市秦州区", "budget", 20, "duration", 1),
        mapOf("name", "仙人崖", "type", "nature", "location", "天水市麦积区", "budget", 40, "duration", 4),
        mapOf("name", "石门山", "type", "nature", "location", "天水市麦积区", "budget", 30, "duration", 3),
        mapOf("name", "曲溪", "type", "nature", "location", "天水市麦积区", "budget", 25, "duration", 2),
        mapOf("name", "温泉度假村", "type", "relax", "location", "天水市麦积区", "budget", 120, "duration", 4),
        mapOf("name", "天水古城", "type", "shopping", "location", "天水市秦州区", "budget", 0, "duration", 2),
        mapOf("name", "伏羲广场", "type", "food", "location", "天水市秦州区", "budget", 50, "duration", 2)
    );

    private static final List<String> HOMESTAYS = Arrays.asList(
        "天水大酒店", "阳光饭店", "麦积山温泉酒店", "天水宾馆", "羲皇大酒店"
    );

    @Autowired(required = false)
    private AIService aiService;
    
    @Autowired(required = false)
    private com.citytravel.citytravel.service.AICostControlService costControlService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/generate")
    public Result<List<Map<String, Object>>> generatePlan(@RequestBody Map<String, Object> request,
                                                          HttpSession session) {
        TripPlanRequest params = parseRequest(request);

        boolean useAI = Boolean.parseBoolean(String.valueOf(request.getOrDefault("useAI", false)));
        
        if (useAI && aiService != null) {
            if (session.getAttribute("user") == null) {
                return Result.error(401, "使用AI行程规划前请先登录");
            }
            return generateAIPlan(params.days, params.budget, params.preferences, params.destination);
        }

        int budgetPerDay = getBudgetPerDay(params.budget);
        List<Map<String, Object>> plan = generateTripPlan(params.days, budgetPerDay, params.preferences);
        
        return Result.success(plan);
    }

    @PostMapping("/generate/ai")
    public Result<?> generateAIPlanEndpoint(@RequestBody Map<String, Object> request) {
        TripPlanRequest params = parseRequest(request);
        return generateAIPlan(params.days, params.budget, params.preferences, params.destination);
    }

    private Result<List<Map<String, Object>>> generateAIPlan(int days, String budget, List<String> preferences, String destination) {
        if (aiService == null) {
            return Result.error("AI服务未配置");
        }

        try {
            String aiResponse = aiService.generateTripPlan(days, budget, preferences, destination);
            
            JsonNode root = objectMapper.readTree(stripMarkdownFence(aiResponse));
            
            if (root.has("error")) {
                return Result.error(root.get("error").asText());
            }

            List<Map<String, Object>> plan = new ArrayList<>();
            JsonNode planNode = root.get("plan");
            if (planNode == null || !planNode.isArray() || planNode.size() == 0) {
                return Result.error("AI响应中缺少有效行程");
            }

            if (planNode.isArray()) {
                for (JsonNode dayNode : planNode) {
                    Map<String, Object> dayPlan = new HashMap<>();
                    dayPlan.put("theme", dayNode.has("theme") ? dayNode.get("theme").asText() : "");
                    dayPlan.put("budget", dayNode.has("budget") ? dayNode.get("budget").asInt() : 0);
                    dayPlan.put("homestay", dayNode.has("homestay") ? dayNode.get("homestay").asText() : "");
                    
                    List<Map<String, Object>> activities = new ArrayList<>();
                    JsonNode activitiesNode = dayNode.get("activities");
                    if (activitiesNode != null && activitiesNode.isArray()) {
                        for (JsonNode activityNode : activitiesNode) {
                            Map<String, Object> activity = new HashMap<>();
                            activity.put("name", activityNode.has("name") ? activityNode.get("name").asText() : "");
                            activity.put("description", activityNode.has("description") ? activityNode.get("description").asText() : "");
                            activity.put("location", activityNode.has("location") ? activityNode.get("location").asText() : "");
                            activity.put("time", activityNode.has("duration") ? activityNode.get("duration").asInt() + "小时" : "");
                            activity.put("price", activityNode.has("cost") ? activityNode.get("cost").asInt() : 0);
                            activity.put("tips", activityNode.has("tips") ? activityNode.get("tips").asText() : "");
                            activities.add(activity);
                        }
                    }
                    dayPlan.put("activities", activities);
                    plan.add(dayPlan);
                }
            }
            
            return Result.success(plan);
        } catch (JsonProcessingException e) {
            return Result.error("AI响应解析失败，请重新生成");
        } catch (Exception e) {
            return Result.error("AI生成失败，请稍后重试");
        }
    }

    @GetMapping("/ai/status")
    public Result<Map<String, Object>> checkAIStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean available = aiService != null && aiService.isConfigured();
        status.put("available", available);
        status.put("message", available ? "AI服务已配置" : "AI服务未配置，请设置环境变量");
        
        return Result.success(status);
    }
    
    @GetMapping("/ai/cost")
    public Result<Map<String, Object>> getCostStatistics() {
        if (costControlService != null) {
            return Result.success(costControlService.getStatistics());
        }
        return Result.error("成本控制服务未配置");
    }

    private int getBudgetPerDay(String budget) {
        switch (budget) {
            case "budget-low": return 200;
            case "budget-medium": return 500;
            case "budget-high": return 1000;
            default: return 500;
        }
    }

    private TripPlanRequest parseRequest(Map<String, Object> request) {
        Object daysValue = request.get("days");
        if (daysValue == null) {
            throw new IllegalArgumentException("旅行天数不能为空");
        }

        int days;
        try {
            days = Integer.parseInt(String.valueOf(daysValue));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("旅行天数必须是整数");
        }
        if (days < 1 || days > 10) {
            throw new IllegalArgumentException("旅行天数必须在1至10天之间");
        }

        String budget = String.valueOf(request.getOrDefault("budget", "budget-medium"));
        if (!ALLOWED_BUDGETS.contains(budget)) {
            throw new IllegalArgumentException("预算类型无效");
        }

        String destination = String.valueOf(request.getOrDefault("destination", "天水")).trim();
        if (destination.isEmpty() || destination.length() > 50) {
            throw new IllegalArgumentException("目的地长度必须在1至50个字符之间");
        }

        List<String> preferences = new ArrayList<>();
        Object preferencesValue = request.get("preferences");
        if (preferencesValue instanceof List) {
            for (Object preference : (List<?>) preferencesValue) {
                String value = String.valueOf(preference);
                if (ALLOWED_PREFERENCES.contains(value) && !preferences.contains(value)) {
                    preferences.add(value);
                }
            }
        }
        if (preferences.isEmpty()) {
            preferences.add("nature");
            preferences.add("culture");
        }
        return new TripPlanRequest(days, budget, destination, preferences);
    }

    private String stripMarkdownFence(String response) {
        String normalized = response == null ? "" : response.trim();
        if (normalized.startsWith("```")) {
            int firstLineEnd = normalized.indexOf('\n');
            int lastFence = normalized.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                normalized = normalized.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        return normalized;
    }

    private static final class TripPlanRequest {
        private final int days;
        private final String budget;
        private final String destination;
        private final List<String> preferences;

        private TripPlanRequest(int days, String budget, String destination, List<String> preferences) {
            this.days = days;
            this.budget = budget;
            this.destination = destination;
            this.preferences = preferences;
        }
    }

    private List<Map<String, Object>> generateTripPlan(int days, int budgetPerDay, List<String> preferences) {
        List<Map<String, Object>> plan = new ArrayList<>();
        
        List<Map<String, Object>> filteredAttractions = ATTRACTIONS.stream()
            .filter(a -> preferences.contains(a.get("type")))
            .collect(java.util.stream.Collectors.toList());
        
        if (filteredAttractions.isEmpty()) {
            filteredAttractions = new ArrayList<>(ATTRACTIONS);
        }
        
        Collections.shuffle(filteredAttractions);
        
        String[] themes = {"文化探索", "自然风光", "休闲放松", "美食之旅", "购物体验"};
        Random random = new Random();
        
        int attractionIndex = 0;
        for (int day = 1; day <= days; day++) {
            Map<String, Object> dayPlan = new HashMap<>();
            dayPlan.put("theme", themes[random.nextInt(themes.length)]);
            
            List<Map<String, Object>> activities = new ArrayList<>();
            int dailyBudget = 0;
            int dailyDuration = 0;
            
            while (dailyDuration < 6 && attractionIndex < filteredAttractions.size()) {
                Map<String, Object> attraction = filteredAttractions.get(attractionIndex);
                int duration = (Integer) attraction.get("duration");
                int cost = (Integer) attraction.get("budget");
                
                if (dailyDuration + duration <= 8 && dailyBudget + cost <= budgetPerDay) {
                    activities.add(mapOf(
                        "name", attraction.get("name"),
                        "description", getDescription((String) attraction.get("name"), (String) attraction.get("type")),
                        "location", attraction.get("location"),
                        "duration", duration,
                        "cost", cost
                    ));
                    dailyBudget += cost;
                    dailyDuration += duration;
                    attractionIndex++;
                } else {
                    break;
                }
            }
            
            if (activities.isEmpty()) {
                activities.add(mapOf(
                    "name", "自由活动",
                    "description", "今天可以自由安排行程，探索当地特色",
                    "location", "酒店周边",
                    "duration", 4,
                    "cost", 0
                ));
            }
            
            dayPlan.put("activities", activities);
            dayPlan.put("homestay", HOMESTAYS.get(random.nextInt(HOMESTAYS.size())));
            dayPlan.put("budget", dailyBudget + getAccommodationCost(budgetPerDay));
            
            plan.add(dayPlan);
        }
        
        return plan;
    }

    private int getAccommodationCost(int budgetPerDay) {
        return (int) (budgetPerDay * 0.4);
    }

    private String getDescription(String name, String type) {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("麦积山石窟", "中国四大石窟之一，拥有丰富的佛教艺术珍品");
        descriptions.put("伏羲庙", "纪念人文始祖伏羲的庙宇，建筑宏伟壮观");
        descriptions.put("南郭寺", "千年古寺，自然风光与人文景观完美结合");
        descriptions.put("玉泉观", "道教圣地，山势巍峨，风景秀丽");
        descriptions.put("仙人崖", "自然风光优美，有仙人指路等著名景点");
        descriptions.put("石门山", "山形如门，景色宜人，适合徒步");
        descriptions.put("曲溪", "溪水潺潺，森林茂密，避暑胜地");
        descriptions.put("温泉度假村", "享受温泉养生，放松身心");
        descriptions.put("天水古城", "历史文化街区，购物餐饮一应俱全");
        descriptions.put("伏羲广场", "品尝天水特色美食，感受当地风情");
        return descriptions.getOrDefault(name, "值得一游的好去处");
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return map;
    }

    private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
}
