package me.lovelace.clans.model.war;

import me.lovelace.clans.model.TerritoryKey;

import java.util.UUID;

public record ClanWar(
        UUID id,
        UUID attackerClanId,
        UUID defenderClanId,
        TerritoryKey contestedTerritory,
        long startedAt,
        long endsAt,
        WarState state,
        int attackerScore,
        int defenderScore
) {

    public boolean involves(UUID clanId) {
        return attackerClanId.equals(clanId) || defenderClanId.equals(clanId);
    }

    public boolean between(UUID first, UUID second) {
        return (attackerClanId.equals(first) && defenderClanId.equals(second))
                || (attackerClanId.equals(second) && defenderClanId.equals(first));
    }

    public ClanWar withState(WarState state) {
        return new ClanWar(id, attackerClanId, defenderClanId, contestedTerritory, startedAt, endsAt, state, attackerScore, defenderScore);
    }

    public ClanWar addAttackerScore(int amount) {
        return new ClanWar(id, attackerClanId, defenderClanId, contestedTerritory, startedAt, endsAt, state, attackerScore + amount, defenderScore);
    }

    public ClanWar addDefenderScore(int amount) {
        return new ClanWar(id, attackerClanId, defenderClanId, contestedTerritory, startedAt, endsAt, state, attackerScore, defenderScore + amount);
    }
}
