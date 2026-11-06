package net.rai.scp_unleashed.scp682;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp682Adaptation {
    public static final int HitsToAdapt = 6;
    public static final int MaxImmunities = 5;
    public static final float UniqueDamage = 10.0f;
    public static final int UniqueToShrink = 4;
    public static final int TallyForget = 1200;
    public static final int Lifetime = 18000;
    public static final int RefreshTicks = 200;
    public static final int RefreshCap = 6000;
    public static final int LossGap = 1200;
    public static final int FadingTicks = 3600;
    public static final ResourceLocation Unknown = ResourceLocation.withDefaultNamespace("generic");
    public static final ResourceLocation Snap = Scp682Registry.location("scp173_snap");
    public static final Set<ResourceLocation> Never = Set.of(Scp682Registry.AcidDamage.location(), DamageTypes.GENERIC_KILL.location(),
            DamageTypes.FELL_OUT_OF_WORLD.location(), DamageTypes.OUTSIDE_BORDER.location(), Scp682Registry.CommandDamage.location());

    public enum Category {
        Fire, Blast, Pierce, Melee, Anomalous, Other;

        public int bit() {
            return 1 << ordinal();
        }

        public static Category byIndex(int Index) {
            Category[] All = values();
            return All[Math.max(0, Math.min(All.length - 1, Index))];
        }
    }

    public record Outcome(boolean Adapted, @Nullable ResourceLocation Displaced, boolean Shrinks, @Nullable ResourceLocation Gained) {
        public static final Outcome None = new Outcome(false, null, false, null);
    }

    private static final class Hold {
        private final Category Kindred;
        private int Remaining;
        private int Refreshed;

        private Hold(Category Kindred, int Remaining, int Refreshed) {
            this.Kindred = Kindred;
            this.Remaining = Remaining;
            this.Refreshed = Refreshed;
        }

        private boolean fading() {
            return Remaining <= FadingTicks;
        }
    }

    private final Map<ResourceLocation, Integer> Hits = new HashMap<>();
    private final LinkedHashMap<ResourceLocation, Hold> Immune = new LinkedHashMap<>();
    private final Map<ResourceLocation, Float> Tally = new HashMap<>();
    private long LastHit = -1L;
    private int LossWait;

    public static ResourceLocation kindOf(DamageSource Source) {
        return Source.typeHolder().unwrapKey().map(Key -> Key.location()).orElse(Unknown);
    }

    public static boolean adaptable(ResourceLocation Kind) {
        return !Never.contains(Kind);
    }

    public static Category categoryOf(DamageSource Source) {
        Holder<DamageType> Damage = Source.typeHolder();
        if (Damage.is(DamageTypeTags.IS_FIRE)) {
            return Category.Fire;
        }
        if (Damage.is(DamageTypeTags.IS_EXPLOSION)) {
            return Category.Blast;
        }
        if (Damage.is(DamageTypeTags.IS_PROJECTILE)) {
            return Category.Pierce;
        }
        ResourceLocation Kind = kindOf(Source);
        if (Kind.getNamespace().equals(Snap.getNamespace())) {
            return Category.Anomalous;
        }
        if (Damage.is(DamageTypes.PLAYER_ATTACK) || Damage.is(DamageTypes.MOB_ATTACK) || Damage.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || Damage.is(DamageTypes.STING)
                || Damage.is(DamageTypes.THORNS)) {
            return Category.Melee;
        }
        return Category.Other;
    }

    public boolean holds(Scp682Signature Signature) {
        return Immune.containsKey(Signature.id());
    }

    public int signatures() {
        int Mask = 0;
        for (Scp682Signature Each : Scp682Signature.values()) {
            if (holds(Each)) {
                Mask |= Each.bit();
            }
        }
        return Mask;
    }

    @Nullable
    public ResourceLocation coverOf(ResourceLocation Kind, @Nullable Entity Attacker) {
        if (Immune.containsKey(Kind)) {
            return Kind;
        }
        for (Scp682Signature Each : Scp682Signature.values()) {
            if (holds(Each) && Each.covers(Kind, Attacker)) {
                return Each.id();
            }
        }
        return null;
    }

    public boolean isImmune(ResourceLocation Kind, @Nullable Entity Attacker) {
        return coverOf(Kind, Attacker) != null;
    }

    public int refresh(ResourceLocation Kind, @Nullable Entity Attacker) {
        ResourceLocation Cover = coverOf(Kind, Attacker);
        return Cover == null ? 0 : renew(Cover);
    }

    public int renew(ResourceLocation Held) {
        Hold Found = Immune.get(Held);
        if (Found == null) {
            return 0;
        }
        int Added = Math.min(Math.min(RefreshTicks, Lifetime - Found.Remaining), RefreshCap - Found.Refreshed);
        if (Added <= 0) {
            return 0;
        }
        Found.Remaining += Added;
        Found.Refreshed += Added;
        return Added;
    }

    @Nullable
    public ResourceLocation decay(int Step) {
        if (Step <= 0) {
            return null;
        }
        LossWait = Math.max(0, LossWait - Step);
        ResourceLocation Expired = null;
        for (Map.Entry<ResourceLocation, Hold> Each : Immune.entrySet()) {
            Hold Found = Each.getValue();
            Found.Remaining = Math.max(0, Found.Remaining - Step);
            if (Expired == null && Found.Remaining <= 0) {
                Expired = Each.getKey();
            }
        }
        if (Expired == null || LossWait > 0) {
            return null;
        }
        Immune.remove(Expired);
        LossWait = LossGap;
        return Expired;
    }

    public int remaining(ResourceLocation Kind) {
        Hold Found = Immune.get(Kind);
        return Found == null ? -1 : Found.Remaining;
    }

    public int refreshed(ResourceLocation Kind) {
        Hold Found = Immune.get(Kind);
        return Found == null ? -1 : Found.Refreshed;
    }

    public int lossWait() {
        return LossWait;
    }

    public boolean fading(ResourceLocation Kind) {
        Hold Found = Immune.get(Kind);
        return Found != null && Found.fading();
    }

    public int fadingCategories() {
        int Mask = 0;
        for (Map.Entry<ResourceLocation, Hold> Each : Immune.entrySet()) {
            if (Each.getValue().fading() && Scp682Signature.byId(Each.getKey()) == null) {
                Mask |= Each.getValue().Kindred.bit();
            }
        }
        return Mask;
    }

    public int fadingSignatures() {
        int Mask = 0;
        for (Scp682Signature Each : Scp682Signature.values()) {
            if (fading(Each.id())) {
                Mask |= Each.bit();
            }
        }
        return Mask;
    }

    public Outcome expose(Scp682Signature Signature) {
        if (holds(Signature)) {
            return Outcome.None;
        }
        int Count = Hits.merge(Signature.id(), 1, Integer::sum);
        if (Count < Signature.threshold()) {
            return Outcome.None;
        }
        Hits.remove(Signature.id());
        ResourceLocation Displaced = adapt(Signature.id(), Category.Anomalous);
        return new Outcome(true, Displaced, false, Signature.id());
    }

    public boolean isImmune(ResourceLocation Kind) {
        return isImmune(Kind, null);
    }

    public List<ResourceLocation> immunities() {
        return new ArrayList<>(Immune.keySet());
    }

    public int count() {
        return Immune.size();
    }

    public int hits(ResourceLocation Kind) {
        return Hits.getOrDefault(Kind, 0);
    }

    public int categories() {
        int Mask = 0;
        for (Map.Entry<ResourceLocation, Hold> Each : Immune.entrySet()) {
            if (Scp682Signature.byId(Each.getKey()) == null) {
                Mask |= Each.getValue().Kindred.bit();
            }
        }
        return Mask;
    }

    public Outcome record(ResourceLocation Kind, Category Kindred, float Amount, long Now) {
        return record(Kind, Kindred, Amount, Now, null);
    }

    public Outcome record(ResourceLocation Kind, Category Kindred, float Amount, long Now, @Nullable Entity Attacker) {
        boolean Adapted = false;
        ResourceLocation Displaced = null;
        ResourceLocation Gained = null;
        Scp682Signature Signature = adaptable(Kind) ? Scp682Signature.of(Kind, Attacker) : null;
        if (Signature != null) {
            Outcome Exposed = expose(Signature);
            Adapted = Exposed.Adapted();
            Displaced = Exposed.Displaced();
            Gained = Exposed.Gained();
        } else if (adaptable(Kind) && !Immune.containsKey(Kind)) {
            int Count = Hits.merge(Kind, 1, Integer::sum);
            if (Count >= HitsToAdapt) {
                Hits.remove(Kind);
                Displaced = adapt(Kind, Kindred);
                Adapted = true;
                Gained = Kind;
            }
        }
        if (LastHit < 0L || Now - LastHit > TallyForget) {
            Tally.clear();
        }
        LastHit = Now;
        if (Amount > 0.0f) {
            Tally.merge(Kind, Amount, Float::sum);
        }
        return new Outcome(Adapted, Displaced, unique() >= UniqueToShrink, Gained);
    }

    @Nullable
    public ResourceLocation adapt(ResourceLocation Kind, Category Kindred) {
        if (!adaptable(Kind) || Immune.containsKey(Kind)) {
            return null;
        }
        Immune.put(Kind, new Hold(Kindred, Lifetime, 0));
        ResourceLocation Displaced = null;
        while (Immune.size() > MaxImmunities) {
            Displaced = Immune.keySet().iterator().next();
            Immune.remove(Displaced);
        }
        return Displaced;
    }

    public int unique() {
        int Count = 0;
        for (Map.Entry<ResourceLocation, Float> Entry : Tally.entrySet()) {
            if (Entry.getValue() >= UniqueDamage && !isImmune(Entry.getKey())) {
                Count++;
            }
        }
        return Count;
    }

    public void clearTally() {
        Tally.clear();
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        ListTag Held = new ListTag();
        for (Map.Entry<ResourceLocation, Hold> Entry : Immune.entrySet()) {
            CompoundTag Item = new CompoundTag();
            Item.putString("Kind", Entry.getKey().toString());
            Item.putInt("Category", Entry.getValue().Kindred.ordinal());
            Item.putInt("Remaining", Entry.getValue().Remaining);
            Item.putInt("Refreshed", Entry.getValue().Refreshed);
            Held.add(Item);
        }
        Saved.put("Immune", Held);
        CompoundTag Counted = new CompoundTag();
        Hits.forEach((Kind, Count) -> Counted.putInt(Kind.toString(), Count));
        Saved.put("Hits", Counted);
        CompoundTag Dealt = new CompoundTag();
        Tally.forEach((Kind, Amount) -> Dealt.putFloat(Kind.toString(), Amount));
        Saved.put("Tally", Dealt);
        Saved.putLong("LastHit", LastHit);
        Saved.putInt("LossWait", LossWait);
        return Saved;
    }

    public void load(CompoundTag Saved) {
        Immune.clear();
        Hits.clear();
        Tally.clear();
        LastHit = Saved.contains("LastHit") ? Saved.getLong("LastHit") : -1L;
        LossWait = Mth.clamp(Saved.getInt("LossWait"), 0, LossGap);
        ListTag Held = Saved.getList("Immune", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Held.size(); Index++) {
            CompoundTag Item = Held.getCompound(Index);
            ResourceLocation Kind = ResourceLocation.tryParse(Item.getString("Kind"));
            if (Kind != null && adaptable(Kind) && Immune.size() < MaxImmunities) {
                int Remaining = Item.contains("Remaining") ? Mth.clamp(Item.getInt("Remaining"), 0, Lifetime) : Lifetime;
                Immune.put(Kind, new Hold(Category.byIndex(Item.getInt("Category")), Remaining, Mth.clamp(Item.getInt("Refreshed"), 0, RefreshCap)));
            }
        }
        CompoundTag Counted = Saved.getCompound("Hits");
        for (String Key : Counted.getAllKeys()) {
            ResourceLocation Kind = ResourceLocation.tryParse(Key);
            if (Kind != null) {
                Hits.put(Kind, Math.max(0, Counted.getInt(Key)));
            }
        }
        CompoundTag Dealt = Saved.getCompound("Tally");
        for (String Key : Dealt.getAllKeys()) {
            ResourceLocation Kind = ResourceLocation.tryParse(Key);
            if (Kind != null) {
                Tally.put(Kind, Math.max(0.0f, Dealt.getFloat(Key)));
            }
        }
    }
}
