package net.rai.scp_unleashed.forcefield;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class ForcefieldRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final String EmitterId = "forcefield_emitter";
    public static final String BarrierId = "forcefield_barrier";
    public static final float EmitterHardness = 5.0f;
    public static final float EmitterBlastResistance = 1200.0f;
    public static final float BarrierBlastResistance = 3600000.0f;
    public static final int BarrierLight = 3;

    public static final DeferredBlock<ForcefieldEmitterBlock> Emitter = BlockRegister.register(EmitterId,
            () -> new ForcefieldEmitterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .requiresCorrectToolForDrops().strength(EmitterHardness, EmitterBlastResistance).sound(SoundType.METAL)
                    .lightLevel(State -> ForcefieldEmitterBlock.lightFor(State.getValue(ForcefieldEmitterBlock.Glow)))));
    public static final DeferredBlock<ForcefieldBarrierBlock> Barrier = BlockRegister.register(BarrierId,
            () -> new ForcefieldBarrierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(-1.0f, BarrierBlastResistance)
                    .noLootTable().noOcclusion().forceSolidOn().pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST).randomTicks()
                    .isValidSpawn((State, Getter, Pos, Type) -> false).isRedstoneConductor((State, Getter, Pos) -> false)
                    .isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false).lightLevel(State -> BarrierLight)));
    public static final DeferredItem<BlockItem> EmitterItem = ItemRegister.register(EmitterId,
            () -> new BlockItem(Emitter.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForcefieldEmitterBlockEntity>> EmitterEntity = BlockEntityTypeRegister.register(EmitterId,
            () -> BlockEntityType.Builder.of(ForcefieldEmitterBlockEntity::new, Emitter.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("forcefield_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumDrone = sound("forcefield_hum_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hit = sound("forcefield_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> HitCrackle = sound("forcefield_hit_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Collapse = sound("forcefield_collapse");
    public static final DeferredHolder<SoundEvent, SoundEvent> CollapseShatter = sound("forcefield_collapse_shatter");
    public static final DeferredHolder<SoundEvent, SoundEvent> CollapseCrack = sound("forcefield_collapse_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> BootStart = sound("forcefield_boot_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> BootStartCharge = sound("forcefield_boot_start_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> BootStep = sound("forcefield_boot_step");
    public static final DeferredHolder<SoundEvent, SoundEvent> BootStepTone = sound("forcefield_boot_step_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Online = sound("forcefield_online");
    public static final DeferredHolder<SoundEvent, SoundEvent> OnlineRing = sound("forcefield_online_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Offline = sound("forcefield_offline");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumDrone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HitLayers = List.of(Hit, HitCrackle);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CollapseLayers = List.of(Collapse, CollapseShatter, CollapseCrack);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BootStartLayers = List.of(BootStart, BootStartCharge);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BootStepLayers = List.of(BootStep, BootStepTone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OnlineLayers = List.of(Online, OnlineRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> OfflineLayers = List.of(Offline);

    private ForcefieldRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
