package net.rai.scp_unleashed.scp469;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.archive.investigation.StartConditions;
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.List;

public final class Scp469Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_469");
    public static final ResourceKey<DamageType> SweepDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp469_sweep"));
    public static final ResourceKey<DamageType> EmbraceDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp469_embrace"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp469Angel>> Angel = EntityTypeRegister.register("scp469_angel",
            () -> EntityType.Builder.<Scp469Angel>of(Scp469Angel::new, MobCategory.MONSTER)
                    .sized(Scp469Angel.SpreadWidth, Scp469Angel.SpreadHeight)
                    .eyeHeight(Scp469Angel.EyeHeight)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("scp469_angel"));

    public static final DeferredItem<DeferredSpawnEggItem> AngelEgg = ItemRegister.register("scp469_angel_spawn_egg",
            () -> new DeferredSpawnEggItem(Angel, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<Scp469FeatherItem> Feather = ItemRegister.register("scp469_feather",
            () -> new Scp469FeatherItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FeatherParticle = ParticleRegister.register("scp469_feather", () -> new SimpleParticleType(false));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CompoundTag>> Embraced = AttachmentRegister.register("scp469_embraced",
            () -> AttachmentType.builder(() -> new CompoundTag()).serialize(CompoundTag.CODEC).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Rustle = sound("scp469_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RustleSoft = sound("scp469_rustle_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flap = sound("scp469_flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlapLow = sound("scp469_flap_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp469_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepScrape = sound("scp469_step_scrape");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shimmer = sound("scp469_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShimmerLow = sound("scp469_shimmer_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sweep = sound("scp469_sweep");
    public static final DeferredHolder<SoundEvent, SoundEvent> SweepGust = sound("scp469_sweep_gust");
    public static final DeferredHolder<SoundEvent, SoundEvent> Embrace = sound("scp469_embrace");
    public static final DeferredHolder<SoundEvent, SoundEvent> EmbraceHush = sound("scp469_embrace_hush");
    public static final DeferredHolder<SoundEvent, SoundEvent> Lunge = sound("scp469_lunge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LungeRush = sound("scp469_lunge_rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp469_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HurtChime = sound("scp469_hurt_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Glance = sound("scp469_glance");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burst = sound("scp469_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstChime = sound("scp469_burst_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Reform = sound("scp469_reform");
    public static final DeferredHolder<SoundEvent, SoundEvent> ReformRustle = sound("scp469_reform_rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Batter = sound("scp469_batter");
    public static final DeferredHolder<SoundEvent, SoundEvent> BatterThud = sound("scp469_batter_thud");
    public static final DeferredHolder<SoundEvent, SoundEvent> Fold = sound("scp469_fold");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> RustleLayers = List.of(Rustle, RustleSoft);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FlapLayers = List.of(Flap, FlapLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShimmerLayers = List.of(Shimmer, ShimmerLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SweepLayers = List.of(Sweep, SweepGust);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EmbraceLayers = List.of(Embrace, EmbraceHush);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> LungeLayers = List.of(Lunge, LungeRush);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HurtLayers = List.of(Hurt, HurtChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurstLayers = List.of(Burst, BurstChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReformLayers = List.of(Reform, ReformRustle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BatterLayers = List.of(Batter, BatterThud);

    private Scp469Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(Scp469SightingCondition.TypeName, Scp469SightingCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp469Arrival::manifest);
        ReaderTargets.onHandoff(Scp469SightingCondition::handOff);
        StasisPods.addReleaseHook((World, Spawned) -> {
            if (Spawned instanceof Scp469Angel Creature) {
                Creature.leaveStasis();
            }
        });
    }
}
