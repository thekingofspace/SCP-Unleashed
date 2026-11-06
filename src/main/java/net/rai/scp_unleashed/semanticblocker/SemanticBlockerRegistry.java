package net.rai.scp_unleashed.semanticblocker;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class SemanticBlockerRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final String BlockerId = "scp148_semantic_blocker";
    public static final float BlockHardness = 5.0f;
    public static final float BlockBlastResistance = 1200.0f;
    public static final int PoweredLight = 8;

    public static final DeferredBlock<SemanticBlockerBlock> Blocker = BlockRegister.register(BlockerId,
            () -> new SemanticBlockerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.EMERALD).instrument(NoteBlockInstrument.CHIME).requiresCorrectToolForDrops()
                    .strength(BlockHardness, BlockBlastResistance).sound(SoundType.METAL)
                    .lightLevel(State -> State.getValue(SemanticBlockerBlock.Powered) ? PoweredLight : 0)));
    public static final DeferredItem<BlockItem> BlockerItem = ItemRegister.register(BlockerId,
            () -> new BlockItem(Blocker.get(), new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SemanticBlockerBlockEntity>> BlockerEntity = BlockEntityTypeRegister.register(BlockerId,
            () -> BlockEntityType.Builder.of(SemanticBlockerBlockEntity::new, Blocker.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound(BlockerId + "_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumDrone = sound(BlockerId + "_hum_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumRing = sound(BlockerId + "_hum_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Start = sound(BlockerId + "_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> StartRing = sound(BlockerId + "_start_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stop = sound(BlockerId + "_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> StopRing = sound(BlockerId + "_stop_ring");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HumLayers = List.of(Hum, HumDrone, HumRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StartLayers = List.of(Start, StartRing);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> StopLayers = List.of(Stop, StopRing);

    private SemanticBlockerRegistry() {
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
