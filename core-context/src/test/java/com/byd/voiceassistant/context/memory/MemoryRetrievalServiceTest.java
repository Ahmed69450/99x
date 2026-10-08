package com.byd.voiceassistant.context.memory;

import org.junit.Assert;
import org.junit.Test;
import java.util.List;

public class MemoryRetrievalServiceTest {

    @Test
    public void testFactRetrievalMatchesRelevantKeywords() {
        MemoryRetrievalService service = new MemoryRetrievalService();
        service.saveFact("ac_pref", "User prefers cabin temperature set to 23 degrees", "ac,temp,temperature");
        service.saveFact("music_pref", "User likes smooth jazz while driving", "music,jazz,media");
        service.saveFact("home_address", "User lives at 123 Tech Park Road", "home,navigation,address");

        List<String> acFacts = service.retrieveRelevantFacts("What temperature should we set?", 2);
        Assert.assertEquals(1, acFacts.size());
        Assert.assertTrue(acFacts.get(0).contains("23 degrees"));

        List<String> musicFacts = service.retrieveRelevantFacts("Play my favorite jazz music", 2);
        Assert.assertEquals(1, musicFacts.size());
        Assert.assertTrue(musicFacts.get(0).contains("smooth jazz"));
    }

    @Test
    public void testSingleTurnLimitEnforced() {
        MemoryRetrievalService service = new MemoryRetrievalService();
        service.saveFact("fact1", "Fact 1 content", "common,info");
        service.saveFact("fact2", "Fact 2 content", "common,info");
        service.saveFact("fact3", "Fact 3 content", "common,info");
        service.saveFact("fact4", "Fact 4 content", "common,info");

        // Limit strictly to 2 facts
        List<String> facts = service.retrieveRelevantFacts("Give me common info", 2);
        Assert.assertEquals(2, facts.size());
    }
}
