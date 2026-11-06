package net.rai.scp_unleashed.tesla;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
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

public final class TeslaRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<TeslaGateBlock> TeslaGate = BlockRegister.register("tesla_gate", () -> new TeslaGateBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0f, 12.0f).sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
            .forceSolidOn().isValidSpawn((State, World, Pos, Kind) -> false)
            .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false).isRedstoneConductor((State, World, Pos) -> false)
            .lightLevel(TeslaGateBlock::lightOf)));
    public static final DeferredItem<BlockItem> TeslaGateItem = ItemRegister.registerSimpleBlockItem("tesla_gate", TeslaGate);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeslaGateBlockEntity>> TeslaGateEntity = BlockEntityTypeRegister.register("tesla_gate",
            () -> BlockEntityType.Builder.of(TeslaGateBlockEntity::new, TeslaGate.get()).build(null));
    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("tesla_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Buzz = sound("tesla_buzz");
    public static final DeferredHolder<SoundEvent, SoundEvent> Charge = sound("tesla_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> Discharge = sound("tesla_discharge");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> Spark = ParticleRegister.register("tesla_spark", () -> new SimpleParticleType(false));
    public static final ResourceKey<DamageType> TeslaDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "tesla"));

    private TeslaRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ParticleRegister.register(ModEventBus);
    }
}
