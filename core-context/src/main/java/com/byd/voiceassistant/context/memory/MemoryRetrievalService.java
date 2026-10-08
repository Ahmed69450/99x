package com.byd.voiceassistant.context.memory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MemoryRetrievalService {

    private final List<FactEntity> inMemoryStore;
    private final FactDao factDao;
    private long autoIncrementId = 1;

    public MemoryRetrievalService() {
        this(null);
    }

    public MemoryRetrievalService(FactDao factDao) {
        this.factDao = factDao;
        this.inMemoryStore = new CopyOnWriteArrayList<>();
    }

    public synchronized void saveFact(String key, String value, String tags) {
        FactEntity entity = new FactEntity(
            autoIncrementId++,
            key,
            value,
            tags != null ? tags.toLowerCase() : "",
            System.currentTimeMillis()
        );

        inMemoryStore.add(entity);

        if (factDao != null) {
            try {
                factDao.insertFact(entity);
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized List<String> retrieveRelevantFacts(String query, int limit) {
        if (query == null || query.trim().isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        String normalizedQuery = query.toLowerCase();
        Set<String> queryTokens = extractTokens(normalizedQuery);

        List<FactScore> scoredFacts = new ArrayList<>();

        for (FactEntity fact : inMemoryStore) {
            int score = 0;
            String factValLower = fact.getValue() != null ? fact.getValue().toLowerCase() : "";
            String factTagsLower = fact.getTags() != null ? fact.getTags().toLowerCase() : "";

            for (String token : queryTokens) {
                if (factValLower.contains(token)) {
                    score += 2;
                }
                if (factTagsLower.contains(token)) {
                    score += 3;
                }
            }

            if (score > 0) {
                scoredFacts.add(new FactScore(fact.getValue(), score, fact.getTimestamp()));
            }
        }

        // Sort descending by score, then recency
        scoredFacts.sort((a, b) -> {
            if (b.score != a.score) {
                return Integer.compare(b.score, a.score);
            }
            return Long.compare(b.timestamp, a.timestamp);
        });

        List<String> results = new ArrayList<>();
        int count = Math.min(limit, scoredFacts.size());
        for (int i = 0; i < count; i++) {
            results.add(scoredFacts.get(i).content);
        }

        return results;
    }

    public List<FactEntity> getAllFacts() {
        return Collections.unmodifiableList(inMemoryStore);
    }

    private Set<String> extractTokens(String text) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "what", "is", "the", "a", "an", "to", "in", "for", "on", "my", "me",
            "we", "you", "should", "could", "would", "how", "give", "please", "set"
        ));

        String[] parts = text.split("[^a-zA-Z0-9]+");
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            if (part.length() > 2 && !stopWords.contains(part)) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private static class FactScore {
        final String content;
        final int score;
        final long timestamp;

        FactScore(String content, int score, long timestamp) {
            this.content = content;
            this.score = score;
            this.timestamp = timestamp;
        }
    }
}
