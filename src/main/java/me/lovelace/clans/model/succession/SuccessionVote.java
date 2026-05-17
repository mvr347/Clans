package me.lovelace.clans.model.succession;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SuccessionVote {
    private final UUID clanId;
    private final UUID oldLeaderId;
    private final long startedAt;
    private final long endsAt;
    private final Map<UUID, UUID> votes = new ConcurrentHashMap<>();

    public SuccessionVote(UUID clanId, UUID oldLeaderId, long startedAt, long endsAt) {
        this.clanId = clanId;
        this.oldLeaderId = oldLeaderId;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
    }

    public UUID clanId() {
        return clanId;
    }

    public UUID oldLeaderId() {
        return oldLeaderId;
    }

    public long startedAt() {
        return startedAt;
    }

    public long endsAt() {
        return endsAt;
    }

    public void vote(UUID voterId, UUID candidateId) {
        votes.put(voterId, candidateId);
    }

    public Map<UUID, UUID> votes() {
        return Collections.unmodifiableMap(votes);
    }

    public boolean expired(long now) {
        return endsAt <= now;
    }

    public Optional<UUID> winner() {
        return votes.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(candidate -> candidate, java.util.stream.Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }
}
