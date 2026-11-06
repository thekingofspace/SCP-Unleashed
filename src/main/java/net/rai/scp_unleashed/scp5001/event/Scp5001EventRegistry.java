package net.rai.scp_unleashed.scp5001.event;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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
import net.rai.scp_unleashed.scp5001.event.block.Scp5001ChamberCoreBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001ConsoleBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001DeviceBlockEntity;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001GeneratorBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001GeneratorBlockEntity;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001GeneratorConsoleBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001LaserEmitterBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001RodEmitterBlock;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Mimic;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Vessel;

import java.util.List;

public final class Scp5001EventRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceKey<DamageType> Resonance = ResourceKey.create(Registries.DAMAGE_TYPE, location("scp5001_resonance"));
    public static final String VoiceId = "scp5001";

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> Exposure = AttachmentRegister.register("scp5001_exposure",
            () -> AttachmentType.builder(() -> 0).serialize(Codec.INT, Saved -> Saved > 0).build());

    public static final DeferredBlock<Scp5001ConsoleBlock> ConsoleMain = BlockRegister.register("scp5001_console_main",
            () -> new Scp5001ConsoleBlock(device(6)));
    public static final DeferredBlock<Scp5001GeneratorConsoleBlock> GeneratorConsole = BlockRegister.register("scp5001_generator_console",
            () -> new Scp5001GeneratorConsoleBlock(device(5)));
    public static final DeferredBlock<Scp5001GeneratorBlock> Generator = BlockRegister.register("scp5001_generator",
            () -> new Scp5001GeneratorBlock(device(0).lightLevel(State -> State.getValue(Scp5001GeneratorBlock.Overdrive) ? 15 : 10)));
    public static final DeferredBlock<Scp5001RodEmitterBlock> RodEmitter = BlockRegister.register("scp5001_rod_emitter",
            () -> new Scp5001RodEmitterBlock(device(0).lightLevel(State -> State.getValue(Scp5001RodEmitterBlock.Spent) ? 0 : 5)));
    public static final DeferredBlock<Scp5001LaserEmitterBlock> LaserEmitter = BlockRegister.register("scp5001_laser_emitter",
            () -> new Scp5001LaserEmitterBlock(device(0).lightLevel(State -> State.getValue(Scp5001LaserEmitterBlock.Firing) ? 15 : 6)));
    public static final DeferredBlock<Scp5001ChamberCoreBlock> ChamberCore = BlockRegister.register("scp5001_chamber_core",
            () -> new Scp5001ChamberCoreBlock(device(0).lightLevel(State -> State.getValue(Scp5001ChamberCoreBlock.Breached) ? 15 : 10)));

    public static final DeferredItem<BlockItem> ConsoleMainItem = item("scp5001_console_main", ConsoleMain);
    public static final DeferredItem<BlockItem> GeneratorConsoleItem = item("scp5001_generator_console", GeneratorConsole);
    public static final DeferredItem<BlockItem> GeneratorItem = item("scp5001_generator", Generator);
    public static final DeferredItem<BlockItem> RodEmitterItem = item("scp5001_rod_emitter", RodEmitter);
    public static final DeferredItem<BlockItem> LaserEmitterItem = item("scp5001_laser_emitter", LaserEmitter);
    public static final DeferredItem<BlockItem> ChamberCoreItem = item("scp5001_chamber_core", ChamberCore);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp5001DeviceBlockEntity>> RodEntity = BlockEntityRegister.register("scp5001_rod_emitter",
            () -> BlockEntityType.Builder.of((Pos, State) -> new Scp5001DeviceBlockEntity(Scp5001EventRegistry.RodEntity.get(), Pos, State), RodEmitter.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp5001DeviceBlockEntity>> LaserEntity = BlockEntityRegister.register("scp5001_laser_emitter",
            () -> BlockEntityType.Builder.of((Pos, State) -> new Scp5001DeviceBlockEntity(Scp5001EventRegistry.LaserEntity.get(), Pos, State), LaserEmitter.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp5001DeviceBlockEntity>> CoreEntity = BlockEntityRegister.register("scp5001_chamber_core",
            () -> BlockEntityType.Builder.of((Pos, State) -> new Scp5001DeviceBlockEntity(Scp5001EventRegistry.CoreEntity.get(), Pos, State), ChamberCore.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp5001GeneratorBlockEntity>> GeneratorEntity = BlockEntityRegister.register("scp5001_generator",
            () -> BlockEntityType.Builder.of(Scp5001GeneratorBlockEntity::new, Generator.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Scp5001Vessel>> VesselType = EntityRegister.register("scp5001_vessel",
            () -> EntityType.Builder.<Scp5001Vessel>of(Scp5001Vessel::new, MobCategory.MISC).sized(4.0f, 14.0f).fireImmune().noSave().noSummon()
                    .clientTrackingRange(16).updateInterval(1).build("scp5001_vessel"));
    public static final DeferredHolder<EntityType<?>, EntityType<Scp5001Mimic>> MimicType = EntityRegister.register("scp5001_mimic",
            () -> EntityType.Builder.<Scp5001Mimic>of(Scp5001Mimic::new, MobCategory.MONSTER).sized(0.6f, 1.8f).eyeHeight(1.62f)
                    .clientTrackingRange(10).build("scp5001_mimic"));

    public static final DeferredHolder<SoundEvent, SoundEvent> Hum = sound("hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> Wave = sound("wave");
    public static final DeferredHolder<SoundEvent, SoundEvent> WaveDeep = sound("wave_deep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Alarm = sound("alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlarmTone = sound("alarm_tone");
    public static final DeferredHolder<SoundEvent, SoundEvent> Cover = sound("cover");
    public static final DeferredHolder<SoundEvent, SoundEvent> Switch = sound("switch");
    public static final DeferredHolder<SoundEvent, SoundEvent> InterlockFail = sound("interlock_fail");
    public static final DeferredHolder<SoundEvent, SoundEvent> RodExtend = sound("rod_extend");
    public static final DeferredHolder<SoundEvent, SoundEvent> RodLatch = sound("rod_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> RodStrain = sound("rod_strain");
    public static final DeferredHolder<SoundEvent, SoundEvent> RodVaporize = sound("rod_vaporize");
    public static final DeferredHolder<SoundEvent, SoundEvent> LaserFire = sound("laser_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> GeneratorSpinup = sound("generator_spinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> GeneratorOverdrive = sound("generator_overdrive");
    public static final DeferredHolder<SoundEvent, SoundEvent> GeneratorHum = sound("generator_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> GeneratorWhine = sound("generator_whine");
    public static final DeferredHolder<SoundEvent, SoundEvent> GeneratorGrind = sound("generator_grind");
    public static final DeferredHolder<SoundEvent, SoundEvent> GeneratorDrone = sound("generator_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> GeneratorCrackle = sound("generator_crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> VesselBurst = sound("vessel_burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> VesselBurstRing = sound("vessel_burst_ring");
    public static final DeferredHolder<SoundEvent, SoundEvent> VesselSettle = sound("vessel_settle");
    public static final DeferredHolder<SoundEvent, SoundEvent> ConsoleBeep = sound("console_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> ConsoleDeny = sound("console_deny");
    public static final DeferredHolder<SoundEvent, SoundEvent> MimicAmbient = sound("mimic_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MimicHurt = sound("mimic_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MimicDeath = sound("mimic_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> Whisper = sound("whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> Footstep = sound("footstep");
    public static final DeferredHolder<SoundEvent, SoundEvent> Heartbeat = sound("heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Sting = sound("sting");
    public static final DeferredHolder<SoundEvent, SoundEvent> Swarm = sound("swarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> Unmade = sound("unmade");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> WaveLayers = List.of(Wave, WaveDeep);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> AlarmLayers = List.of(Alarm, AlarmTone);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BurstLayers = List.of(VesselBurst, VesselBurstRing);

    private Scp5001EventRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static BlockBehaviour.Properties device(int Light) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.NETHERITE_BLOCK)
                .noOcclusion().pushReaction(PushReaction.BLOCK).isValidSpawn((State, Getter, Pos, Type) -> false).lightLevel(State -> Light);
    }

    private static DeferredItem<BlockItem> item(String Name, DeferredBlock<?> Block) {
        return ItemRegister.register(Name, () -> new BlockItem(Block.get(), new Item.Properties().rarity(Rarity.EPIC)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        String Path = "scp5001.event." + Name;
        return SoundRegister.register(Path, () -> SoundEvent.createVariableRangeEvent(location(Path)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityRegister.register(ModEventBus);
        EntityRegister.register(ModEventBus);
        SoundRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
    }
}
