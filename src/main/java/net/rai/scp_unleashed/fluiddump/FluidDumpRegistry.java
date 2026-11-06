package net.rai.scp_unleashed.fluiddump;

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

public final class FluidDumpRegistry {
    public static final ResourceKey<DamageType> PollutedWaterDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "polluted_water"));

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<FluidDumpBlock> FluidDump = BlockRegister.register("fluid_dump", () -> new FluidDumpBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL).pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<BlockItem> FluidDumpItem = ItemRegister.registerSimpleBlockItem("fluid_dump", FluidDump);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidDumpBlockEntity>> FluidDumpEntity = BlockEntityRegister.register("fluid_dump",
            () -> BlockEntityType.Builder.of(FluidDumpBlockEntity::new, FluidDump.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> DumpBubble = sound("fluid_dump_bubble");
    public static final DeferredHolder<SoundEvent, SoundEvent> DumpGurgle = sound("fluid_dump_gurgle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DumpPour = sound("fluid_dump_pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> DumpHiss = sound("fluid_dump_hiss");

    private FluidDumpRegistry() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
