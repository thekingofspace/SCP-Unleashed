package net.rai.scp_unleashed.scp1730.rooms;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
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
import net.rai.scp_unleashed.scp1730.block.Scp1730PropShapes;

public final class Scp1730RoomsRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<ParticleType<?>> ParticleRegister = DeferredRegister.create(Registries.PARTICLE_TYPE, Scp_unleashed.ModId);

    public static final DeferredBlock<Scp1730TelevisionBlock> Television = BlockRegister.register("scp1730_television",
            () -> new Scp1730TelevisionBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(2.0f, 6.0f).noOcclusion().sound(SoundType.WOOD)
                    .pushReaction(PushReaction.BLOCK).lightLevel(State -> State.getValue(Scp1730TelevisionBlock.ScreenState).glow())));
    public static final DeferredItem<BlockItem> TelevisionItem = ItemRegister.registerSimpleBlockItem("scp1730_television", Television, new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp1730TelevisionBlockEntity>> TelevisionEntity = BlockEntityTypeRegister.register("scp1730_television",
            () -> BlockEntityType.Builder.of(Scp1730TelevisionBlockEntity::new, Television.get()).build(null));

    public static final DeferredBlock<Scp1730FoamBlock> AcousticFoam = BlockRegister.register("scp1730_acoustic_foam",
            () -> new Scp1730FoamBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.0f, 3.0f).sound(SoundType.WOOL).noOcclusion()));
    public static final DeferredItem<BlockItem> AcousticFoamItem = ItemRegister.registerSimpleBlockItem("scp1730_acoustic_foam", AcousticFoam, new Item.Properties());
    public static final DeferredBlock<Scp1730PropBlock> MicStand = BlockRegister.register("scp1730_mic_stand",
            () -> new Scp1730PropBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0f, 3.0f).sound(SoundType.METAL).noOcclusion().dynamicShape().pushReaction(PushReaction.DESTROY),
                    Block.box(5, 0, 5, 11, 16, 11), Block.box(5, 0, 5, 11, 16, 11), Scp1730PropShapes.resting("scp1730_mic_stand")));
    public static final DeferredItem<BlockItem> MicStandItem = ItemRegister.registerSimpleBlockItem("scp1730_mic_stand", MicStand, new Item.Properties());
    public static final DeferredBlock<Scp1730PropBlock> SpeakerCabinet = BlockRegister.register("scp1730_speaker_cabinet",
            () -> new Scp1730PropBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f, 4.0f).sound(SoundType.WOOD).noOcclusion().dynamicShape(),
                    Block.box(3, 0, 3, 13, 16, 13), Block.box(3, 0, 3, 13, 16, 13), Scp1730PropShapes.resting("scp1730_speaker_cabinet")));
    public static final DeferredItem<BlockItem> SpeakerCabinetItem = ItemRegister.registerSimpleBlockItem("scp1730_speaker_cabinet", SpeakerCabinet, new Item.Properties());
    public static final DeferredBlock<Scp1730PropBlock> StudioMonitor = BlockRegister.register("scp1730_studio_monitor",
            () -> new Scp1730PropBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f, 4.0f).sound(SoundType.WOOD).noOcclusion().dynamicShape(),
                    Block.box(4, 0, 4, 12, 16, 12), Block.box(4, 0, 4, 12, 16, 12), Scp1730PropShapes.resting("scp1730_studio_monitor")));
    public static final DeferredItem<BlockItem> StudioMonitorItem = ItemRegister.registerSimpleBlockItem("scp1730_studio_monitor", StudioMonitor, new Item.Properties());

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MistParticle = ParticleRegister.register("scp1730_mist", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DustParticle = ParticleRegister.register("scp1730_still_dust", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ConfettiParticle = ParticleRegister.register("scp1730_confetti", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PaperParticle = ParticleRegister.register("scp1730_drift_paper", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> StreamerParticle = ParticleRegister.register("scp1730_streamer", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LedParticle = ParticleRegister.register("scp1730_rack_led", () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> RoomSeal = sound("scp1730_room_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> TelevisionClick = sound("scp1730_tv_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> TelevisionStatic = sound("scp1730_tv_static");
    public static final DeferredHolder<SoundEvent, SoundEvent> LoopShift = sound("scp1730_loop_shift");

    public static final ResourceKey<DamageType> BroadcastDamage = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp1730_broadcast"));

    private Scp1730RoomsRegistry() {
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
        ParticleRegister.register(ModEventBus);
    }
}
