package net.rai.scp_unleashed.scpsign;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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

public final class ScpSignRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> DocumentComponent = ComponentRegister.registerComponentType("scp_sign_document",
            Builder -> Builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static final DeferredBlock<ScpWideSignBlock> Sign = BlockRegister.register("scp_sign", () -> new ScpWideSignBlock(properties()));
    public static final DeferredBlock<ScpSmallSignBlock> SmallSign = BlockRegister.register("scp_sign_small", () -> new ScpSmallSignBlock(properties()));
    public static final DeferredItem<ScpSignItem> SignItem = ItemRegister.register("scp_sign", () -> new ScpSignItem(Sign.get(), new Item.Properties()));
    public static final DeferredItem<ScpSignItem> SmallSignItem = ItemRegister.register("scp_sign_small", () -> new ScpSignItem(SmallSign.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScpSignBlockEntity>> SignEntity = BlockEntityRegister.register("scp_sign",
            () -> BlockEntityType.Builder.of(ScpSignBlockEntity::new, Sign.get(), SmallSign.get()).build(null));

    private ScpSignRegistry() {
    }

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.5f, 6.0f).sound(SoundType.METAL).noCollission().noOcclusion()
                .pushReaction(PushReaction.DESTROY).lightLevel(State -> State.getValue(ScpSignBlock.Lit) ? ScpSignBlock.GlowLight : 0)
                .isValidSpawn((State, World, Pos, Kind) -> false).isRedstoneConductor((State, World, Pos) -> false)
                .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
    }
}
