package net.rai.scp_unleashed.scp682;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public enum Scp682Signature {
    Cinder("cinder", 8, Set.of(id("scp457_burn"), vanilla("in_fire"), vanilla("on_fire"), vanilla("lava"), vanilla("hot_floor"), vanilla("campfire")), null,
            ParticleTypes.LAVA),
    Rime("rime", 6, Set.of(vanilla("freeze")), null, ParticleTypes.SNOWFLAKE),
    Crystal("crystal", 4, Set.of(id("scp409_overgrown")), null, ParticleTypes.WAX_ON),
    RedIce("red_ice", 3, Set.of(id("scp009_crystallized")), null, ParticleTypes.DRIPPING_LAVA),
    Plague("plague", 6, Set.of(id("scp049_cure"), id("scp049_fell"), id("scp016_split")), Scp682Registry.PlagueCarriers, ParticleTypes.CRIMSON_SPORE),
    Insulated("insulated", 5, Set.of(id("tesla"), vanilla("lightning_bolt")), null, ParticleTypes.ELECTRIC_SPARK),
    Plated("plated", 8, Set.of(id("gunshot"), id("sentry_turret")), null, ParticleTypes.CRIT),
    Scarred("scarred", 8, Set.of(id("scp427_maul"), id("scp323_claw"), id("scp076_blades"), id("scp076_knife"), id("scp354_taken")), Scp682Registry.Predators,
            ParticleTypes.SWEEP_ATTACK);

    private final String Name;
    private final int Threshold;
    private final Set<ResourceLocation> Kinds;
    @Nullable
    private final TagKey<EntityType<?>> Attackers;
    private final ParticleOptions Flare;
    private final ResourceLocation Key;

    Scp682Signature(String Name, int Threshold, Set<ResourceLocation> Kinds, @Nullable TagKey<EntityType<?>> Attackers, ParticleOptions Flare) {
        this.Name = Name;
        this.Threshold = Threshold;
        this.Kinds = Kinds;
        this.Attackers = Attackers;
        this.Flare = Flare;
        this.Key = Scp682Registry.location("scp682_signature/" + Name);
    }

    private static ResourceLocation id(String Path) {
        return Scp682Registry.location(Path);
    }

    private static ResourceLocation vanilla(String Path) {
        return ResourceLocation.withDefaultNamespace(Path);
    }

    public String key() {
        return Name;
    }

    public ResourceLocation id() {
        return Key;
    }

    public int threshold() {
        return Threshold;
    }

    public ParticleOptions flare() {
        return Flare;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public boolean covers(ResourceLocation Kind, @Nullable Entity Attacker) {
        return Kinds.contains(Kind) || Attackers != null && Attacker != null && Attacker.getType().is(Attackers);
    }

    @Nullable
    public static Scp682Signature of(ResourceLocation Kind, @Nullable Entity Attacker) {
        for (Scp682Signature Each : values()) {
            if (Each.Kinds.contains(Kind)) {
                return Each;
            }
        }
        if (Attacker != null) {
            for (Scp682Signature Each : values()) {
                if (Each.Attackers != null && Attacker.getType().is(Each.Attackers)) {
                    return Each;
                }
            }
        }
        return null;
    }

    @Nullable
    public static Scp682Signature byId(ResourceLocation Key) {
        for (Scp682Signature Each : values()) {
            if (Each.Key.equals(Key)) {
                return Each;
            }
        }
        return null;
    }
}
