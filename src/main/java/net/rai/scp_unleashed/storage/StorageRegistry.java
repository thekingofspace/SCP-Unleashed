package net.rai.scp_unleashed.storage;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
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

public final class StorageRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<StorageContainerBlock> SmallContainer = BlockRegister.register("storage_container", () -> new StorageContainerBlock(properties()));
    public static final DeferredBlock<TallStorageContainerBlock> TallContainer = BlockRegister.register("storage_container_tall", () -> new TallStorageContainerBlock(properties()));
    public static final DeferredItem<BlockItem> SmallContainerItem = ItemRegister.registerSimpleBlockItem("storage_container", SmallContainer);
    public static final DeferredItem<BlockItem> TallContainerItem = ItemRegister.registerSimpleBlockItem("storage_container_tall", TallContainer);
    public static final DeferredItem<StorageUpgradeItem> HeatedUpgrade = ItemRegister.register("heated_storage_upgrade",
            () -> new StorageUpgradeItem(StorageEnvironment.Heated, new Item.Properties().stacksTo(16)));
    public static final DeferredItem<StorageUpgradeItem> ColdUpgrade = ItemRegister.register("cold_storage_upgrade",
            () -> new StorageUpgradeItem(StorageEnvironment.Cold, new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageContainerBlockEntity>> ContainerEntity = BlockEntityRegister.register("storage_container",
            () -> BlockEntityType.Builder.of(StorageContainerBlockEntity::new, SmallContainer.get(), TallContainer.get()).build(null));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Ember = ParticleRegister.register("storage_ember", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Haze = ParticleRegister.register("storage_heat_haze", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Mist = ParticleRegister.register("storage_frost_mist", () -> new SimpleParticleType(false));

    private StorageRegistry() {
    }

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 9.0f).requiresCorrectToolForDrops().sound(SoundType.NETHERITE_BLOCK)
                .noOcclusion().pushReaction(PushReaction.BLOCK);
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
        StorageSounds.SoundEventRegister.register(ModEventBus);
    }
}
