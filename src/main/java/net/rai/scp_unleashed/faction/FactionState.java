package net.rai.scp_unleashed.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public final class FactionState {
    private final ResourceLocation Faction;
    private FactionPhase Phase = FactionPhase.Dormant;
    private double Power;
    private int Era;
    private long AwakenedAt;
    private long LastAccrual = -1L;
    private long LastExpansion;
    private long LastPatrol;
    private long LastRaid;
    private int NextSite;
    private int Kills;
    private int Losses;
    private final List<FactionSite> Sites = new ArrayList<>();
    private final Map<UUID, FactionMember> Roster = new LinkedHashMap<>();
    private final Map<String, Long> Timers = new LinkedHashMap<>();
    private CompoundTag Extra = new CompoundTag();
    private Runnable Dirty = () -> {
    };

    public FactionState(ResourceLocation Faction) {
        this.Faction = Faction;
    }

    void bind(Runnable Marker) {
        Dirty = Marker;
    }

    public void changed() {
        Dirty.run();
    }

    public ResourceLocation faction() {
        return Faction;
    }

    public FactionPhase phase() {
        return Phase;
    }

    public void setPhase(FactionPhase Value) {
        Phase = Value;
        changed();
    }

    public double power() {
        return Power;
    }

    public void setPower(double Value, double Max) {
        Power = Math.max(0.0, Math.min(Max, Value));
        changed();
    }

    public int era() {
        return Era;
    }

    public void nextEra() {
        Era++;
        changed();
    }

    public long awakenedAt() {
        return AwakenedAt;
    }

    public void setAwakenedAt(long Time) {
        AwakenedAt = Time;
        changed();
    }

    public long lastAccrual() {
        return LastAccrual;
    }

    public void setLastAccrual(long Time) {
        LastAccrual = Time;
    }

    public long lastExpansion() {
        return LastExpansion;
    }

    public void setLastExpansion(long Time) {
        LastExpansion = Time;
        changed();
    }

    public long lastPatrol() {
        return LastPatrol;
    }

    public void setLastPatrol(long Time) {
        LastPatrol = Time;
        changed();
    }

    public long lastRaid() {
        return LastRaid;
    }

    public void setLastRaid(long Time) {
        LastRaid = Time;
        changed();
    }

    public int kills() {
        return Kills;
    }

    public void addKill() {
        Kills++;
        changed();
    }

    public int losses() {
        return Losses;
    }

    public void addLoss() {
        Losses++;
        changed();
    }

    public List<FactionSite> sites() {
        return Sites;
    }

    public FactionSite addSite(String Kind, ResourceKey<Level> Dimension, BlockPos Anchor, long Seed) {
        FactionSite Site = new FactionSite(NextSite++, Kind, Dimension, Anchor, Seed);
        Sites.add(Site);
        changed();
        return Site;
    }

    @Nullable
    public FactionSite site(int Id) {
        for (FactionSite Site : Sites) {
            if (Site.id() == Id) {
                return Site;
            }
        }
        return null;
    }

    public Optional<FactionSite> firstSite(Predicate<FactionSite> Test) {
        return Sites.stream().filter(Test).findFirst();
    }

    public long countSites(String Kind) {
        return Sites.stream().filter(Site -> Site.is(Kind)).count();
    }

    public Collection<FactionMember> roster() {
        return Roster.values();
    }

    @Nullable
    public FactionMember member(UUID Id) {
        return Roster.get(Id);
    }

    public void enroll(FactionMember Member) {
        Roster.put(Member.Id(), Member);
        changed();
    }

    public boolean dismiss(UUID Id) {
        if (Roster.remove(Id) != null) {
            changed();
            return true;
        }
        return false;
    }

    public void touch(FactionMember Member) {
        Roster.put(Member.Id(), Member);
    }

    public int rosterAt(int Site) {
        int Count = 0;
        for (FactionMember Member : Roster.values()) {
            if (Member.Site() == Site) {
                Count++;
            }
        }
        return Count;
    }

    public int rosterAt(int Site, String Role) {
        int Count = 0;
        for (FactionMember Member : Roster.values()) {
            if (Member.Site() == Site && Member.Role().equals(Role)) {
                Count++;
            }
        }
        return Count;
    }

    public long timer(String Name) {
        return Timers.getOrDefault(Name, 0L);
    }

    public void setTimer(String Name, long Value) {
        Timers.put(Name, Value);
        changed();
    }

    public CompoundTag extra() {
        return Extra;
    }

    public void clearAll() {
        Phase = FactionPhase.Dormant;
        Power = 0.0;
        AwakenedAt = 0L;
        LastAccrual = -1L;
        LastExpansion = 0L;
        LastPatrol = 0L;
        LastRaid = 0L;
        Kills = 0;
        Losses = 0;
        Sites.clear();
        Roster.clear();
        Timers.clear();
        Extra = new CompoundTag();
        changed();
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putString("Faction", Faction.toString());
        Saved.putString("Phase", Phase.getSerializedName());
        Saved.putDouble("Power", Power);
        Saved.putInt("Era", Era);
        Saved.putLong("AwakenedAt", AwakenedAt);
        Saved.putLong("LastAccrual", LastAccrual);
        Saved.putLong("LastExpansion", LastExpansion);
        Saved.putLong("LastPatrol", LastPatrol);
        Saved.putLong("LastRaid", LastRaid);
        Saved.putInt("NextSite", NextSite);
        Saved.putInt("Kills", Kills);
        Saved.putInt("Losses", Losses);
        ListTag SiteList = new ListTag();
        for (FactionSite Site : Sites) {
            SiteList.add(Site.save());
        }
        Saved.put("Sites", SiteList);
        ListTag MemberList = new ListTag();
        for (FactionMember Member : Roster.values()) {
            MemberList.add(Member.save());
        }
        Saved.put("Roster", MemberList);
        CompoundTag TimerTag = new CompoundTag();
        for (Map.Entry<String, Long> Entry : Timers.entrySet()) {
            TimerTag.putLong(Entry.getKey(), Entry.getValue());
        }
        Saved.put("Timers", TimerTag);
        Saved.put("Extra", Extra);
        return Saved;
    }

    @Nullable
    public static FactionState load(CompoundTag Saved) {
        ResourceLocation Id = ResourceLocation.tryParse(Saved.getString("Faction"));
        if (Id == null) {
            return null;
        }
        FactionState State = new FactionState(Id);
        State.Phase = FactionPhase.byName(Saved.getString("Phase"));
        State.Power = Saved.getDouble("Power");
        State.Era = Saved.getInt("Era");
        State.AwakenedAt = Saved.getLong("AwakenedAt");
        State.LastAccrual = Saved.contains("LastAccrual") ? Saved.getLong("LastAccrual") : -1L;
        State.LastExpansion = Saved.getLong("LastExpansion");
        State.LastPatrol = Saved.getLong("LastPatrol");
        State.LastRaid = Saved.getLong("LastRaid");
        State.NextSite = Saved.getInt("NextSite");
        State.Kills = Saved.getInt("Kills");
        State.Losses = Saved.getInt("Losses");
        ListTag SiteList = Saved.getList("Sites", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < SiteList.size(); Index++) {
            FactionSite Site = FactionSite.load(SiteList.getCompound(Index));
            if (Site != null) {
                State.Sites.add(Site);
                State.NextSite = Math.max(State.NextSite, Site.id() + 1);
            }
        }
        ListTag MemberList = Saved.getList("Roster", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < MemberList.size(); Index++) {
            FactionMember Member = FactionMember.load(MemberList.getCompound(Index));
            if (Member != null) {
                State.Roster.put(Member.Id(), Member);
            }
        }
        CompoundTag TimerTag = Saved.getCompound("Timers");
        for (String Key : TimerTag.getAllKeys()) {
            State.Timers.put(Key, TimerTag.getLong(Key));
        }
        State.Extra = Saved.getCompound("Extra");
        return State;
    }
}
