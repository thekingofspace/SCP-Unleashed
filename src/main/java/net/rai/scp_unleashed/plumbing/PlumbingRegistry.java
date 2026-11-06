package net.rai.scp_unleashed.plumbing;

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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;

public final class PlumbingRegistry {
    public static final ResourceKey<DamageType> WasteWaterDamage = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "waste_water"));

    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final DeferredBlock<DrainBlock> Drain = BlockRegister.register("drain", () -> new DrainBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.0f, 6.0f).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> DrainItem = ItemRegister.registerSimpleBlockItem("drain", Drain);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrainBlockEntity>> DrainEntity = BlockEntityRegister.register("drain",
            () -> BlockEntityType.Builder.of(DrainBlockEntity::new, Drain.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> DrainFlow = SoundRegister.register("drain_flow",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "drain_flow")));

    public static final DeferredHolder<SoundEvent, SoundEvent> DrainPatter = SoundRegister.register("drain_patter",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "drain_patter")));

    private PlumbingRegistry() {
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
    }
}
