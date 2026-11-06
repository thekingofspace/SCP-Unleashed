package net.rai.scp_unleashed.powerbridge;

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

public final class PowerBridgeRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final PowerBridgeKind ReceiverKind = PowerBridgeKind.Inbound;
    public static final PowerBridgeKind EmitterKind = PowerBridgeKind.Outbound;
    public static final String ReceiverId = "power_receiver";
    public static final String EmitterId = "power_emitter";
    public static final String BridgeId = "power_bridge";
    public static final float BlockHardness = 3.5f;
    public static final float BlockBlastResistance = 6.0f;
    public static final int ActiveLight = 3;

    public static final DeferredBlock<PowerBridgeBlock> Receiver = BlockRegister.register(ReceiverId, () -> new PowerBridgeBlock(ReceiverKind, properties()));
    public static final DeferredItem<BlockItem> ReceiverItem = ItemRegister.register(ReceiverId, () -> new BlockItem(Receiver.get(), new Item.Properties()));
    public static final DeferredBlock<PowerBridgeBlock> Emitter = BlockRegister.register(EmitterId, () -> new PowerBridgeBlock(EmitterKind, properties()));
    public static final DeferredItem<BlockItem> EmitterItem = ItemRegister.register(EmitterId, () -> new BlockItem(Emitter.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerBridgeBlockEntity>> ReceiverEntity = BlockEntityTypeRegister.register(ReceiverId,
            () -> BlockEntityType.Builder.of((Pos, State) -> new PowerBridgeBlockEntity(ReceiverKind, Pos, State), Receiver.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerBridgeBlockEntity>> EmitterEntity = BlockEntityTypeRegister.register(EmitterId,
            () -> BlockEntityType.Builder.of((Pos, State) -> new PowerBridgeBlockEntity(EmitterKind, Pos, State), Emitter.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> InboundHum = sound(BridgeId + "_inbound_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> InboundHumShimmer = sound(BridgeId + "_inbound_hum_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> OutboundHum = sound(BridgeId + "_outbound_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> OutboundHumShimmer = sound(BridgeId + "_outbound_hum_shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Switch = sound(BridgeId + "_switch");

    private PowerBridgeRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static String idOf(PowerBridgeKind Kind) {
        return Kind == ReceiverKind ? ReceiverId : EmitterId;
    }

    public static BlockEntityType<PowerBridgeBlockEntity> entityType(PowerBridgeKind Kind) {
        return Kind == ReceiverKind ? ReceiverEntity.get() : EmitterEntity.get();
    }

    public static List<DeferredHolder<SoundEvent, SoundEvent>> humLayers(PowerBridgeKind Kind) {
        return Kind == PowerBridgeKind.Inbound ? List.of(InboundHum, InboundHumShimmer) : List.of(OutboundHum, OutboundHumShimmer);
    }

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).requiresCorrectToolForDrops().strength(BlockHardness, BlockBlastResistance)
                .sound(SoundType.METAL).lightLevel(State -> State.getValue(PowerBridgeBlock.Active) ? ActiveLight : 0);
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
