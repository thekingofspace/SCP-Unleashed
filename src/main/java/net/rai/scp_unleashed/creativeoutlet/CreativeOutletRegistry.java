package net.rai.scp_unleashed.creativeoutlet;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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

public final class CreativeOutletRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);

    public static final String OutletId = "creative_outlet";
    public static final float BlockHardness = 3.0f;
    public static final float BlockBlastResistance = 3600000.0f;
    public static final int GlowLight = 7;

    public static final DeferredBlock<CreativeOutletBlock> Outlet = BlockRegister.register(OutletId,
            () -> new CreativeOutletBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(BlockHardness, BlockBlastResistance)
                    .sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.DESTROY).lightLevel(State -> GlowLight)));
    public static final DeferredItem<BlockItem> OutletItem = ItemRegister.register(OutletId, () -> new BlockItem(Outlet.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreativeOutletBlockEntity>> OutletEntity = BlockEntityTypeRegister.register(OutletId,
            () -> BlockEntityType.Builder.of(CreativeOutletBlockEntity::new, Outlet.get()).build(null));

    private CreativeOutletRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
    }
}
