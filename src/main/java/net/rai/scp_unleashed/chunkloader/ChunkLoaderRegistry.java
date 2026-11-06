package net.rai.scp_unleashed.chunkloader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class ChunkLoaderRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<ChunkLoaderBlock> ChunkLoader = BlockRegister.register("chunk_loader", () -> new ChunkLoaderBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0f, 1200.0f).sound(SoundType.NETHERITE_BLOCK).pushReaction(PushReaction.BLOCK).noOcclusion()
            .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)
            .lightLevel(State -> State.getValue(ChunkLoaderBlock.Active) ? ChunkLoaderBlock.ActiveLight : 0)));
    public static final DeferredItem<BlockItem> ChunkLoaderItem = ItemRegister.register("chunk_loader", () -> new BlockItem(ChunkLoader.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChunkLoaderBlockEntity>> ChunkLoaderEntity = BlockEntityRegister.register("chunk_loader",
            () -> BlockEntityType.Builder.of(ChunkLoaderBlockEntity::new, ChunkLoader.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<ChunkLoaderMenu>> ChunkLoaderMenuType = MenuRegister.register("chunk_loader",
            () -> IMenuTypeExtension.create(ChunkLoaderMenu::new));

    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("chunk_loader_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pulse = sound("chunk_loader_pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> Start = sound("chunk_loader_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stop = sound("chunk_loader_stop");

    private ChunkLoaderRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        MenuRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
