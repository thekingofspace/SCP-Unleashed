package net.rai.scp_unleashed.solar;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
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

public final class SolarRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<SolarPanelBlock> SolarPanel = BlockRegister.register("solar_panel", () -> new SolarPanelBlock(properties()));
    public static final DeferredItem<BlockItem> SolarPanelItem = ItemRegister.registerSimpleBlockItem("solar_panel", SolarPanel);
    public static final DeferredBlock<SolarArrayBlock> SolarArray = BlockRegister.register("solar_array", () -> new SolarArrayBlock(properties().pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> SolarArrayItem = ItemRegister.registerSimpleBlockItem("solar_array", SolarArray);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarBlockEntity>> SolarPanelEntity = BlockEntityTypeRegister.register("solar_panel",
            () -> BlockEntityType.Builder.of((Pos, State) -> new SolarBlockEntity(SolarTier.Panel, Pos, State), SolarPanel.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarBlockEntity>> SolarArrayEntity = BlockEntityTypeRegister.register("solar_array",
            () -> BlockEntityType.Builder.of((Pos, State) -> new SolarBlockEntity(SolarTier.Array, Pos, State), SolarArray.get()).build(null));

    private SolarRegistry() {
    }

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()
                .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
    }
}
