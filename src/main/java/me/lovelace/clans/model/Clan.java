package me.lovelace.clans.model;

import org.bukkit.Material;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Clan {
    private final UUID id;
    private final long createdAt;
    private String name;
    private String tag;
    private String tagColor;
    private String description;
    private Material emblem;
    private int level;
    private long experience;
    private int chestRows;
    private ClanSpirit spirit;

    private final Map<UUID, ClanMember> members = new ConcurrentHashMap<>();
    private final Map<TerritoryKey, ClanTerritory> territories = new ConcurrentHashMap<>();
    private final Map<UUID, DiplomacyRelation> diplomacy = new ConcurrentHashMap<>();
    private final Map<ClanUpgrade, Integer> upgrades = new ConcurrentHashMap<>();

    public Clan(UUID id,
                String name,
                String tag,
                String tagColor,
                String description,
                Material emblem,
                int level,
                long experience,
                int chestRows,
                ClanSpirit spirit,
                long createdAt) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.tagColor = tagColor;
        this.description = description;
        this.emblem = emblem;
        this.level = level;
        this.experience = experience;
        this.chestRows = chestRows;
        this.spirit = spirit;
        this.createdAt = createdAt;
        for (ClanUpgrade upgrade : ClanUpgrade.values()) {
            upgrades.put(upgrade, 0);
        }
    }

    public static Clan create(UUID id, String name, String tag, String tagColor, Material emblem, UUID founderId, int chestRows) {
        Clan clan = new Clan(id, name, tag, tagColor, "", emblem, 1, 0L, chestRows, ClanSpirit.fresh(), System.currentTimeMillis());
        clan.addMember(founderId, ClanRank.GUILDMASTER);
        return clan;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String tag() {
        return tag;
    }

    public String tagColor() {
        return tagColor;
    }

    public String coloredTag() {
        return tagColor + tag;
    }

    public String description() {
        return description;
    }

    public Material emblem() {
        return emblem;
    }

    public int level() {
        return level;
    }

    public long experience() {
        return experience;
    }

    public int chestRows() {
        return chestRows;
    }

    public ClanSpirit spirit() {
        return spirit;
    }

    public long createdAt() {
        return createdAt;
    }

    public Map<UUID, ClanMember> members() {
        return Collections.unmodifiableMap(members);
    }

    public Collection<ClanTerritory> territories() {
        return Collections.unmodifiableCollection(territories.values());
    }

    public Map<UUID, DiplomacyRelation> diplomacy() {
        return Collections.unmodifiableMap(diplomacy);
    }

    public Map<ClanUpgrade, Integer> upgrades() {
        return Collections.unmodifiableMap(new EnumMap<>(upgrades));
    }

    public Optional<ClanMember> member(UUID playerId) {
        return Optional.ofNullable(members.get(playerId));
    }

    public boolean hasMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public Optional<UUID> guildmasterId() {
        return members.values().stream()
                .filter(member -> member.rank() == ClanRank.GUILDMASTER)
                .map(ClanMember::playerId)
                .findFirst();
    }

    public void addMember(UUID playerId, ClanRank rank) {
        long now = System.currentTimeMillis();
        members.put(playerId, new ClanMember(playerId, rank, now, now));
    }

    public void putMember(ClanMember member) {
        members.put(member.playerId(), member);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public void setRank(UUID playerId, ClanRank rank) {
        ClanMember current = members.get(playerId);
        if (current != null) {
            members.put(playerId, current.withRank(rank));
        }
    }

    public void markSeen(UUID playerId, long timestamp) {
        ClanMember current = members.get(playerId);
        if (current != null) {
            members.put(playerId, current.withLastSeen(timestamp));
        }
    }

    public void addTerritory(ClanTerritory territory) {
        territories.put(territory.key(), territory);
    }

    public void removeTerritory(TerritoryKey key) {
        territories.remove(key);
    }

    public Optional<ClanTerritory> territory(TerritoryKey key) {
        return Optional.ofNullable(territories.get(key));
    }

    public void setDiplomacy(UUID targetClanId, DiplomacyRelation relation) {
        if (relation == DiplomacyRelation.NEUTRAL) {
            diplomacy.remove(targetClanId);
        } else {
            diplomacy.put(targetClanId, relation);
        }
    }

    public DiplomacyRelation relationTo(UUID targetClanId) {
        return diplomacy.getOrDefault(targetClanId, DiplomacyRelation.NEUTRAL);
    }

    public int upgradeLevel(ClanUpgrade upgrade) {
        return upgrades.getOrDefault(upgrade, 0);
    }

    public void setUpgradeLevel(ClanUpgrade upgrade, int level) {
        upgrades.put(upgrade, Math.max(0, level));
    }

    public long addExperience(long amount) {
        experience = Math.max(0L, experience + amount);
        spirit = spirit.addEnergy(Math.max(0L, amount / 4));
        return experience;
    }

    public void levelUp() {
        level++;
        spirit = spirit.withLevel(Math.max(spirit.level(), level));
    }

    public void setExperience(long experience) {
        this.experience = Math.max(0L, experience);
    }

    public void setSpirit(ClanSpirit spirit) {
        this.spirit = spirit;
    }

    public void setChestRows(int chestRows) {
        this.chestRows = chestRows;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public void setTagColor(String tagColor) {
        this.tagColor = tagColor;
    }

    public void setEmblem(Material emblem) {
        this.emblem = emblem;
    }
}
