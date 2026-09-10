package com.citytravel.citytravel.controller;

import com.citytravel.citytravel.common.Result;
import com.citytravel.citytravel.service.AIService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripPlanControllerTest {

    @Test
    void shouldGenerateLocalPlanWithoutLogin() {
        TripPlanController controller = new TripPlanController();
        Map<String, Object> request = validRequest();

        Result<List<Map<String, Object>>> result =
                controller.generatePlan(request, new MockHttpSession());

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(2);
    }

    @Test
    void shouldRejectInvalidTravelDays() {
        TripPlanController controller = new TripPlanController();
        Map<String, Object> request = validRequest();
        request.put("days", 11);

        assertThatThrownBy(() -> controller.generatePlan(request, new MockHttpSession()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1至10天");
    }

    @Test
    void shouldRequireLoginForAiMode() {
        TripPlanController controller = new TripPlanController();
        ReflectionTestUtils.setField(controller, "aiService", fakeAiService());
        Map<String, Object> request = validRequest();
        request.put("useAI", true);

        Result<List<Map<String, Object>>> result =
                controller.generatePlan(request, new MockHttpSession());

        assertThat(result.getCode()).isEqualTo(401);
    }

    @Test
    void shouldParseJsonWrappedInMarkdownFence() {
        TripPlanController controller = new TripPlanController();
        ReflectionTestUtils.setField(controller, "aiService", fakeAiService());

        Result<?> result = controller.generateAIPlanEndpoint(validRequest());

        assertThat(result.getCode()).isEqualTo(200);
        assertThat((List<?>) result.getData()).hasSize(1);
    }

    private Map<String, Object> validRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("days", 2);
        request.put("budget", "budget-medium");
        request.put("destination", "天水");
        request.put("preferences", Arrays.asList("nature", "culture"));
        return request;
    }

    private AIService fakeAiService() {
        return new AIService() {
            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public String generateTripPlan(int days, String budget, List<String> preferences, String destination) {
                return "```json\n{\"plan\":[{\"theme\":\"文化探索\",\"budget\":300," +
                        "\"homestay\":\"测试民宿\",\"activities\":[]}]}\n```";
            }

            @Override
            public String generateDescription(String placeName, String type) {
                return "";
            }

            @Override
            public String generateStrategy(String destination, int days, String preferences) {
                return "";
            }
        };
    }
}
