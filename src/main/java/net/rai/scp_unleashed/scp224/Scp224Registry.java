package net.rai.scp_unleashed.scp224;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.List;

public final class Scp224Registry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceKey<DamageType> OldAge = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp224_old_age"));
    public static final ResourceLocation SlowId = location("scp224_aging");
    public static final float TickRange = 10.0f;

    public static final DeferredBlock<Scp224ClockBlock> Clock = BlockRegister.register("scp224_clock",
            () -> new Scp224ClockBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.5f, 1200.0f).sound(SoundType.WOOD)
                    .noOcclusion().pushReaction(PushReaction.BLOCK).isRedstoneConductor((State, World, Pos) -> false)
                    .isSuffocating((State, World, Pos) -> false).isViewBlocking((State, World, Pos) -> false)));
    public static final DeferredItem<BlockItem> ClockItem = ItemRegister.register("scp224_clock",
            () -> new BlockItem(Clock.get(), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp224ClockBlockEntity>> ClockEntity = BlockEntityTypeRegister.register("scp224_clock",
            () -> BlockEntityType.Builder.of(Scp224ClockBlockEntity::new, Clock.get()).build(null));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Scp224Age>> Age = AttachmentRegister.register("scp224_age",
            () -> AttachmentType.builder(() -> Scp224Age.Fresh).serialize(Scp224Age.AgeCodec, Saved -> Saved.Elapsed() > 0).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SentAge = AttachmentRegister.register("scp224_sent_age",
            () -> AttachmentType.builder(() -> -1).build());

    public static final DeferredHolder<SoundEvent, SoundEvent> Toll = wide("scp224_toll");
    public static final DeferredHolder<SoundEvent, SoundEvent> TollHum = wide("scp224_toll_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> TollChime = wide("scp224_toll_chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> Tick = near("scp224_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> TickWood = near("scp224_tick_wood");
    public static final DeferredHolder<SoundEvent, SoundEvent> Halt = sound("scp224_halt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HaltWood = sound("scp224_halt_wood");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wheeze = sound("scp224_wheeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> WheezeBreath = sound("scp224_wheeze_breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cough = sound("scp224_cough");
    public static final DeferredHolder<SoundEvent, SoundEvent> CoughHuff = sound("scp224_cough_huff");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TollLayers = List.of(Toll, TollHum, TollChime);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> TickLayers = List.of(Tick, TickWood);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HaltLayers = List.of(Halt, HaltWood);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WheezeLayers = List.of(Wheeze, WheezeBreath);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> CoughLayers = List.of(Cough, CoughHuff);

    private Scp224Registry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> wide(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(location(Name), Scp224Toll.HearingRange));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> near(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createFixedRangeEvent(location(Name), TickRange));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        Scp224Stranger.register();
    }
}
