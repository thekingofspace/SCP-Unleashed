package net.rai.scp_unleashed.scp109;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp109Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_109");

    public static final DeferredItem<Scp109CanteenItem> Canteen = ItemRegister.register("scp109_canteen",
            () -> new Scp109CanteenItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredBlock<Scp109TapBlock> Tap = BlockRegister.register("scp109_canteen_tap",
            () -> new Scp109TapBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f, 6.0f)
                    .sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK)
                    .lightLevel(State -> State.getValue(Scp109TapBlock.Canteen) ? Scp109TapBlock.SeatedLight : 0)));
    public static final DeferredItem<BlockItem> TapItem = ItemRegister.registerSimpleBlockItem("scp109_canteen_tap", Tap);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp109TapBlockEntity>> TapEntity = BlockEntityTypeRegister.register("scp109_canteen_tap",
            () -> BlockEntityType.Builder.of(Scp109TapBlockEntity::new, Tap.get()).build(null));

    public static final DeferredHolder<SoundEvent, SoundEvent> Drink = sound("scp109_drink");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slosh = sound("scp109_slosh");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cap = sound("scp109_cap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CapTwist = sound("scp109_cap_twist");
    public static final DeferredHolder<SoundEvent, SoundEvent> Pour = sound("scp109_pour");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bottle = sound("scp109_bottle");
    public static final DeferredHolder<SoundEvent, SoundEvent> Douse = sound("scp109_douse");
    public static final DeferredHolder<SoundEvent, SoundEvent> DouseSplash = sound("scp109_douse_splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> TapSeat = sound("scp109_tap_seat");
    public static final DeferredHolder<SoundEvent, SoundEvent> TapChain = sound("scp109_tap_chain");
    public static final DeferredHolder<SoundEvent, SoundEvent> TapRelease = sound("scp109_tap_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> TapFlow = sound("scp109_tap_flow");
    public static final DeferredHolder<SoundEvent, SoundEvent> TapFlowBed = sound("scp109_tap_flow_bed");
    public static final DeferredHolder<SoundEvent, SoundEvent> TapDrip = sound("scp109_tap_drip");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CapLayers = List.of(Cap, CapTwist);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> PourLayers = List.of(Pour, Slosh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BottleLayers = List.of(Bottle, Slosh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DouseLayers = List.of(Douse, DouseSplash);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SeatLayers = List.of(TapSeat, TapChain, Slosh);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> ReleaseLayers = List.of(TapRelease, TapChain);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> FlowLayers = List.of(TapFlow, TapFlowBed);

    private Scp109Registry() {
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
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
