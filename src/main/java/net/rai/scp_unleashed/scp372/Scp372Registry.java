package net.rai.scp_unleashed.scp372;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
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

public final class Scp372Registry {
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final String InsectId = "scp372";
    public static final String GlimpseId = "scp372_glimpse";
    public static final String AnchorId = "scp372_anchor";
    public static final String EggId = "scp372_spawn_egg";
    public static final ResourceLocation DocumentId = location("scp_372");
    public static final float AnchorHardness = 5.0f;
    public static final float AnchorBlastResistance = 1200.0f;
    public static final int HoldingLight = 7;
    public static final int PoweredLight = 4;

    public static final DeferredHolder<EntityType<?>, EntityType<Scp372Insect>> Insect = EntityTypeRegister.register(InsectId,
            () -> EntityType.Builder.<Scp372Insect>of(Scp372Insect::new, MobCategory.CREATURE)
                    .sized(Scp372Insect.Width, Scp372Insect.Height)
                    .eyeHeight(Scp372Insect.EyeHeight)
                    .clientTrackingRange(10)
                    .build(InsectId));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp372Glimpse>> Glimpse = EntityTypeRegister.register(GlimpseId,
            () -> EntityType.Builder.<Scp372Glimpse>of(Scp372Glimpse::new, MobCategory.MISC)
                    .sized(Scp372Glimpse.Width, Scp372Glimpse.Height)
                    .noSummon()
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(0)
                    .updateInterval(20)
                    .build(GlimpseId));

    public static final DeferredItem<DeferredSpawnEggItem> InsectEgg = ItemRegister.register(EggId,
            () -> new DeferredSpawnEggItem(Insect, 0xFFFFFF, 0xFFFFFF, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredBlock<Scp372AnchorBlock> Anchor = BlockRegister.register(AnchorId,
            () -> new Scp372AnchorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops()
                    .strength(AnchorHardness, AnchorBlastResistance).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(State -> switch (State.getValue(Scp372AnchorBlock.Light)) {
                        case Holding -> HoldingLight;
                        case Powered -> PoweredLight;
                        default -> 0;
                    })));
    public static final DeferredItem<BlockItem> AnchorItem = ItemRegister.register(AnchorId,
            () -> new BlockItem(Anchor.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp372AnchorBlockEntity>> AnchorEntity = BlockEntityTypeRegister.register(AnchorId,
            () -> BlockEntityType.Builder.of(Scp372AnchorBlockEntity::new, Anchor.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Skitter = sound("scp372_skitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkitterTick = sound("scp372_skitter_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chitter = sound("scp372_chitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChitterClick = sound("scp372_chitter_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Step = sound("scp372_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurt = sound("scp372_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> Charge = sound("scp372_anchor_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChargeRing = sound("scp372_anchor_charge_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("scp372_anchor_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumDrone = sound("scp372_anchor_hum_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Start = sound("scp372_anchor_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stop = sound("scp372_anchor_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("scp372_anchor_refuse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Snap = sound("scp372_snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> SnapCrack = sound("scp372_snap_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> Flicker = sound("scp372_flicker");
    public static final DeferredHolder<SoundEvent, SoundEvent> FlickerAir = sound("scp372_flicker_air");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SkitterLayers = List.of(Skitter, SkitterTick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChitterLayers = List.of(Chitter, ChitterClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ChargeLayers = List.of(Charge, ChargeRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumDrone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SnapLayers = List.of(Snap, SnapCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FlickerLayers = List.of(Flicker, FlickerAir);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Flare = ParticleRegister.register("scp372_flare", () -> new SimpleParticleType(false));

    private Scp372Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        EntityTypeRegister.register(ModEventBus);
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StartConditions.register(Scp372GlimpseCondition.TypeName, Scp372GlimpseCondition.ConditionCodec);
        AnomalySpawners.register(DocumentId, Scp372Arrival::arrive);
    }
}
