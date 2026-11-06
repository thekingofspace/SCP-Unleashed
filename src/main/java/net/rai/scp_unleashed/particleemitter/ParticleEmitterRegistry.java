package net.rai.scp_unleashed.particleemitter;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
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

public final class ParticleEmitterRegistry {
    public static final String EmitterId = "particle_emitter";

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<ParticleEmitterBlock> Emitter = BlockRegister.register(EmitterId, () -> new ParticleEmitterBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(2.5f, 6.0f).sound(SoundType.METAL).noOcclusion()
            .isRedstoneConductor((State, World, Pos) -> false).isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> EmitterItem = ItemRegister.registerSimpleBlockItem(EmitterId, Emitter);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ParticleEmitterBlockEntity>> EmitterEntity = BlockEntityTypeRegister.register(EmitterId,
            () -> BlockEntityType.Builder.of(ParticleEmitterBlockEntity::new, Emitter.get()).build(null));

    private ParticleEmitterRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
    }
}
