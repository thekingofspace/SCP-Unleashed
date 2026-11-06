package net.rai.scp_unleashed.gateway;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class GatewayRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents DataComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> Destination = DataComponentRegister.registerComponentType("gateway_destination",
            Builder -> Builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static final DeferredBlock<ContainedGatewayBlock> Gateway = BlockRegister.register("contained_gateway", () -> new ContainedGatewayBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(6.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)
            .forceSolidOn().isValidSpawn((State, World, Pos, Kind) -> false)
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false).isRedstoneConductor((State, World, Pos) -> false)
            .lightLevel(ContainedGatewayBlock::lightOf)));
    public static final DeferredItem<BlockItem> GatewayItem = ItemRegister.registerSimpleBlockItem("contained_gateway", Gateway,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ContainedGatewayBlockEntity>> GatewayEntity = BlockEntityTypeRegister.register("contained_gateway",
            () -> BlockEntityType.Builder.of(ContainedGatewayBlockEntity::new, Gateway.get()).build(null));
    public static final DeferredItem<DimensionalShardItem> Shard = ItemRegister.register("dimensional_shard",
            () -> new DimensionalShardItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));

    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("gateway_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Insert = sound("gateway_insert");
    public static final DeferredHolder<SoundEvent, SoundEvent> Remove = sound("gateway_remove");
    public static final DeferredHolder<SoundEvent, SoundEvent> Open = sound("gateway_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> Close = sound("gateway_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> Travel = sound("gateway_travel");
    public static final DeferredHolder<SoundEvent, SoundEvent> Refuse = sound("gateway_refuse");

    private GatewayRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static ItemStack shardFor(ResourceLocation Id) {
        ItemStack Stack = new ItemStack(Shard.get());
        Stack.set(Destination.get(), Id);
        return Stack;
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        DataComponentRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
    }
}
