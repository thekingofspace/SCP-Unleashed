package net.rai.scp_unleashed.intro;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.intro.block.IntroGateCoreBlock;
import net.rai.scp_unleashed.intro.block.IntroGateCoreBlockEntity;
import net.rai.scp_unleashed.intro.block.IntroGateFrameBlock;
import net.rai.scp_unleashed.intro.block.IntroPropBlock;

import java.util.UUID;

public final class IntroRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final DeferredRegister<AttachmentType<?>> AttachmentRegister = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Scp_unleashed.ModId);

    public static final ResourceKey<Level> Dimension = ResourceKey.create(Registries.DIMENSION, location("intro"));
    public static final ResourceLocation OfficeTemplate = location("intro_office");
    public static final ResourceLocation GateRoomTemplate = location("intro_gate_room");
    public static final TicketController Tickets = new TicketController(location("intro_plot"), (World, Helper) -> {
        for (UUID Owner : Helper.getEntityTickets().keySet()) {
            Helper.removeAllTickets(Owner);
        }
    });

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<IntroRecord>> Record = AttachmentRegister.register("intro_record",
            () -> AttachmentType.builder(() -> IntroRecord.Fresh).serialize(IntroRecord.RecordCodec, Saved -> !Saved.equals(IntroRecord.Fresh)).copyOnDeath().build());

    public static final DeferredBlock<IntroPropBlock> Nameplate = BlockRegister.register("intro_nameplate",
            () -> new IntroPropBlock(prop().sound(SoundType.WOOD), IntroPropBlock.NameplateShape));
    public static final DeferredBlock<IntroPropBlock> Folder = BlockRegister.register("intro_folder",
            () -> new IntroPropBlock(prop().sound(SoundType.WOOL), IntroPropBlock.FolderShape));
    public static final DeferredBlock<IntroPropBlock> Emblem = BlockRegister.register("intro_emblem",
            () -> new IntroPropBlock(prop().sound(SoundType.WOOD).noCollission(), IntroPropBlock.EmblemShape));
    public static final DeferredBlock<IntroPropBlock> Clock = BlockRegister.register("intro_clock",
            () -> new IntroPropBlock(prop().sound(SoundType.WOOD).noCollission(), IntroPropBlock.ClockShape));
    public static final DeferredBlock<IntroGateFrameBlock> GateFrame = BlockRegister.register("intro_gate_frame",
            () -> new IntroGateFrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0f, 3600000.0f).sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion().noLootTable().lightLevel(IntroGateFrameBlock::lightOf).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<IntroGateCoreBlock> GateCore = BlockRegister.register("intro_gate_core",
            () -> new IntroGateCoreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0f, 3600000.0f).sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion().noLootTable().lightLevel(State -> 12).pushReaction(PushReaction.BLOCK)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IntroGateCoreBlockEntity>> GateCoreEntity = BlockEntityTypeRegister.register("intro_gate_core",
            () -> BlockEntityType.Builder.of(IntroGateCoreBlockEntity::new, GateCore.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<IntroScientist>> Scientist = EntityTypeRegister.register("intro_scientist",
            () -> EntityType.Builder.<IntroScientist>of(IntroScientist::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f)
                    .vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT).clientTrackingRange(10).noSave().noSummon().build("intro_scientist"));

    public static final DeferredHolder<EntityType<?>, EntityType<IntroStaff>> Staff = EntityTypeRegister.register("intro_staff",
            () -> EntityType.Builder.<IntroStaff>of(IntroStaff::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f)
                    .vehicleAttachment(Player.DEFAULT_VEHICLE_ATTACHMENT).clientTrackingRange(10).updateInterval(2).noSave().noSummon().build("intro_staff"));

    public static final DeferredHolder<SoundEvent, SoundEvent> OfficeHum = sound("intro_office_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> ClockTick = sound("intro_clock_tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> DistantDoor = sound("intro_distant_door");
    public static final DeferredHolder<SoundEvent, SoundEvent> Paper = sound("intro_paper");
    public static final DeferredHolder<SoundEvent, SoundEvent> FolderTap = sound("intro_folder_tap");
    public static final DeferredHolder<SoundEvent, SoundEvent> Chair = sound("intro_chair");
    public static final DeferredHolder<SoundEvent, SoundEvent> Voice = sound("intro_voice");
    public static final DeferredHolder<SoundEvent, SoundEvent> Intercom = sound("intro_intercom");
    public static final DeferredHolder<SoundEvent, SoundEvent> IntercomClick = sound("intro_intercom_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> MachineDrone = sound("intro_machine_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> MachineClank = sound("intro_machine_clank");
    public static final DeferredHolder<SoundEvent, SoundEvent> MachineHiss = sound("intro_machine_hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> Spark = sound("intro_spark");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateHum = sound("intro_gate_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> GateRise = sound("intro_gate_rise");
    public static final DeferredHolder<SoundEvent, SoundEvent> Crossing = sound("intro_crossing");
    public static final DeferredHolder<SoundEvent, SoundEvent> CrossingLayer = sound("intro_crossing_layer");
    public static final DeferredHolder<SoundEvent, SoundEvent> Arrival = sound("intro_arrival");
    public static final DeferredHolder<SoundEvent, SoundEvent> SkipFill = sound("intro_skip");

    private IntroRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static BlockBehaviour.Properties prop() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000.0f).noOcclusion().noLootTable().pushReaction(PushReaction.DESTROY);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        AttachmentRegister.register(ModEventBus);
        ModEventBus.addListener(FMLCommonSetupEvent.class, Event -> Event.enqueueWork(IntroSettings::registerRules));
        ModEventBus.addListener(RegisterTicketControllersEvent.class, Event -> Event.register(Tickets));
    }
}
