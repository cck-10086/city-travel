package com.citytravel.citytravel.service;

import java.util.List;
import java.util.Map;

public interface AIService {
    boolean isConfigured();

    String generateTripPlan(int days, String budget, List<String> preferences, String destination);
    
    String generateDescription(String placeName, String type);
    
    String generateStrategy(String destination, int days, String preferences);
}
