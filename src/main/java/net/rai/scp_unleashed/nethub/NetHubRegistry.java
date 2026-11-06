package net.rai.scp_unleashed.nethub;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

public final class NetHubRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<NetworkHubBlock> NetworkHub = BlockRegister.register("network_hub", () -> new NetworkHubBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(NetworkHubBlock::lightOf).isRedstoneConductor((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> NetworkHubItem = ItemRegister.register("network_hub", () -> new BlockItem(NetworkHub.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkHubBlockEntity>> NetworkHubEntity = BlockEntityTypeRegister.register("network_hub",
            () -> BlockEntityType.Builder.of(NetworkHubBlockEntity::new, NetworkHub.get()).build(null));
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("nethub_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumDrone = sound("nethub_hum_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chirp = sound("nethub_chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> ChirpTick = sound("nethub_chirp_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Link = sound("nethub_link");
    public static final DeferredHolder<SoundEvent, SoundEvent> Unlink = sound("nethub_unlink");

    private NetHubRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
