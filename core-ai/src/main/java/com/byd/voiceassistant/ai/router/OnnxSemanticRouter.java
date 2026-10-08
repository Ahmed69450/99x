package com.byd.voiceassistant.ai.router;

import com.byd.voiceassistant.vehicle.SeatPosition;
import com.byd.voiceassistant.vehicle.VehicleAction;
import com.byd.voiceassistant.vehicle.WindowPosition;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnnxSemanticRouter implements SemanticRouter {

    public static final float ROUTING_CONFIDENCE_THRESHOLD = 0.70f;

    private static final Pattern TEMP_REGEX = Pattern.compile(
        "(?:to|at|set|make)\\s+(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*(?:degrees|deg|°|c|celsius)",
        Pattern.CASE_INSENSITIVE
    );

    private final List<CarCommandCatalog.CommandPrototype> prototypes;

    public OnnxSemanticRouter() {
        this.prototypes = CarCommandCatalog.getPrototypes();
    }

    @Override
    public SemanticRouterResult route(String query) {
        if (query == null || query.trim().isEmpty()) {
            return SemanticRouterResult.noMatch(0.0f);
        }

        String normalizedQuery = query.toLowerCase().trim();

        // 1. Check for temperature command pattern first
        Matcher tempMatcher = TEMP_REGEX.matcher(normalizedQuery);
        float extractedTemp = -1.0f;
        if (tempMatcher.find()) {
            String val = tempMatcher.group(1) != null ? tempMatcher.group(1) : tempMatcher.group(2);
            if (val != null) {
                try {
                    extractedTemp = Float.parseFloat(val);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // 2. Compute similarity against prototype exemplars
        float bestScore = 0.0f;
        String bestIntent = null;

        for (CarCommandCatalog.CommandPrototype proto : prototypes) {
            for (String exemplar : proto.getExemplarPhrases()) {
                float similarity = computeSemanticSimilarity(normalizedQuery, exemplar.toLowerCase());
                if (similarity > bestScore) {
                    bestScore = similarity;
                    bestIntent = proto.getIntentId();
                }
            }
        }

        // Guard against informational question false-positives
        boolean isInformationalQuestion = normalizedQuery.contains("what was") ||
                                          normalizedQuery.contains("what is") ||
                                          normalizedQuery.contains("yesterday") ||
                                          normalizedQuery.contains("forecast") ||
                                          normalizedQuery.contains("how was");

        // If temperature was explicitly extracted and query is an active command for temperature/climate
        boolean isTempCommand = (normalizedQuery.contains("set") ||
                                normalizedQuery.contains("change") ||
                                normalizedQuery.contains("adjust") ||
                                normalizedQuery.contains("make")) &&
                                (normalizedQuery.contains("temp") ||
                                 normalizedQuery.contains("deg") ||
                                 normalizedQuery.contains("climate") ||
                                 normalizedQuery.contains("ac")) &&
                                !normalizedQuery.contains("fan") &&
                                !normalizedQuery.contains("seat");

        if (extractedTemp > 0.0f && isTempCommand && !isInformationalQuestion) {
            bestScore = Math.max(bestScore, 0.95f);
            bestIntent = CarCommandCatalog.INTENT_AC_SET_TEMP;
        }

        // 3. Evaluate threshold
        if (bestScore >= ROUTING_CONFIDENCE_THRESHOLD && bestIntent != null && !isInformationalQuestion) {
            VehicleAction action = mapIntentToAction(bestIntent, extractedTemp, normalizedQuery);
            if (action != null) {
                return SemanticRouterResult.match(action, bestScore);
            }
        }

        return SemanticRouterResult.noMatch(bestScore);
    }

    private VehicleAction mapIntentToAction(String intentId, float extractedTemp, String query) {
        switch (intentId) {
            case CarCommandCatalog.INTENT_AC_ON:
                return new VehicleAction.SetAcEnabled(true);
            case CarCommandCatalog.INTENT_AC_OFF:
                return new VehicleAction.SetAcEnabled(false);
            case CarCommandCatalog.INTENT_AC_SET_TEMP:
                float temp = extractedTemp > 0.0f ? extractedTemp : 22.0f;
                return new VehicleAction.SetAcTemperature(temp);
            case CarCommandCatalog.INTENT_AC_TEMP_UP:
                return new VehicleAction.AdjustAcTemperature(1.0f);
            case CarCommandCatalog.INTENT_AC_TEMP_DOWN:
                return new VehicleAction.AdjustAcTemperature(-1.0f);
            case CarCommandCatalog.INTENT_SEAT_HEATING:
                SeatPosition seatPos = query.contains("passenger") ? SeatPosition.PASSENGER : SeatPosition.DRIVER;
                return new VehicleAction.SetSeatHeating(seatPos, 2);
            case CarCommandCatalog.INTENT_FAN_SPEED:
                int speed = 3;
                Matcher fanMatcher = Pattern.compile("(\\d+)").matcher(query);
                if (fanMatcher.find()) {
                    try {
                        speed = Integer.parseInt(fanMatcher.group(1));
                    } catch (NumberFormatException ignored) {}
                }
                return new VehicleAction.SetFanSpeed(speed);
            case CarCommandCatalog.INTENT_WINDOW_OPEN:
                return new VehicleAction.SetWindowPosition(WindowPosition.ALL, 100);
            case CarCommandCatalog.INTENT_WINDOW_CLOSE:
                return new VehicleAction.SetWindowPosition(WindowPosition.ALL, 0);
            case CarCommandCatalog.INTENT_MEDIA_PLAY:
                return new VehicleAction.MediaPlay();
            case CarCommandCatalog.INTENT_MEDIA_PAUSE:
                return new VehicleAction.MediaPause();
            case CarCommandCatalog.INTENT_MEDIA_NEXT:
                return new VehicleAction.MediaNext();
            case CarCommandCatalog.INTENT_MEDIA_PREV:
                return new VehicleAction.MediaPrevious();
            default:
                return null;
        }
    }

    /**
     * Normalized Jaccard + n-gram token similarity (simulates quantized sentence embedding dot-product)
     */
    private float computeSemanticSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0f;
        }
        if (s1.contains(s2) || s2.contains(s1)) {
            return 0.85f;
        }

        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));

        // Remove stop words
        Set<String> stopWords = new HashSet<>(Arrays.asList("the", "a", "an", "to", "my", "is", "in", "it", "on", "of"));
        words1.removeAll(stopWords);
        words2.removeAll(stopWords);

        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0f;
        }

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return (float) intersection.size() / (float) union.size();
    }
}
