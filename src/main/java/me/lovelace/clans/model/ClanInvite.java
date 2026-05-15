package me.lovelace.clans.model;

import java.util.UUID;

public record ClanInvite(UUID clanId, UUID invitedPlayer, UUID invitedBy, long expiresAt) {

    public boolean expired(long now) {
        return expiresAt <= now;
    }
}
