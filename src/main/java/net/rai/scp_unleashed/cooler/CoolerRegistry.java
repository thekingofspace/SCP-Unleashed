package net.rai.scp_unleashed.cooler;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class CoolerRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final String CoolerId = "industrial_cooler";
    public static final float BlockHardness = 3.5f;
    public static final float BlockBlastResistance = 6.0f;
    public static final int PoweredLight = 4;

    public static final DeferredBlock<CoolerBlock> Cooler = BlockRegister.register(CoolerId,
            () -> new CoolerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops()
                    .strength(BlockHardness, BlockBlastResistance).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(State -> State.getValue(CoolerBlock.Powered) ? PoweredLight : 0)));
    public static final DeferredItem<BlockItem> CoolerItem = ItemRegister.register(CoolerId, () -> new BlockItem(Cooler.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoolerBlockEntity>> CoolerEntity = BlockEntityTypeRegister.register(CoolerId,
            () -> BlockEntityType.Builder.of(CoolerBlockEntity::new, Cooler.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound(CoolerId + "_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumHiss = sound(CoolerId + "_hum_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Start = sound(CoolerId + "_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> StartHiss = sound(CoolerId + "_start_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stop = sound(CoolerId + "_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> StopHiss = sound(CoolerId + "_stop_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Freeze = sound(CoolerId + "_freeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> FreezeCrackle = sound(CoolerId + "_freeze_crackle");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StartLayers = List.of(Start, StartHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StopLayers = List.of(Stop, StopHiss);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FreezeLayers = List.of(Freeze, FreezeCrackle);

    private CoolerRegistry() {
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
