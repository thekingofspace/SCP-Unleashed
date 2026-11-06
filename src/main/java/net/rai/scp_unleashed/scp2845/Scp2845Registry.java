package net.rai.scp_unleashed.scp2845;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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
import net.rai.scp_unleashed.stasis.StasisPods;

import java.util.List;

public final class Scp2845Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_2845");
    public static final ResourceKey<DamageType> Petrified = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp2845_petrified"));
    public static final TagKey<EntityType<?>> Ignored = TagKey.create(Registries.ENTITY_TYPE, location("scp2845_ignores"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp2845>> DeerType = EntityTypeRegister.register("scp2845",
            () -> EntityType.Builder.<Scp2845>of(Scp2845::new, MobCategory.MONSTER)
                    .sized(Scp2845.Width, Scp2845.Height)
                    .eyeHeight(Scp2845.EyeHeight)
                    .fireImmune()
                    .clientTrackingRange(12)
                    .build("scp2845"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp2845Spirit>> SpiritType = EntityTypeRegister.register("scp2845_spirit",
            () -> EntityType.Builder.<Scp2845Spirit>of(Scp2845Spirit::new, MobCategory.MISC)
                    .sized(Scp2845Spirit.Width, Scp2845Spirit.Height)
                    .fireImmune()
                    .noSave()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("scp2845_spirit"));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp2845Comet>> CometType = EntityTypeRegister.register("scp2845_comet",
            () -> EntityType.Builder.<Scp2845Comet>of(Scp2845Comet::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .fireImmune()
                    .clientTrackingRange(32)
                    .updateInterval(20)
                    .build("scp2845_comet"));

    public static final DeferredBlock<Scp2845StoneBlock> Stone = BlockRegister.register("scp2845_stone",
            () -> new Scp2845StoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(3.5f, 3600000.0f).requiresCorrectToolForDrops().sound(SoundType.CALCITE).noOcclusion().pushReaction(PushReaction.BLOCK)
                    .isValidSpawn((State, Getter, Pos, Type) -> false)));
    public static final DeferredItem<BlockItem> StoneItem = ItemRegister.register("scp2845_stone",
            () -> new BlockItem(Stone.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredBlock<Scp2845PillarBlock> Pillar = BlockRegister.register("scp2845_pillar",
            () -> new Scp2845PillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(2.0f, 6.0f).requiresCorrectToolForDrops().sound(SoundType.DRIPSTONE_BLOCK).noOcclusion()));
    public static final DeferredItem<BlockItem> PillarItem = ItemRegister.register("scp2845_pillar",
            () -> new BlockItem(Pillar.get(), new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> Egg = ItemRegister.register("scp2845_spawn_egg",
            () -> new DeferredSpawnEggItem(DeerType, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Call = sound("scp2845_call");
    public static final DeferredHolder<SoundEvent, SoundEvent> CallLow = sound("scp2845_call_low");
    public static final DeferredHolder<SoundEvent, SoundEvent> CallBell = sound("scp2845_call_bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chime = sound("scp2845_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChimeShimmer = sound("scp2845_chime_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hoof = sound("scp2845_hoof");
    public static final DeferredHolder<SoundEvent, SoundEvent> HoofStone = sound("scp2845_hoof_stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gallop = sound("scp2845_gallop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Grind = sound("scp2845_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> GrindCrack = sound("scp2845_grind_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Petrify = sound("scp2845_petrify");
    public static final DeferredHolder<SoundEvent, SoundEvent> PetrifySet = sound("scp2845_petrify_set");
    public static final DeferredHolder<SoundEvent, SoundEvent> Descent = sound("scp2845_descent");
    public static final DeferredHolder<SoundEvent, SoundEvent> DescentHum = sound("scp2845_descent_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> DescentChime = sound("scp2845_descent_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Impact = sound("scp2845_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> ImpactRumble = sound("scp2845_impact_rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> ImpactRing = sound("scp2845_impact_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stomp = sound("scp2845_stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> StompCrack = sound("scp2845_stomp_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gore = sound("scp2845_gore");
    public static final DeferredHolder<SoundEvent, SoundEvent> Warp = sound("scp2845_warp");
    public static final DeferredHolder<SoundEvent, SoundEvent> Uplift = sound("scp2845_uplift");
    public static final DeferredHolder<SoundEvent, SoundEvent> Mark = sound("scp2845_mark");
    public static final DeferredHolder<SoundEvent, SoundEvent> Shrug = sound("scp2845_shrug");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spirit = sound("scp2845_spirit");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bow = sound("scp2845_bow");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rend = sound("scp2845_rend");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CallLayers = List.of(Call, CallLow, CallBell);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChimeLayers = List.of(Chime, ChimeShimmer);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HoofLayers = List.of(Hoof, HoofStone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GrindLayers = List.of(Grind, GrindCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PetrifyLayers = List.of(Petrify, PetrifySet);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DescentLayers = List.of(Descent, DescentHum, DescentChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ImpactLayers = List.of(Impact, ImpactRumble, ImpactRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StompLayers = List.of(Stomp, StompCrack);

    private Scp2845Registry() {
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
        BlockRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        StartConditions.register(Scp2845Arrival.TypeName, Scp2845Arrival.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp2845Arrival::manifest);
        StasisPods.addCaptureVeto(Target -> Target instanceof Scp2845Spirit || Target instanceof Scp2845 Deer && !Deer.isBowing());
        StasisPods.addReleaseHook((World, Spawned) -> {
            if (Spawned instanceof Scp2845 Deer) {
                Deer.releasedFromStasis(World);
            }
        });
    }
}
