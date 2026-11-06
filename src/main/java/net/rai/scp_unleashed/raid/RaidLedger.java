package net.rai.scp_unleashed.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RaidLedger {
    public record Intel(ResourceKey<Level> Dimension, BlockPos Pos, int Count, long Stamp) {
    }

    private final ResourceLocation Faction;
    private int Era;
    private long NextRaid;
    private int Raids;
    private int Repelled;
    private String LastOutcome = "";
    private final List<Intel> Known = new ArrayList<>();
    private final List<ItemStack> Spoils = new ArrayList<>();
    private Runnable Dirty = () -> {
    };

    public RaidLedger(ResourceLocation Faction, int Era) {
        this.Faction = Faction;
        this.Era = Era;
    }

    void bind(Runnable Marker) {
        Dirty = Marker;
    }

    public ResourceLocation faction() {
        return Faction;
    }

    public int era() {
        return Era;
    }

    public void renew(int NewEra) {
        Era = NewEra;
        NextRaid = 0L;
        Raids = 0;
        Repelled = 0;
        LastOutcome = "";
        Known.clear();
        Spoils.clear();
        Dirty.run();
    }

    public long nextRaid() {
        return NextRaid;
    }

    public void setNextRaid(long Time) {
        NextRaid = Time;
        Dirty.run();
    }

    public int raids() {
        return Raids;
    }

    public int repelled() {
        return Repelled;
    }

    public String lastOutcome() {
        return LastOutcome;
    }

    public void record(RaidOutcome Outcome) {
        Raids++;
        if (Outcome == RaidOutcome.Defeated) {
            Repelled++;
        }
        LastOutcome = Outcome.serialName();
        Dirty.run();
    }

    public int intelNear(ResourceKey<Level> Dimension, BlockPos Pos) {
        int Count = 0;
        for (Intel Entry : Known) {
            if (Entry.Dimension().equals(Dimension) && horizontal(Entry.Pos(), Pos) <= RaidRules.IntelReach) {
                Count += Entry.Count();
            }
        }
        return Count;
    }

    public void learn(ResourceKey<Level> Dimension, BlockPos Pos, long Now) {
        for (int Index = 0; Index < Known.size(); Index++) {
            Intel Entry = Known.get(Index);
            if (Entry.Dimension().equals(Dimension) && horizontal(Entry.Pos(), Pos) <= RaidRules.IntelReach / 2.0) {
                Known.set(Index, new Intel(Dimension, Pos.immutable(), Entry.Count() + 1, Now));
                Dirty.run();
                return;
            }
        }
        Known.add(new Intel(Dimension, Pos.immutable(), 1, Now));
        while (Known.size() > RaidRules.MaxIntel) {
            Known.remove(0);
        }
        Dirty.run();
    }

    public List<Intel> intel() {
        return Collections.unmodifiableList(Known);
    }

    public List<ItemStack> spoils() {
        return Collections.unmodifiableList(Spoils);
    }

    public void stow(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return;
        }
        Spoils.add(Stack.copy());
        while (Spoils.size() > RaidRules.MaxSpoils) {
            Spoils.remove(0);
        }
        Dirty.run();
    }

    @Nullable
    public ItemStack issue() {
        if (Spoils.isEmpty()) {
            return null;
        }
        ItemStack Taken = Spoils.remove(Spoils.size() - 1);
        Dirty.run();
        return Taken;
    }

    private static double horizontal(BlockPos First, BlockPos Second) {
        double Dx = First.getX() - Second.getX();
        double Dz = First.getZ() - Second.getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public CompoundTag save(HolderLookup.Provider Lookup) {
        CompoundTag Saved = new CompoundTag();
        Saved.putString("Faction", Faction.toString());
        Saved.putInt("Era", Era);
        Saved.putLong("NextRaid", NextRaid);
        Saved.putInt("Raids", Raids);
        Saved.putInt("Repelled", Repelled);
        Saved.putString("LastOutcome", LastOutcome);
        ListTag IntelList = new ListTag();
        for (Intel Entry : Known) {
            CompoundTag One = new CompoundTag();
            One.putString("Dimension", Entry.Dimension().location().toString());
            One.putLong("Pos", Entry.Pos().asLong());
            One.putInt("Count", Entry.Count());
            One.putLong("Stamp", Entry.Stamp());
            IntelList.add(One);
        }
        Saved.put("Intel", IntelList);
        ListTag SpoilList = new ListTag();
        for (ItemStack Stack : Spoils) {
            if (!Stack.isEmpty()) {
                SpoilList.add(Stack.save(Lookup));
            }
        }
        Saved.put("Spoils", SpoilList);
        return Saved;
    }

    @Nullable
    public static RaidLedger load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ResourceLocation Faction = ResourceLocation.tryParse(Saved.getString("Faction"));
        if (Faction == null) {
            return null;
        }
        RaidLedger Ledger = new RaidLedger(Faction, Saved.getInt("Era"));
        Ledger.NextRaid = Saved.getLong("NextRaid");
        Ledger.Raids = Saved.getInt("Raids");
        Ledger.Repelled = Saved.getInt("Repelled");
        Ledger.LastOutcome = Saved.getString("LastOutcome");
        ListTag IntelList = Saved.getList("Intel", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < IntelList.size(); Index++) {
            CompoundTag One = IntelList.getCompound(Index);
            ResourceLocation Place = ResourceLocation.tryParse(One.getString("Dimension"));
            if (Place != null) {
                Ledger.Known.add(new Intel(ResourceKey.create(Registries.DIMENSION, Place), BlockPos.of(One.getLong("Pos")), One.getInt("Count"), One.getLong("Stamp")));
            }
        }
        ListTag SpoilList = Saved.getList("Spoils", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < SpoilList.size(); Index++) {
            ItemStack.parse(Lookup, SpoilList.getCompound(Index)).ifPresent(Ledger.Spoils::add);
        }
        return Ledger;
    }
}
