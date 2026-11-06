package net.rai.scp_unleashed.scp149;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp149Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_149");
    public static final ResourceKey<DamageType> BiteDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp149_bite"));
    public static final ResourceKey<DamageType> BroodDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp149_brood"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Whine = sound("scp149_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> WhineDrone = sound("scp149_whine_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bite = sound("scp149_bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> BiteDraw = sound("scp149_bite_draw");
    public static final DeferredHolder<SoundEvent, SoundEvent> Miss = sound("scp149_miss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp149_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Death = sound("scp149_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> DeathPop = sound("scp149_death_pop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chew = sound("scp149_chew");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChewGrind = sound("scp149_chew_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> LarvaSkitter = sound("scp149_larva_skitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> LarvaSquelch = sound("scp149_larva_squelch");
    public static final DeferredHolder<SoundEvent, SoundEvent> LarvaHurt = sound("scp149_larva_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> LarvaDeath = sound("scp149_larva_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shed = sound("scp149_shed");
    public static final DeferredHolder<SoundEvent, SoundEvent> ShedWet = sound("scp149_shed_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Build = sound("scp149_build");
    public static final DeferredHolder<SoundEvent, SoundEvent> BuildWet = sound("scp149_build_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pulse = sound("scp149_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> PulseLow = sound("scp149_pulse_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> Burst = sound("scp149_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstWet = sound("scp149_burst_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> BurstWhine = sound("scp149_burst_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tear = sound("scp149_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> TearWet = sound("scp149_tear_wet");
    public static final DeferredHolder<SoundEvent, SoundEvent> Itch = sound("scp149_itch");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stir = sound("scp149_stir");
    public static final DeferredHolder<SoundEvent, SoundEvent> StirHum = sound("scp149_stir_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> StirSkitter = sound("scp149_stir_skitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wither = sound("scp149_wither");
    public static final DeferredHolder<SoundEvent, SoundEvent> CocoonStep = sound("scp149_cocoon_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> CocoonHit = sound("scp149_cocoon_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> CocoonPlace = sound("scp149_cocoon_place");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BiteLayers = List.of(Bite, BiteDraw);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChewLayers = List.of(Chew, ChewGrind);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ShedLayers = List.of(Shed, ShedWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BuildLayers = List.of(Build, BuildWet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PulseLayers = List.of(Pulse, PulseLow);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurstLayers = List.of(Burst, BurstWet, BurstWhine);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StirLayers = List.of(Stir, StirHum, StirSkitter);

    public static final DeferredSoundType CocoonSounds = new DeferredSoundType(1.0f, 1.0f, Tear, CocoonStep, CocoonPlace, CocoonHit, CocoonStep);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Mote = ParticleRegister.register("scp149_mote", () -> new SimpleParticleType(false));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp149Fly>> Fly = EntityTypeRegister.register("scp149_fly",
            () -> EntityType.Builder.<Scp149Fly>of(Scp149Fly::new, MobCategory.MONSTER)
                    .sized(Scp149Fly.Width, Scp149Fly.Height)
                    .eyeHeight(Scp149Fly.EyeHeight)
                    .clientTrackingRange(8)
                    .build("scp149_fly"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp149Larva>> Larva = EntityTypeRegister.register("scp149_larva",
            () -> EntityType.Builder.<Scp149Larva>of(Scp149Larva::new, MobCategory.MONSTER)
                    .sized(Scp149Larva.Width, Scp149Larva.Height)
                    .eyeHeight(Scp149Larva.EyeHeight)
                    .clientTrackingRange(6)
                    .build("scp149_larva"));

    public static final DeferredBlock<Scp149CocoonBlock> Cocoon = BlockRegister.register("scp149_cocoon", () -> new Scp149CocoonBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_RED).strength(Scp149CocoonBlock.Hardness, Scp149CocoonBlock.Resistance).sound(CocoonSounds).noOcclusion()
            .pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<BlockItem> CocoonItem = ItemRegister.register("scp149_cocoon", () -> new BlockItem(Cocoon.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp149CocoonBlockEntity>> CocoonEntity = BlockEntityTypeRegister.register("scp149_cocoon",
            () -> BlockEntityType.Builder.of(Scp149CocoonBlockEntity::new, Cocoon.get()).build(null));

    public static final DeferredItem<DeferredSpawnEggItem> FlyEgg = ItemRegister.register("scp149_fly_spawn_egg",
            () -> new DeferredSpawnEggItem(Fly, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<DeferredSpawnEggItem> LarvaEgg = ItemRegister.register("scp149_larva_spawn_egg",
            () -> new DeferredSpawnEggItem(Larva, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp149Host.State>> Host = AttachmentRegister.register("scp149_host",
            () -> AttachmentType.builder(() -> Scp149Host.State.None).serialize(Scp149Host.State.StateCodec, Saved -> Saved.active()).build());

    private Scp149Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        StartConditions.register(Scp149ArrivalCondition.TypeName, Scp149ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp149ArrivalCondition::manifest);
    }
}
