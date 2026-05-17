package me.lovelace.clans.model.quest;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record ClanQuestProgress(
        UUID clanId,
        String questId,
        Map<Integer, Integer> objectiveProgress, // Key: objective index, Value: current progress
        boolean completed,
        boolean claimed,
        long lastReset // Timestamp for daily/weekly/monthly quests
) {
    public ClanQuestProgress {
        // Ensure objectiveProgress is not null and is mutable if needed
        if (objectiveProgress == null) {
            objectiveProgress = new ConcurrentHashMap<>();
        } else {
            objectiveProgress = new ConcurrentHashMap<>(objectiveProgress);
        }
    }

    public ClanQuestProgress withObjectiveProgress(int objectiveIndex, int progress) {
        Map<Integer, Integer> newProgressMap = new ConcurrentHashMap<>(this.objectiveProgress);
        newProgressMap.put(objectiveIndex, progress);
        return new ClanQuestProgress(clanId, questId, newProgressMap, completed, claimed, lastReset);
    }
    
    public ClanQuestProgress withObjectiveProgress(Map<Integer, Integer> newProgressMap) {
        return new ClanQuestProgress(clanId, questId, newProgressMap, completed, claimed, lastReset);
    }

    public ClanQuestProgress withCompleted(boolean completed) {
        return new ClanQuestProgress(clanId, questId, objectiveProgress, completed, claimed, lastReset);
    }

    public ClanQuestProgress withClaimed(boolean claimed) {
        return new ClanQuestProgress(clanId, questId, objectiveProgress, completed, claimed, lastReset);
    }

    public ClanQuestProgress withLastReset(long lastReset) {
        return new ClanQuestProgress(clanId, questId, objectiveProgress, completed, claimed, lastReset);
    }
}
