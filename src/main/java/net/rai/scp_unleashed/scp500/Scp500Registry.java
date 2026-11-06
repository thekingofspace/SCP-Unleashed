package net.rai.scp_unleashed.scp500;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.stranger.StrangerEncounters;

import java.util.List;

public final class Scp500Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister.DataComponents ComponentRegister = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Scp_unleashed.ModId);

    public static final ResourceLocation DocumentId = location("scp_500");
    public static final float Hardness = 0.6f;
    public static final float Resistance = 1200.0f;
    public static final float BottleVolume = 0.7f;
    public static final float BottlePitch = 1.65f;
    public static final int PillStack = 64;

    public static final DeferredHolder<SoundEvent, SoundEvent> Cap = sound("scp500_cap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CapClick = sound("scp500_cap_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rattle = sound("scp500_rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RattleTick = sound("scp500_rattle_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> Empty = sound("scp500_empty");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swallow = sound("scp500_swallow");
    public static final DeferredHolder<SoundEvent, SoundEvent> SwallowGulp = sound("scp500_swallow_gulp");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TakeLayers = List.of(Cap, CapClick, Rattle, RattleTick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> EmptyLayers = List.of(Empty, CapClick);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> SwallowLayers = List.of(Swallow, SwallowGulp);

    public static final SoundType BottleSound = new SoundType(BottleVolume, BottlePitch, SoundEvents.BONE_BLOCK_BREAK, SoundEvents.BONE_BLOCK_STEP,
            SoundEvents.BONE_BLOCK_PLACE, SoundEvents.BONE_BLOCK_HIT, SoundEvents.BONE_BLOCK_FALL);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Scp500Stock>> Stock = ComponentRegister.registerComponentType("scp500_stock",
            Builder -> Builder.persistent(Scp500Stock.StockCodec).networkSynchronized(Scp500Stock.StockStreamCodec));

    public static final DeferredBlock<Scp500BottleBlock> Bottle = BlockRegister.register("scp500_bottle",
            () -> new Scp500BottleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(Hardness, Resistance).sound(BottleSound)
                    .noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false)
                    .isRedstoneConductor((State, Getter, Pos) -> false).isSuffocating((State, Getter, Pos) -> false).isViewBlocking((State, Getter, Pos) -> false)));
    public static final DeferredItem<Scp500BottleItem> BottleItem = ItemRegister.register("scp500_bottle",
            () -> new Scp500BottleItem(Bottle.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp500BottleBlockEntity>> BottleEntity = BlockEntityTypeRegister.register("scp500_bottle",
            () -> BlockEntityType.Builder.of(Scp500BottleBlockEntity::new, Bottle.get()).build(null));

    public static final DeferredItem<Scp500PillItem> Pill = ItemRegister.register("scp500_pill",
            () -> new Scp500PillItem(new Item.Properties().stacksTo(PillStack).rarity(Rarity.RARE).fireResistant()));

    private Scp500Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void playLayers(Level World, Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, SoundSource Source, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), Source, Volume, Pitch);
        }
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ComponentRegister.register(ModEventBus);
        AnomalySpawners.register(DocumentId, StrangerEncounters::confirm);
    }
}
