package com.sang.be_capnong.service.mealplan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.service.mealplan.GeneratedPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Primary
@Slf4j
public class GeminiMealPlanAiClient implements MealPlanAiClient {

    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final MealPlanAiClient fallbackClient;
    private final RestClient restClient;

    public GeminiMealPlanAiClient(
            @Value("${google.gemini.api-key:${GEMINI_API_KEY:${GEMINI_KEY_1:}}}") String apiKey,
            @Value("${google.gemini.model:gemini-1.5-flash}") String model,
            @Value("${google.gemini.timeout-seconds:15}") int timeoutSeconds,
            ObjectMapper objectMapper,
            @Qualifier("fakeMealPlanAiClient") MealPlanAiClient fallbackClient
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-1.5-flash";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 15;
        this.objectMapper = objectMapper;
        this.fallbackClient = fallbackClient;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(this.timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(this.timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public ProfileExtraction extractProfile(ExtractionInput in) {
        if (apiKey.isBlank()) {
            log.info("Gemini API key not configured, falling back to FakeMealPlanAiClient for extraction");
            return fallbackClient.extractProfile(in);
        }

        try {
            String prompt = buildExtractionPrompt(in);
            String jsonResponse = callGemini(prompt);
            return objectMapper.readValue(jsonResponse, ProfileExtraction.class);
        } catch (Exception e) {
            log.warn("Gemini call failed for extractProfile: {}, falling back to deterministic fake client", e.getMessage());
            return fallbackClient.extractProfile(in);
        }
    }

    @Override
    public GeneratedPlan generatePlan(GenerationInput in) {
        if (apiKey.isBlank()) {
            log.info("Gemini API key not configured, falling back to FakeMealPlanAiClient for plan generation");
            return fallbackClient.generatePlan(in);
        }

        try {
            String prompt = buildGenerationPrompt(in);
            String jsonResponse = callGemini(prompt);
            return objectMapper.readValue(jsonResponse, GeneratedPlan.class);
        } catch (Exception e) {
            log.warn("Gemini call failed for generatePlan: {}, falling back to deterministic fake client", e.getMessage());
            return fallbackClient.generatePlan(in);
        }
    }

    private String callGemini(String prompt) {
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                model, apiKey
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json",
                        "temperature", 0.2
                )
        );

        String response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractTextFromGeminiResponse(response);
    }

    private String extractTextFromGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new IllegalStateException("Empty text in Gemini response");
            }
            return textNode.asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }

    private String buildExtractionPrompt(ExtractionInput in) {
        return """
                You are a medical nutrition assistant for an agricultural marketplace.
                A buyer wrote the following free text about their health, diet, or meal planning:
                \"\"\"%s\"\"\"
                
                Known catalog of allergens: %s
                Known catalog of health conditions: %s
                Buyer's current registered allergies: %s
                Buyer's current registered health conditions: %s
                
                Rules:
                1. Map synonyms to the EXACT names from the known lists (e.g. 'lạc' -> 'Đậu phộng', 'đau dạ dày' -> 'Đau dạ dày').
                2. Never invent items outside the known lists unless strongly evident.
                3. Words like 'hết', 'không còn', 'khỏi' -> put in remove_* arrays.
                4. Output JSON ONLY matching this exact structure:
                {
                  "add_conditions": ["..."],
                  "remove_conditions": ["..."],
                  "add_allergies": ["..."],
                  "remove_allergies": ["..."],
                  "plan_request": {
                    "number_of_days": 2,
                    "number_of_people": null,
                    "target_budget": null,
                    "goal": "ăn khỏe",
                    "meal_types": []
                  },
                  "needs_clarification": null
                }
                If the text is ambiguous, set needs_clarification instead of guessing.
                """.formatted(
                in.getText(),
                in.getKnownAllergens(),
                in.getKnownConditions(),
                in.getCurrentAllergies(),
                in.getCurrentConditions()
        );
    }

    private String buildGenerationPrompt(GenerationInput in) {
        try {
            String candidatesJson = objectMapper.writeValueAsString(in.getCandidates());
            return """
                    You are an AI meal planning assistant for an agricultural marketplace.
                    Generate a weekly meal plan matching the following constraints:
                    - Days: %d
                    - People: %d
                    - Target budget: %s VND
                    - Goal / Style: %s
                    - Buyer diet preference: %s
                    - Buyer health conditions: %s
                    
                    Available candidate combos:
                    %s
                    
                    Rules:
                    1. Output JSON ONLY matching this exact structure:
                    {
                      "days": [
                        {
                          "day_index": 1,
                          "meals": [
                            {
                              "meal_type": "BREAKFAST",
                              "items": [
                                { "combo_id": 12, "quantity": %d }
                              ]
                            }
                          ]
                        }
                      ],
                      "notes": "short rationale in Vietnamese"
                    }
                    2. Use ONLY combo_ids present in the candidate combos list.
                    3. Match combo meal_type to slot meal_type.
                    4. Vary combos across days to avoid repetitive meals.
                    5. Stay within the target budget if specified.
                    """.formatted(
                    in.getConstraints().getNumberOfDays(),
                    in.getConstraints().getNumberOfPeople(),
                    in.getConstraints().getTargetBudget(),
                    in.getConstraints().getGoal(),
                    in.getBuyer().getDietPreference(),
                    in.getBuyer().getConditions(),
                    candidatesJson,
                    in.getConstraints().getNumberOfPeople()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error building generation prompt", e);
        }
    }
}
