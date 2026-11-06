package net.rai.scp_unleashed.matrix;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.flag.FeatureFlags;
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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class MatrixRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<MenuType<?>> MenuRegister = DeferredRegister.create(Registries.MENU, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<InstabilityMatrixBlock> InstabilityMatrix = BlockRegister.register("instability_matrix", () -> new InstabilityMatrixBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(6.0f, 24.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)
            .lightLevel(InstabilityMatrixBlock::lightOf)));
    public static final DeferredItem<BlockItem> InstabilityMatrixItem = ItemRegister.registerSimpleBlockItem("instability_matrix", InstabilityMatrix,
            new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstabilityMatrixBlockEntity>> MatrixEntity = BlockEntityRegister.register("instability_matrix",
            () -> BlockEntityType.Builder.of(InstabilityMatrixBlockEntity::new, InstabilityMatrix.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<MatrixMenu>> MatrixMenuType = MenuRegister.register("instability_matrix",
            () -> new MenuType<>(MatrixMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("matrix_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HumBed = sound("matrix_hum_bed");
    public static final DeferredHolder<SoundEvent, SoundEvent> TierClick = sound("matrix_tier_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> TierChime = sound("matrix_tier_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Surge = sound("matrix_surge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SurgeCrack = sound("matrix_surge_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> RiftTear = sound("matrix_rift_tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> RiftTearBoom = sound("matrix_rift_tear_boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> Start = sound("matrix_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> Stop = sound("matrix_stop");

    private MatrixRegistry() {
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
