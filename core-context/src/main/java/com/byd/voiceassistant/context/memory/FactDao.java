package com.byd.voiceassistant.context.memory;

import java.util.List;

public interface FactDao {
    long insertFact(FactEntity fact);
    List<FactEntity> getAllFacts();
    List<FactEntity> searchFacts(String keyword);
    void deleteFact(long id);
    void clearAll();
}
