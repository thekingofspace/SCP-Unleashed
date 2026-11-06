package net.rai.scp_unleashed.storage.gel;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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

import java.util.List;

public final class GelStorageRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final String Id = "storage_container_gel";

    public static final DeferredHolder<SoundEvent, SoundEvent> GelForm = sound("storage_gel_form");
    public static final DeferredHolder<SoundEvent, SoundEvent> GelFormSlosh = sound("storage_gel_form_slosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> GelBreak = sound("storage_gel_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> GelBreakDrip = sound("storage_gel_break_drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> GelAlarm = sound("storage_gel_alarm");
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FormLayers = List.of(GelForm, GelFormSlosh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BreakLayers = List.of(GelBreak, GelBreakDrip);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GelPacked>> Packed = ComponentRegister.registerComponentType("storage_gel_packed",
            Builder -> Builder.persistent(GelPacked.PackedCodec).networkSynchronized(GelPacked.PackedStreamCodec).cacheEncoding());

    public static final DeferredBlock<GelStorageContainerBlock> GelContainer = BlockRegister.register(Id,
            () -> new GelStorageContainerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 9.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredItem<GelStorageContainerItem> GelContainerItem = ItemRegister.register(Id,
            () -> new GelStorageContainerItem(GelContainer.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GelStorageContainerBlockEntity>> GelContainerEntity = BlockEntityRegister.register(Id,
            () -> BlockEntityType.Builder.of(GelStorageContainerBlockEntity::new, GelContainer.get()).build(null));

    private GelStorageRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
    }
}
