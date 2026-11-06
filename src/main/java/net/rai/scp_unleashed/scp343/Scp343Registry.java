package net.rai.scp_unleashed.scp343;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.StartConditions;

import java.util.List;

public final class Scp343Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp_343");
    public static final ResourceKey<DamageType> Judged = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp343_judged"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp343God>> God = EntityTypeRegister.register("scp343",
            () -> EntityType.Builder.<Scp343God>of(Scp343God::new, MobCategory.MISC)
                    .sized(Scp343God.Width, Scp343God.Height)
                    .eyeHeight(Scp343God.EyeHeight)
                    .vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build("scp343"));

    public static final DeferredBlock<Scp343ThroneBlock> Throne = BlockRegister.register("scp343_throne", () -> new Scp343ThroneBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.5f, 9.0f).sound(SoundType.STONE)
            .noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Type) -> false)));

    public static final DeferredItem<BlockItem> ThroneItem = ItemRegister.register("scp343_throne", () -> new BlockItem(Throne.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredItem<DeferredSpawnEggItem> SpawnEgg = ItemRegister.register("scp343_spawn_egg",
            () -> new DeferredSpawnEggItem(God, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Light = ParticleRegister.register("scp343_light", () -> new SimpleParticleType(true));

    public static final DeferredHolder<SoundEvent, SoundEvent> Speak = sound("scp343_speak");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpeakThird = sound("scp343_speak_third");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpeakFifth = sound("scp343_speak_fifth");
    public static final DeferredHolder<SoundEvent, SoundEvent> SpeakBell = sound("scp343_speak_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bless = sound("scp343_bless");
    public static final DeferredHolder<SoundEvent, SoundEvent> BlessShimmer = sound("scp343_bless_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp343_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumSoft = sound("scp343_hum_soft");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepOut = sound("scp343_step_out");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepIn = sound("scp343_step_in");
    public static final DeferredHolder<SoundEvent, SoundEvent> StepChime = sound("scp343_step_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Judgement = sound("scp343_judgement");
    public static final DeferredHolder<SoundEvent, SoundEvent> JudgementChime = sound("scp343_judgement_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> JudgementBell = sound("scp343_judgement_bell");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SpeakLayers = List.of(Speak, SpeakThird, SpeakFifth, SpeakBell);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BlessLayers = List.of(Bless, BlessShimmer);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumSoft);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepOutLayers = List.of(StepOut, StepChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StepInLayers = List.of(StepIn, StepChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> JudgementLayers = List.of(Judgement, JudgementChime, JudgementBell);

    private Scp343Registry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp343ArrivalCondition.TypeName, Scp343ArrivalCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp343Arrival::arrive);
    }
}
