package net.rai.scp_unleashed.scp5001.mekhane;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
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
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001EmergencyConsoleBlock;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001EmergencyConsoleBlockEntity;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001EmergencyIntakeBlock;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001EmergencyIntakeBlockEntity;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001SkyEmitterBlock;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001SkyEmitterBlockEntity;
import net.rai.scp_unleashed.scp5001.mekhane.entity.Mekhane;

public final class Scp5001MekhaneRegistry {
    public static final DeferredRegister.Blocks BlockRegister = DeferredRegister.createBlocks(Scp_unleashed.ModId);
    public static final DeferredRegister.Items ItemRegister = DeferredRegister.createItems(Scp_unleashed.ModId);
    public static final DeferredRegister<BlockEntityType<?>> BlockEntityTypeRegister = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<EntityType<?>> EntityTypeRegister = DeferredRegister.create(Registries.ENTITY_TYPE, Scp_unleashed.ModId);
    public static final DeferredRegister<SoundEvent> SoundEventRegister = DeferredRegister.create(Registries.SOUND_EVENT, Scp_unleashed.ModId);
    public static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> Ticket = net.minecraft.server.level.TicketType.create("scp_unleashed_mekhane",
            java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 80);
    public static final TagKey<net.minecraft.world.item.Item> Electronics = TagKey.create(Registries.ITEM, location("mekhane_electronics"));

    public static final DeferredBlock<Scp5001EmergencyConsoleBlock> Console = BlockRegister.register("scp5001_emergency_console",
            () -> new Scp5001EmergencyConsoleBlock(machine().lightLevel(State -> State.getValue(Scp5001EmergencyConsoleBlock.Awake) ? 9 : 2)));
    public static final DeferredBlock<Scp5001EmergencyIntakeBlock> Intake = BlockRegister.register("scp5001_emergency_intake",
            () -> new Scp5001EmergencyIntakeBlock(machine().lightLevel(State -> State.getValue(Scp5001EmergencyIntakeBlock.Awake) ? 8 : 1)));
    public static final DeferredBlock<Scp5001SkyEmitterBlock> Emitter = BlockRegister.register("scp5001_sky_emitter",
            () -> new Scp5001SkyEmitterBlock(machine().lightLevel(State -> State.getValue(Scp5001SkyEmitterBlock.Lit) ? 15 : 4)));
    public static final DeferredItem<BlockItem> ConsoleItem = ItemRegister.registerSimpleBlockItem("scp5001_emergency_console", Console, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> IntakeItem = ItemRegister.registerSimpleBlockItem("scp5001_emergency_intake", Intake, new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> EmitterItem = ItemRegister.registerSimpleBlockItem("scp5001_sky_emitter", Emitter, new Item.Properties().rarity(Rarity.EPIC));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp5001EmergencyConsoleBlockEntity>> ConsoleEntity = BlockEntityTypeRegister.register(
            "scp5001_emergency_console", () -> BlockEntityType.Builder.of(Scp5001EmergencyConsoleBlockEntity::new, Console.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp5001EmergencyIntakeBlockEntity>> IntakeEntity = BlockEntityTypeRegister.register(
            "scp5001_emergency_intake", () -> BlockEntityType.Builder.of(Scp5001EmergencyIntakeBlockEntity::new, Intake.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Scp5001SkyEmitterBlockEntity>> EmitterEntity = BlockEntityTypeRegister.register(
            "scp5001_sky_emitter", () -> BlockEntityType.Builder.of(Scp5001SkyEmitterBlockEntity::new, Emitter.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Mekhane>> MekhaneType = EntityTypeRegister.register("mekhane",
            () -> EntityType.Builder.<Mekhane>of(Mekhane::new, MobCategory.MISC)
                    .sized(Mekhane.Width, Mekhane.Height)
                    .clientTrackingRange(Mekhane.TrackChunks)
                    .updateInterval(1)
                    .noSave()
                    .noSummon()
                    .fireImmune()
                    .build("mekhane"));

    public static final DeferredHolder<SoundEvent, SoundEvent> ConsoleBeep = sound("scp5001.mekhane.console_beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> ConsoleEngage = sound("scp5001.mekhane.console_engage");
    public static final DeferredHolder<SoundEvent, SoundEvent> ConsoleDeny = sound("scp5001.mekhane.console_deny");
    public static final DeferredHolder<SoundEvent, SoundEvent> IntakeFeed = sound("scp5001.mekhane.intake_feed");
    public static final DeferredHolder<SoundEvent, SoundEvent> IntakeFull = sound("scp5001.mekhane.intake_full");
    public static final DeferredHolder<SoundEvent, SoundEvent> Alarm = fight("scp5001.mekhane.alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> Rumble = fight("scp5001.mekhane.rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> BeamCharge = fight("scp5001.mekhane.beam_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> BeamFire = fight("scp5001.mekhane.beam_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> Descend = fight("scp5001.mekhane.descend");
    public static final DeferredHolder<SoundEvent, SoundEvent> Bell = fight("scp5001.mekhane.bell");
    public static final DeferredHolder<SoundEvent, SoundEvent> Gears = fight("scp5001.mekhane.gears");
    public static final DeferredHolder<SoundEvent, SoundEvent> WingFlare = fight("scp5001.mekhane.wing_flare");
    public static final DeferredHolder<SoundEvent, SoundEvent> AxeSwing = fight("scp5001.mekhane.axe_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> AxeImpact = fight("scp5001.mekhane.axe_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> Clang = fight("scp5001.mekhane.clang");
    public static final DeferredHolder<SoundEvent, SoundEvent> PlateBreak = fight("scp5001.mekhane.plate_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> Strain = fight("scp5001.mekhane.strain");
    public static final DeferredHolder<SoundEvent, SoundEvent> Hurl = fight("scp5001.mekhane.hurl");
    public static final DeferredHolder<SoundEvent, SoundEvent> LaserCharge = fight("scp5001.mekhane.laser_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> Laser = fight("scp5001.mekhane.laser");
    public static final DeferredHolder<SoundEvent, SoundEvent> PortalOpen = fight("scp5001.mekhane.portal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> PortalSeal = fight("scp5001.mekhane.portal_seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> Ascend = fight("scp5001.mekhane.ascend");
    public static final DeferredHolder<SoundEvent, SoundEvent> Thunder = fight("scp5001.mekhane.thunder");
    public static final DeferredHolder<SoundEvent, SoundEvent> AlarmDrone = fight("scp5001.mekhane.alarm_drone");
    public static final DeferredHolder<SoundEvent, SoundEvent> BeamHum = fight("scp5001.mekhane.beam_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> WingBeat = fight("scp5001.mekhane.wing_beat");
    public static final DeferredHolder<SoundEvent, SoundEvent> Dash = fight("scp5001.mekhane.dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> ImpactBody = fight("scp5001.mekhane.impact_body");
    public static final DeferredHolder<SoundEvent, SoundEvent> SigilHum = fight("scp5001.mekhane.sigil_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> PortalPull = fight("scp5001.mekhane.portal_pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> Slam = fight("scp5001.mekhane.slam");

    private Scp5001MekhaneRegistry() {
    }

    public static ResourceLocation location(String Path) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path);
    }

    private static BlockBehaviour.Properties machine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(-1.0f, 3600000.0f).sound(SoundType.NETHERITE_BLOCK).noOcclusion()
                .pushReaction(PushReaction.BLOCK).isValidSpawn((State, World, Pos, Kind) -> false);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String Name) {
        return SoundEventRegister.register(Name, () -> SoundEvent.createVariableRangeEvent(location(Name)));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> fight(String Name) {
        return SoundEventRegister.register(Name, () -> net.rai.scp_unleashed.scp5001.core.FightAudio.event(location(Name)));
    }

    public static void register(IEventBus ModEventBus) {
        BlockRegister.register(ModEventBus);
        ItemRegister.register(ModEventBus);
        BlockEntityTypeRegister.register(ModEventBus);
        EntityTypeRegister.register(ModEventBus);
        SoundEventRegister.register(ModEventBus);
        ModEventBus.addListener(ModConfigEvent.Loading.class, Event -> MekhaneValues.invalidate());
        ModEventBus.addListener(ModConfigEvent.Reloading.class, Event -> MekhaneValues.invalidate());
    }
}
